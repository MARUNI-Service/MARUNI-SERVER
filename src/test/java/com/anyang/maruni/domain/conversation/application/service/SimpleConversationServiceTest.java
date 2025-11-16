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

    // 신규 Mock (Phase 2: 키워드 감지)
    @Mock
    private com.anyang.maruni.domain.alertrule.application.service.core.AlertDetectionService alertDetectionService;

    @Mock
    private com.anyang.maruni.domain.alertrule.application.service.core.AlertNotificationService alertNotificationService;

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

        ConversationResponseDto expectedResponse = ConversationResponseDto.withId(100L);

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

        ConversationResponseDto expectedResponse = ConversationResponseDto.withId(200L);

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

        ConversationResponseDto expectedResponse = ConversationResponseDto.withId(300L);

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

        ConversationResponseDto response = ConversationResponseDto.withId(500L);

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

    // ========== Phase 2: 키워드 감지 테스트 ==========

    @Test
    @DisplayName("EMERGENCY 키워드 감지 시 즉시 알림 발송")
    void processUserMessage_EmergencyKeyword_TriggersAlert() {
        // Given
        Long memberId = 6L;
        String emergencyMessage = "죽고싶어요";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(600L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(10L)
                .conversationId(600L)
                .type(MessageType.USER_MESSAGE)
                .content(emergencyMessage)
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(11L)
                .conversationId(600L)
                .type(MessageType.AI_RESPONSE)
                .content("걱정하지 마세요. 제가 함께 있어요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        ConversationResponseDto response = ConversationResponseDto.withId(600L);

        // AlertResult: EMERGENCY 키워드 감지
        var emergencyAlert = com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult.createAlert(
                com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel.EMERGENCY,
                com.anyang.maruni.domain.alertrule.domain.entity.AlertType.KEYWORD_DETECTION,
                "긴급 키워드 감지: 죽고싶어요",
                null
        );

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, emergencyMessage))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(response);
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
                .thenReturn(emergencyAlert);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, emergencyMessage);

        // Then: 대화 처리 성공
        assertThat(result).isEqualTo(response);

        // 키워드 감지 호출됨
        verify(alertDetectionService).detectKeywordAlert(userMessage, memberId);

        // EMERGENCY 레벨이므로 즉시 알림 발송
        verify(alertNotificationService).triggerAlert(eq(memberId), any());
    }

    @Test
    @DisplayName("HIGH 키워드는 알림 미발송 (로그만 기록)")
    void processUserMessage_HighKeyword_NoImmediateAlert() {
        // Given
        Long memberId = 7L;
        String highMessage = "우울해요";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(700L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(12L)
                .conversationId(700L)
                .type(MessageType.USER_MESSAGE)
                .content(highMessage)
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(13L)
                .conversationId(700L)
                .type(MessageType.AI_RESPONSE)
                .content("힘든 일이 있으신가요?")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        ConversationResponseDto response = ConversationResponseDto.withId(700L);

        // AlertResult: HIGH 키워드 감지
        var highAlert = com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult.createAlert(
                com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel.HIGH,
                com.anyang.maruni.domain.alertrule.domain.entity.AlertType.KEYWORD_DETECTION,
                "경고 키워드 감지: 우울해요",
                null
        );

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, highMessage))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(response);
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
                .thenReturn(highAlert);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, highMessage);

        // Then: 대화 처리 성공
        assertThat(result).isEqualTo(response);

        // 키워드 감지 호출됨
        verify(alertDetectionService).detectKeywordAlert(userMessage, memberId);

        // HIGH 레벨은 알림 미발송 (로그만 기록)
        verify(alertNotificationService, never()).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("키워드 감지 실패 시 대화 흐름 유지")
    void processUserMessage_KeywordDetectionFails_ConversationContinues() {
        // Given
        Long memberId = 8L;
        String message = "테스트 메시지";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(800L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(14L)
                .conversationId(800L)
                .type(MessageType.USER_MESSAGE)
                .content(message)
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(15L)
                .conversationId(800L)
                .type(MessageType.AI_RESPONSE)
                .content("네, 알겠습니다.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult exchangeResult = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        ConversationResponseDto response = ConversationResponseDto.withId(800L);

        // Mock 설정
        when(conversationManager.findOrCreateActive(memberId))
                .thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, message))
                .thenReturn(exchangeResult);
        when(mapper.toResponseDto(exchangeResult))
                .thenReturn(response);

        // 키워드 감지 실패 (예외 발생)
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
                .thenThrow(new RuntimeException("Analyzer error"));

        // When & Then: 예외가 전파되지 않고 정상 응답
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, message);

        // 대화 흐름은 정상 유지
        assertThat(result).isEqualTo(response);
        verify(mapper).toResponseDto(exchangeResult);
    }
}