package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.service.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.global.exception.NotificationException;
import com.anyang.maruni.global.response.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * FallbackNotificationService 단위 테스트
 *
 * Primary 서비스 실패 시 Fallback 서비스로 자동 전환하는
 * Fallback 시스템의 정확한 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FallbackNotificationService 테스트")
class FallbackNotificationServiceTest {

    @Mock
    private NotificationService primaryService;

    @Mock
    private NotificationService fallbackService;

    private FallbackNotificationService fallbackNotificationService;

    @BeforeEach
    void setUp() {
        fallbackNotificationService = new FallbackNotificationService(primaryService, fallbackService);
    }

    @Test
    @DisplayName("Primary 서비스 성공 시 Fallback 서비스 사용하지 않음")
    void shouldUsePrimaryServiceWhenAvailableAndSuccessful() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(primaryService.isAvailable()).willReturn(true);
        given(primaryService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = fallbackNotificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(primaryService).isAvailable();
        verify(primaryService).sendPushNotification(memberId, title, message);
        verifyNoInteractions(fallbackService); // Fallback 서비스는 호출되지 않아야 함
    }

    @Test
    @DisplayName("Primary 서비스 실패 시 Fallback 서비스로 자동 전환")
    void shouldUseFallbackServiceWhenPrimaryServiceFails() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(primaryService.isAvailable()).willReturn(true);
        given(primaryService.sendPushNotification(memberId, title, message)).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(true);
        given(fallbackService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = fallbackNotificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(primaryService).sendPushNotification(memberId, title, message);
        verify(fallbackService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("Primary 서비스 예외 발생 시 Fallback 서비스로 자동 전환")
    void shouldUseFallbackServiceWhenPrimaryServiceThrowsException() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(primaryService.isAvailable()).willReturn(true);
        given(primaryService.sendPushNotification(memberId, title, message))
                .willThrow(new RuntimeException("Firebase connection failed"));
        given(fallbackService.isAvailable()).willReturn(true);
        given(fallbackService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = fallbackNotificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(primaryService).sendPushNotification(memberId, title, message);
        verify(fallbackService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("Primary 서비스 사용 불가능 시 바로 Fallback 서비스 사용")
    void shouldUseFallbackServiceWhenPrimaryServiceUnavailable() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(primaryService.isAvailable()).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(true);
        given(fallbackService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = fallbackNotificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(primaryService).isAvailable();
        verify(primaryService, never()).sendPushNotification(any(), any(), any()); // Primary는 호출되지 않음
        verify(fallbackService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("모든 서비스 실패 시 NotificationException 발생")
    void shouldThrowNotificationExceptionWhenAllServicesFail() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(primaryService.isAvailable()).willReturn(true);
        given(primaryService.sendPushNotification(memberId, title, message)).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(true);
        given(fallbackService.sendPushNotification(memberId, title, message)).willReturn(false);

        // When & Then
        assertThatThrownBy(() ->
            fallbackNotificationService.sendPushNotification(memberId, title, message))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FALLBACK_FAILED);

        verify(primaryService).sendPushNotification(memberId, title, message);
        verify(fallbackService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("모든 서비스 사용 불가능 시 NotificationException 발생")
    void shouldThrowNotificationExceptionWhenAllServicesUnavailable() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(primaryService.isAvailable()).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(false);

        // When & Then
        assertThatThrownBy(() ->
            fallbackNotificationService.sendPushNotification(memberId, title, message))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FALLBACK_FAILED);

        verify(primaryService).isAvailable();
        verify(fallbackService).isAvailable();
        verify(primaryService, never()).sendPushNotification(any(), any(), any());
        verify(fallbackService, never()).sendPushNotification(any(), any(), any());
    }

    @Test
    @DisplayName("가용성 확인 - Primary 또는 Fallback 중 하나라도 사용 가능하면 true")
    void shouldReturnTrueWhenEitherServiceIsAvailable() {
        // Given - Primary 가능, Fallback 불가능
        given(primaryService.isAvailable()).willReturn(true);
        given(fallbackService.isAvailable()).willReturn(false);

        // When
        boolean result = fallbackNotificationService.isAvailable();

        // Then
        assertThat(result).isTrue();

        // Given - Primary 불가능, Fallback 가능
        given(primaryService.isAvailable()).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(true);

        // When
        result = fallbackNotificationService.isAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("가용성 확인 - 모든 서비스 불가능 시 false")
    void shouldReturnFalseWhenAllServicesUnavailable() {
        // Given
        given(primaryService.isAvailable()).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(false);

        // When
        boolean result = fallbackNotificationService.isAvailable();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("채널 타입은 Primary 서비스의 채널 타입 반환")
    void shouldReturnPrimaryServiceChannelType() {
        // Given
        NotificationChannelType expectedType = NotificationChannelType.PUSH;
        given(primaryService.getChannelType()).willReturn(expectedType);

        // When
        NotificationChannelType result = fallbackNotificationService.getChannelType();

        // Then
        assertThat(result).isEqualTo(expectedType);
        verify(primaryService).getChannelType();
    }

    @Test
    @DisplayName("Primary 서비스 상태 확인 메서드")
    void shouldCheckPrimaryServiceAvailability() {
        // Given
        given(primaryService.isAvailable()).willReturn(true);

        // When
        boolean result = fallbackNotificationService.isPrimaryServiceAvailable();

        // Then
        assertThat(result).isTrue();
        verify(primaryService).isAvailable();
    }

    @Test
    @DisplayName("Fallback 서비스 상태 확인 메서드")
    void shouldCheckFallbackServiceAvailability() {
        // Given
        given(fallbackService.isAvailable()).willReturn(true);

        // When
        boolean result = fallbackNotificationService.isFallbackServiceAvailable();

        // Then
        assertThat(result).isTrue();
        verify(fallbackService).isAvailable();
    }

    @Test
    @DisplayName("활성 서비스 정보 반환 - Primary 사용 가능")
    void shouldReturnActiveServiceInfoWhenPrimaryAvailable() {
        // Given
        given(primaryService.isAvailable()).willReturn(true);

        // When
        String result = fallbackNotificationService.getActiveServiceInfo();

        // Then
        assertThat(result).contains("Primary:");
    }

    @Test
    @DisplayName("활성 서비스 정보 반환 - Fallback만 사용 가능")
    void shouldReturnActiveServiceInfoWhenOnlyFallbackAvailable() {
        // Given
        given(primaryService.isAvailable()).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(true);

        // When
        String result = fallbackNotificationService.getActiveServiceInfo();

        // Then
        assertThat(result).contains("Fallback:");
    }

    @Test
    @DisplayName("활성 서비스 정보 반환 - 모든 서비스 불가능")
    void shouldReturnNoServicesAvailableMessage() {
        // Given
        given(primaryService.isAvailable()).willReturn(false);
        given(fallbackService.isAvailable()).willReturn(false);

        // When
        String result = fallbackNotificationService.getActiveServiceInfo();

        // Then
        assertThat(result).isEqualTo("No services available");
    }
}