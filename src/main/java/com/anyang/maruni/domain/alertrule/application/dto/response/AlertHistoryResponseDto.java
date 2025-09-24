package com.anyang.maruni.domain.alertrule.application.dto.response;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 이력 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "알림 이력 정보")
public class AlertHistoryResponseDto {

    /**
     * 알림 이력 ID
     */
    @Schema(description = "알림 이력 고유 식별자", example = "1")
    private Long id;

    /**
     * 알림 규칙 ID
     */
    @Schema(description = "알림을 발생시킨 규칙 ID", example = "5")
    private Long alertRuleId;

    /**
     * 회원 ID
     */
    @Schema(description = "알림 대상 회원 ID", example = "10")
    private Long memberId;

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
    private String alertMessage;

    /**
     * 감지 상세 정보
     */
    @Schema(description = "감지된 이상징후에 대한 상세 정보 (JSON 형태)",
            example = "{\"alertLevel\":\"HIGH\",\"analysisDetails\":\"3일 연속 부정감정 70%\"}")
    private String detectionDetails;

    /**
     * 알림 발송 여부
     */
    @Schema(description = "보호자에게 알림 발송 여부", example = "true")
    private Boolean isNotificationSent;

    /**
     * 알림 발송 시간
     */
    @Schema(description = "알림 발송이 완료된 날짜와 시간",
            example = "2025-09-25T14:35:00")
    private LocalDateTime notificationSentAt;

    /**
     * 알림 발송 결과
     */
    @Schema(description = "알림 발송 결과 메시지",
            example = "보호자에게 알림 전송 성공")
    private String notificationResult;

    /**
     * 알림 날짜
     */
    @Schema(description = "알림이 발생한 날짜 (중복 방지용)",
            example = "2025-09-25T14:30:00")
    private LocalDateTime alertDate;

    /**
     * 생성일시
     */
    @Schema(description = "이력 레코드 생성 날짜와 시간",
            example = "2025-09-25T14:30:00")
    private LocalDateTime createdAt;

    /**
     * Entity에서 DTO로 변환하는 정적 팩토리 메서드
     * @param alertHistory 알림 이력 엔티티
     * @return AlertHistoryResponseDto
     */
    public static AlertHistoryResponseDto from(AlertHistory alertHistory) {
        return AlertHistoryResponseDto.builder()
                .id(alertHistory.getId())
                .alertRuleId(alertHistory.getAlertRule() != null ? alertHistory.getAlertRule().getId() : null)
                .memberId(alertHistory.getMember().getId())
                .alertLevel(alertHistory.getAlertLevel())
                .alertMessage(alertHistory.getAlertMessage())
                .detectionDetails(alertHistory.getDetectionDetails())
                .isNotificationSent(alertHistory.getIsNotificationSent())
                .notificationSentAt(alertHistory.getNotificationSentAt())
                .notificationResult(alertHistory.getNotificationResult())
                .alertDate(alertHistory.getAlertDate())
                .createdAt(alertHistory.getCreatedAt())
                .build();
    }
}