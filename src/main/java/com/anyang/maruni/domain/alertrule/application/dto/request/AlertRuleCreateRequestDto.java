package com.anyang.maruni.domain.alertrule.application.dto.request;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 규칙 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "알림 규칙 생성 요청")
public class AlertRuleCreateRequestDto {

    /**
     * 알림 유형
     */
    @NotNull(message = "알림 유형은 필수입니다")
    @Schema(description = "알림 유형",
            example = "EMOTION_PATTERN",
            allowableValues = {"EMOTION_PATTERN", "NO_RESPONSE", "KEYWORD_DETECTION"})
    private AlertType alertType;

    /**
     * 알림 레벨
     */
    @NotNull(message = "알림 레벨은 필수입니다")
    @Schema(description = "알림 레벨",
            example = "HIGH",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "EMERGENCY"})
    private AlertLevel alertLevel;

    /**
     * 알림 규칙 이름
     */
    @NotNull(message = "알림 규칙 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "알림 규칙 이름은 1~100자여야 합니다")
    @Schema(description = "알림 규칙 이름",
            example = "연속 부정감정 감지 규칙",
            minLength = 1, maxLength = 100)
    private String ruleName;

    /**
     * 알림 조건
     */
    @NotNull(message = "알림 조건은 필수입니다")
    @Valid
    @Schema(description = "알림 감지 조건")
    private AlertConditionDto condition;

    /**
     * 알림 규칙 설명
     */
    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다")
    @Schema(description = "알림 규칙 설명",
            example = "3일 연속 부정적 감정 감지 시 보호자에게 알림을 발송합니다",
            maxLength = 255)
    private String description;

    /**
     * 활성화 여부 (기본값: true)
     */
    @Builder.Default
    @Schema(description = "활성화 여부",
            example = "true",
            defaultValue = "true")
    private Boolean active = true;
}