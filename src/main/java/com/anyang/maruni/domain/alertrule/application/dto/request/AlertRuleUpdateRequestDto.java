package com.anyang.maruni.domain.alertrule.application.dto.request;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
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
public class AlertRuleUpdateRequestDto {

    /**
     * 알림 레벨 (수정 가능)
     */
    private AlertLevel alertLevel;

    /**
     * 알림 규칙 이름 (수정 가능)
     */
    @Size(min = 1, max = 100, message = "알림 규칙 이름은 1~100자여야 합니다")
    private String ruleName;

    /**
     * 알림 조건 (수정 가능)
     */
    @Valid
    private AlertConditionDto condition;

    /**
     * 알림 규칙 설명 (수정 가능)
     */
    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다")
    private String description;

    /**
     * 활성화 여부 (수정 가능)
     */
    private Boolean active;
}