package com.anyang.maruni.domain.alertrule.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 조건 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConditionDto {

    /**
     * 연속 일수 (감정패턴, 무응답 감지용)
     */
    @Min(value = 1, message = "연속 일수는 1일 이상이어야 합니다")
    private Integer consecutiveDays;

    /**
     * 임계값 (필요시 사용)
     */
    @Min(value = 0, message = "임계값은 0 이상이어야 합니다")
    private Integer thresholdCount;

    /**
     * 키워드 목록 (키워드 감지용, 쉼표로 구분)
     */
    @Size(max = 500, message = "키워드는 500자를 초과할 수 없습니다")
    private String keywords;

    /**
     * 알림 조건 설명
     */
    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다")
    private String description;
}