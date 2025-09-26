package com.anyang.maruni.domain.alertrule.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "알림 감지 조건")
public class AlertConditionDto {

    /**
     * 연속 일수 (감정패턴, 무응답 감지용)
     */
    @Min(value = 1, message = "연속 일수는 1일 이상이어야 합니다")
    @Schema(description = "연속 일수 (감정패턴/무응답 감지용)",
            example = "3",
            minimum = "1")
    private Integer consecutiveDays;

    /**
     * 임계값 (필요시 사용)
     */
    @Min(value = 0, message = "임계값은 0 이상이어야 합니다")
    @Schema(description = "임계값 (감지 기준값)",
            example = "2",
            minimum = "0")
    private Integer thresholdCount;

    /**
     * 키워드 목록 (키워드 감지용, 쉼표로 구분)
     */
    @Size(max = 500, message = "키워드는 500자를 초과할 수 없습니다")
    @Schema(description = "감지할 키워드 목록 (쉼표로 구분)",
            example = "도와주세요,아파요,우울해",
            maxLength = 500)
    private String keywords;

    /**
     * 알림 조건 설명
     */
    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다")
    @Schema(description = "알림 조건에 대한 상세 설명",
            example = "부정적 감정이 3일 연속 감지될 경우",
            maxLength = 255)
    private String description;
}