package com.anyang.maruni.domain.alertrule.application.service.core;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertCondition;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.alertrule.domain.exception.AlertRuleNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 알림 규칙 CRUD 관리 전담 서비스
 *
 * 기존 AlertRuleService에서 CRUD 관련 로직만 분리하여 SRP 준수
 * 알림 규칙의 생성, 조회, 수정, 삭제에만 집중
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleManagementService {

    private final AlertRuleRepository alertRuleRepository;
    private final MemberRepository memberRepository;

    /**
     * 알림 규칙 생성
     *
     * @param member 회원
     * @param alertType 알림 유형
     * @param alertLevel 알림 레벨
     * @param condition 알림 조건
     * @return 생성된 알림 규칙
     */
    @Transactional
    public AlertRule createAlertRule(MemberEntity member, AlertType alertType,
                                   AlertLevel alertLevel, AlertCondition condition) {
        AlertRule alertRule = createAlertRuleByType(member, alertType, alertLevel, condition);
        return alertRuleRepository.save(alertRule);
    }

    /**
     * 알림 규칙 조회 (단일, 성능 최적화 버전)
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 알림 규칙 (Member, Guardian 포함)
     */
    public AlertRule getAlertRuleById(Long alertRuleId) {
        return alertRuleRepository.findByIdWithMemberAndGuardian(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));
    }

    /**
     * 알림 규칙 수정
     *
     * @param alertRuleId 알림 규칙 ID
     * @param ruleName 새로운 규칙 이름
     * @param ruleDescription 새로운 규칙 설명
     * @param alertLevel 새로운 알림 레벨
     * @return 수정된 알림 규칙
     */
    @Transactional
    public AlertRule updateAlertRule(Long alertRuleId, String ruleName,
                                   String ruleDescription, AlertLevel alertLevel) {
        AlertRule alertRule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));

        alertRule.updateRule(ruleName, ruleDescription, alertLevel);

        // JPA 더티 체킹으로 자동 업데이트
        return alertRule;
    }

    /**
     * 알림 규칙 삭제
     *
     * @param alertRuleId 알림 규칙 ID
     */
    @Transactional
    public void deleteAlertRule(Long alertRuleId) {
        AlertRule alertRule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));

        alertRuleRepository.delete(alertRule);
    }

    /**
     * 알림 규칙 활성화/비활성화
     *
     * @param alertRuleId 알림 규칙 ID
     * @param active 활성화 상태
     * @return 업데이트된 알림 규칙
     */
    @Transactional
    public AlertRule toggleAlertRule(Long alertRuleId, boolean active) {
        AlertRule alertRule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));

        if (active) {
            alertRule.activate();
        } else {
            alertRule.deactivate();
        }

        // JPA 더티 체킹으로 자동 업데이트
        return alertRule;
    }

    // ========== Private 메서드들 (Phase 2에서 구현) ==========

    /**
     * 알림 유형에 따른 알림 규칙 생성
     */
    private AlertRule createAlertRuleByType(MemberEntity member, AlertType alertType,
                                          AlertLevel alertLevel, AlertCondition condition) {
        switch (alertType) {
            case EMOTION_PATTERN:
                return createEmotionPatternAlertRule(member, alertLevel, condition);
            case NO_RESPONSE:
                return createNoResponseAlertRule(member, alertLevel, condition);
            case KEYWORD_DETECTION:
                return createKeywordAlertRule(member, alertLevel, condition);
            default:
                throw new IllegalArgumentException("지원하지 않는 알림 유형: " + alertType);
        }
    }

    /**
     * 감정 패턴 알림 규칙 생성
     */
    private AlertRule createEmotionPatternAlertRule(MemberEntity member,
                                                  AlertLevel alertLevel, AlertCondition condition) {
        return AlertRule.createEmotionPatternRule(member, condition.getConsecutiveDays(), alertLevel);
    }

    /**
     * 무응답 패턴 알림 규칙 생성
     */
    private AlertRule createNoResponseAlertRule(MemberEntity member,
                                              AlertLevel alertLevel, AlertCondition condition) {
        return AlertRule.createNoResponseRule(member, condition.getConsecutiveDays(), alertLevel);
    }

    /**
     * 키워드 감지 알림 규칙 생성
     */
    private AlertRule createKeywordAlertRule(MemberEntity member,
                                           AlertLevel alertLevel, AlertCondition condition) {
        return AlertRule.createKeywordRule(member, condition.getKeywords(), alertLevel);
    }
}