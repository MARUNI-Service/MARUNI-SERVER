package com.anyang.maruni.domain.alertrule.application.service.core;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.application.service.util.AlertServiceUtils;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;

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

    private final NotificationService notificationService;
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

        // 3. 보호자 알림 발송 트리거
        sendGuardianNotification(memberId, alertResult.getAlertLevel(), alertResult.getMessage());

        return savedHistory.getId();
    }

    /**
     * MVP용 AlertHistory 생성 (AlertRule 없이)
     */
    private AlertHistory createAlertHistoryForMVP(MemberEntity member, AlertResult alertResult) {
        // 알림 결과를 JSON 형태로 저장할 상세 정보 구성
        String detectionDetails = alertServiceUtils.createDetectionDetailsJson(alertResult);

        // AlertHistory 빌더를 사용하여 직접 생성 (MVP용)
        return AlertHistory.builder()
                .alertRule(null) // MVP에서는 AlertRule 없이 생성
                .member(member)
                .alertLevel(alertResult.getAlertLevel())
                .alertMessage(alertResult.getMessage())
                .detectionDetails(detectionDetails)
                .alertDate(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0))
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
            boolean notificationSent = notificationService.sendPushNotification(
                    member.getGuardian().getId(),
                    alertTitle,
                    alertMessage
            );

            handleNotificationResult(memberId, notificationSent, null);
        } catch (Exception e) {
            handleNotificationResult(memberId, false, e.getMessage());
        }
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