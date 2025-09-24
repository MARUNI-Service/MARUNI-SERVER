package com.anyang.maruni.domain.alertrule.application.dto.response;

import com.anyang.maruni.domain.alertrule.application.dto.request.AlertConditionDto;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 규칙 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleResponseDto {

    /**
     * 알림 규칙 ID
     */
    private Long id;

    /**
     * 회원 ID
     */
    private Long memberId;

    /**
     * 알림 유형
     */
    private AlertType alertType;

    /**
     * 알림 레벨
     */
    private AlertLevel alertLevel;

    /**
     * 알림 규칙 이름
     */
    private String ruleName;

    /**
     * 알림 조건
     */
    private AlertConditionDto condition;

    /**
     * 알림 규칙 설명
     */
    private String description;

    /**
     * 활성화 여부
     */
    private Boolean active;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * Entity에서 DTO로 변환하는 정적 팩토리 메서드
     * @param alertRule 알림 규칙 엔티티
     * @return AlertRuleResponseDto
     */
    public static AlertRuleResponseDto from(AlertRule alertRule) {
        AlertConditionDto conditionDto = AlertConditionDto.builder()
                .consecutiveDays(alertRule.getCondition().getConsecutiveDays())
                .thresholdCount(alertRule.getCondition().getThresholdCount())
                .keywords(alertRule.getCondition().getKeywords())
                .description(null) // AlertCondition에는 description 필드 없음
                .build();

        return AlertRuleResponseDto.builder()
                .id(alertRule.getId())
                .memberId(alertRule.getMember().getId())
                .alertType(alertRule.getAlertType())
                .alertLevel(alertRule.getAlertLevel())
                .ruleName(alertRule.getRuleName())
                .condition(conditionDto)
                .description(alertRule.getRuleDescription())
                .active(alertRule.getIsActive())
                .createdAt(alertRule.getCreatedAt())
                .updatedAt(alertRule.getUpdatedAt())
                .build();
    }
}