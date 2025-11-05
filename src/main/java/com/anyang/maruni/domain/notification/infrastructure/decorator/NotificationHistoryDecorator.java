package com.anyang.maruni.domain.notification.infrastructure.decorator;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.repository.NotificationHistoryRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationSourceType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 서비스 이력 저장 데코레이터 (MVP 단순화 버전)
 *
 * Decorator 패턴을 적용하여 기존 NotificationService 구현체를 감싸서
 * 모든 알림 발송 시도를 자동으로 이력에 저장합니다.
 *
 * MVP 변경사항: NotificationHistoryService 대신 Repository 직접 사용
 */
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryDecorator implements NotificationService {

    private final NotificationService delegate;
    private final NotificationHistoryRepository repository;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        log.debug("📝 Recording notification attempt - memberId: {}, title: {}", memberId, title);

        try {
            // 실제 알림 발송 시도
            boolean success = delegate.sendPushNotification(memberId, title, message);

            if (success) {
                // 성공 이력 저장 (Repository 직접 사용)
                try {
                    NotificationHistory history = NotificationHistory.createSuccess(
                            memberId, title, message, getChannelType());
                    repository.save(history);
                    log.info("✅ Notification sent and recorded - historyId: {}", history.getId());
                } catch (Exception historyException) {
                    log.warn("⚠️ Failed to record success history, but notification was sent - error: {}",
                            historyException.getMessage());
                }
                return true;
            } else {
                // 실패 이력 저장 (일반적인 실패)
                try {
                    NotificationHistory history = NotificationHistory.createFailure(
                            memberId, title, message, getChannelType(),
                            "Notification service returned false");
                    repository.save(history);
                    log.warn("❌ Notification failed and recorded - historyId: {}", history.getId());
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
                NotificationHistory history = NotificationHistory.createFailure(
                        memberId, title, message, getChannelType(), errorMessage);
                repository.save(history);
                log.error("💥 Notification exception and recorded - historyId: {}, error: {}",
                        history.getId(), e.getMessage(), e);
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
     * 타입 정보를 포함한 알림 발송 (MVP 추가)
     *
     * @param memberId 알림 수신 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param notificationType 알림 타입
     * @param sourceType 알림 출처 타입
     * @param sourceEntityId 출처 엔티티 ID
     * @return 발송 성공 여부
     */
    @Override
    public boolean sendNotificationWithType(
            Long memberId,
            String title,
            String message,
            NotificationType notificationType,
            NotificationSourceType sourceType,
            Long sourceEntityId
    ) {
        log.debug("📝 Recording notification with type - memberId: {}, type: {}", memberId, notificationType);

        try {
            // 실제 알림 발송 시도 (delegate 사용)
            boolean success = delegate.sendPushNotification(memberId, title, message);

            if (success) {
                // 성공 이력 저장 (타입 정보 포함)
                try {
                    NotificationHistory history = NotificationHistory.createSuccessWithType(
                            memberId, title, message, getChannelType(),
                            notificationType, sourceType, sourceEntityId, null
                    );
                    repository.save(history);
                    log.info("✅ Notification sent and recorded with type - historyId: {}, type: {}",
                            history.getId(), notificationType);
                } catch (Exception historyException) {
                    log.warn("⚠️ Failed to record success history, but notification was sent");
                }
                return true;
            } else {
                // 실패 이력 저장 (타입 정보 포함)
                try {
                    NotificationHistory history = NotificationHistory.createFailureWithType(
                            memberId, title, message, getChannelType(),
                            notificationType, sourceType, sourceEntityId,
                            "Notification service returned false"
                    );
                    repository.save(history);
                    log.warn("❌ Notification failed and recorded with type - type: {}", notificationType);
                } catch (Exception historyException) {
                    log.warn("⚠️ Failed to record failure history");
                }
                return false;
            }
        } catch (Exception e) {
            // 예외 발생 시 실패 이력 저장 (타입 정보 포함)
            String errorMessage = "Exception occurred: " + e.getMessage();
            try {
                NotificationHistory history = NotificationHistory.createFailureWithType(
                        memberId, title, message, getChannelType(),
                        notificationType, sourceType, sourceEntityId,
                        errorMessage
                );
                repository.save(history);
                log.error("💥 Notification exception and recorded with type - type: {}", notificationType, e);
            } catch (Exception historyException) {
                log.error("💥 Notification exception and failed to record history", e);
            }
            return false;
        }
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