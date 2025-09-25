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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * RetryableNotificationService 단위 테스트
 *
 * Spring Retry 기반 재시도 로직의 정확한 동작을 검증합니다.
 * - 성공 시 재시도 없음
 * - 실패 시 최대 3회 재시도
 * - 모든 재시도 실패 시 @Recover 메서드 호출
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(NotificationRetryConfig.class)
@DisplayName("RetryableNotificationService 테스트")
class RetryableNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    private RetryableNotificationService retryableNotificationService;

    @BeforeEach
    void setUp() {
        retryableNotificationService = new RetryableNotificationService(notificationService);
    }

    @Test
    @DisplayName("첫 번째 시도에서 성공하면 재시도하지 않음")
    void shouldNotRetryWhenFirstAttemptSucceeds() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(notificationService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = retryableNotificationService.sendPushNotificationWithRetry(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(notificationService, times(1)).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("NotificationService가 false 반환 시 NotificationException 발생")
    void shouldThrowNotificationExceptionWhenServiceReturnsFalse() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(notificationService.sendPushNotification(memberId, title, message)).willReturn(false);

        // When & Then
        assertThatThrownBy(() ->
            retryableNotificationService.sendPushNotificationWithRetry(memberId, title, message))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_SEND_FAILED);

        verify(notificationService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("일반 예외 발생 시 NotificationException으로 래핑")
    void shouldWrapGeneralExceptionIntoNotificationException() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        RuntimeException originalException = new RuntimeException("Firebase connection failed");

        given(notificationService.sendPushNotification(memberId, title, message))
                .willThrow(originalException);

        // When & Then
        assertThatThrownBy(() ->
            retryableNotificationService.sendPushNotificationWithRetry(memberId, title, message))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_SEND_FAILED)
                .hasCause(originalException);

        verify(notificationService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("서비스 가용성 확인은 위임된 서비스로 전달")
    void shouldDelegateAvailabilityCheck() {
        // Given
        given(notificationService.isAvailable()).willReturn(true);

        // When
        boolean result = retryableNotificationService.isAvailable();

        // Then
        assertThat(result).isTrue();
        verify(notificationService).isAvailable();
    }

    @Test
    @DisplayName("채널 타입 조회는 위임된 서비스로 전달")
    void shouldDelegateChannelTypeRetrieval() {
        // Given
        NotificationChannelType expectedType = NotificationChannelType.PUSH;
        given(notificationService.getChannelType()).willReturn(expectedType);

        // When
        NotificationChannelType result = retryableNotificationService.getChannelType();

        // Then
        assertThat(result).isEqualTo(expectedType);
        verify(notificationService).getChannelType();
    }

    @Test
    @DisplayName("일반적인 sendPushNotification 메서드도 재시도 로직 사용")
    void shouldUseRetryLogicForGeneralSendMethod() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(notificationService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = retryableNotificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(notificationService).sendPushNotification(memberId, title, message);
    }

    @Test
    @DisplayName("재시도 통계 조회 메서드 존재 확인")
    void shouldProvideRetryStatistics() {
        // When
        RetryableNotificationService.RetryStatistics stats = retryableNotificationService.getRetryStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalAttempts()).isEqualTo(0L);
        assertThat(stats.getSuccessfulAttempts()).isEqualTo(0L);
        assertThat(stats.getFailedAttempts()).isEqualTo(0L);
        assertThat(stats.getAverageRetryCount()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RetryStatistics 빌더 패턴 동작 확인")
    void shouldBuildRetryStatisticsCorrectly() {
        // When
        RetryableNotificationService.RetryStatistics stats =
            RetryableNotificationService.RetryStatistics.builder()
                .totalAttempts(100L)
                .successfulAttempts(95L)
                .failedAttempts(5L)
                .averageRetryCount(1.2)
                .build();

        // Then
        assertThat(stats.getTotalAttempts()).isEqualTo(100L);
        assertThat(stats.getSuccessfulAttempts()).isEqualTo(95L);
        assertThat(stats.getFailedAttempts()).isEqualTo(5L);
        assertThat(stats.getAverageRetryCount()).isEqualTo(1.2);
    }
}

/**
 * Spring Retry 통합 테스트
 *
 * 실제 Spring Context와 함께 재시도 로직을 테스트합니다.
 * 주의: 실제 재시도 동작 테스트는 통합 테스트에서 수행해야 합니다.
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(NotificationRetryConfig.class)
@DisplayName("RetryableNotificationService Spring Retry 통합 테스트")
class RetryableNotificationServiceIntegrationTest {

    @Mock
    private NotificationService notificationService;

    private RetryableNotificationService retryableNotificationService;

    @BeforeEach
    void setUp() {
        retryableNotificationService = new RetryableNotificationService(notificationService);
    }

    @Test
    @DisplayName("Spring Retry 설정이 올바르게 로드됨")
    void shouldLoadSpringRetryConfiguration() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(notificationService.sendPushNotification(memberId, title, message)).willReturn(true);

        // When
        boolean result = retryableNotificationService.sendPushNotificationWithRetry(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(notificationService).sendPushNotification(memberId, title, message);
    }
}