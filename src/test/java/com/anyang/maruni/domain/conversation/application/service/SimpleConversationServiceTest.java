package com.anyang.maruni.domain.conversation.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.mapper.ConversationMapper;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;

/**
 * SimpleConversationService 통합 테스트 (리팩토링 완료)
 *
 * 리팩토링된 구조에서 서비스 조율 기능을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대화 서비스 통합 테스트 (리팩토링 완료)")
class SimpleConversationServiceTest {

    @Mock
    private ConversationManager conversationManager;

    @Mock
    private MessageProcessor messageProcessor;

    @Mock
    private ConversationMapper mapper;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private SimpleConversationService simpleConversationService;

    @Test
    @DisplayName("사용자 메시지 처리: ConversationManager → MessageProcessor → ConversationMapper 순으로 위임한다")
    void processUserMessage_Success_DelegationFlow() {
        // Given
        Long memberId = 1L;
        String userContent = "안녕하세요, 오늘 기분이 좋아요!";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(100L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(1L)
                .conversationId(100L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.POSITIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(2L)
                .conversationId(100L)
                .type(MessageType.AI_RESPONSE)
                .content("안녕하세요! 기분이 좋으시다니 다행이네요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        ConversationResponseDto expectedResponse = ConversationResponseDto.builder()
                .conversationId(100L)
                .build();

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, userContent))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(expectedResponse);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result).isEqualTo(expectedResponse);

        // 위임 순서 검증
        verify(conversationManager).findOrCreateActive(memberId);
        verify(messageProcessor).processMessage(conversation, userContent);
        verify(mapper).toResponseDto(exchangeResult);
    }

    @Test
    @DisplayName("새로운 대화 생성: ConversationManager가 새 대화를 생성하고 처리한다")
    void processUserMessage_NewConversation_Success() {
        // Given
        Long memberId = 2L;
        String userContent = "처음 인사드립니다";

        ConversationEntity newConversation = ConversationEntity.builder()
                .id(200L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(3L)
                .conversationId(200L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(4L)
                .conversationId(200L)
                .type(MessageType.AI_RESPONSE)
                .content("안녕하세요! 만나서 반가워요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(newConversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        ConversationResponseDto expectedResponse = ConversationResponseDto.builder()
                .conversationId(200L)
                .build();

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(newConversation);
        when(messageProcessor.processMessage(newConversation, userContent))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(expectedResponse);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(conversationManager).findOrCreateActive(memberId);
        verify(messageProcessor).processMessage(newConversation, userContent);
        verify(mapper).toResponseDto(exchangeResult);
    }

    @Test
    @DisplayName("부정적 감정 메시지 처리: MessageProcessor가 감정 분석을 담당한다")
    void processUserMessage_NegativeEmotion_Success() {
        // Given
        Long memberId = 3L;
        String userContent = "오늘 너무 우울해요...";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(300L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(5L)
                .conversationId(300L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(6L)
                .conversationId(300L)
                .type(MessageType.AI_RESPONSE)
                .content("마음이 힘드시겠어요. 제가 함께 있어요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        ConversationResponseDto expectedResponse = ConversationResponseDto.builder()
                .conversationId(300L)
                .build();

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, userContent))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(expectedResponse);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(messageProcessor).processMessage(conversation, userContent);
    }

    @Test
    @DisplayName("시스템 메시지 처리: 도메인 로직을 활용하여 AI 메시지로 저장한다")
    void processSystemMessage_Success() {
        // Given
        Long memberId = 4L;
        String systemMessage = "안녕하세요! 오늘 안부 인사드립니다. 어떻게 지내세요?";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(400L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity savedSystemMessage = MessageEntity.builder()
                .id(7L)
                .conversationId(400L)
                .type(MessageType.AI_RESPONSE)
                .content(systemMessage)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);

        // ConversationEntity의 도메인 메서드 mock
        ConversationEntity spyConversation = spy(conversation);
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(spyConversation);
        when(spyConversation.addAIMessage(systemMessage))
                .thenReturn(savedSystemMessage);
        when(messageRepository.save(savedSystemMessage))
                .thenReturn(savedSystemMessage);

        // When
        simpleConversationService.processSystemMessage(memberId, systemMessage);

        // Then
        verify(conversationManager).findOrCreateActive(memberId);
        verify(spyConversation).addAIMessage(systemMessage);
        verify(messageRepository).save(savedSystemMessage);
    }

    @Test
    @DisplayName("서비스 조율 기능: 각 컴포넌트에 올바른 순서로 위임한다")
    void processUserMessage_CorrectDelegationOrder() {
        // Given
        Long memberId = 5L;
        String userContent = "테스트 메시지";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(500L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(mock(MessageEntity.class))
                .aiMessage(mock(MessageEntity.class))
                .build();

        ConversationResponseDto response = ConversationResponseDto.builder()
                .conversationId(500L)
                .build();

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, userContent))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(response);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result).isEqualTo(response);

        // InOrder를 사용하여 호출 순서 검증
        var inOrder = inOrder(conversationManager, messageProcessor, mapper);
        inOrder.verify(conversationManager).findOrCreateActive(memberId);
        inOrder.verify(messageProcessor).processMessage(conversation, userContent);
        inOrder.verify(mapper).toResponseDto(exchangeResult);
    }
}