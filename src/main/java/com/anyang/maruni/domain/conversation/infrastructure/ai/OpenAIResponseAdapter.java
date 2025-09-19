package com.anyang.maruni.domain.conversation.infrastructure.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.anyang.maruni.domain.conversation.config.ConversationProperties;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.conversation.domain.exception.AIResponseGenerationException;
import com.anyang.maruni.domain.conversation.domain.port.AIResponsePort;
import com.anyang.maruni.domain.conversation.domain.vo.ConversationContext;
import com.anyang.maruni.domain.conversation.domain.vo.MemberProfile;

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
     * 대화 컨텍스트를 활용한 AI 응답 생성
     *
     * @param context 대화 컨텍스트
     * @return AI 응답 내용
     */
    @Override
    public String generateResponse(ConversationContext context) {
        try {
            log.info("AI 응답 생성 요청 (컨텍스트): {}", context.getCurrentMessage());

            // 컨텍스트 기반 프롬프트 생성
            String enhancedPrompt = buildPromptWithContext(context);
            String response = callSpringAI(enhancedPrompt);
            String finalResponse = truncateResponse(response);

            log.info("AI 응답 생성 완료 (컨텍스트): {}", finalResponse);
            return finalResponse;

        } catch (AIResponseGenerationException e) {
            // AI 응답 생성 실패 시 기본 응답 반환 (사용자 경험 우선)
            log.warn("AI 응답 생성 실패, 기본 응답 사용: {}", e.getMessage());
            return properties.getAi().getDefaultResponse();
        } catch (Exception e) {
            // 예상치 못한 오류 시 기본 응답 반환
            log.error("AI 응답 생성 중 예상치 못한 오류: {}", e.getMessage(), e);
            return properties.getAi().getDefaultResponse();
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
     * Spring AI를 사용한 응답 생성 (예외 처리 강화)
     */
    private String callSpringAI(String userMessage) {
        try {
            // 시스템 프롬프트와 사용자 메시지를 결합한 프롬프트 생성
            String systemPrompt = properties.getAi().getSystemPrompt();
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

            String content = response.getResult().getOutput().getContent();
            if (!StringUtils.hasText(content)) {
                throw AIResponseGenerationException.responseParsingFailed("응답 내용이 비어있음", null);
            }

            return content.trim();

        } catch (RuntimeException e) {
            // Spring AI 관련 예외들을 포괄적으로 처리
            String errorMessage = e.getMessage();
            log.error("OpenAI API 호출 오류: {}", errorMessage, e);

            // 특정 오류 유형 구분
            if (errorMessage != null) {
                if (errorMessage.contains("429") || errorMessage.contains("rate limit")) {
                    throw AIResponseGenerationException.apiLimitExceeded();
                } else if (errorMessage.contains("timeout") || errorMessage.contains("connection")) {
                    throw AIResponseGenerationException.networkError(e);
                }
            }

            throw AIResponseGenerationException.apiCallFailed(e);
        } catch (Exception e) {
            log.error("AI 응답 생성 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw AIResponseGenerationException.apiCallFailed(e);
        }
    }

    /**
     * 대화 컨텍스트를 활용한 프롬프트 생성
     */
    private String buildPromptWithContext(ConversationContext context) {
        StringBuilder prompt = new StringBuilder();

        // 기본 시스템 프롬프트
        prompt.append(properties.getAi().getSystemPrompt());

        // 사용자 프로필 반영
        MemberProfile profile = context.getMemberProfile();
        if (profile != null) {
            prompt.append("\n\n사용자 정보: ").append(profile.getAgeGroup());
            if (StringUtils.hasText(profile.getPersonalityType())) {
                prompt.append(", 성격: ").append(profile.getPersonalityType());
            }

            if (!profile.getHealthConcerns().isEmpty()) {
                prompt.append(", 건강 관심사: ").append(String.join(", ", profile.getHealthConcerns()));
            }
        }

        // 최근 대화 히스토리 반영
        if (!context.getRecentHistory().isEmpty()) {
            prompt.append("\n\n최근 대화:");
            for (MessageEntity message : context.getRecentHistory()) {
                String sender = message.getType() == MessageType.USER_MESSAGE ? "사용자" : "AI";
                prompt.append("\n").append(sender).append(": ").append(message.getContent());
            }
        }

        // 현재 메시지와 감정 상태
        prompt.append("\n\n현재 메시지: ").append(context.getCurrentMessage());

        if (context.getCurrentEmotion() != null) {
            prompt.append("\n감정 상태: ").append(context.getCurrentEmotion().name());
        }

        prompt.append("\n\nAI 응답:");

        return prompt.toString();
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