package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.service.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * StabilityEnhancedNotificationConfig 단위 테스트
 *
 * 안정성 강화 알림 서비스의 통합 설정이 올바르게 동작하는지 검증합니다.
 * - 원본 서비스 탐지
 * - Fallback 시스템 적용
 * - History 시스템 적용
 * - Retry 시스템 적용
 * - 최종 래퍼 생성
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StabilityEnhancedNotificationConfig 테스트")
class StabilityEnhancedNotificationConfigTest {

    @Mock
    private NotificationHistoryService historyService;

    @Mock
    private NotificationService originalService;

    @Mock
    private FallbackNotificationService fallbackService;

    @Mock
    private NotificationHistoryDecorator historyDecorator;

    private StabilityEnhancedNotificationConfig config;

    @BeforeEach
    void setUp() {
        config = new StabilityEnhancedNotificationConfig(historyService);
    }

    @Test
    @DisplayName("원본 서비스 찾기 - 정상적인 원본 서비스 탐지")
    void shouldFindOriginalNotificationService() {
        // Given
        List<NotificationService> services = Arrays.asList(
                historyDecorator,  // Decorator (제외)
                fallbackService,   // Fallback (제외)
                originalService    // Original (선택)
        );

        // When
        NotificationService result = config.stabilityEnhancedNotificationService(services);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper.class);
    }

    @Test
    @DisplayName("원본 서비스 찾기 실패 시 IllegalStateException 발생")
    void shouldThrowIllegalStateExceptionWhenNoOriginalServiceFound() {
        // Given
        List<NotificationService> services = Arrays.asList(
                historyDecorator,  // Decorator만 있음
                fallbackService    // Fallback만 있음
        );

        // When & Then
        assertThatThrownBy(() -> config.stabilityEnhancedNotificationService(services))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No original NotificationService found");
    }

    @Test
    @DisplayName("StabilityEnhancedNotificationServiceWrapper 동작 확인")
    void shouldWrapRetryableServiceCorrectly() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        NotificationChannelType expectedType = NotificationChannelType.PUSH;

        given(originalService.sendPushNotification(memberId, title, message)).willReturn(true);
        given(originalService.isAvailable()).willReturn(true);
        given(originalService.getChannelType()).willReturn(expectedType);

        List<NotificationService> services = Arrays.asList(originalService);

        // When
        NotificationService result = config.stabilityEnhancedNotificationService(services);
        StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper wrapper =
                (StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper) result;

        // Then
        assertThat(wrapper.sendPushNotification(memberId, title, message)).isTrue();
        assertThat(wrapper.isAvailable()).isTrue();
        assertThat(wrapper.getChannelType()).isEqualTo(expectedType);
        assertThat(wrapper.getRetryableService()).isNotNull();
        assertThat(wrapper.getRetryStatistics()).isNotNull();
    }

    @Test
    @DisplayName("Fallback 서비스가 존재하는 경우 우선 적용")
    void shouldApplyFallbackServiceWhenAvailable() {
        // Given
        List<NotificationService> services = Arrays.asList(
                fallbackService,   // Fallback 서비스 존재
                originalService    // Original 서비스
        );

        // Mock FallbackNotificationService를 실제처럼 설정
        FallbackNotificationService mockFallback = mock(FallbackNotificationService.class);
        services = Arrays.asList(mockFallback, originalService);

        // When
        NotificationService result = config.stabilityEnhancedNotificationService(services);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper.class);
    }

    @Test
    @DisplayName("여러 데코레이터가 있어도 원본 서비스 정확히 탐지")
    void shouldFindOriginalServiceAmongMultipleDecorators() {
        // Given
        NotificationHistoryDecorator decorator1 = mock(NotificationHistoryDecorator.class);
        NotificationHistoryDecorator decorator2 = mock(NotificationHistoryDecorator.class);
        StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper wrapper =
                mock(StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper.class);

        List<NotificationService> services = Arrays.asList(
                decorator1,        // 제외
                decorator2,        // 제외
                wrapper,           // 제외
                originalService    // 선택
        );

        // When
        NotificationService result = config.stabilityEnhancedNotificationService(services);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(StabilityEnhancedNotificationConfig.StabilityEnhancedNotificationServiceWrapper.class);
    }

    @Test
    @DisplayName("빈 서비스 리스트에서 IllegalStateException 발생")
    void shouldThrowIllegalStateExceptionForEmptyServiceList() {
        // Given
        List<NotificationService> services = Arrays.asList();

        // When & Then
        assertThatThrownBy(() -> config.stabilityEnhancedNotificationService(services))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No original NotificationService found");
    }

    /**
     * 테스트용 Mock 서비스 클래스
     */
    private static class MockOriginalService implements NotificationService {
        @Override
        public boolean sendPushNotification(Long memberId, String title, String message) {
            return true;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public NotificationChannelType getChannelType() {
            return NotificationChannelType.PUSH;
        }
    }
}