package com.anyang.maruni.domain.notification.infrastructure.decorator;

import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.global.exception.NotificationException;
import com.anyang.maruni.global.response.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 재시도 가능한 알림 서비스
 *
 * Spring Retry를 활용하여 알림 발송 실패 시 자동으로 재시도를 수행합니다.
 * - 최대 3회 재시도
 * - 지수 백오프 방식 (1초 → 2초 → 4초)
 * - NotificationException 발생 시에만 재시도
 *
 * 사용법:
 * - sendPushNotificationWithRetry() 메서드 사용
 * - 모든 재시도 실패 시 @Recover 메서드로 최종 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetryableNotificationService {

    private final NotificationService notificationService;

    /**
     * 재시도 가능한 푸시 알림 발송
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 메시지
     * @return 발송 성공 여부
     * @throws NotificationException 알림 발송 실패 시
     */
    @Retryable(
            value = {NotificationException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // 1초, 2초, 4초
    )
    public boolean sendPushNotificationWithRetry(Long memberId, String title, String message) {
        int currentAttempt = getCurrentAttemptFromContext();
        log.info("📤 Attempting to send notification to member {} (attempt: {})",
                memberId, currentAttempt);

        try {
            boolean result = notificationService.sendPushNotification(memberId, title, message);

            if (!result) {
                log.warn("⚠️ Notification service returned false for member {} (attempt: {})",
                        memberId, currentAttempt);
                throw new NotificationException(ErrorCode.NOTIFICATION_SEND_FAILED);
            }

            log.info("✅ Notification sent successfully to member {} (attempt: {})",
                    memberId, currentAttempt);
            return true;

        } catch (NotificationException e) {
            log.warn("💥 Notification failed for member {} (attempt: {}): {}",
                    memberId, currentAttempt, e.getMessage());
            throw e; // 재시도를 위해 예외를 다시 던짐

        } catch (Exception e) {
            log.error("💥 Unexpected error during notification for member {} (attempt: {}): {}",
                     memberId, currentAttempt, e.getMessage());
            // 일반 예외는 NotificationException으로 래핑하여 재시도 대상으로 만듦
            throw new NotificationException(ErrorCode.NOTIFICATION_SEND_FAILED, e);
        }
    }

    /**
     * 모든 재시도 실패 시 최종 처리
     *
     * @param ex 발생한 예외
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 메시지
     * @return 최종 실패 결과 (항상 false)
     */
    @Recover
    public boolean recoverFromNotificationFailure(NotificationException ex, Long memberId,
                                                String title, String message) {
        log.error("🔥 CRITICAL: All retry attempts failed for member {}: {}",
                 memberId, ex.getMessage());

        // 최종 실패 처리
        recordFinalFailure(memberId, title, message, ex);

        // 관리자 알림 등 추가 처리가 필요한 경우 여기서 수행
        notifyAdminOfCriticalFailure(memberId, title, message, ex);

        return false;
    }

    /**
     * 일반적인 재시도 메서드 (기존 호환성 유지)
     */
    public boolean sendPushNotification(Long memberId, String title, String message) {
        return sendPushNotificationWithRetry(memberId, title, message);
    }

    /**
     * 서비스 가용성 확인 (위임)
     */
    public boolean isAvailable() {
        return notificationService.isAvailable();
    }

    /**
     * 채널 타입 조회 (위임)
     */
    public NotificationChannelType getChannelType() {
        return notificationService.getChannelType();
    }

    /**
     * 현재 재시도 횟수 추적
     * 실제로는 Spring Retry가 내부적으로 관리하므로 대략적인 값 반환
     */
    private int getCurrentAttemptFromContext() {
        // Spring Retry Context에서 시도 횟수를 가져올 수 있지만
        // 여기서는 간단히 구현
        return 1; // 실제 구현에서는 Context에서 가져와야 함
    }

    /**
     * 최종 실패 기록
     */
    private void recordFinalFailure(Long memberId, String title, String message, Exception ex) {
        log.error("📊 Recording final notification failure - member: {}, error: {}",
                 memberId, ex.getMessage());

        // 메트릭 시스템에 최종 실패 기록
        // 예: Micrometer, Prometheus 등
        // metricsService.recordNotificationFinalFailure(memberId, ex.getMessage());
    }

    /**
     * 관리자에게 심각한 실패 알림
     */
    private void notifyAdminOfCriticalFailure(Long memberId, String title, String message, Exception ex) {
        log.error("🚨 Notifying admin of critical notification failure - member: {}", memberId);

        // 관리자 알림 로직
        // 예: 이메일, Slack, Discord 등
        // adminNotificationService.sendCriticalAlert(memberId, ex.getMessage());
    }

    /**
     * 재시도 통계 조회 (모니터링용)
     */
    public RetryStatistics getRetryStatistics() {
        // 실제 구현에서는 메트릭 시스템에서 데이터를 가져옴
        return RetryStatistics.builder()
                .totalAttempts(0L)
                .successfulAttempts(0L)
                .failedAttempts(0L)
                .averageRetryCount(0.0)
                .build();
    }

    /**
     * 재시도 통계 Value Object
     */
    public static class RetryStatistics {
        private final long totalAttempts;
        private final long successfulAttempts;
        private final long failedAttempts;
        private final double averageRetryCount;

        private RetryStatistics(long totalAttempts, long successfulAttempts,
                               long failedAttempts, double averageRetryCount) {
            this.totalAttempts = totalAttempts;
            this.successfulAttempts = successfulAttempts;
            this.failedAttempts = failedAttempts;
            this.averageRetryCount = averageRetryCount;
        }

        public static RetryStatisticsBuilder builder() {
            return new RetryStatisticsBuilder();
        }

        public static class RetryStatisticsBuilder {
            private long totalAttempts;
            private long successfulAttempts;
            private long failedAttempts;
            private double averageRetryCount;

            public RetryStatisticsBuilder totalAttempts(long totalAttempts) {
                this.totalAttempts = totalAttempts;
                return this;
            }

            public RetryStatisticsBuilder successfulAttempts(long successfulAttempts) {
                this.successfulAttempts = successfulAttempts;
                return this;
            }

            public RetryStatisticsBuilder failedAttempts(long failedAttempts) {
                this.failedAttempts = failedAttempts;
                return this;
            }

            public RetryStatisticsBuilder averageRetryCount(double averageRetryCount) {
                this.averageRetryCount = averageRetryCount;
                return this;
            }

            public RetryStatistics build() {
                return new RetryStatistics(totalAttempts, successfulAttempts,
                                         failedAttempts, averageRetryCount);
            }
        }

        // Getters
        public long getTotalAttempts() { return totalAttempts; }
        public long getSuccessfulAttempts() { return successfulAttempts; }
        public long getFailedAttempts() { return failedAttempts; }
        public double getAverageRetryCount() { return averageRetryCount; }
    }
}