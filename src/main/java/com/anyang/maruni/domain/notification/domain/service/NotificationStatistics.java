package com.anyang.maruni.domain.notification.domain.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 통계 정보 Value Object
 *
 * 알림 발송 성과와 통계를 표현하는 도메인 객체입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatistics {

    /**
     * 전체 알림 발송 건수
     */
    private long totalNotifications;

    /**
     * 성공한 알림 건수
     */
    private long successNotifications;

    /**
     * 실패한 알림 건수
     */
    private long failureNotifications;

    /**
     * 성공률 (0.0 ~ 1.0)
     */
    private double successRate;

    /**
     * 실패율 (0.0 ~ 1.0)
     */
    private double failureRate;

    /**
     * 통계 생성을 위한 정적 팩토리 메서드
     *
     * @param totalNotifications 전체 알림 건수
     * @param successNotifications 성공한 알림 건수
     * @param failureNotifications 실패한 알림 건수
     * @return 통계 객체
     */
    public static NotificationStatistics of(long totalNotifications,
                                           long successNotifications,
                                           long failureNotifications) {
        double successRate = totalNotifications > 0 ?
            (double) successNotifications / totalNotifications : 0.0;
        double failureRate = totalNotifications > 0 ?
            (double) failureNotifications / totalNotifications : 0.0;

        return NotificationStatistics.builder()
                .totalNotifications(totalNotifications)
                .successNotifications(successNotifications)
                .failureNotifications(failureNotifications)
                .successRate(successRate)
                .failureRate(failureRate)
                .build();
    }

    /**
     * 성공률 퍼센트 반환
     *
     * @return 성공률 퍼센트 (0 ~ 100)
     */
    public double getSuccessPercentage() {
        return successRate * 100.0;
    }

    /**
     * 실패율 퍼센트 반환
     *
     * @return 실패율 퍼센트 (0 ~ 100)
     */
    public double getFailurePercentage() {
        return failureRate * 100.0;
    }

    /**
     * 통계 요약 문자열 반환
     *
     * @return 통계 요약
     */
    public String getSummary() {
        return String.format("전체: %d건, 성공: %d건(%.1f%%), 실패: %d건(%.1f%%)",
                totalNotifications, successNotifications, getSuccessPercentage(),
                failureNotifications, getFailurePercentage());
    }
}