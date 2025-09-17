package com.anyang.maruni.domain.conversation.infrastructure.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.anyang.maruni.domain.conversation.config.ConversationProperties;
import com.anyang.maruni.domain.conversation.domain.port.AIResponsePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI 기반 AI 응답 생성기
 *
 * 기존 SimpleAIResponseGenerator의 AI 응답 생성 로직을 OpenAI 전용으로 분리했습니다.
 * Port-Adapter 패턴을 통해 AI 모델 변경에 대비한 독립적인 구조를 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIResponseAdapter implements AIResponsePort {

    private final ChatModel chatModel;
    private final ConversationProperties properties;  // 추가

    // Spring AI 기본 설정은 여전히 @Value로 사용 (application-ai.yml에서)
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    // 응답 관련 상수
    private static final int ELLIPSIS_LENGTH = 3;
    private static final String ELLIPSIS = "...";

    /**
     * 사용자 메시지에 대한 AI 응답 생성
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답 내용
     */
    @Override
    public String generateResponse(String userMessage) {
        try {
            log.info("AI 응답 생성 요청: {}", userMessage);

            // 입력 검증 (기존 로직 그대로)
            String sanitizedMessage = sanitizeUserMessage(userMessage);

            // Spring AI로 응답 생성 (기존 로직 그대로)
            String response = callSpringAI(sanitizedMessage);
            String finalResponse = truncateResponse(response);

            log.info("AI 응답 생성 완료: {}", finalResponse);
            return finalResponse;

        } catch (Exception e) {
            return handleApiError(e);
        }
    }

    /**
     * 사용자 메시지 입력 검증 및 정제 (Properties 사용)
     */
    private String sanitizeUserMessage(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return properties.getAi().getDefaultUserMessage();  // Properties 사용
        }
        return userMessage;
    }

    /**
     * Spring AI를 사용한 응답 생성 (Properties 사용)
     */
    private String callSpringAI(String userMessage) {
        // 시스템 프롬프트와 사용자 메시지를 결합한 프롬프트 생성
        String systemPrompt = properties.getAi().getSystemPrompt();  // Properties 사용
        String combinedPrompt = systemPrompt + "\n\n사용자: " + userMessage + "\n\nAI:";

        // OpenAI Chat Options 설정
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();

        // Prompt 생성 및 호출
        Prompt prompt = new Prompt(combinedPrompt, options);
        ChatResponse response = chatModel.call(prompt);

        return response.getResult().getOutput().getContent().trim();
    }

    /**
     * 응답 길이 제한 (SMS 특성상) - Properties 사용
     */
    private String truncateResponse(String response) {
        int maxLength = properties.getAi().getMaxResponseLength();  // Properties 사용
        if (response.length() > maxLength) {
            return response.substring(0, maxLength - ELLIPSIS_LENGTH) + ELLIPSIS;
        }
        return response;
    }

    /**
     * API 에러 처리 (Properties 사용)
     */
    private String handleApiError(Exception e) {
        log.error("AI 응답 생성 실패: {}", e.getMessage(), e);
        return properties.getAi().getDefaultResponse();  // Properties 사용
    }
}