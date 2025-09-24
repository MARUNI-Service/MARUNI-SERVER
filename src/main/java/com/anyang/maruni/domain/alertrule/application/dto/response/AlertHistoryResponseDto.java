package com.anyang.maruni.domain.alertrule.application.dto.response;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
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
public class AlertHistoryResponseDto {

    /**
     * 알림 이력 ID
     */
    private Long id;

    /**
     * 알림 규칙 ID
     */
    private Long alertRuleId;

    /**
     * 회원 ID
     */
    private Long memberId;

    /**
     * 알림 레벨
     */
    private AlertLevel alertLevel;

    /**
     * 알림 메시지
     */
    private String alertMessage;

    /**
     * 감지 상세 정보
     */
    private String detectionDetails;

    /**
     * 알림 발송 여부
     */
    private Boolean isNotificationSent;

    /**
     * 알림 발송 시간
     */
    private LocalDateTime notificationSentAt;

    /**
     * 알림 발송 결과
     */
    private String notificationResult;

    /**
     * 알림 날짜
     */
    private LocalDateTime alertDate;

    /**
     * 생성일시
     */
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