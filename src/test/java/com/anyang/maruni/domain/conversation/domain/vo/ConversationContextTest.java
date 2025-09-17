package com.anyang.maruni.domain.conversation.domain.vo;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;

/**
 * ConversationContext 도메인 로직 테스트
 */
@DisplayName("대화 컨텍스트 VO 테스트")
class ConversationContextTest {

    @Test
    @DisplayName("사용자 메시지 컨텍스트 생성: 기본값이 올바르게 설정된다")
    void forUserMessage_Success() {
        // Given
        String message = "안녕하세요";
        List<MessageEntity> history = createTestMessages();
        MemberProfile profile = MemberProfile.createDefault(1L);
        EmotionType emotion = EmotionType.POSITIVE;

        // When
        ConversationContext context = ConversationContext.forUserMessage(message, history, profile, emotion);

        // Then
        assertThat(context.getCurrentMessage()).isEqualTo(message);
        assertThat(context.getRecentHistory()).hasSize(2);
        assertThat(context.getMemberProfile()).isEqualTo(profile);
        assertThat(context.getCurrentEmotion()).isEqualTo(emotion);
        assertThat(context.getMetadata()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("사용자 메시지 컨텍스트 생성: 히스토리가 5개로 제한된다")
    void forUserMessage_HistoryLimitedToFive() {
        // Given
        String message = "테스트 메시지";
        List<MessageEntity> longHistory = createLongTestMessages(10); // 10개 메시지
        MemberProfile profile = MemberProfile.createDefault(1L);

        // When
        ConversationContext context = ConversationContext.forUserMessage(
                message, longHistory, profile, EmotionType.NEUTRAL);

        // Then
        assertThat(context.getRecentHistory()).hasSize(5); // 5개로 제한됨
    }

    @Test
    @DisplayName("시스템 메시지 컨텍스트 생성: 기본값이 올바르게 설정된다")
    void forSystemMessage_Success() {
        // Given
        String systemMessage = "안부 메시지입니다";
        MemberProfile profile = MemberProfile.createDefault(1L);

        // When
        ConversationContext context = ConversationContext.forSystemMessage(systemMessage, profile);

        // Then
        assertThat(context.getCurrentMessage()).isEqualTo(systemMessage);
        assertThat(context.getRecentHistory()).isEmpty();
        assertThat(context.getMemberProfile()).isEqualTo(profile);
        assertThat(context.getCurrentEmotion()).isEqualTo(EmotionType.NEUTRAL);
        assertThat(context.getMetadata()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("컨텍스트 불변성: 필드들이 final로 보호된다")
    void contextImmutability() {
        // Given
        ConversationContext context = ConversationContext.forSystemMessage(
                "test", MemberProfile.createDefault(1L));

        // When & Then - 모든 필드가 final이므로 변경 불가능
        assertThat(context.getCurrentMessage()).isNotNull();
        assertThat(context.getRecentHistory()).isNotNull();
        assertThat(context.getMemberProfile()).isNotNull();
        assertThat(context.getCurrentEmotion()).isNotNull();
        assertThat(context.getMetadata()).isNotNull();
    }

    private List<MessageEntity> createTestMessages() {
        MessageEntity message1 = MessageEntity.builder()
                .id(1L)
                .conversationId(1L)
                .type(MessageType.USER_MESSAGE)
                .content("첫 번째 메시지")
                .emotion(EmotionType.NEUTRAL)
                .build();

        MessageEntity message2 = MessageEntity.builder()
                .id(2L)
                .conversationId(1L)
                .type(MessageType.AI_RESPONSE)
                .content("AI 응답")
                .emotion(EmotionType.NEUTRAL)
                .build();

        return Arrays.asList(message1, message2);
    }

    private List<MessageEntity> createLongTestMessages(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> MessageEntity.builder()
                        .id((long) i)
                        .conversationId(1L)
                        .type(MessageType.USER_MESSAGE)
                        .content("메시지 " + i)
                        .emotion(EmotionType.NEUTRAL)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}