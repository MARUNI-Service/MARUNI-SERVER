package com.anyang.maruni.domain.conversation.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Conversation 도메인 설정 관리 클래스
 *
 * AI 모델 변경 대비 설정 중앙화를 제공합니다.
 * OpenAI, Claude, Gemini 등 다양한 AI 모델을 지원할 수 있도록 설계되었습니다.
 */
@ConfigurationProperties(prefix = "maruni.conversation")
@Component
@Data
public class ConversationProperties {

    private Ai ai = new Ai();
    private Emotion emotion = new Emotion();

    /**
     * AI 모델 관련 설정
     * Spring AI 기본 설정(spring.ai.openai)과 함께 사용되는 추가 설정들
     */
    @Data
    public static class Ai {
        /**
         * 최대 응답 길이 (SMS 특성상 제한)
         */
        private Integer maxResponseLength = 100;

        /**
         * 시스템 프롬프트 (AI 역할 정의)
         */
        private String systemPrompt = "당신은 노인 돌봄 전문 AI 상담사입니다. 따뜻하고 공감적으로 30자 이내로 응답하세요.";

        /**
         * 기본 응답 (API 에러 시 사용)
         */
        private String defaultResponse = "안녕하세요! 어떻게 지내세요?";

        /**
         * 기본 사용자 메시지 (입력값 없을 시 사용)
         */
        private String defaultUserMessage = "안녕하세요";
    }

    /**
     * 감정 분석 관련 설정
     */
    @Data
    public static class Emotion {
        /**
         * 감정별 키워드 맵
         * 키: 감정 타입 (negative, positive)
         * 값: 해당 감정을 나타내는 키워드 리스트
         */
        private Map<String, List<String>> keywords = Map.of(
            "negative", List.of("슬프", "우울", "아프", "힘들", "외로", "무서", "걱정", "답답"),
            "positive", List.of("좋", "행복", "기쁘", "감사", "즐거", "만족", "고마")
        );
    }
}