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
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.alertrule.application.util.AlertServiceUtils;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    private final AlertServiceUtils alertServiceUtils;

    /**
     * 회원의 이상징후 종합 감지
     *
     * @param memberId 회원 ID
     * @return 감지된 이상징후 목록
     */
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        MemberEntity member = alertServiceUtils.validateAndGetMember(memberId);
        List<AlertRule> activeRules = alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId);

        return processAlertRules(member, activeRules);
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
        MemberEntity member = alertServiceUtils.validateAndGetMember(memberId);

        // Strategy Pattern을 사용한 키워드 분석
        AnalysisContext context = AnalysisContext.forKeyword(message);
        AlertResult keywordResult = analysisOrchestrator.analyzeByType(AlertType.KEYWORD_DETECTION, member, context);

        // 키워드가 감지된 경우, 즉시 알림 처리 고려할 수 있음
        if (keywordResult.isAlert() && keywordResult.getAlertLevel() == AlertLevel.EMERGENCY) {
            // 긴급 키워드의 경우 즉시 처리 로직 추가 가능
            // 현재는 단순히 결과만 반환
        }

        return keywordResult;
    }

    /**
     * 회원의 활성 알림 규칙 조회 (성능 최적화 버전)
     *
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록 (Member, Guardian 포함)
     */
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        return alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId);
    }

    /**
     * 우선순위 기반으로 정렬된 활성 알림 규칙 조회
     *
     * @param memberId 회원 ID
     * @return 우선순위 정렬된 활성 알림 규칙 목록
     */
    public List<AlertRule> getActiveRulesByMemberIdOrderedByPriority(Long memberId) {
        List<AlertRule> rules = alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId);

        return rules.stream()
                .sorted(Comparator.comparing(AlertRule::getAlertLevel, AlertLevel.descendingComparator())
                    .thenComparing(AlertRule::getAlertType)
                    .thenComparing(AlertRule::getCreatedAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

    /**
     * 활성 알림 규칙들 처리
     */
    private List<AlertResult> processAlertRules(MemberEntity member, List<AlertRule> activeRules) {
        List<AlertResult> detectedAnomalies = new ArrayList<>();

        for (AlertRule rule : activeRules) {
            AlertResult analysisResult = analyzeByRuleType(member, rule);

            if (isAlertTriggered(analysisResult)) {
                detectedAnomalies.add(analysisResult);
            }
        }

        return detectedAnomalies;
    }

    /**
     * 알림 발생 여부 판단
     */
    private boolean isAlertTriggered(AlertResult analysisResult) {
        return analysisResult != null && analysisResult.isAlert();
    }

    /**
     * 알림 규칙 타입별 이상징후 분석
     */
    private AlertResult analyzeByRuleType(MemberEntity member, AlertRule rule) {
        AlertType alertType = rule.getAlertType();

        // 키워드 감지는 실시간 처리이므로 종합 분석에서는 제외
        if (alertType == AlertType.KEYWORD_DETECTION) {
            return AlertResult.noAlert();
        }

        // Strategy Pattern을 사용한 분석
        if (analysisOrchestrator.isSupported(alertType)) {
            AnalysisContext context = createAnalysisContext(alertType, alertConfig.getAnalysis().getDefaultDays());
            return analysisOrchestrator.analyzeByType(alertType, member, context);
        }

        // 지원하지 않는 타입인 경우
        return AlertResult.noAlert();
    }

    /**
     * 알림 타입에 맞는 분석 컨텍스트 생성
     */
    private AnalysisContext createAnalysisContext(AlertType alertType, int defaultDays) {
        switch (alertType) {
            case EMOTION_PATTERN:
                return AnalysisContext.forEmotionPattern(defaultDays);
            case NO_RESPONSE:
                return AnalysisContext.forNoResponse(defaultDays);
            case KEYWORD_DETECTION:
                // 키워드 분석은 실시간 메시지가 필요하므로 여기서는 빈 컨텍스트
                return AnalysisContext.builder().build();
            default:
                return AnalysisContext.builder().analysisDays(defaultDays).build();
        }
    }
}