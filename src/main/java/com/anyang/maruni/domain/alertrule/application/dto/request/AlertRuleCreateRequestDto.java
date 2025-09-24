package com.anyang.maruni.domain.alertrule.application.dto.request;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
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
public class AlertRuleCreateRequestDto {

    /**
     * 알림 유형
     */
    @NotNull(message = "알림 유형은 필수입니다")
    private AlertType alertType;

    /**
     * 알림 레벨
     */
    @NotNull(message = "알림 레벨은 필수입니다")
    private AlertLevel alertLevel;

    /**
     * 알림 규칙 이름
     */
    @NotNull(message = "알림 규칙 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "알림 규칙 이름은 1~100자여야 합니다")
    private String ruleName;

    /**
     * 알림 조건
     */
    @NotNull(message = "알림 조건은 필수입니다")
    @Valid
    private AlertConditionDto condition;

    /**
     * 알림 규칙 설명
     */
    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다")
    private String description;

    /**
     * 활성화 여부 (기본값: true)
     */
    @Builder.Default
    private Boolean active = true;
}