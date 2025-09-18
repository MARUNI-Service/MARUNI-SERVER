package com.anyang.maruni.domain.conversation.application.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;

/**
 * ConversationMapper 단위 테스트
 *
 * Entity ↔ DTO 변환 로직을 검증합니다.
 */
@DisplayName("대화 매퍼 테스트")
class ConversationMapperTest {

    private final ConversationMapper mapper = new ConversationMapper();

    @Test
    @DisplayName("MessageExchangeResult를 ConversationResponseDto로 변환: 모든 필드가 올바르게 매핑된다")
    void toResponseDto_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Long conversationId = 100L;

        ConversationEntity conversation = ConversationEntity.builder()
                .id(conversationId)
                .memberId(1L)
                .startedAt(now.minusMinutes(10))
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(1L)
                .conversationId(conversationId)
                .type(MessageType.USER_MESSAGE)
                .content("안녕하세요, 오늘 기분이 좋아요!")
                .emotion(EmotionType.POSITIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(2L)
                .conversationId(conversationId)
                .type(MessageType.AI_RESPONSE)
                .content("안녕하세요! 기분이 좋으시다니 다행이네요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult result = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        // When
        ConversationResponseDto responseDto = mapper.toResponseDto(result);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getConversationId()).isEqualTo(conversationId);

        // 사용자 메시지 검증
        assertThat(responseDto.getUserMessage()).isNotNull();
        assertThat(responseDto.getUserMessage().getId()).isEqualTo(1L);
        assertThat(responseDto.getUserMessage().getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(responseDto.getUserMessage().getContent()).isEqualTo("안녕하세요, 오늘 기분이 좋아요!");
        assertThat(responseDto.getUserMessage().getEmotion()).isEqualTo(EmotionType.POSITIVE);

        // AI 메시지 검증
        assertThat(responseDto.getAiMessage()).isNotNull();
        assertThat(responseDto.getAiMessage().getId()).isEqualTo(2L);
        assertThat(responseDto.getAiMessage().getType()).isEqualTo(MessageType.AI_RESPONSE);
        assertThat(responseDto.getAiMessage().getContent()).isEqualTo("안녕하세요! 기분이 좋으시다니 다행이네요.");
        assertThat(responseDto.getAiMessage().getEmotion()).isEqualTo(EmotionType.NEUTRAL);
    }

    @Test
    @DisplayName("부정적 감정 메시지 변환: 감정 타입이 올바르게 변환된다")
    void toResponseDto_NegativeEmotion_Success() {
        // Given
        ConversationEntity conversation = ConversationEntity.builder()
                .id(200L)
                .memberId(2L)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(3L)
                .conversationId(200L)
                .type(MessageType.USER_MESSAGE)
                .content("오늘 너무 우울해요...")
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(4L)
                .conversationId(200L)
                .type(MessageType.AI_RESPONSE)
                .content("마음이 힘드시겠어요. 제가 함께 있어요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult result = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        // When
        ConversationResponseDto responseDto = mapper.toResponseDto(result);

        // Then
        assertThat(responseDto.getUserMessage().getEmotion()).isEqualTo(EmotionType.NEGATIVE);
        assertThat(responseDto.getAiMessage().getContent()).contains("함께 있어요");
    }

    @Test
    @DisplayName("중립적 감정 메시지 변환: 기본값이 올바르게 설정된다")
    void toResponseDto_NeutralEmotion_Success() {
        // Given
        ConversationEntity conversation = ConversationEntity.builder()
                .id(300L)
                .memberId(3L)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(5L)
                .conversationId(300L)
                .type(MessageType.USER_MESSAGE)
                .content("처음 인사드립니다")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(6L)
                .conversationId(300L)
                .type(MessageType.AI_RESPONSE)
                .content("안녕하세요! 만나서 반가워요")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult result = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        // When
        ConversationResponseDto responseDto = mapper.toResponseDto(result);

        // Then
        assertThat(responseDto.getUserMessage().getEmotion()).isEqualTo(EmotionType.NEUTRAL);
        assertThat(responseDto.getAiMessage().getEmotion()).isEqualTo(EmotionType.NEUTRAL);
    }
}