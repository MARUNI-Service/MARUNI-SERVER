package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 안정성 강화 알림 서비스 통합 설정
 *
 * Phase 1 Week 4-5의 안정성 강화 시스템을 통합하여 구성합니다:
 * 1. Fallback 시스템: Firebase 실패 시 Mock으로 자동 전환
 * 2. Retry 시스템: 실패 시 최대 3회 자동 재시도
 * 3. History 시스템: 모든 시도를 자동으로 이력에 저장
 *
 * 설정 우선순위:
 * StabilityEnhanced > History > Fallback > Original
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class StabilityEnhancedNotificationConfig {

    private final NotificationHistoryService historyService;

    /**
     * 안정성이 강화된 알림 서비스 구성
     *
     * 설정에 따라 다음과 같이 서비스를 계층적으로 구성합니다:
     * - notification.stability.enabled=true: 전체 안정성 강화 시스템 활성화
     * - notification.fallback.enabled=true: Fallback 시스템 활성화
     * - notification.history.enabled=true: History 시스템 활성화 (기본값)
     *
     * 최종 구성: RetryableService -> HistoryDecorator -> FallbackService -> OriginalService
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            name = "notification.stability.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public NotificationService stabilityEnhancedNotificationService(@Autowired List<NotificationService> services) {
        log.info("🚀 Configuring stability-enhanced notification service...");

        // 1. 원본 서비스 찾기 (데코레이터가 아닌 실제 구현체)
        NotificationService originalService = findOriginalNotificationService(services);
        log.info("📋 Found original service: {}", originalService.getClass().getSimpleName());

        // 2. Fallback 시스템 적용 (설정에 따라)
        NotificationService serviceWithFallback = applyFallbackIfEnabled(originalService, services);
        log.info("🔄 Service with fallback: {}", serviceWithFallback.getClass().getSimpleName());

        // 3. History 시스템 적용
        NotificationService serviceWithHistory = applyHistoryDecorator(serviceWithFallback);
        log.info("📝 Service with history: {}", serviceWithHistory.getClass().getSimpleName());

        // 4. Retry 시스템 적용 (최상위 래퍼)
        RetryableNotificationService finalService = new RetryableNotificationService(serviceWithHistory);
        log.info("🔁 Final service with retry: {}", finalService.getClass().getSimpleName());

        log.info("✅ Stability-enhanced notification service configured successfully");
        return new StabilityEnhancedNotificationServiceWrapper(finalService);
    }

    /**
     * 원본 NotificationService 찾기
     */
    private NotificationService findOriginalNotificationService(List<NotificationService> services) {
        return services.stream()
                .filter(service -> !(service instanceof NotificationHistoryDecorator))
                .filter(service -> !(service instanceof FallbackNotificationService))
                .filter(service -> !(service instanceof StabilityEnhancedNotificationServiceWrapper))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No original NotificationService found"));
    }

    /**
     * Fallback 시스템 적용 (조건부)
     */
    private NotificationService applyFallbackIfEnabled(NotificationService originalService,
                                                     List<NotificationService> allServices) {
        // Fallback이 활성화되어 있고, FallbackNotificationService가 존재하는 경우
        boolean fallbackExists = allServices.stream()
                .anyMatch(service -> service instanceof FallbackNotificationService);

        if (fallbackExists) {
            log.info("🔄 Fallback system is enabled and will be used");
            return allServices.stream()
                    .filter(service -> service instanceof FallbackNotificationService)
                    .findFirst()
                    .orElse(originalService);
        } else {
            log.info("🔄 Fallback system is not available, using original service");
            return originalService;
        }
    }

    /**
     * History Decorator 적용
     */
    private NotificationService applyHistoryDecorator(NotificationService service) {
        return new NotificationHistoryDecorator(service, historyService);
    }

    /**
     * 안정성 강화 서비스 래퍼
     *
     * RetryableNotificationService를 NotificationService 인터페이스로 래핑하여
     * Spring의 의존성 주입 시스템과 호환되도록 합니다.
     */
    public static class StabilityEnhancedNotificationServiceWrapper implements NotificationService {

        private final RetryableNotificationService retryableService;

        public StabilityEnhancedNotificationServiceWrapper(RetryableNotificationService retryableService) {
            this.retryableService = retryableService;
        }

        @Override
        public boolean sendPushNotification(Long memberId, String title, String message) {
            return retryableService.sendPushNotificationWithRetry(memberId, title, message);
        }

        @Override
        public boolean isAvailable() {
            return retryableService.isAvailable();
        }

        @Override
        public com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType getChannelType() {
            return retryableService.getChannelType();
        }

        /**
         * 내부 RetryableService 접근 (디버깅용)
         */
        public RetryableNotificationService getRetryableService() {
            return retryableService;
        }

        /**
         * 재시도 통계 조회 (모니터링용)
         */
        public RetryableNotificationService.RetryStatistics getRetryStatistics() {
            return retryableService.getRetryStatistics();
        }
    }
}