package com.anyang.maruni.domain.alertrule.domain.entity;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlertCondition Entity 테스트
 *
 * TDD Red 단계: 조건 평가 로직 테스트
 */
@DisplayName("AlertCondition 조건 평가 테스트")
class AlertConditionTest {

    @Test
    @DisplayName("감정 패턴 조건 생성 테스트")
    void createEmotionCondition_shouldCreateValidCondition() {
        // Given
        int consecutiveDays = 3;

        // When
        AlertCondition condition = AlertCondition.createEmotionCondition(consecutiveDays);

        // Then
        assertThat(condition.getConsecutiveDays()).isEqualTo(3);
        assertThat(condition.getTargetEmotion()).isEqualTo(EmotionType.NEGATIVE);
        assertThat(condition.getThresholdCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("무응답 조건 생성 테스트")
    void createNoResponseCondition_shouldCreateValidCondition() {
        // Given
        int noResponseDays = 2;

        // When
        AlertCondition condition = AlertCondition.createNoResponseCondition(noResponseDays);

        // Then
        assertThat(condition.getConsecutiveDays()).isEqualTo(2);
        assertThat(condition.getThresholdCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("키워드 조건 생성 테스트")
    void createKeywordCondition_shouldCreateValidCondition() {
        // Given
        String keywords = "도움말,응급,아파요";

        // When
        AlertCondition condition = AlertCondition.createKeywordCondition(keywords);

        // Then
        assertThat(condition.getKeywords()).isEqualTo(keywords);
        assertThat(condition.getThresholdCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("연속 부정감정 패턴 감지 - 조건 만족")
    void evaluateEmotionPattern_shouldDetectConsecutiveNegative() {
        // Given
        AlertCondition condition = AlertCondition.createEmotionCondition(3);
        List<MessageEntity> messages = Arrays.asList(
                createMessage("우울해요", EmotionType.NEGATIVE),
                createMessage("힘들어요", EmotionType.NEGATIVE),
                createMessage("절망적이에요", EmotionType.NEGATIVE)
        );

        // When
        boolean result = condition.evaluate(messages, AlertType.EMOTION_PATTERN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("연속 부정감정 패턴 감지 - 조건 불만족")
    void evaluateEmotionPattern_shouldNotDetectWhenNotConsecutive() {
        // Given
        AlertCondition condition = AlertCondition.createEmotionCondition(3);
        List<MessageEntity> messages = Arrays.asList(
                createMessage("우울해요", EmotionType.NEGATIVE),
                createMessage("괜찮아요", EmotionType.POSITIVE),
                createMessage("힘들어요", EmotionType.NEGATIVE)
        );

        // When
        boolean result = condition.evaluate(messages, AlertType.EMOTION_PATTERN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("무응답 패턴 감지 - 조건 만족")
    void evaluateNoResponsePattern_shouldDetectNoResponse() {
        // Given
        AlertCondition condition = AlertCondition.createNoResponseCondition(2);
        List<MessageEntity> messages = Collections.singletonList(
                createMessage("하나의 메시지만", EmotionType.NEUTRAL)
        );

        // When
        boolean result = condition.evaluate(messages, AlertType.NO_RESPONSE);

        // Then
        assertThat(result).isTrue(); // 기대: 2개, 실제: 1개 → 무응답
    }

    @Test
    @DisplayName("키워드 감지 - 위험 키워드 포함")
    void evaluateKeywordPattern_shouldDetectDangerousKeywords() {
        // Given
        AlertCondition condition = AlertCondition.createKeywordCondition("도움말,응급,아파요");
        List<MessageEntity> messages = Arrays.asList(
                createMessage("정말 아파요", EmotionType.NEGATIVE)
        );

        // When
        boolean result = condition.evaluate(messages, AlertType.KEYWORD_DETECTION);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키워드 감지 - 위험 키워드 없음")
    void evaluateKeywordPattern_shouldNotDetectSafeContent() {
        // Given
        AlertCondition condition = AlertCondition.createKeywordCondition("도움말,응급,아파요");
        List<MessageEntity> messages = Arrays.asList(
                createMessage("오늘 날씨가 좋네요", EmotionType.POSITIVE)
        );

        // When
        boolean result = condition.evaluate(messages, AlertType.KEYWORD_DETECTION);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 메시지 리스트 처리 테스트")
    void evaluate_shouldHandleEmptyMessages() {
        // Given
        AlertCondition condition = AlertCondition.createEmotionCondition(1);
        List<MessageEntity> emptyMessages = Collections.emptyList();

        // When
        boolean result = condition.evaluate(emptyMessages, AlertType.EMOTION_PATTERN);

        // Then
        assertThat(result).isFalse();
    }

    private MessageEntity createMessage(String content, EmotionType emotion) {
        return MessageEntity.createUserMessage(
                null, // ConversationEntity는 null로 설정 (테스트용)
                content,
                emotion
        );
    }
}