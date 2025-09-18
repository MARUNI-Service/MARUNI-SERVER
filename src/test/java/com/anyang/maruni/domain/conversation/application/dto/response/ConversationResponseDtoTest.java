package com.anyang.maruni.domain.conversation.application.dto.response;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;

/**
 * ConversationResponseDto 테스트
 *
 * 정적 팩토리 메서드 동작을 검증합니다.
 */
@DisplayName("대화 응답 DTO 테스트")
class ConversationResponseDtoTest {

    @Test
    @DisplayName("MessageExchangeResult.from(): 메시지 교환 결과를 응답 DTO로 변환한다")
    void from_MessageExchangeResult_Success() {
        // Given
        Long conversationId = 100L;
        Long memberId = 1L;

        ConversationEntity conversation = ConversationEntity.builder()
                .id(conversationId)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        MessageEntity userMessage = MessageEntity.builder()
                .id(1L)
                .conversationId(conversationId)
                .type(MessageType.USER_MESSAGE)
                .content("안녕하세요!")
                .emotion(EmotionType.POSITIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(2L)
                .conversationId(conversationId)
                .type(MessageType.AI_RESPONSE)
                .content("안녕하세요! 반갑습니다.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageExchangeResult result = MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();

        // When
        ConversationResponseDto responseDto = ConversationResponseDto.from(result);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getConversationId()).isEqualTo(conversationId);

        // 사용자 메시지 검증
        assertThat(responseDto.getUserMessage()).isNotNull();
        assertThat(responseDto.getUserMessage().getId()).isEqualTo(1L);
        assertThat(responseDto.getUserMessage().getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(responseDto.getUserMessage().getContent()).isEqualTo("안녕하세요!");
        assertThat(responseDto.getUserMessage().getEmotion()).isEqualTo(EmotionType.POSITIVE);

        // AI 메시지 검증
        assertThat(responseDto.getAiMessage()).isNotNull();
        assertThat(responseDto.getAiMessage().getId()).isEqualTo(2L);
        assertThat(responseDto.getAiMessage().getType()).isEqualTo(MessageType.AI_RESPONSE);
        assertThat(responseDto.getAiMessage().getContent()).isEqualTo("안녕하세요! 반갑습니다.");
        assertThat(responseDto.getAiMessage().getEmotion()).isEqualTo(EmotionType.NEUTRAL);
    }

    @Test
    @DisplayName("withId(): 대화 ID만으로 응답 DTO를 생성한다")
    void withId_ConversationId_Success() {
        // Given
        Long conversationId = 200L;

        // When
        ConversationResponseDto responseDto = ConversationResponseDto.withId(conversationId);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getConversationId()).isEqualTo(conversationId);
        assertThat(responseDto.getUserMessage()).isNull();
        assertThat(responseDto.getAiMessage()).isNull();
    }

    @Test
    @DisplayName("withId(): null ID로도 생성 가능하다")
    void withId_NullConversationId_Success() {
        // Given
        Long conversationId = null;

        // When
        ConversationResponseDto responseDto = ConversationResponseDto.withId(conversationId);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getConversationId()).isNull();
        assertThat(responseDto.getUserMessage()).isNull();
        assertThat(responseDto.getAiMessage()).isNull();
    }

    @Test
    @DisplayName("from(): 부정적 감정 메시지도 올바르게 변환한다")
    void from_NegativeEmotionMessage_Success() {
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
                .content("오늘 너무 우울해요...")
                .emotion(EmotionType.NEGATIVE)
                .build();

        MessageEntity aiMessage = MessageEntity.builder()
                .id(6L)
                .conversationId(300L)
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
        ConversationResponseDto responseDto = ConversationResponseDto.from(result);

        // Then
        assertThat(responseDto.getUserMessage().getEmotion()).isEqualTo(EmotionType.NEGATIVE);
        assertThat(responseDto.getAiMessage().getContent()).contains("함께 있어요");
        assertThat(responseDto.getConversationId()).isEqualTo(300L);
    }
}