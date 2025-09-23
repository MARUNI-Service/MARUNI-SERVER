package com.anyang.maruni.domain.alertrule.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

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
    private final MemberRepository memberRepository;

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
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 회원별 최근 알림 이력 조회
     *
     * @param memberId 회원 ID
     * @param days 조회 기간 (일)
     * @return 알림 이력 목록
     */
    public List<AlertHistory> getRecentAlertHistory(Long memberId, int days) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

    /**
     * MVP용 AlertHistory 생성 (AlertRule 없이)
     */
    private AlertHistory createAlertHistoryForMVP(MemberEntity member, AlertResult alertResult) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }
}