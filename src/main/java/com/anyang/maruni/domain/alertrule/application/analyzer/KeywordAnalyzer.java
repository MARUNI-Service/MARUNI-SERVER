package com.anyang.maruni.domain.alertrule.application.analyzer;

import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import org.springframework.stereotype.Component;

/**
 * 키워드 위험도 분석기
 *
 * 메시지 내용에서 위험 키워드를 감지합니다.
 * TDD Red 단계: 더미 구현
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
        // TODO: TDD Red 단계 - 더미 구현
        // 실제 구현에서는:
        // 1. 긴급 키워드 감지
        // 2. 경고 키워드 감지
        // 3. 적절한 AlertLevel로 결과 반환

        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
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