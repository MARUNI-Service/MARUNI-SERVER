package com.anyang.maruni.domain.alertrule.application.dto;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
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
public class AlertDetectionResultDto {

    /**
     * 회원 ID
     */
    private Long memberId;

    /**
     * 감지된 이상징후 개수
     */
    private Integer detectedCount;

    /**
     * 최고 위험 레벨
     */
    private AlertLevel highestAlertLevel;

    /**
     * 감지된 이상징후 목록
     */
    private List<DetectedAlertDto> detectedAlerts;

    /**
     * 전체 감지 요약 메시지
     */
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
    public static class DetectedAlertDto {
        /**
         * 알림 레벨
         */
        private AlertLevel alertLevel;

        /**
         * 알림 메시지
         */
        private String message;

        /**
         * 분석 상세 정보
         */
        private String analysisDetails;

        /**
         * AlertResult에서 변환
         */
        public static DetectedAlertDto from(AlertResult alertResult) {
            return DetectedAlertDto.builder()
                    .alertLevel(alertResult.getAlertLevel())
                    .message(alertResult.getMessage())
                    .analysisDetails(alertResult.getAnalysisDetails())
                    .build();
        }
    }
}