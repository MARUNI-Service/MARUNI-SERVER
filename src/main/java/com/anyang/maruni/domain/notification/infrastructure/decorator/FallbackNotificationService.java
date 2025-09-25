package com.anyang.maruni.domain.notification.infrastructure.decorator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.global.exception.NotificationException;
import com.anyang.maruni.global.response.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback 알림 서비스
 *
 * 기본 알림 서비스(Firebase FCM)가 실패할 경우
 * 보조 알림 서비스(Mock)로 자동 전환하여 알림 연속성을 보장합니다.
 *
 * 사용 조건:
 * - notification.fallback.enabled=true 일 때만 활성화
 * - Primary 서비스 실패 시 자동으로 Fallback 서비스 사용
 * - 모든 시도가 실패할 경우에만 최종 실패 처리
 */
@Service
@ConditionalOnProperty(
        name = "notification.fallback.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class FallbackNotificationService implements NotificationService {

    @Qualifier("firebasePushNotificationService")
    private final NotificationService primaryService;

    @Qualifier("mockPushNotificationService")
    private final NotificationService fallbackService;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        log.debug("🔄 Starting fallback notification attempt for member: {}", memberId);

        // 1차: Primary 서비스 시도 (Firebase FCM)
        if (primaryService.isAvailable()) {
            try {
                log.debug("📱 Trying primary notification service (Firebase) for member: {}", memberId);
                boolean result = primaryService.sendPushNotification(memberId, title, message);

                if (result) {
                    log.info("✅ Primary notification service succeeded for member: {}", memberId);
                    return true;
                } else {
                    log.warn("⚠️ Primary notification service returned false for member: {}", memberId);
                }
            } catch (Exception e) {
                log.warn("💥 Primary notification service failed for member {}: {}", memberId, e.getMessage());
            }
        } else {
            log.warn("🚫 Primary notification service is not available for member: {}", memberId);
        }

        // 2차: Fallback 서비스 시도 (Mock)
        if (fallbackService.isAvailable()) {
            log.info("🔄 Using fallback notification service for member: {}", memberId);
            try {
                boolean fallbackResult = fallbackService.sendPushNotification(memberId, title, message);

                if (fallbackResult) {
                    log.info("✅ Fallback notification service succeeded for member: {}", memberId);
                    return true;
                } else {
                    log.error("❌ Fallback notification service also returned false for member: {}", memberId);
                }
            } catch (Exception e) {
                log.error("💥 Fallback notification service also failed for member {}: {}",
                         memberId, e.getMessage());
            }
        } else {
            log.error("🚫 Fallback notification service is also not available for member: {}", memberId);
        }

        // 모든 서비스 실패
        log.error("🔥 CRITICAL: All notification services failed for member: {}", memberId);
        throw new NotificationException(ErrorCode.NOTIFICATION_FALLBACK_FAILED);
    }

    @Override
    public boolean isAvailable() {
        // Primary 또는 Fallback 중 하나라도 사용 가능하면 true
        boolean primaryAvailable = primaryService.isAvailable();
        boolean fallbackAvailable = fallbackService.isAvailable();

        boolean available = primaryAvailable || fallbackAvailable;

        log.debug("🔍 Fallback service availability check - Primary: {}, Fallback: {}, Result: {}",
                 primaryAvailable, fallbackAvailable, available);

        return available;
    }

    @Override
    public NotificationChannelType getChannelType() {
        // Primary 서비스의 채널 타입을 우선 반환
        return primaryService.getChannelType();
    }

    /**
     * Primary 서비스 상태 확인 (모니터링용)
     */
    public boolean isPrimaryServiceAvailable() {
        return primaryService.isAvailable();
    }

    /**
     * Fallback 서비스 상태 확인 (모니터링용)
     */
    public boolean isFallbackServiceAvailable() {
        return fallbackService.isAvailable();
    }

    /**
     * 현재 사용 중인 서비스 정보 반환 (디버깅용)
     */
    public String getActiveServiceInfo() {
        if (primaryService.isAvailable()) {
            return "Primary: " + primaryService.getClass().getSimpleName();
        } else if (fallbackService.isAvailable()) {
            return "Fallback: " + fallbackService.getClass().getSimpleName();
        } else {
            return "No services available";
        }
    }
}