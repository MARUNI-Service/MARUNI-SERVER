package com.anyang.maruni.domain.alertrule.domain.entity;

import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 알림 규칙 엔티티
 *
 * 이상징후 감지를 위한 규칙을 정의하고 관리합니다.
 * 각 회원별로 다양한 감지 규칙을 설정할 수 있습니다.
 */
@Entity
@Table(name = "alert_rule", indexes = {
    @Index(name = "idx_alert_rule_member_type_active", columnList = "member_id, alert_type, is_active"),
    @Index(name = "idx_alert_rule_level_active", columnList = "alert_level, is_active"),
    @Index(name = "idx_alert_rule_member_active", columnList = "member_id, is_active")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 규칙이 적용되는 회원
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    /**
     * 알림 유형 (감정패턴/무응답/키워드감지 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    /**
     * 규칙 이름
     */
    @Column(nullable = false)
    private String ruleName;

    /**
     * 규칙 설명
     */
    @Column(columnDefinition = "TEXT")
    private String ruleDescription;

    /**
     * 감지 조건
     */
    @Embedded
    private AlertCondition condition;

    /**
     * 알림 레벨
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    /**
     * 규칙 활성화 상태
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 감정 패턴 감지 규칙 생성
     * @param member 대상 회원
     * @param consecutiveDays 연속 부정감정 일수
     * @param alertLevel 알림 레벨
     * @return AlertRule
     */
    public static AlertRule createEmotionPatternRule(
            MemberEntity member, int consecutiveDays, AlertLevel alertLevel) {
        return AlertRule.builder()
                .member(member)
                .alertType(AlertType.EMOTION_PATTERN)
                .ruleName("연속 부정감정 감지")
                .ruleDescription(consecutiveDays + "일 연속 부정적 감정 감지 시 알림")
                .condition(AlertCondition.createEmotionCondition(consecutiveDays))
                .alertLevel(alertLevel)
                .isActive(true)
                .build();
    }

    /**
     * 무응답 감지 규칙 생성
     * @param member 대상 회원
     * @param noResponseDays 무응답 일수
     * @param alertLevel 알림 레벨
     * @return AlertRule
     */
    public static AlertRule createNoResponseRule(
            MemberEntity member, int noResponseDays, AlertLevel alertLevel) {
        return AlertRule.builder()
                .member(member)
                .alertType(AlertType.NO_RESPONSE)
                .ruleName("무응답 감지")
                .ruleDescription(noResponseDays + "일 연속 무응답 시 알림")
                .condition(AlertCondition.createNoResponseCondition(noResponseDays))
                .alertLevel(alertLevel)
                .isActive(true)
                .build();
    }

    /**
     * 키워드 감지 규칙 생성
     * @param member 대상 회원
     * @param keywords 감지할 키워드들
     * @param alertLevel 알림 레벨
     * @return AlertRule
     */
    public static AlertRule createKeywordRule(
            MemberEntity member, String keywords, AlertLevel alertLevel) {
        return AlertRule.builder()
                .member(member)
                .alertType(AlertType.KEYWORD_DETECTION)
                .ruleName("키워드 감지")
                .ruleDescription("위험 키워드 감지 시 알림: " + keywords)
                .condition(AlertCondition.createKeywordCondition(keywords))
                .alertLevel(alertLevel)
                .isActive(true)
                .build();
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 주어진 메시지들이 이 규칙의 알림 조건을 만족하는지 확인
     * @param recentMessages 최근 메시지들
     * @return 알림 발생 여부
     */
    public boolean shouldTriggerAlert(List<MessageEntity> recentMessages) {
        if (!isActive) {
            return false;
        }

        return condition.evaluate(recentMessages, alertType);
    }

    /**
     * 규칙 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 규칙 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 규칙 정보 업데이트
     * @param ruleName 새로운 규칙 이름
     * @param ruleDescription 새로운 규칙 설명
     * @param alertLevel 새로운 알림 레벨
     */
    public void updateRule(String ruleName, String ruleDescription, AlertLevel alertLevel) {
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.alertLevel = alertLevel;
    }
}