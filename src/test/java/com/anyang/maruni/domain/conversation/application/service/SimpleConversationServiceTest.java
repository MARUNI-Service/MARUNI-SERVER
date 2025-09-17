package com.anyang.maruni.domain.conversation.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.conversation.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.conversation.domain.port.AIResponsePort;
import com.anyang.maruni.domain.conversation.domain.port.EmotionAnalysisPort;

/**
 * SimpleConversationService 테스트 (실제 비즈니스 로직)
 *
 * 실제 구현된 비즈니스 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대화 서비스 테스트")
class SimpleConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AIResponsePort aiResponsePort;              // 변경

    @Mock
    private EmotionAnalysisPort emotionAnalysisPort;    // 변경

    @InjectMocks
    private SimpleConversationService simpleConversationService;

    @Test
    @DisplayName("기존 대화가 있는 경우: 사용자 메시지를 처리하고 AI 응답을 생성한다")
    void processUserMessage_ExistingConversation_Success() {
        // Given
        Long memberId = 1L;
        String userContent = "안녕하세요, 오늘 기분이 좋아요!";
        String aiResponse = "안녕하세요! 오늘 기분이 좋으시다니 정말 다행이네요.";

        ConversationEntity existingConversation = ConversationEntity.builder()
                .id(100L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now().minusHours(1))
                .build();

        MessageEntity savedUserMessage = MessageEntity.builder()
                .id(1L)
                .conversationId(100L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.POSITIVE)
                .build();

        MessageEntity savedAiMessage = MessageEntity.builder()
                .id(2L)
                .conversationId(100L)
                .type(MessageType.AI_RESPONSE)
                .content(aiResponse)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(conversationRepository.findActiveByMemberId(memberId))
                .thenReturn(Optional.of(existingConversation));
        when(emotionAnalysisPort.analyzeEmotion(userContent))
                .thenReturn(EmotionType.POSITIVE);
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(savedUserMessage)
                .thenReturn(savedAiMessage);
        when(aiResponsePort.generateResponse(userContent))
                .thenReturn(aiResponse);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo(100L);

        // 사용자 메시지 검증
        assertThat(result.getUserMessage()).isNotNull();
        assertThat(result.getUserMessage().getContent()).isEqualTo(userContent);
        assertThat(result.getUserMessage().getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(result.getUserMessage().getEmotion()).isEqualTo(EmotionType.POSITIVE);

        // AI 메시지 검증
        assertThat(result.getAiMessage()).isNotNull();
        assertThat(result.getAiMessage().getContent()).isEqualTo(aiResponse);
        assertThat(result.getAiMessage().getType()).isEqualTo(MessageType.AI_RESPONSE);
        assertThat(result.getAiMessage().getEmotion()).isEqualTo(EmotionType.NEUTRAL);

        // Repository 메서드 호출 검증
        verify(conversationRepository).findActiveByMemberId(memberId);
        verify(conversationRepository, never()).save(any()); // 기존 대화 사용
        verify(emotionAnalysisPort).analyzeEmotion(userContent);
        verify(aiResponsePort).generateResponse(userContent);
        verify(messageRepository, times(2)).save(any(MessageEntity.class));
    }

    @Test
    @DisplayName("새로운 대화인 경우: 대화를 생성하고 메시지를 처리한다")
    void processUserMessage_NewConversation_Success() {
        // Given
        Long memberId = 2L;
        String userContent = "처음 인사드립니다";
        String aiResponse = "안녕하세요! 만나서 반가워요.";

        ConversationEntity newConversation = ConversationEntity.builder()
                .id(200L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity savedUserMessage = MessageEntity.builder()
                .id(3L)
                .conversationId(200L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity savedAiMessage = MessageEntity.builder()
                .id(4L)
                .conversationId(200L)
                .type(MessageType.AI_RESPONSE)
                .content(aiResponse)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(conversationRepository.findActiveByMemberId(memberId))
                .thenReturn(Optional.empty()); // 기존 대화 없음
        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenReturn(newConversation);
        when(emotionAnalysisPort.analyzeEmotion(userContent))
                .thenReturn(EmotionType.NEUTRAL);
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(savedUserMessage)
                .thenReturn(savedAiMessage);
        when(aiResponsePort.generateResponse(userContent))
                .thenReturn(aiResponse);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo(200L);

        // Repository 메서드 호출 검증
        verify(conversationRepository).findActiveByMemberId(memberId);
        verify(conversationRepository).save(any(ConversationEntity.class)); // 새 대화 생성
        verify(messageRepository, times(2)).save(any(MessageEntity.class));
    }

    @Test
    @DisplayName("부정적 감정의 메시지도 올바르게 처리한다")
    void processUserMessage_NegativeEmotion_Success() {
        // Given
        Long memberId = 3L;
        String userContent = "오늘 너무 우울해요... 아무도 저를 신경쓰지 않는 것 같아요";
        String aiResponse = "마음이 많이 힘드시겠어요. 항상 당신을 걱정하고 있어요.";

        ConversationEntity conversation = ConversationEntity.builder()
                .id(300L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity savedUserMessage = MessageEntity.builder()
                .id(5L)
                .conversationId(300L)
                .type(MessageType.USER_MESSAGE)
                .content(userContent)
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity savedAiMessage = MessageEntity.builder()
                .id(6L)
                .conversationId(300L)
                .type(MessageType.AI_RESPONSE)
                .content(aiResponse)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // Mock 설정
        when(conversationRepository.findActiveByMemberId(memberId))
                .thenReturn(Optional.of(conversation));
        when(emotionAnalysisPort.analyzeEmotion(userContent))
                .thenReturn(EmotionType.NEGATIVE);
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(savedUserMessage)
                .thenReturn(savedAiMessage);
        when(aiResponsePort.generateResponse(userContent))
                .thenReturn(aiResponse);

        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, userContent);

        // Then
        assertThat(result.getUserMessage().getEmotion()).isEqualTo(EmotionType.NEGATIVE);
        assertThat(result.getAiMessage().getContent()).contains("걱정하고 있어요");

        // 감정 분석 호출 검증
        verify(emotionAnalysisPort).analyzeEmotion(userContent);
    }
}