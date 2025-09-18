package com.anyang.maruni.domain.conversation.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.conversation.domain.port.AIResponsePort;
import com.anyang.maruni.domain.conversation.domain.port.EmotionAnalysisPort;
import com.anyang.maruni.domain.conversation.domain.vo.ConversationContext;

/**
 * MessageProcessor 단위 테스트
 *
 * 메시지 처리 핵심 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("메시지 처리 서비스 테스트")
class MessageProcessorTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AIResponsePort aiResponsePort;

    @Mock
    private EmotionAnalysisPort emotionAnalysisPort;

    @InjectMocks
    private MessageProcessor messageProcessor;

    @Test
    @DisplayName("메시지 처리: 감정분석 → 컨텍스트구성 → AI응답생성 → 메시지저장 순으로 처리한다")
    void processMessage_Success() {
        // Given
        Long conversationId = 100L;
        Long memberId = 1L;
        String userContent = "안녕하세요, 오늘 기분이 좋아요!";
        String aiResponse = "안녕하세요! 기분이 좋으시다니 다행이네요.";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(conversationId)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        // 최근 히스토리 mock (대화에서 반환)
        MessageEntity pastMessage = MessageEntity.builder()
                .id(10L)
                .conversationId(conversationId)
                .type(MessageType.AI_RESPONSE)
                .content("이전 메시지")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity savedUserMessage = MessageEntity.builder()
                .id(1L)
                .conversationId(conversationId)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.POSITIVE)
                .build();

        MessageEntity savedAiMessage = MessageEntity.builder()
                .id(2L)
                .conversationId(conversationId)
                .type(MessageType.AI_RESPONSE)
                .content(aiResponse)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(emotionAnalysisPort.analyzeEmotion(userContent))
                .thenReturn(EmotionType.POSITIVE);
        when(aiResponsePort.generateResponse(any(ConversationContext.class)))
                .thenReturn(aiResponse);
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(savedUserMessage)
                .thenReturn(savedAiMessage);

        // ConversationEntity mock으로 완전 교체
        ConversationEntity mockConversation = mock(ConversationEntity.class);
        when(mockConversation.getId()).thenReturn(conversationId);
        when(mockConversation.getMemberId()).thenReturn(memberId);
        when(mockConversation.getRecentHistory(5))
                .thenReturn(Arrays.asList(pastMessage));
        when(mockConversation.addUserMessage(userContent, EmotionType.POSITIVE))
                .thenReturn(savedUserMessage);
        when(mockConversation.addAIMessage(aiResponse))
                .thenReturn(savedAiMessage);

        // When
        MessageExchangeResult result = messageProcessor.processMessage(mockConversation, userContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.conversation()).isEqualTo(mockConversation);
        assertThat(result.userMessage()).isEqualTo(savedUserMessage);
        assertThat(result.aiMessage()).isEqualTo(savedAiMessage);

        // 호출 순서 검증
        verify(emotionAnalysisPort, times(1)).analyzeEmotion(userContent);
        verify(mockConversation, times(1)).getRecentHistory(5);
        verify(mockConversation, times(1)).addUserMessage(userContent, EmotionType.POSITIVE);
        verify(aiResponsePort, times(1)).generateResponse(any(ConversationContext.class));
        verify(mockConversation, times(1)).addAIMessage(aiResponse);
        verify(messageRepository, times(2)).save(any(MessageEntity.class));
    }

    @Test
    @DisplayName("부정적 감정 메시지 처리: 감정분석 결과가 올바르게 반영된다")
    void processMessage_NegativeEmotion_Success() {
        // Given
        String userContent = "오늘 너무 우울해요...";
        String aiResponse = "마음이 힘드시겠어요. 제가 함께 있어요.";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(200L)
                .memberId(2L)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity savedUserMessage = MessageEntity.builder()
                .id(3L)
                .conversationId(200L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity savedAiMessage = MessageEntity.builder()
                .id(4L)
                .conversationId(200L)
                .type(MessageType.AI_RESPONSE)
                .content(aiResponse)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(emotionAnalysisPort.analyzeEmotion(userContent))
                .thenReturn(EmotionType.NEGATIVE);
        when(aiResponsePort.generateResponse(any(ConversationContext.class)))
                .thenReturn(aiResponse);
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(savedUserMessage)
                .thenReturn(savedAiMessage);

        ConversationEntity mockConversation = mock(ConversationEntity.class);
        when(mockConversation.getId()).thenReturn(200L);
        when(mockConversation.getMemberId()).thenReturn(2L);
        when(mockConversation.getRecentHistory(5))
                .thenReturn(Collections.emptyList());
        when(mockConversation.addUserMessage(userContent, EmotionType.NEGATIVE))
                .thenReturn(savedUserMessage);
        when(mockConversation.addAIMessage(aiResponse))
                .thenReturn(savedAiMessage);

        // When
        MessageExchangeResult result = messageProcessor.processMessage(mockConversation, userContent);

        // Then
        assertThat(result.userMessage().getEmotion()).isEqualTo(EmotionType.NEGATIVE);
        assertThat(result.aiMessage().getContent()).contains("함께 있어요");

        // 감정분석 호출 검증
        verify(emotionAnalysisPort, times(1)).analyzeEmotion(userContent);
        verify(mockConversation, times(1)).addUserMessage(userContent, EmotionType.NEGATIVE);
    }

    @Test
    @DisplayName("빈 히스토리로 첫 메시지 처리: 컨텍스트에 빈 히스토리가 포함된다")
    void processMessage_FirstMessage_EmptyHistory() {
        // Given
        String userContent = "처음 인사드립니다";
        String aiResponse = "안녕하세요! 만나서 반가워요";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(300L)
                .memberId(3L)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity savedUserMessage = MessageEntity.builder()
                .id(5L)
                .conversationId(300L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity savedAiMessage = MessageEntity.builder()
                .id(6L)
                .conversationId(300L)
                .type(MessageType.AI_RESPONSE)
                .content(aiResponse)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(emotionAnalysisPort.analyzeEmotion(userContent))
                .thenReturn(EmotionType.NEUTRAL);
        when(aiResponsePort.generateResponse(any(ConversationContext.class)))
                .thenReturn(aiResponse);
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(savedUserMessage)
                .thenReturn(savedAiMessage);

        ConversationEntity mockConversation = mock(ConversationEntity.class);
        when(mockConversation.getId()).thenReturn(300L);
        when(mockConversation.getMemberId()).thenReturn(3L);
        when(mockConversation.getRecentHistory(5))
                .thenReturn(Collections.emptyList()); // 빈 히스토리
        when(mockConversation.addUserMessage(userContent, EmotionType.NEUTRAL))
                .thenReturn(savedUserMessage);
        when(mockConversation.addAIMessage(aiResponse))
                .thenReturn(savedAiMessage);

        // When
        MessageExchangeResult result = messageProcessor.processMessage(mockConversation, userContent);

        // Then
        assertThat(result).isNotNull();
        verify(mockConversation, times(1)).getRecentHistory(5);
        verify(aiResponsePort, times(1)).generateResponse(argThat((ConversationContext context) ->
            context.getRecentHistory().isEmpty() &&
            context.getCurrentMessage().equals(userContent) &&
            context.getCurrentEmotion() == EmotionType.NEUTRAL
        ));
    }
}