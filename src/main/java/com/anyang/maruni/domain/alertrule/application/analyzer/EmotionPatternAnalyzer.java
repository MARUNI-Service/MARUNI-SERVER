package com.anyang.maruni.domain.alertrule.application.analyzer;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 감정 패턴 분석기
 *
 * 연속적인 부정적 감정을 감지하여 위험도를 평가합니다.
 * TDD Red 단계: 더미 구현
 */
@Component
public class EmotionPatternAnalyzer {

    /**
     * 회원의 감정 패턴 분석
     * @param member 분석 대상 회원
     * @param analysisDays 분석 기간 (일)
     * @return 감정 패턴 분석 결과
     */
    public AlertResult analyzeEmotionPattern(MemberEntity member, int analysisDays) {
        // TODO: TDD Red 단계 - 더미 구현
        // 실제 구현에서는:
        // 1. 최근 N일간 메시지 조회
        // 2. 감정 패턴 분석
        // 3. 위험도 판정

        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 감정 추세 계산 (내부 메서드)
     * @param messages 메시지 목록
     * @return 감정 추세 정보
     */
    private EmotionTrend calculateEmotionTrend(List<MessageEntity> messages) {
        // TODO: TDD Red 단계 - 더미 구현
        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
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