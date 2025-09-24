package com.anyang.maruni.domain.alertrule.application.dto.response;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 이상징후 감지 결과 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수동 이상징후 감지 결과")
public class AlertDetectionResultDto {

    /**
     * 회원 ID
     */
    @Schema(description = "감지 대상 회원 ID", example = "10")
    private Long memberId;

    /**
     * 감지된 이상징후 개수
     */
    @Schema(description = "감지된 이상징후의 총 개수", example = "2")
    private Integer detectedCount;

    /**
     * 최고 위험 레벨
     */
    @Schema(description = "감진된 이상징후 중 가장 높은 위험 레벨",
            example = "HIGH")
    private AlertLevel highestAlertLevel;

    /**
     * 감지된 이상징후 목록
     */
    @Schema(description = "감지된 각 이상징후의 상세 정보 목록")
    private List<DetectedAlertDto> detectedAlerts;

    /**
     * 전체 감지 요약 메시지
     */
    @Schema(description = "감지 결과에 대한 전체 요약",
            example = "2개의 이상징후가 감지되었습니다. (최고 위험도: HIGH)")
    private String summaryMessage;

    /**
     * AlertResult 목록에서 DTO로 변환하는 정적 팩토리 메서드
     * @param memberId 회원 ID
     * @param alertResults 감지 결과 목록
     * @return AlertDetectionResultDto
     */
    public static AlertDetectionResultDto from(Long memberId, List<AlertResult> alertResults) {
        if (alertResults.isEmpty()) {
            return AlertDetectionResultDto.builder()
                    .memberId(memberId)
                    .detectedCount(0)
                    .highestAlertLevel(null)
                    .detectedAlerts(List.of())
                    .summaryMessage("이상징후가 감지되지 않았습니다.")
                    .build();
        }

        List<DetectedAlertDto> detectedAlerts = alertResults.stream()
                .map(DetectedAlertDto::from)
                .collect(Collectors.toList());

        AlertLevel highestLevel = alertResults.stream()
                .map(AlertResult::getAlertLevel)
                .max((level1, level2) -> level1.ordinal() - level2.ordinal())
                .orElse(null);

        String summaryMessage = String.format("%d개의 이상징후가 감지되었습니다. (최고 위험도: %s)",
                alertResults.size(), highestLevel != null ? highestLevel.name() : "알 수 없음");

        return AlertDetectionResultDto.builder()
                .memberId(memberId)
                .detectedCount(alertResults.size())
                .highestAlertLevel(highestLevel)
                .detectedAlerts(detectedAlerts)
                .summaryMessage(summaryMessage)
                .build();
    }

    /**
     * 개별 감지 알림 정보 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "감지된 개별 알림 정보")
    public static class DetectedAlertDto {
        /**
         * 알림 레벨
         */
        @Schema(description = "알림 레벨", example = "HIGH")
        private AlertLevel alertLevel;

        /**
         * 알림 메시지
         */
        @Schema(description = "알림 메시지",
                example = "부정적 감정이 3일 연속 감지되었습니다")
        private String message;

        /**
         * 분석 상세 정보
         */
        @Schema(description = "이상징후 분석에 대한 상세 정보",
                example = "3일 연속 부정감정 70% 감지")
        private String analysisDetails;

        /**
         * AlertResult에서 변환
         */
        public static DetectedAlertDto from(AlertResult alertResult) {
            return DetectedAlertDto.builder()
                    .alertLevel(alertResult.getAlertLevel())
                    .message(alertResult.getMessage())
                    .analysisDetails(alertResult.getAnalysisDetails() != null ?
                        alertResult.getAnalysisDetails().toString() : "상세 분석 정보 없음")
                    .build();
        }
    }
}