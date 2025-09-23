package com.anyang.maruni.domain.alertrule.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.AnalysisContext;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.application.service.AlertAnalysisOrchestrator;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.RequiredArgsConstructor;

/**
 * 이상징후 감지 전담 서비스
 *
 * 기존 AlertRuleService에서 감지 관련 로직만 분리하여 SRP 준수
 * 감지 알고리즘 실행과 결과 반환에만 집중
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertDetectionService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertAnalysisOrchestrator analysisOrchestrator;
    private final AlertConfigurationProperties alertConfig;

    /**
     * 회원의 이상징후 종합 감지
     *
     * @param memberId 회원 ID
     * @return 감지된 이상징후 목록
     */
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 특정 메시지에 대한 실시간 키워드 감지
     *
     * @param message 분석할 메시지
     * @param memberId 회원 ID
     * @return 감지 결과
     */
    @Transactional
    public AlertResult detectKeywordAlert(MessageEntity message, Long memberId) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 회원의 활성 알림 규칙 조회 (성능 최적화 버전)
     *
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록 (Member, Guardian 포함)
     */
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 우선순위 기반으로 정렬된 활성 알림 규칙 조회
     *
     * @param memberId 회원 ID
     * @return 우선순위 정렬된 활성 알림 규칙 목록
     */
    public List<AlertRule> getActiveRulesByMemberIdOrderedByPriority(Long memberId) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

    /**
     * 활성 알림 규칙들 처리
     */
    private List<AlertResult> processAlertRules(MemberEntity member, List<AlertRule> activeRules) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 알림 발생 여부 판단
     */
    private boolean isAlertTriggered(AlertResult analysisResult) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 알림 규칙 타입별 이상징후 분석
     */
    private AlertResult analyzeByRuleType(MemberEntity member, AlertRule rule) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 알림 타입에 맞는 분석 컨텍스트 생성
     */
    private AnalysisContext createAnalysisContext(AlertType alertType, int defaultDays) {
        // TODO: Phase 2에서 기존 AlertRuleService에서 이동 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }
}