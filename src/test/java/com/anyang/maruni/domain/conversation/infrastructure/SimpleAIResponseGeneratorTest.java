package com.anyang.maruni.domain.conversation.infrastructure;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SimpleAIResponseGenerator 테스트 (Spring AI 버전)
 *
 * Spring AI ChatModel을 사용한 AI 응답 생성기 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI 응답 생성기 테스트")
class SimpleAIResponseGeneratorTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private SimpleAIResponseGenerator simpleAIResponseGenerator;

    @Test
    @DisplayName("사용자 메시지에 대한 AI 응답을 생성한다")
    void generateResponse_WithUserMessage_ReturnsAIResponse() {
        // Given
        String userMessage = "안녕하세요, 오늘 기분이 좋지 않아요";

        // Spring AI Mock 응답 설정
        ChatResponse mockResponse = createMockChatResponse(
            "안녕하세요! 무슨 일이 있으셨나요? 이야기해보세요."
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        // 필요한 프로퍼티 설정
        setUpProperties();

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

        ChatResponse mockResponse = createMockChatResponse(
            "안녕하세요! 어떻게 지내세요?"
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        // 필요한 프로퍼티 설정
        setUpProperties();

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
     * Spring AI ChatResponse Mock 객체 생성 헬퍼 메서드
     */
    private ChatResponse createMockChatResponse(String content) {
        Generation generation = new Generation(content);
        return new ChatResponse(List.of(generation));
    }

    /**
     * 테스트에 필요한 프로퍼티 설정
     */
    private void setUpProperties() {
        ReflectionTestUtils.setField(simpleAIResponseGenerator, "model", "gpt-4o");
        ReflectionTestUtils.setField(simpleAIResponseGenerator, "temperature", 0.7);
        ReflectionTestUtils.setField(simpleAIResponseGenerator, "maxTokens", 100);
    }
}