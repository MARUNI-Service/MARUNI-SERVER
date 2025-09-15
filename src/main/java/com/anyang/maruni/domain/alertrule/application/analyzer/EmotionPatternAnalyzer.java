package com.anyang.maruni.domain.alertrule.application.analyzer;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 감정 패턴 분석기
 *
 * 연속적인 부정적 감정을 감지하여 위험도를 평가합니다.
 */
@Component
@RequiredArgsConstructor
public class EmotionPatternAnalyzer {

    // 위험도 평가 임계값
    private static final int HIGH_RISK_CONSECUTIVE_DAYS = 3;
    private static final double HIGH_RISK_NEGATIVE_RATIO = 0.7;
    private static final int MEDIUM_RISK_CONSECUTIVE_DAYS = 2;
    private static final double MEDIUM_RISK_NEGATIVE_RATIO = 0.5;

    private final MessageRepository messageRepository;

    /**
     * 회원의 감정 패턴 분석
     * @param member 분석 대상 회원
     * @param analysisDays 분석 기간 (일)
     * @return 감정 패턴 분석 결과
     */
    public AlertResult analyzeEmotionPattern(MemberEntity member, int analysisDays) {
        // 1. 최근 N일간 사용자 메시지 조회
        LocalDateTime startDate = LocalDateTime.now().minusDays(analysisDays);
        List<MessageEntity> recentMessages = messageRepository.findRecentUserMessagesByMemberId(
                member.getId(), MessageType.USER_MESSAGE, startDate);

        if (recentMessages.isEmpty()) {
            return AlertResult.noAlert();
        }

        // 2. 감정 패턴 분석
        EmotionTrend emotionTrend = calculateEmotionTrend(recentMessages);

        // 3. 위험도 판정
        return evaluateRiskLevel(emotionTrend);
    }

    /**
     * 감정 추세 계산 (내부 메서드)
     * @param messages 메시지 목록
     * @return 감정 추세 정보
     */
    private EmotionTrend calculateEmotionTrend(List<MessageEntity> messages) {
        int totalMessages = messages.size();

        // 감정별 개수 계산
        Map<EmotionType, Long> emotionCounts = messages.stream()
                .collect(Collectors.groupingBy(MessageEntity::getEmotion, Collectors.counting()));

        int positiveCount = emotionCounts.getOrDefault(EmotionType.POSITIVE, 0L).intValue();
        int negativeCount = emotionCounts.getOrDefault(EmotionType.NEGATIVE, 0L).intValue();
        int neutralCount = emotionCounts.getOrDefault(EmotionType.NEUTRAL, 0L).intValue();

        // 부정 감정 비율 계산
        double negativeRatio = totalMessages > 0 ? (double) negativeCount / totalMessages : 0.0;

        // 연속적인 부정 감정 일수 계산 (최신 메시지부터)
        int consecutiveNegativeDays = calculateConsecutiveNegativeDays(messages);

        return new EmotionTrend(totalMessages, positiveCount, negativeCount, neutralCount,
                               consecutiveNegativeDays, negativeRatio);
    }

    /**
     * 연속적인 부정 감정 일수 계산
     * @param messages 메시지 목록 (최신순 정렬)
     * @return 연속적인 부정 감정 일수
     */
    private int calculateConsecutiveNegativeDays(List<MessageEntity> messages) {
        int consecutiveDays = 0;
        LocalDateTime currentDay = null;

        for (MessageEntity message : messages) {
            LocalDateTime messageDay = message.getCreatedAt().toLocalDate().atStartOfDay();

            if (message.getEmotion() == EmotionType.NEGATIVE) {
                if (currentDay == null || !messageDay.equals(currentDay)) {
                    consecutiveDays++;
                    currentDay = messageDay;
                }
            } else {
                // 부정 감정이 아닌 메시지가 나오면 연속성이 끊어짐
                break;
            }
        }

        return consecutiveDays;
    }

    /**
     * 위험도 평가
     * @param emotionTrend 감정 추세
     * @return 알림 결과
     */
    private AlertResult evaluateRiskLevel(EmotionTrend emotionTrend) {
        int consecutiveNegativeDays = emotionTrend.getConsecutiveNegativeDays();
        double negativeRatio = emotionTrend.getNegativeRatio();

        // 고위험: 연속 부정감정 + 부정비율 기준 초과
        if (consecutiveNegativeDays >= HIGH_RISK_CONSECUTIVE_DAYS && negativeRatio >= HIGH_RISK_NEGATIVE_RATIO) {
            String message = String.format("%d일 연속 부정감정 감지 (부정비율: %.1f%%)",
                    consecutiveNegativeDays, negativeRatio * 100);
            return AlertResult.createAlert(AlertLevel.HIGH, message, emotionTrend);
        }

        // 중위험: 연속 부정감정 + 부정비율 기준 초과
        if (consecutiveNegativeDays >= MEDIUM_RISK_CONSECUTIVE_DAYS && negativeRatio >= MEDIUM_RISK_NEGATIVE_RATIO) {
            String message = String.format("%d일 연속 부정감정 감지 (부정비율: %.1f%%)",
                    consecutiveNegativeDays, negativeRatio * 100);
            return AlertResult.createAlert(AlertLevel.MEDIUM, message, emotionTrend);
        }

        // 저위험 또는 알림 없음
        return AlertResult.noAlert();
    }

    /**
     * 감정 추세 정보 VO
     */
    public static class EmotionTrend {
        private final int totalMessages;
        private final int positiveCount;
        private final int negativeCount;
        private final int neutralCount;
        private final int consecutiveNegativeDays;
        private final double negativeRatio;

        public EmotionTrend(int totalMessages, int positiveCount, int negativeCount,
                           int neutralCount, int consecutiveNegativeDays, double negativeRatio) {
            this.totalMessages = totalMessages;
            this.positiveCount = positiveCount;
            this.negativeCount = negativeCount;
            this.neutralCount = neutralCount;
            this.consecutiveNegativeDays = consecutiveNegativeDays;
            this.negativeRatio = negativeRatio;
        }

        // Getter 메서드들
        public int getTotalMessages() { return totalMessages; }
        public int getPositiveCount() { return positiveCount; }
        public int getNegativeCount() { return negativeCount; }
        public int getNeutralCount() { return neutralCount; }
        public int getConsecutiveNegativeDays() { return consecutiveNegativeDays; }
        public double getNegativeRatio() { return negativeRatio; }
    }
}