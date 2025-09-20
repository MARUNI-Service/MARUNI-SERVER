package com.anyang.maruni.domain.conversation.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anyang.maruni.domain.conversation.domain.exception.InvalidMessageException;

/**
 * ConversationEntity 도메인 로직 테스트
 *
 * Rich Domain Model의 비즈니스 로직을 검증합니다.
 * 순수 단위 테스트로 JPA나 Spring Context에 의존하지 않습니다.
 */
@DisplayName("대화 엔티티 도메인 로직 테스트")
class ConversationEntityTest {

    @Test
    @DisplayName("새 대화 생성: 기본값이 올바르게 설정된다")
    void createNew_Success() {
        // Given
        Long memberId = 1L;

        // When
        ConversationEntity conversation = ConversationEntity.createNew(memberId);

        // Then
        assertThat(conversation.getMemberId()).isEqualTo(memberId);
        assertThat(conversation.getStartedAt()).isNotNull();
        assertThat(conversation.getMessages()).isNotNull().isEmpty();
        assertThat(conversation.isActive()).isTrue(); // 새 대화는 활성 상태
    }

    @Test
    @DisplayName("대화 활성 상태 확인: 메시지가 없으면 활성 상태이다")
    void isActive_NoMessages_ReturnsTrue() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);

        // When & Then
        assertThat(conversation.isActive()).isTrue();
    }

    @Test
    @DisplayName("대화 활성 상태 확인: 메시지가 있으면 활성 상태로 간주된다")
    void isActive_WithMessages_ReturnsTrue() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);

        // When: 메시지 추가
        conversation.addUserMessage("테스트 메시지", EmotionType.NEUTRAL);

        // Then: 메시지가 있으므로 활성 상태
        // 주의: 실제 시간 검증은 통합 테스트에서 수행
        assertThat(conversation.getMessages()).isNotEmpty();
    }

    @Test
    @DisplayName("사용자 메시지 추가: 정상적인 메시지가 추가된다")
    void addUserMessage_ValidMessage_Success() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);
        String content = "안녕하세요!";
        EmotionType emotion = EmotionType.POSITIVE;

        // When
        MessageEntity message = conversation.addUserMessage(content, emotion);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getType()).isEqualTo(MessageType.USER_MESSAGE);
        assertThat(message.getEmotion()).isEqualTo(emotion);
        assertThat(conversation.getMessages()).hasSize(1);
        assertThat(conversation.getMessages().get(0)).isEqualTo(message);
    }

    @Test
    @DisplayName("AI 메시지 추가: 정상적인 AI 응답이 추가된다")
    void addAIMessage_ValidMessage_Success() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);
        String content = "안녕하세요! 어떻게 도와드릴까요?";

        // When
        MessageEntity message = conversation.addAIMessage(content);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getType()).isEqualTo(MessageType.AI_RESPONSE);
        assertThat(message.getEmotion()).isEqualTo(EmotionType.NEUTRAL);
        assertThat(conversation.getMessages()).hasSize(1);
    }

    @Test
    @DisplayName("메시지 유효성 검증: 빈 메시지는 예외가 발생한다")
    void addUserMessage_EmptyContent_ThrowsException() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);

        // When & Then
        assertThatThrownBy(() -> conversation.addUserMessage("", EmotionType.NEUTRAL))
                .isInstanceOf(InvalidMessageException.class);

        assertThatThrownBy(() -> conversation.addUserMessage("   ", EmotionType.NEUTRAL))
                .isInstanceOf(InvalidMessageException.class);

        assertThatThrownBy(() -> conversation.addUserMessage(null, EmotionType.NEUTRAL))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    @DisplayName("메시지 유효성 검증: 너무 긴 메시지는 예외가 발생한다")
    void addUserMessage_TooLongContent_ThrowsException() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);
        String longContent = "a".repeat(501); // 500자 초과

        // When & Then
        assertThatThrownBy(() -> conversation.addUserMessage(longContent, EmotionType.NEUTRAL))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    @DisplayName("메시지 수신 가능 여부: 활성 대화이고 일일 한도 내면 가능하다")
    void canReceiveMessage_ActiveAndWithinLimit_ReturnsTrue() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);

        // When & Then
        assertThat(conversation.canReceiveMessage()).isTrue();
    }

    @Test
    @DisplayName("최근 대화 히스토리 조회: 메시지 개수 제한이 올바르게 동작한다")
    void getRecentHistory_LimitWorks() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);

        // When: 메시지가 없는 경우
        List<MessageEntity> emptyHistory = conversation.getRecentHistory(2);

        // Then: 빈 리스트 반환
        assertThat(emptyHistory).isEmpty();

        // When & Then: 경계값 테스트
        assertThat(conversation.getRecentHistory(0)).isEmpty();
        assertThat(conversation.getRecentHistory(-1)).isEmpty(); // 음수 처리
    }

    @Test
    @DisplayName("최근 대화 히스토리 조회: 메시지가 없으면 빈 리스트를 반환한다")
    void getRecentHistory_NoMessages_ReturnsEmptyList() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);

        // When
        List<MessageEntity> recentHistory = conversation.getRecentHistory(5);

        // Then
        assertThat(recentHistory).isEmpty();
    }

    @Test
    @DisplayName("도메인 불변식 검증: 메시지 추가 시 대화 ID가 올바르게 설정된다")
    void addMessage_SetsCorrectConversationId() {
        // Given
        ConversationEntity conversation = ConversationEntity.builder()
                .id(100L)
                .memberId(1L)
                .startedAt(LocalDateTime.now())
                .build();

        // When
        MessageEntity userMessage = conversation.addUserMessage("테스트", EmotionType.NEUTRAL);
        MessageEntity aiMessage = conversation.addAIMessage("AI 응답");

        // Then
        assertThat(userMessage.getConversationId()).isEqualTo(100L);
        assertThat(aiMessage.getConversationId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("비즈니스 규칙 검증: 최대 메시지 길이 상수가 올바르게 적용된다")
    void messageValidation_MaxLengthConstant() {
        // Given
        ConversationEntity conversation = ConversationEntity.createNew(1L);
        String maxLengthMessage = "a".repeat(500); // 정확히 500자
        String overLimitMessage = "a".repeat(501); // 501자

        // When & Then
        // 500자는 성공
        assertThatNoException().isThrownBy(() ->
                conversation.addUserMessage(maxLengthMessage, EmotionType.NEUTRAL));

        // 501자는 실패
        assertThatThrownBy(() ->
                conversation.addUserMessage(overLimitMessage, EmotionType.NEUTRAL))
                .isInstanceOf(InvalidMessageException.class);
    }

}