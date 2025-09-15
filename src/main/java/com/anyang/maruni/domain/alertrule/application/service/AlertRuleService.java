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

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
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
        // 1. 회원 조회
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원: " + memberId));

        // 2. 회원의 모든 활성 알림 규칙 조회
        List<AlertRule> activeRules = alertRuleRepository.findActiveRulesByMemberId(memberId);

        // 3. 각 규칙별 이상징후 감지 실행
        List<AlertResult> detectedAnomalies = new ArrayList<>();

        for (AlertRule rule : activeRules) {
            AlertResult analysisResult = analyzeByRuleType(member, rule);

            if (analysisResult != null && analysisResult.isAlert()) {
                detectedAnomalies.add(analysisResult);
            }
        }

        return detectedAnomalies;
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
                return emotionAnalyzer.analyzeEmotionPattern(member, 7); // 7일간 분석
            case NO_RESPONSE:
                return noResponseAnalyzer.analyzeNoResponsePattern(member, 7); // 7일간 분석
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
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원: " + memberId));

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
        String detectionDetails = String.format("{\"alertLevel\":\"%s\",\"analysisDetails\":\"%s\"}",
                alertResult.getAlertLevel(), alertResult.getAnalysisDetails());

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
        // 1. 회원 조회
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원: " + memberId));

        // 2. 회원의 보호자 확인
        if (member.getGuardian() == null) {
            // 보호자가 없는 경우 로깅 후 종료
            return;
        }

        // 3. 알림 제목 구성
        String alertTitle = String.format("[MARUNI 알림] %s 단계 이상징후 감지", alertLevel.name());

        // 4. 보호자에게 알림 발송 시도
        try {
            boolean notificationSent = notificationService.sendPushNotification(
                    member.getGuardian().getId(),
                    alertTitle,
                    alertMessage
            );

            if (!notificationSent) {
                // 발송 실패 로깅 (실제로는 로거 사용)
                System.err.println("Guardian notification failed for member: " + memberId);
            }
        } catch (Exception e) {
            // 알림 발송 실패 처리 (서비스 계속 진행)
            System.err.println("Error sending guardian notification: " + e.getMessage());
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
        // 알림 규칙 조회
        AlertRule alertRule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림 규칙: " + alertRuleId));

        // 활성화/비활성화 처리
        if (active) {
            alertRule.activate();
        } else {
            alertRule.deactivate();
        }

        // JPA 더티 체킹으로 자동 업데이트
    }
}