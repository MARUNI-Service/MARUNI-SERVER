package com.anyang.maruni.domain.alertrule.domain.entity;

import static java.time.Duration.*;
import static java.time.LocalTime.*;

import java.time.LocalDateTime;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 이력 엔티티
 *
 * 이상징후 감지 및 알림 발송 이력을 추적합니다.
 * 중복 알림 방지와 이력 관리를 위해 사용됩니다.
 */
@Entity
@Table(name = "alert_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "alert_rule_id", "alert_date"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림이 발생한 알림 규칙
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    /**
     * 알림 대상 회원
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    /**
     * 알림 레벨
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    /**
     * 알림 메시지
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String alertMessage;

    /**
     * 감지 상세 정보 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String detectionDetails;

    /**
     * 알림 발송 완료 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isNotificationSent = false;

    /**
     * 알림 발송 완료 시각
     */
    @Column
    private LocalDateTime notificationSentAt;

    /**
     * 알림 발송 결과
     */
    @Column(columnDefinition = "TEXT")
    private String notificationResult;

    /**
     * 알림 발생 날짜 (중복 방지용)
     */
    @Column(name = "alert_date", nullable = false)
    private LocalDateTime alertDate;

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 알림 이력 생성
     * @param alertRule 알림 규칙
     * @param member 대상 회원
     * @param alertMessage 알림 메시지
     * @param detectionDetails 감지 상세 정보
     * @return AlertHistory
     */
    public static AlertHistory createAlert(
            AlertRule alertRule, MemberEntity member,
            String alertMessage, String detectionDetails) {
        return AlertHistory.builder()
                .alertRule(alertRule)
                .member(member)
                .alertLevel(alertRule.getAlertLevel())
                .alertMessage(alertMessage)
                .detectionDetails(detectionDetails)
                .alertDate(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)) // 일자 기준으로 중복 방지
                .isNotificationSent(false)
                .build();
    }

    /**
     * 긴급 알림 이력 생성 (즉시 발송용)
     * @param alertRule 알림 규칙
     * @param member 대상 회원
     * @param alertMessage 알림 메시지
     * @param detectionDetails 감지 상세 정보
     * @return AlertHistory
     */
    public static AlertHistory createEmergencyAlert(
            AlertRule alertRule, MemberEntity member,
            String alertMessage, String detectionDetails) {
        return AlertHistory.builder()
                .alertRule(alertRule)
                .member(member)
                .alertLevel(AlertLevel.EMERGENCY)
                .alertMessage(alertMessage)
                .detectionDetails(detectionDetails)
                .alertDate(LocalDateTime.now()) // 긴급상황은 실시간으로 기록
                .isNotificationSent(false)
                .build();
    }

    /**
     * 테스트용 알림 이력 생성 (특정 시간으로 생성)
     * @param alertRule 알림 규칙
     * @param member 대상 회원
     * @param alertMessage 알림 메시지
     * @param detectionDetails 감지 상세 정보
     * @param alertDate 특정 알림 날짜
     * @return AlertHistory
     */
    public static AlertHistory createAlertWithDate(
            AlertRule alertRule, MemberEntity member,
            String alertMessage, String detectionDetails,
            LocalDateTime alertDate) {
        return AlertHistory.builder()
                .alertRule(alertRule)
                .member(member)
                .alertLevel(alertRule.getAlertLevel())
                .alertMessage(alertMessage)
                .detectionDetails(detectionDetails)
                .alertDate(alertDate)
                .isNotificationSent(false)
                .build();
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 알림 발송 완료 처리
     * @param result 발송 결과
     */
    public void markNotificationSent(String result) {
        this.isNotificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
        this.notificationResult = result;
    }

    /**
     * 알림 발송 실패 처리
     * @param errorMessage 실패 사유
     */
    public void markNotificationFailed(String errorMessage) {
        this.isNotificationSent = false;
        this.notificationResult = "FAILED: " + errorMessage;
    }

    /**
     * 긴급 알림인지 확인
     */
    public boolean isEmergency() {
        return alertLevel == AlertLevel.EMERGENCY;
    }

    /**
     * 알림 발송 재시도 가능 여부 확인
     */
    public boolean canRetryNotification() {
        return !isNotificationSent &&
               (notificationResult == null || notificationResult.startsWith("FAILED"));
    }

    /**
     * 알림 발생 후 경과 시간(분) 계산
     */
    public long getMinutesElapsed() {
        return between(getCreatedAt(), now()).toMinutes();
    }
}