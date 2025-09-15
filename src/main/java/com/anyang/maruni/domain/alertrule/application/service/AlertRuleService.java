package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.EmotionPatternAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.KeywordAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.NoResponseAnalyzer;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 규칙 서비스
 *
 * 이상징후 감지 및 알림 처리의 핵심 비즈니스 로직을 담당합니다.
 * TDD Red 단계: 더미 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final EmotionPatternAnalyzer emotionAnalyzer;
    private final NoResponseAnalyzer noResponseAnalyzer;
    private final KeywordAnalyzer keywordAnalyzer;

    /**
     * 회원의 이상징후 종합 감지
     * @param memberId 회원 ID
     * @return 감지된 이상징후 목록
     */
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        // TODO: TDD Red 단계 - 더미 구현
        // 실제 구현에서는:
        // 1. 회원의 모든 활성 알림 규칙 조회
        // 2. 각 규칙별 이상징후 감지 실행
        // 3. 감지된 이상징후를 AlertResult로 반환

        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 특정 메시지에 대한 실시간 키워드 감지
     * @param message 분석할 메시지
     * @param memberId 회원 ID
     * @return 감지 결과
     */
    @Transactional
    public AlertResult detectKeywordAlert(MessageEntity message, Long memberId) {
        // TODO: TDD Red 단계 - 더미 구현
        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 알림 발생 처리
     * @param memberId 회원 ID
     * @param alertResult 알림 결과
     * @return 생성된 알림 이력 ID
     */
    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        // TODO: TDD Red 단계 - 더미 구현
        // 실제 구현에서는:
        // 1. AlertHistory 생성
        // 2. 보호자 알림 발송 트리거
        // 3. 알림 이력 저장

        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 보호자들에게 알림 발송
     * @param memberId 회원 ID
     * @param alertLevel 알림 레벨
     * @param alertMessage 알림 메시지
     */
    @Transactional
    public void sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage) {
        // TODO: TDD Red 단계 - 더미 구현
        // 실제 구현에서는 Guardian 서비스와 연동

        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 알림 이력 기록
     * @param alertRule 알림 규칙
     * @param member 회원
     * @param alertResult 알림 결과
     * @return 저장된 알림 이력
     */
    @Transactional
    public AlertHistory recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult) {
        // 알림 결과를 JSON 형태로 저장할 상세 정보 구성
        String detectionDetails = String.format("{\"alertLevel\":\"%s\",\"analysisDetails\":\"%s\"}",
                alertResult.getAlertLevel(), alertResult.getAnalysisDetails());

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
     * 회원의 활성 알림 규칙 조회
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록
     */
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        return alertRuleRepository.findActiveRulesByMemberId(memberId);
    }

    /**
     * 회원별 최근 알림 이력 조회
     * @param memberId 회원 ID
     * @param days 조회 기간 (일)
     * @return 알림 이력 목록
     */
    public List<AlertHistory> getRecentAlertHistory(Long memberId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        return alertHistoryRepository.findByMemberIdAndDateRange(memberId, startDate, endDate);
    }

    /**
     * 알림 규칙 생성
     * @param member 회원
     * @param alertType 알림 유형
     * @param alertLevel 알림 레벨
     * @param condition 알림 조건
     * @return 생성된 알림 규칙
     */
    @Transactional
    public AlertRule createAlertRule(MemberEntity member, AlertType alertType, AlertLevel alertLevel, AlertCondition condition) {
        AlertRule alertRule;

        // AlertType에 따라 적절한 정적 팩토리 메서드 호출
        switch (alertType) {
            case EMOTION_PATTERN:
                alertRule = AlertRule.createEmotionPatternRule(member, condition.getConsecutiveDays(), alertLevel);
                break;
            case NO_RESPONSE:
                alertRule = AlertRule.createNoResponseRule(member, condition.getConsecutiveDays(), alertLevel);
                break;
            case KEYWORD_DETECTION:
                alertRule = AlertRule.createKeywordRule(member, condition.getKeywords(), alertLevel);
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 알림 유형: " + alertType);
        }

        // 데이터베이스에 저장
        return alertRuleRepository.save(alertRule);
    }

    /**
     * 알림 규칙 활성화/비활성화
     * @param alertRuleId 알림 규칙 ID
     * @param active 활성화 상태
     */
    @Transactional
    public void toggleAlertRule(Long alertRuleId, boolean active) {
        // TODO: TDD Red 단계 - 더미 구현
        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }
}