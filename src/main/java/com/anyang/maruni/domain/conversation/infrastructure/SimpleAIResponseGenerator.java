package com.anyang.maruni.domain.conversation.infrastructure;

import java.util.List;
import java.util.Map;

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
    
    // 감정분석 키워드 맵
    private static final Map<EmotionType, List<String>> EMOTION_KEYWORDS = Map.of(
        EmotionType.NEGATIVE, List.of("슬프", "우울", "아프", "힘들", "외로", "무서", "걱정", "답답"),
        EmotionType.POSITIVE, List.of("좋", "행복", "기쁘", "감사", "즐거", "만족", "고마")
    );

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
            String sanitizedMessage = sanitizeUserMessage(userMessage);

            // OpenAI API 요청 생성 및 호출
            ChatCompletionRequest request = buildChatRequest(sanitizedMessage);
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            
            // 응답 추출 및 길이 제한
            String response = extractResponseContent(result);
            String finalResponse = truncateResponse(response);

            log.info("AI 응답 생성 완료: {}", finalResponse);
            return finalResponse;
            
        } catch (Exception e) {
            return handleApiError(e);
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
        if (containsAnyKeyword(lowerMessage, EMOTION_KEYWORDS.get(EmotionType.NEGATIVE))) {
            log.debug("부정적 감정 감지: NEGATIVE");
            return EmotionType.NEGATIVE;
        }

        // 긍정적 키워드 체크
        if (containsAnyKeyword(lowerMessage, EMOTION_KEYWORDS.get(EmotionType.POSITIVE))) {
            log.debug("긍정적 감정 감지: POSITIVE");
            return EmotionType.POSITIVE;
        }

        // 기본값: 중립
        log.debug("중립적 감정: NEUTRAL");
        return EmotionType.NEUTRAL;
    }

    /**
     * 사용자 메시지 입력 검증 및 정제
     */
    private String sanitizeUserMessage(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return DEFAULT_USER_MESSAGE;
        }
        return userMessage;
    }

    /**
     * OpenAI API 요청 객체 생성
     */
    private ChatCompletionRequest buildChatRequest(String userMessage) {
        return ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT),
                        new ChatMessage(ChatMessageRole.USER.value(), userMessage)
                ))
                .maxTokens(MAX_TOKENS)
                .build();
    }

    /**
     * OpenAI API 응답에서 실제 응답 내용 추출
     */
    private String extractResponseContent(ChatCompletionResult result) {
        return result.getChoices().get(0).getMessage().getContent().trim();
    }

    /**
     * 응답 길이 제한 (SMS 특성상)
     */
    private String truncateResponse(String response) {
        if (response.length() > MAX_RESPONSE_LENGTH) {
            return response.substring(0, MAX_RESPONSE_LENGTH - ELLIPSIS_LENGTH) + ELLIPSIS;
        }
        return response;
    }

    /**
     * API 에러 처리
     */
    private String handleApiError(Exception e) {
        log.error("AI 응답 생성 실패: {}", e.getMessage(), e);
        return DEFAULT_RESPONSE;
    }

    /**
     * 메시지에 키워드 목록 중 하나라도 포함되는지 확인
     */
    private boolean containsAnyKeyword(String message, List<String> keywords) {
        return keywords.stream()
                .anyMatch(message::contains);
    }
}