package com.anyang.maruni.domain.notification.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 알림 시스템 재시도 설정
 *
 * Spring Retry 기능을 활성화하여 RetryableNotificationService에서
 * @Retryable 및 @Recover 어노테이션을 사용할 수 있도록 합니다.
 *
 * 재시도 정책:
 * - NotificationException 발생 시 최대 3회 재시도
 * - 지수 백오프 방식 (1초 → 2초 → 4초)
 * - 모든 재시도 실패 시 @Recover 메서드로 최종 처리
 */
@Configuration
@EnableRetry
@Slf4j
public class NotificationRetryConfig {

    public NotificationRetryConfig() {
        log.info("🔧 Spring Retry enabled for notification services");
    }
}