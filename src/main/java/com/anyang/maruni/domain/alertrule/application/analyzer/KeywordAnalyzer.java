package com.anyang.maruni.domain.alertrule.application.analyzer;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;

/**
 * 키워드 위험도 분석기
 *
 * 메시지 내용에서 위험 키워드를 감지합니다.
 * TDD Red 단계: 더미 구현ter
 */
@Component
public class KeywordAnalyzer {

    /**
     * 긴급 상황 키워드 목록
     */
    private static final String[] EMERGENCY_KEYWORDS = {
            "도와주세요", "아파요", "숨이", "가슴이", "쓰러짐", "응급실", "119", "병원"
    };

    /**
     * 경고 단계 키워드 목록
     */
    private static final String[] WARNING_KEYWORDS = {
            "우울해", "외로워", "죽고싶어", "포기", "희망없어", "의미없어", "힘들어"
    };

    /**
     * 메시지의 키워드 위험도 분석
     * @param message 분석할 메시지
     * @return 키워드 분석 결과
     */
    public AlertResult analyzeKeywordRisk(MessageEntity message) {
        String content = message.getContent().toLowerCase();

        // 1. 긴급 키워드 감지 (최우선)
        for (String emergencyKeyword : EMERGENCY_KEYWORDS) {
            if (content.contains(emergencyKeyword.toLowerCase())) {
                String alertMessage = AnalyzerUtils.createKeywordDetectionMessage(AlertLevel.EMERGENCY, emergencyKeyword);
                KeywordMatch keywordMatch = KeywordMatch.emergency(emergencyKeyword, message.getContent());
                return AlertResult.createAlert(AlertLevel.EMERGENCY, alertMessage, keywordMatch);
            }
        }

        // 2. 경고 키워드 감지
        for (String warningKeyword : WARNING_KEYWORDS) {
            if (content.contains(warningKeyword.toLowerCase())) {
                String alertMessage = AnalyzerUtils.createKeywordDetectionMessage(AlertLevel.HIGH, warningKeyword);
                KeywordMatch keywordMatch = KeywordMatch.warning(warningKeyword, message.getContent());
                return AlertResult.createAlert(AlertLevel.HIGH, alertMessage, keywordMatch);
            }
        }

        // 3. 위험 키워드 없음
        return AlertResult.noAlert();
    }

    /**
     * 키워드 매칭 정보 VO
     */
    public static class KeywordMatch {
        private final String matchedKeyword;
        private final String originalMessage;
        private final KeywordType keywordType;

        private KeywordMatch(String matchedKeyword, String originalMessage, KeywordType keywordType) {
            this.matchedKeyword = matchedKeyword;
            this.originalMessage = originalMessage;
            this.keywordType = keywordType;
        }

        public static KeywordMatch emergency(String keyword, String message) {
            return new KeywordMatch(keyword, message, KeywordType.EMERGENCY);
        }

        public static KeywordMatch warning(String keyword, String message) {
            return new KeywordMatch(keyword, message, KeywordType.WARNING);
        }

        // Getter 메서드들
        public String getMatchedKeyword() { return matchedKeyword; }
        public String getOriginalMessage() { return originalMessage; }
        public KeywordType getKeywordType() { return keywordType; }

        public enum KeywordType {
            EMERGENCY, WARNING
        }
    }
}