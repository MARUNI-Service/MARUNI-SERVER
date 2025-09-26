package com.anyang.maruni.domain.alertrule.application.service.core;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.service.util.AlertServiceUtils;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.RequiredArgsConstructor;

/**
 * 알림 이력 관리 전담 서비스
 *
 * 기존 AlertRuleService에서 이력 관리 관련 로직만 분리하여 SRP 준수
 * 알림 이력의 기록과 조회에만 집중
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertServiceUtils alertServiceUtils;

    /**
     * 알림 이력 기록
     *
     * @param alertRule 알림 규칙
     * @param member 회원
     * @param alertResult 알림 결과
     * @return 저장된 알림 이력
     */
    @Transactional
    public AlertHistory recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult) {
        // 알림 결과를 JSON 형태로 저장할 상세 정보 구성
        String detectionDetails = alertServiceUtils.createDetectionDetailsJson(alertResult);

        // AlertHistory 엔티티 생성
        AlertHistory alertHistory = AlertHistory.createAlert(
                alertRule,
                member,
                alertResult.getMessage(),
                detectionDetails
        );

        // 데이터베이스에 저장
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * 회원별 최근 알림 이력 조회
     *
     * @param memberId 회원 ID
     * @param days 조회 기간 (일)
     * @return 알림 이력 목록
     */
    public List<AlertHistory> getRecentAlertHistory(Long memberId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        return alertHistoryRepository.findByMemberIdAndDateRange(memberId, startDate, endDate);
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

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
}