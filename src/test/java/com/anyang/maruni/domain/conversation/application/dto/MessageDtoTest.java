package com.anyang.maruni.domain.conversation.application.dto;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;

/**
 * MessageDto 테스트
 *
 * Entity → DTO 변환 로직을 검증합니다.
 */
@DisplayName("메시지 DTO 테스트")
class MessageDtoTest {

    @Test
    @DisplayName("MessageEntity.from(): 사용자 메시지 엔티티를 DTO로 변환한다")
    void from_UserMessage_Success() {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 18, 10, 30, 0);

        MessageEntity userMessage = MessageEntity.builder()
                .id(1L)
                .conversationId(100L)
                .type(MessageType.USER_MESSAGE)
                .content("안녕하세요, 오늘 기분이 좋아요!")
                .emotion(EmotionType.POSITIVE)
                .build();

        // BaseTimeEntity의 createdAt을 mock (실제로는 JPA가 설정)
        // 여기서는 직접 설정된다고 가정
        MessageEntity messageWithTime = MessageEntity.builder()
                .id(1L)
                .conversationId(100L)
                .type(MessageType.USER_MESSAGE)
                .content("안녕하세요, 오늘 기분이 좋아요!")
                .emotion(EmotionType.POSITIVE)
                .build();

        // When
        MessageDto dto = MessageDto.from(messageWithTime);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(dto.getContent()).isEqualTo("안녕하세요, 오늘 기분이 좋아요!");
        assertThat(dto.getEmotion()).isEqualTo(EmotionType.POSITIVE);
        // createdAt은 BaseTimeEntity에서 관리되므로 null일 수 있음
    }

    @Test
    @DisplayName("MessageEntity.from(): AI 응답 메시지 엔티티를 DTO로 변환한다")
    void from_AIMessage_Success() {
        // Given
        MessageEntity aiMessage = MessageEntity.builder()
                .id(2L)
                .conversationId(100L)
                .type(MessageType.AI_RESPONSE)
                .content("안녕하세요! 기분이 좋으시다니 정말 다행이네요.")
                .emotion(EmotionType.NEUTRAL)
                .build();

        // When
        MessageDto dto = MessageDto.from(aiMessage);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getType()).isEqualTo(MessageType.AI_RESPONSE);
        assertThat(dto.getContent()).isEqualTo("안녕하세요! 기분이 좋으시다니 정말 다행이네요.");
        assertThat(dto.getEmotion()).isEqualTo(EmotionType.NEUTRAL);
    }

    @Test
    @DisplayName("MessageEntity.from(): 부정적 감정 메시지를 올바르게 변환한다")
    void from_NegativeEmotionMessage_Success() {
        // Given
        MessageEntity negativeMessage = MessageEntity.builder()
                .id(3L)
                .conversationId(200L)
                .type(MessageType.USER_MESSAGE)
                .content("오늘 너무 우울해요... 아무도 저를 신경쓰지 않는 것 같아요")
                .emotion(EmotionType.NEGATIVE)
                .build();

        // When
        MessageDto dto = MessageDto.from(negativeMessage);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(dto.getContent()).contains("우울해요");
        assertThat(dto.getEmotion()).isEqualTo(EmotionType.NEGATIVE);
    }

    @Test
    @DisplayName("MessageEntity.from(): 모든 필드가 null이 아닌 엔티티를 올바르게 변환한다")
    void from_CompleteEntity_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MessageEntity completeMessage = MessageEntity.builder()
                .id(4L)
                .conversationId(300L)
                .type(MessageType.USER_MESSAGE)
                .content("완전한 메시지입니다")
                .emotion(EmotionType.POSITIVE)
                .build();

        // When
        MessageDto dto = MessageDto.from(completeMessage);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(dto.getContent()).isEqualTo("완전한 메시지입니다");
        assertThat(dto.getEmotion()).isEqualTo(EmotionType.POSITIVE);
    }

    @Test
    @DisplayName("MessageEntity.from(): 긴 메시지 내용도 올바르게 변환한다")
    void from_LongContent_Success() {
        // Given
        String longContent = "이것은 매우 긴 메시지입니다. ".repeat(10); // 300자 정도

        MessageEntity longMessage = MessageEntity.builder()
                .id(5L)
                .conversationId(400L)
                .type(MessageType.USER_MESSAGE)
                .content(longContent)
                .emotion(EmotionType.NEUTRAL)
                .build();

        // When
        MessageDto dto = MessageDto.from(longMessage);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getContent()).isEqualTo(longContent);
        assertThat(dto.getContent().length()).isGreaterThan(100);
    }
}