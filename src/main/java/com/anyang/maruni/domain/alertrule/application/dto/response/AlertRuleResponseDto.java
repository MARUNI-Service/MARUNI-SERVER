package com.anyang.maruni.domain.alertrule.application.dto.response;

import com.anyang.maruni.domain.alertrule.application.dto.request.AlertConditionDto;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "알림 규칙 상세 정보")
public class AlertRuleResponseDto {

    /**
     * 알림 규칙 ID
     */
    @Schema(description = "알림 규칙 고유 식별자", example = "1")
    private Long id;

    /**
     * 회원 ID
     */
    @Schema(description = "알림 규칙을 소유한 회원의 ID", example = "10")
    private Long memberId;

    /**
     * 알림 유형
     */
    @Schema(description = "알림 유형", example = "EMOTION_PATTERN")
    private AlertType alertType;

    /**
     * 알림 레벨
     */
    @Schema(description = "알림 레벨", example = "HIGH")
    private AlertLevel alertLevel;

    /**
     * 알림 규칙 이름
     */
    @Schema(description = "알림 규칙 이름", example = "연속 부정감정 감지")
    private String ruleName;

    /**
     * 알림 조건
     */
    @Schema(description = "알림 감지 조건")
    private AlertConditionDto condition;

    /**
     * 알림 규칙 설명
     */
    @Schema(description = "알림 규칙에 대한 상세 설명",
            example = "3일 연속 부정적 감정 감지 시 알림")
    private String description;

    /**
     * 활성화 여부
     */
    @Schema(description = "규칙 활성화 상태", example = "true")
    private Boolean active;

    /**
     * 생성일시
     */
    @Schema(description = "규칙 생성 날짜와 시간",
            example = "2025-09-25T14:30:00")
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @Schema(description = "규칙 마지막 수정 날짜와 시간",
            example = "2025-09-25T16:45:00")
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