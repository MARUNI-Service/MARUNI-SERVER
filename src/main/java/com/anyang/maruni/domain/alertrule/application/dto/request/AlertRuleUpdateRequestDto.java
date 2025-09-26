package com.anyang.maruni.domain.alertrule.application.dto.request;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 규칙 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "알림 규칙 수정 요청")
public class AlertRuleUpdateRequestDto {

    /**
     * 알림 레벨 (수정 가능)
     */
    @Schema(description = "알림 레벨",
            example = "HIGH",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "EMERGENCY"})
    private AlertLevel alertLevel;

    /**
     * 알림 규칙 이름 (수정 가능)
     */
    @Size(min = 1, max = 100, message = "알림 규칙 이름은 1~100자여야 합니다")
    @Schema(description = "알림 규칙 이름",
            example = "수정된 감정패턴 감지 규칙",
            minLength = 1, maxLength = 100)
    private String ruleName;

    /**
     * 알림 조건 (수정 가능)
     */
    @Valid
    @Schema(description = "수정할 알림 감지 조건")
    private AlertConditionDto condition;

    /**
     * 알림 규칙 설명 (수정 가능)
     */
    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다")
    @Schema(description = "알림 규칙 설명",
            example = "수정된 알림 규칙에 대한 상세 설명",
            maxLength = 255)
    private String description;

    /**
     * 활성화 여부 (수정 가능)
     */
    @Schema(description = "활성화 여부",
            example = "true")
    private Boolean active;
}