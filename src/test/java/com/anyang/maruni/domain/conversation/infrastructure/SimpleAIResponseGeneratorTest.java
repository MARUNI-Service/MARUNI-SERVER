package com.anyang.maruni.domain.conversation.infrastructure;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SimpleAIResponseGenerator 테스트 (TDD Red 단계)
 * 
 * 이 테스트들은 현재 모두 실패할 예정입니다.
 * TDD의 Red 단계에서 먼저 실패하는 테스트를 작성하고,
 * Green 단계에서 테스트를 통과하는 최소한의 코드를 작성할 예정입니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI 응답 생성기 테스트")
class SimpleAIResponseGeneratorTest {

    @Mock
    private OpenAiService openAiService;

    @InjectMocks
    private SimpleAIResponseGenerator simpleAIResponseGenerator;

    @Test
    @DisplayName("사용자 메시지에 대한 AI 응답을 생성한다")
    void generateResponse_WithUserMessage_ReturnsAIResponse() {
        // Given
        String userMessage = "안녕하세요, 오늘 기분이 좋지 않아요";
        
        // OpenAI API Mock 응답 설정
        ChatCompletionResult mockResult = createMockChatCompletionResult(
            "안녕하세요! 무슨 일이 있으셨나요? 이야기해보세요."
        );
        when(openAiService.createChatCompletion(any())).thenReturn(mockResult);
        
        // When
        String response = simpleAIResponseGenerator.generateResponse(userMessage);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response).isNotBlank();
        assertThat(response).contains("안녕하세요");
        assertThat(response.length()).isLessThanOrEqualTo(100); // SMS 특성상 짧은 응답
    }

    @Test
    @DisplayName("빈 메시지에 대해서도 기본 응답을 생성한다")
    void generateResponse_WithEmptyMessage_ReturnsDefaultResponse() {
        // Given
        String emptyMessage = "";
        
        ChatCompletionResult mockResult = createMockChatCompletionResult(
            "안녕하세요! 어떻게 지내세요?"
        );
        when(openAiService.createChatCompletion(any())).thenReturn(mockResult);
        
        // When
        String response = simpleAIResponseGenerator.generateResponse(emptyMessage);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response).isNotBlank();
    }

    @Test
    @DisplayName("긍정적인 메시지의 감정을 POSITIVE로 분석한다")
    void analyzeBasicEmotion_WithPositiveMessage_ReturnsPositive() {
        // Given
        String positiveMessage = "오늘 정말 기분이 좋아요! 감사합니다.";
        
        // When
        EmotionType emotion = simpleAIResponseGenerator.analyzeBasicEmotion(positiveMessage);
        
        // Then
        assertThat(emotion).isEqualTo(EmotionType.POSITIVE);
    }

    @Test
    @DisplayName("부정적인 메시지의 감정을 NEGATIVE로 분석한다")
    void analyzeBasicEmotion_WithNegativeMessage_ReturnsNegative() {
        // Given
        String negativeMessage = "오늘 너무 슬프고 우울해요. 힘들어요.";
        
        // When
        EmotionType emotion = simpleAIResponseGenerator.analyzeBasicEmotion(negativeMessage);
        
        // Then
        assertThat(emotion).isEqualTo(EmotionType.NEGATIVE);
    }

    @Test
    @DisplayName("중립적인 메시지의 감정을 NEUTRAL로 분석한다")
    void analyzeBasicEmotion_WithNeutralMessage_ReturnsNeutral() {
        // Given
        String neutralMessage = "그냥 그래요. 별일 없어요.";
        
        // When
        EmotionType emotion = simpleAIResponseGenerator.analyzeBasicEmotion(neutralMessage);
        
        // Then
        assertThat(emotion).isEqualTo(EmotionType.NEUTRAL);
    }

    @Test
    @DisplayName("null 메시지에 대해서도 NEUTRAL 감정을 반환한다")
    void analyzeBasicEmotion_WithNullMessage_ReturnsNeutral() {
        // Given
        String nullMessage = null;
        
        // When
        EmotionType emotion = simpleAIResponseGenerator.analyzeBasicEmotion(nullMessage);
        
        // Then
        assertThat(emotion).isEqualTo(EmotionType.NEUTRAL);
    }

    /**
     * OpenAI API 응답 Mock 객체 생성 헬퍼 메서드
     */
    private ChatCompletionResult createMockChatCompletionResult(String content) {
        // 간단한 Mock 객체 생성 (실제 SDK 클래스 구조에 맞춰 수정)
        ChatMessage responseMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value(), content);
        
        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(responseMessage);
        choice.setFinishReason("stop");

        ChatCompletionResult result = new ChatCompletionResult();
        result.setId("chatcmpl-test-id");
        result.setObject("chat.completion");
        result.setChoices(List.of(choice));
        
        return result;
    }
}