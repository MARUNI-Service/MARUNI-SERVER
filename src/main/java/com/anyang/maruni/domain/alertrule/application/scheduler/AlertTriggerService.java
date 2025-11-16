package com.anyang.maruni.domain.alertrule.application.scheduler;

import java.util.List;

import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertDetectionService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertNotificationService;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 트리거 서비스
 *
 * AlertRule 호출을 전담하는 서비스 (SRP)
 * - 전체 회원 순회
 * - 예외 격리
 * - 성공/실패 카운트 추적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertTriggerService {

    private final AlertDetectionService alertDetectionService;
    private final AlertNotificationService alertNotificationService;
    private final MemberRepository memberRepository;

    /**
     * 전체 활성 회원 이상징후 감지 (예외 격리)
     *
     * Note: @Transactional 없음 - 각 회원 처리마다 독립적인 트랜잭션 사용
     *       (AlertDetectionService, AlertNotificationService가 각자 트랜잭션 관리)
     */
    public void detectAnomaliesForAllMembers() {
        List<Long> activeMemberIds = memberRepository.findDailyCheckEnabledMemberIds();
        int successCount = 0;
        int failureCount = 0;

        log.info("🔍 이상징후 감지 시작: 대상 회원 {}명", activeMemberIds.size());

        for (Long memberId : activeMemberIds) {
            try {
                detectAndNotifyForMember(memberId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ Member {}의 이상징후 감지 처리 실패", memberId, e);
                // Phase 3: 모니터링 시스템에 알림 (선택)
            }
        }

        log.info("✅ 이상징후 감지 완료: 성공 {}, 실패 {}", successCount, failureCount);
    }

    /**
     * 개별 회원 감지 및 알림 (private)
     */
    private void detectAndNotifyForMember(Long memberId) {
        // 1. 이상징후 감지 (NoResponse + EmotionPattern)
        List<AlertResult> results = alertDetectionService.detectAnomalies(memberId);

        // 2. 감지된 위험 신호 처리
        for (AlertResult result : results) {
            if (result.isAlert()) {
                alertNotificationService.triggerAlert(memberId, result);
                log.info("⚠️ Member {}에게 {} 알림 발송", memberId, result.getAlertLevel());
            }
        }
    }
}
