package com.anyang.maruni.domain.alertrule.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertCondition;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.RequiredArgsConstructor;

/**
 * 알림 규칙 서비스 (Facade 패턴)
 *
 * 기존 AlertRuleService의 모든 API를 유지하면서
 * 내부적으로는 새로 분리된 4개 서비스에게 위임하는 Facade 역할
 *
 * ✅ 100% API 호환성 보장
 * ✅ SRP 준수를 위한 내부 구조 개선
 * ✅ 기존 테스트 코드 동작 보장
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleService {

    // 새로 분리된 서비스들에게 위임
    private final AlertDetectionService detectionService;
    private final AlertRuleManagementService managementService;
    private final AlertNotificationService notificationService;
    private final AlertHistoryService historyService;

    // ========== 이상징후 감지 관련 API (→ AlertDetectionService) ==========

    /**
     * 회원의 이상징후 종합 감지
     * @param memberId 회원 ID
     * @return 감지된 이상징후 목록
     */
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        return detectionService.detectAnomalies(memberId);
    }

    /**
     * 특정 메시지에 대한 실시간 키워드 감지
     * @param message 분석할 메시지
     * @param memberId 회원 ID
     * @return 감지 결과
     */
    @Transactional
    public AlertResult detectKeywordAlert(MessageEntity message, Long memberId) {
        return detectionService.detectKeywordAlert(message, memberId);
    }

    /**
     * 회원의 활성 알림 규칙 조회 (성능 최적화 버전)
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록 (Member, Guardian 포함)
     */
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        return detectionService.getActiveRulesByMemberId(memberId);
    }

    /**
     * 우선순위 기반으로 정렬된 활성 알림 규칙 조회
     * @param memberId 회원 ID
     * @return 우선순위 정렬된 활성 알림 규칙 목록
     */
    public List<AlertRule> getActiveRulesByMemberIdOrderedByPriority(Long memberId) {
        return detectionService.getActiveRulesByMemberIdOrderedByPriority(memberId);
    }

    // ========== 알림 규칙 CRUD 관련 API (→ AlertRuleManagementService) ==========

    /**
     * 알림 규칙 생성
     * @param member 회원
     * @param alertType 알림 유형
     * @param alertLevel 알림 레벨
     * @param condition 알림 조건
     * @return 생성된 알림 규칙
     */
    @Transactional
    public AlertRule createAlertRule(MemberEntity member, AlertType alertType,
                                   AlertLevel alertLevel, AlertCondition condition) {
        return managementService.createAlertRule(member, alertType, alertLevel, condition);
    }

    /**
     * 알림 규칙 조회 (단일, 성능 최적화 버전)
     * @param alertRuleId 알림 규칙 ID
     * @return 알림 규칙 (Member, Guardian 포함)
     */
    public AlertRule getAlertRuleById(Long alertRuleId) {
        return managementService.getAlertRuleById(alertRuleId);
    }

    /**
     * 알림 규칙 수정
     * @param alertRuleId 알림 규칙 ID
     * @param ruleName 새로운 규칙 이름
     * @param ruleDescription 새로운 규칙 설명
     * @param alertLevel 새로운 알림 레벨
     * @return 수정된 알림 규칙
     */
    @Transactional
    public AlertRule updateAlertRule(Long alertRuleId, String ruleName,
                                   String ruleDescription, AlertLevel alertLevel) {
        return managementService.updateAlertRule(alertRuleId, ruleName, ruleDescription, alertLevel);
    }

    /**
     * 알림 규칙 삭제
     * @param alertRuleId 알림 규칙 ID
     */
    @Transactional
    public void deleteAlertRule(Long alertRuleId) {
        managementService.deleteAlertRule(alertRuleId);
    }

    /**
     * 알림 규칙 활성화/비활성화
     * @param alertRuleId 알림 규칙 ID
     * @param active 활성화 상태
     * @return 업데이트된 알림 규칙
     */
    @Transactional
    public AlertRule toggleAlertRule(Long alertRuleId, boolean active) {
        return managementService.toggleAlertRule(alertRuleId, active);
    }

    // ========== 알림 발송 관련 API (→ AlertNotificationService) ==========

    /**
     * 알림 발생 처리
     * @param memberId 회원 ID
     * @param alertResult 알림 결과
     * @return 생성된 알림 이력 ID
     */
    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        return notificationService.triggerAlert(memberId, alertResult);
    }

    /**
     * 보호자들에게 알림 발송
     * @param memberId 회원 ID
     * @param alertLevel 알림 레벨
     * @param alertMessage 알림 메시지
     */
    @Transactional
    public void sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage) {
        notificationService.sendGuardianNotification(memberId, alertLevel, alertMessage);
    }

    // ========== 알림 이력 관련 API (→ AlertHistoryService) ==========

    /**
     * 알림 이력 기록
     * @param alertRule 알림 규칙
     * @param member 회원
     * @param alertResult 알림 결과
     * @return 저장된 알림 이력
     */
    @Transactional
    public AlertHistory recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult) {
        return historyService.recordAlertHistory(alertRule, member, alertResult);
    }

    /**
     * 회원별 최근 알림 이력 조회
     * @param memberId 회원 ID
     * @param days 조회 기간 (일)
     * @return 알림 이력 목록
     */
    public List<AlertHistory> getRecentAlertHistory(Long memberId, int days) {
        return historyService.getRecentAlertHistory(memberId, days);
    }
}