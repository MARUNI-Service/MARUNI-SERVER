package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.EmotionPatternAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.KeywordAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.NoResponseAnalyzer;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.alertrule.domain.exception.AlertRuleNotFoundException;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // 분석 기간 설정
    private static final int DEFAULT_ANALYSIS_DAYS = 7;

    // 알림 메시지 템플릿
    private static final String GUARDIAN_ALERT_TITLE_TEMPLATE = "[MARUNI 알림] %s 단계 이상징후 감지";
    private static final String DETECTION_DETAILS_JSON_TEMPLATE = "{\"alertLevel\":\"%s\",\"analysisDetails\":\"%s\"}";

    // 로깅 메시지
    private static final String NOTIFICATION_FAILURE_LOG = "Guardian notification failed for member: %d";
    private static final String NOTIFICATION_ERROR_LOG = "Error sending guardian notification: %s";

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final EmotionPatternAnalyzer emotionAnalyzer;
    private final NoResponseAnalyzer noResponseAnalyzer;
    private final KeywordAnalyzer keywordAnalyzer;

    /**
     * 회원 검증 및 조회 공통 메서드
     * @param memberId 회원 ID
     * @return 검증된 회원 엔티티
     */
    private MemberEntity validateAndGetMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원: " + memberId));
    }

    /**
     * 감지 상세 정보 JSON 생성 공통 메서드
     * @param alertResult 알림 결과
     * @return JSON 형태의 상세 정보
     */
    private String createDetectionDetailsJson(AlertResult alertResult) {
        return String.format(DETECTION_DETAILS_JSON_TEMPLATE,
                alertResult.getAlertLevel(), alertResult.getAnalysisDetails());
    }

    /**
     * 알림 발송 결과 처리 공통 메서드
     * @param memberId 회원 ID
     * @param success 발송 성공 여부
     * @param errorMessage 오류 메시지 (실패시)
     */
    private void handleNotificationResult(Long memberId, boolean success, String errorMessage) {
        if (!success) {
            System.err.printf(NOTIFICATION_FAILURE_LOG + "%n", memberId);
            if (errorMessage != null) {
                System.err.printf(NOTIFICATION_ERROR_LOG + "%n", errorMessage);
            }
        }
    }

    /**
     * 회원의 이상징후 종합 감지
     * @param memberId 회원 ID
     * @return 감지된 이상징후 목록
     */
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        MemberEntity member = validateAndGetMember(memberId);
        List<AlertRule> activeRules = alertRuleRepository.findActiveRulesByMemberId(memberId);

        return processAlertRules(member, activeRules);
    }

    /**
     * 활성 알림 규칙들 처리
     * @param member 회원
     * @param activeRules 활성 알림 규칙 목록
     * @return 감지된 이상징후 목록
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
     * @param analysisResult 분석 결과
     * @return 알림 발생 여부
     */
    private boolean isAlertTriggered(AlertResult analysisResult) {
        return analysisResult != null && analysisResult.isAlert();
    }

    /**
     * 알림 규칙 타입별 이상징후 분석
     * @param member 회원
     * @param rule 알림 규칙
     * @return 분석 결과 (알림 없을 시 null)
     */
    private AlertResult analyzeByRuleType(MemberEntity member, AlertRule rule) {
        switch (rule.getAlertType()) {
            case EMOTION_PATTERN:
                return emotionAnalyzer.analyzeEmotionPattern(member, DEFAULT_ANALYSIS_DAYS);
            case NO_RESPONSE:
                return noResponseAnalyzer.analyzeNoResponsePattern(member, DEFAULT_ANALYSIS_DAYS);
            case KEYWORD_DETECTION:
                // 키워드 감지는 실시간 처리이므로 여기서는 제외
                return null;
            default:
                return null;
        }
    }

    /**
     * 특정 메시지에 대한 실시간 키워드 감지
     * @param message 분석할 메시지
     * @param memberId 회원 ID
     * @return 감지 결과
     */
    @Transactional
    public AlertResult detectKeywordAlert(MessageEntity message, Long memberId) {
        // KeywordAnalyzer를 사용하여 메시지 분석
        AlertResult keywordResult = keywordAnalyzer.analyzeKeywordRisk(message);

        // 키워드가 감지된 경우, 즉시 알림 처리 고려할 수 있음
        if (keywordResult.isAlert() && keywordResult.getAlertLevel() == AlertLevel.EMERGENCY) {
            // 긴급 키워드의 경우 즉시 처리 로직 추가 가능
            // 현재는 단순히 결과만 반환
        }

        return keywordResult;
    }

    /**
     * 알림 발생 처리
     * @param memberId 회원 ID
     * @param alertResult 알림 결과
     * @return 생성된 알림 이력 ID
     */
    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        // 1. 회원 조회
        MemberEntity member = validateAndGetMember(memberId);

        // 2. AlertHistory 생성 및 저장 (MVP: AlertRule 없이 생성)
        AlertHistory alertHistory = createAlertHistoryForMVP(member, alertResult);
        AlertHistory savedHistory = alertHistoryRepository.save(alertHistory);

        // 3. 보호자 알림 발송 트리거
        sendGuardianNotification(memberId, alertResult.getAlertLevel(), alertResult.getMessage());

        return savedHistory.getId();
    }

    /**
     * MVP용 AlertHistory 생성 (AlertRule 없이)
     * @param member 회원
     * @param alertResult 알림 결과
     * @return AlertHistory
     */
    private AlertHistory createAlertHistoryForMVP(MemberEntity member, AlertResult alertResult) {
        // 알림 결과를 JSON 형태로 저장할 상세 정보 구성
        String detectionDetails = createDetectionDetailsJson(alertResult);

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
     * @param memberId 회원 ID
     * @param alertLevel 알림 레벨
     * @param alertMessage 알림 메시지
     */
    @Transactional
    public void sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage) {
        MemberEntity member = validateAndGetMember(memberId);

        if (!hasGuardian(member)) {
            return;
        }

        performNotificationSending(member, alertLevel, alertMessage, memberId);
    }

    /**
     * 보호자 존재 여부 확인
     * @param member 회원 엔티티
     * @return 보호자 존재 여부
     */
    private boolean hasGuardian(MemberEntity member) {
        return member.getGuardian() != null;
    }

    /**
     * 실제 알림 발송 수행
     * @param member 회원 엔티티
     * @param alertLevel 알림 레벨
     * @param alertMessage 알림 메시지
     * @param memberId 회원 ID (로깅용)
     */
    private void performNotificationSending(MemberEntity member, AlertLevel alertLevel, String alertMessage, Long memberId) {
        String alertTitle = String.format(GUARDIAN_ALERT_TITLE_TEMPLATE, alertLevel.name());

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
     * 알림 이력 기록
     * @param alertRule 알림 규칙
     * @param member 회원
     * @param alertResult 알림 결과
     * @return 저장된 알림 이력
     */
    @Transactional
    public AlertHistory recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult) {
        // 알림 결과를 JSON 형태로 저장할 상세 정보 구성
        String detectionDetails = createDetectionDetailsJson(alertResult);

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
     * 회원의 활성 알림 규칙 조회 (성능 최적화 버전)
     * N+1 쿼리 문제 해결을 위해 JOIN FETCH 사용
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록 (Member, Guardian 포함)
     */
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        return alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId);
    }

    /**
     * 우선순위 기반으로 정렬된 활성 알림 규칙 조회 (성능 최적화)
     * @param memberId 회원 ID
     * @return 우선순위 정렬된 활성 알림 규칙 목록
     */
    public List<AlertRule> getActiveRulesByMemberIdOrderedByPriority(Long memberId) {
        return alertRuleRepository.findActiveRulesByMemberIdOrderedByPriority(memberId);
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
        AlertRule alertRule = createAlertRuleByType(member, alertType, alertLevel, condition);
        return alertRuleRepository.save(alertRule);
    }

    /**
     * 알림 유형에 따른 알림 규칙 생성
     * @param member 회원
     * @param alertType 알림 유형
     * @param alertLevel 알림 레벨
     * @param condition 알림 조건
     * @return 생성된 알림 규칙
     */
    private AlertRule createAlertRuleByType(MemberEntity member, AlertType alertType, AlertLevel alertLevel, AlertCondition condition) {
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
    private AlertRule createEmotionPatternAlertRule(MemberEntity member, AlertLevel alertLevel, AlertCondition condition) {
        return AlertRule.createEmotionPatternRule(member, condition.getConsecutiveDays(), alertLevel);
    }

    /**
     * 무응답 패턴 알림 규칙 생성
     */
    private AlertRule createNoResponseAlertRule(MemberEntity member, AlertLevel alertLevel, AlertCondition condition) {
        return AlertRule.createNoResponseRule(member, condition.getConsecutiveDays(), alertLevel);
    }

    /**
     * 키워드 감지 알림 규칙 생성
     */
    private AlertRule createKeywordAlertRule(MemberEntity member, AlertLevel alertLevel, AlertCondition condition) {
        return AlertRule.createKeywordRule(member, condition.getKeywords(), alertLevel);
    }

    /**
     * 알림 규칙 조회 (단일, 성능 최적화 버전)
     * N+1 쿼리 문제 해결을 위해 JOIN FETCH 사용
     * @param alertRuleId 알림 규칙 ID
     * @return 알림 규칙 (Member, Guardian 포함)
     */
    public AlertRule getAlertRuleById(Long alertRuleId) {
        return alertRuleRepository.findByIdWithMemberAndGuardian(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));
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
    public AlertRule updateAlertRule(Long alertRuleId, String ruleName, String ruleDescription, AlertLevel alertLevel) {
        AlertRule alertRule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));

        alertRule.updateRule(ruleName, ruleDescription, alertLevel);

        // JPA 더티 체킹으로 자동 업데이트
        return alertRule;
    }

    /**
     * 알림 규칙 삭제
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
}