package com.anyang.maruni.domain.alertrule.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
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

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final AlertConfigurationProperties alertConfig;

    /**
     * 알림 발생 처리
     *
     * @param memberId 회원 ID
     * @param alertResult 알림 결과
     * @return 생성된 알림 이력 ID
     */
    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
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
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

    /**
     * 보호자 존재 여부 확인
     */
    private boolean hasGuardian(MemberEntity member) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 실제 알림 발송 수행
     */
    private void performNotificationSending(MemberEntity member, AlertLevel alertLevel,
                                           String alertMessage, Long memberId) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 알림 발송 결과 처리 (기존 AlertServiceUtils에서 이동)
     */
    private void handleNotificationResult(Long memberId, boolean success, String errorMessage) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        // 알림 발송 전용 로직이므로 이 서비스의 private 메서드로 배치
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }
}