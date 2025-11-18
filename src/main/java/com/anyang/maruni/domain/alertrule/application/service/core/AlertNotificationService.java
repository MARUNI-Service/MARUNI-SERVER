package com.anyang.maruni.domain.alertrule.application.service.core;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.application.service.util.AlertServiceUtils;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.domain.notification.domain.vo.NotificationType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationSourceType;

import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 처리 전담 서비스
 *
 * 기존 AlertRuleService에서 알림 발송 관련 로직만 분리하여 SRP 준수
 * 보호자 알림 발송과 발송 결과 처리에만 집중
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertNotificationService {

    private final NotificationHistoryService notificationHistoryService;
    private final AlertConfigurationProperties alertConfig;
    private final AlertServiceUtils alertServiceUtils;
    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * 알림 발생 처리
     *
     * @param memberId 회원 ID
     * @param alertResult 알림 결과
     * @return 생성된 알림 이력 ID
     */
    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        // 1. 회원 조회
        MemberEntity member = alertServiceUtils.validateAndGetMember(memberId);

        // 2. AlertHistory 생성 및 저장 (MVP: AlertRule 없이 생성)
        AlertHistory alertHistory = createAlertHistoryForMVP(member, alertResult);
        AlertHistory savedHistory = alertHistoryRepository.save(alertHistory);

        // 3. 보호자 알림 발송 트리거 (MVP: AlertHistory ID 전달)
        sendGuardianNotificationWithType(
            memberId,
            alertResult.getAlertLevel(),
            alertResult.getMessage(),
            alertResult.getAlertType(),
            savedHistory.getId()
        );

        return savedHistory.getId();
    }

    /**
     * MVP용 AlertHistory 생성 (AlertRule 없이)
     */
    private AlertHistory createAlertHistoryForMVP(MemberEntity member, AlertResult alertResult) {
        // 알림 결과를 JSON 형태로 저장할 상세 정보 구성
        String detectionDetails = alertServiceUtils.createDetectionDetailsJson(alertResult);

        // 키워드 감지는 실시간으로 여러 번 기록 가능 (정확한 시간)
        // 감정 패턴, 무응답은 하루에 한 번만 (날짜만)
        LocalDateTime alertDate = alertResult.getAlertType() == AlertType.KEYWORD_DETECTION
                ? LocalDateTime.now()  // 키워드: 정확한 시간으로 저장 (중복 허용)
                : LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);  // 기타: 날짜만 (중복 방지)

        // AlertHistory 빌더를 사용하여 직접 생성 (MVP용)
        return AlertHistory.builder()
                .alertRule(null) // MVP에서는 AlertRule 없이 생성
                .member(member)
                .alertLevel(alertResult.getAlertLevel())
                .alertType(alertResult.getAlertType()) // ⭐ alertType 추가 (중복 방지용)
                .alertMessage(alertResult.getMessage())
                .detectionDetails(detectionDetails)
                .alertDate(alertDate)
                .isNotificationSent(false)
                .build();
    }

    /**
     * 보호자들에게 알림 발송
     *
     * @param memberId 회원 ID
     * @param alertLevel 알림 레벨
     * @param alertMessage 알림 메시지
     */
    @Transactional
    public void sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage) {
        MemberEntity member = alertServiceUtils.validateAndGetMember(memberId);

        if (!hasGuardian(member)) {
            return;
        }

        performNotificationSending(member, alertLevel, alertMessage, memberId);
    }

    /**
     * 보호자들에게 알림 발송 (MVP: 타입 정보 포함)
     *
     * @param memberId 회원 ID
     * @param alertLevel 알림 레벨
     * @param alertMessage 알림 메시지
     * @param alertType 알림 타입 (EMOTION_PATTERN, NO_RESPONSE, KEYWORD)
     * @param alertHistoryId AlertHistory ID
     */
    @Transactional
    public void sendGuardianNotificationWithType(
            Long memberId,
            AlertLevel alertLevel,
            String alertMessage,
            AlertType alertType,
            Long alertHistoryId
    ) {
        MemberEntity member = alertServiceUtils.validateAndGetMember(memberId);

        if (!hasGuardian(member)) {
            return;
        }

        performNotificationSendingWithType(member, alertLevel, alertMessage, alertType, alertHistoryId, memberId);
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

    /**
     * 보호자 존재 여부 확인
     */
    private boolean hasGuardian(MemberEntity member) {
        return member.getGuardian() != null;
    }

    /**
     * 실제 알림 발송 수행
     */
    private void performNotificationSending(MemberEntity member, AlertLevel alertLevel,
                                           String alertMessage, Long memberId) {
        String alertTitle = String.format(alertConfig.getNotification().getTitleTemplate(), alertLevel.name());

        try {
            var notificationHistory = notificationHistoryService.recordNotification(
                    member.getGuardian().getId(),
                    alertTitle,
                    alertMessage
            );

            handleNotificationResult(memberId, notificationHistory != null, null);
        } catch (Exception e) {
            handleNotificationResult(memberId, false, e.getMessage());
        }
    }

    /**
     * 실제 알림 발송 수행 (MVP: 타입 정보 포함)
     */
    private void performNotificationSendingWithType(
            MemberEntity member,
            AlertLevel alertLevel,
            String alertMessage,
            AlertType alertType,
            Long alertHistoryId,
            Long memberId
    ) {
        String alertTitle = String.format(alertConfig.getNotification().getTitleTemplate(), alertLevel.name());

        // AlertType → NotificationType 매핑
        NotificationType notificationType = mapAlertTypeToNotificationType(alertType);

        try {
            var notificationHistory = notificationHistoryService.recordNotificationWithType(
                    member.getGuardian().getId(),
                    alertTitle,
                    alertMessage,
                    notificationType,
                    NotificationSourceType.ALERT_RULE,
                    alertHistoryId
            );

            handleNotificationResult(memberId, notificationHistory != null, null);
        } catch (Exception e) {
            handleNotificationResult(memberId, false, e.getMessage());
        }
    }

    /**
     * AlertType을 NotificationType으로 매핑
     */
    private NotificationType mapAlertTypeToNotificationType(AlertType alertType) {
        return switch (alertType) {
            case EMOTION_PATTERN -> NotificationType.EMOTION_ALERT;
            case NO_RESPONSE -> NotificationType.NO_RESPONSE_ALERT;
            case KEYWORD_DETECTION -> NotificationType.KEYWORD_ALERT;
            default -> NotificationType.SYSTEM; // HEALTH_CONCERN, EMERGENCY 등 기타 타입
        };
    }

    /**
     * 알림 발송 결과 처리 (기존 AlertServiceUtils에서 이동)
     */
    private void handleNotificationResult(Long memberId, boolean success, String errorMessage) {
        if (!success) {
            System.err.printf(alertConfig.getNotification().getNotificationFailureLog() + "%n", memberId);
            if (errorMessage != null) {
                System.err.printf(alertConfig.getNotification().getNotificationErrorLog() + "%n", errorMessage);
            }
        }
    }
}