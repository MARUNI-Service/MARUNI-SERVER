package com.anyang.maruni.domain.conversation.infrastructure;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 간단한 AI 응답 생성기 (MVP 버전 - TDD Green 구현)
 * 
 * OpenAI GPT API를 사용하여 사용자 메시지에 대한 AI 응답을 생성합니다.
 * MVP에서는 기본적인 프롬프트와 간단한 감정 분석만 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleAIResponseGenerator {

    private final OpenAiService openAiService;
    
    @Value("${openai.model}")
    private String model;
    
    // 응답 관련 상수
    private static final int MAX_TOKENS = 100;
    private static final int MAX_RESPONSE_LENGTH = 100;
    private static final int ELLIPSIS_LENGTH = 3;
    private static final String ELLIPSIS = "...";
    
    // 메시지 관련 상수
    private static final String DEFAULT_USER_MESSAGE = "안녕하세요";
    private static final String DEFAULT_RESPONSE = "안녕하세요! 어떻게 지내세요?";
    private static final String SYSTEM_PROMPT = "당신은 노인 돌봄 전문 AI 상담사입니다. 따뜻하고 공감적으로 30자 이내로 응답하세요.";

    /**
     * 사용자 메시지에 대한 AI 응답 생성
     * 
     * @param userMessage 사용자 메시지
     * @return AI 응답 내용
     */
    public String generateResponse(String userMessage) {
        try {
            log.info("AI 응답 생성 요청: {}", userMessage);
            
            // 입력 검증
            if (!StringUtils.hasText(userMessage)) {
                userMessage = DEFAULT_USER_MESSAGE;
            }

            // OpenAI API 요청 생성
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT),
                            new ChatMessage(ChatMessageRole.USER.value(), userMessage)
                    ))
                    .maxTokens(MAX_TOKENS)
                    .build();

            // API 호출
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            
            // 응답 추출
            String response = result.getChoices().get(0).getMessage().getContent().trim();
            
            // 응답 길이 제한 (SMS 특성상)
            if (response.length() > MAX_RESPONSE_LENGTH) {
                response = response.substring(0, MAX_RESPONSE_LENGTH - ELLIPSIS_LENGTH) + ELLIPSIS;
            }

            log.info("AI 응답 생성 완료: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("AI 응답 생성 실패: {}", e.getMessage(), e);
            // 기본 응답 반환 (방어적 코딩)
            return DEFAULT_RESPONSE;
        }
    }

    /**
     * 간단한 감정 분석 수행 (MVP: 키워드 기반)
     * 
     * @param message 분석할 메시지
     * @return 감정 타입
     */
    public EmotionType analyzeBasicEmotion(String message) {
        log.debug("감정 분석 시작: {}", message);
        
        // null 또는 빈 문자열 처리
        if (!StringUtils.hasText(message)) {
            return EmotionType.NEUTRAL;
        }

        String lowerMessage = message.toLowerCase();

        // 부정적 키워드 체크 (우선 순위 높음)
        if (lowerMessage.contains("슬프") || lowerMessage.contains("우울") || 
            lowerMessage.contains("아프") || lowerMessage.contains("힘들") ||
            lowerMessage.contains("외로") || lowerMessage.contains("무서") ||
            lowerMessage.contains("걱정") || lowerMessage.contains("답답")) {
            log.debug("부정적 감정 감지: NEGATIVE");
            return EmotionType.NEGATIVE;
        }

        // 긍정적 키워드 체크
        if (lowerMessage.contains("좋") || lowerMessage.contains("행복") ||
            lowerMessage.contains("기쁘") || lowerMessage.contains("감사") ||
            lowerMessage.contains("즐거") || lowerMessage.contains("만족") ||
            lowerMessage.contains("고마")) {
            log.debug("긍정적 감정 감지: POSITIVE");
            return EmotionType.POSITIVE;
        }

        // 기본값: 중립
        log.debug("중립적 감정: NEUTRAL");
        return EmotionType.NEUTRAL;
    }
}