package com.anyang.maruni.domain.notification.infrastructure.decorator;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 서비스 이력 저장 데코레이터
 *
 * Decorator 패턴을 적용하여 기존 NotificationService 구현체를 감싸서
 * 모든 알림 발송 시도를 자동으로 이력에 저장합니다.
 *
 * 이를 통해 어떤 NotificationService 구현체든 이력 저장 기능을
 * 투명하게 추가할 수 있습니다.
 */
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryDecorator implements NotificationService {

    private final NotificationService delegate;
    private final NotificationHistoryService historyService;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        log.debug("📝 Recording notification attempt - memberId: {}, title: {}", memberId, title);

        try {
            // 실제 알림 발송 시도
            boolean success = delegate.sendPushNotification(memberId, title, message);

            if (success) {
                // 성공 이력 저장
                try {
                    NotificationHistory history = historyService.recordSuccess(
                            memberId, title, message, getChannelType());
                    log.info("✅ Notification sent and recorded - historyId: {}",
                            history != null ? history.getId() : "unknown");
                } catch (Exception historyException) {
                    log.warn("⚠️ Failed to record success history, but notification was sent - error: {}",
                            historyException.getMessage());
                }
                return true;
            } else {
                // 실패 이력 저장 (일반적인 실패)
                try {
                    NotificationHistory history = historyService.recordFailure(
                            memberId, title, message, getChannelType(),
                            "Notification service returned false");
                    log.warn("❌ Notification failed and recorded - historyId: {}",
                            history != null ? history.getId() : "unknown");
                } catch (Exception historyException) {
                    log.warn("⚠️ Failed to record failure history - error: {}",
                            historyException.getMessage());
                }
                return false;
            }
        } catch (Exception e) {
            // 예외 발생 시 실패 이력 저장
            String errorMessage = "Exception occurred: " + e.getMessage();
            try {
                NotificationHistory history = historyService.recordFailure(
                        memberId, title, message, getChannelType(), errorMessage);
                log.error("💥 Notification exception and recorded - historyId: {}, error: {}",
                        history != null ? history.getId() : "unknown", e.getMessage(), e);
            } catch (Exception historyException) {
                log.error("💥 Notification exception and failed to record history - original error: {}, history error: {}",
                        e.getMessage(), historyException.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        // 델리게이트의 가용성을 그대로 반환
        boolean available = delegate.isAvailable();
        log.debug("🔍 Notification service availability check: {}", available);
        return available;
    }

    @Override
    public NotificationChannelType getChannelType() {
        // 델리게이트의 채널 타입을 그대로 반환
        return delegate.getChannelType();
    }

    /**
     * 데코레이터가 감싸고 있는 실제 서비스 반환
     * 테스트나 디버깅 목적으로 사용할 수 있습니다.
     *
     * @return 실제 NotificationService 구현체
     */
    public NotificationService getDelegate() {
        return delegate;
    }

    /**
     * 데코레이터 체인에서 특정 타입의 서비스 찾기
     * 여러 데코레이터가 중첩된 경우 유용합니다.
     *
     * @param serviceClass 찾고자 하는 서비스 클래스
     * @param <T> 서비스 타입
     * @return 해당 타입의 서비스 또는 null
     */
    @SuppressWarnings("unchecked")
    public <T extends NotificationService> T unwrap(Class<T> serviceClass) {
        if (serviceClass.isInstance(this)) {
            return (T) this;
        }

        if (serviceClass.isInstance(delegate)) {
            return (T) delegate;
        }

        // 델리게이트가 또 다른 데코레이터인 경우 재귀적으로 찾기
        if (delegate instanceof NotificationHistoryDecorator) {
            return ((NotificationHistoryDecorator) delegate).unwrap(serviceClass);
        }

        return null;
    }
}