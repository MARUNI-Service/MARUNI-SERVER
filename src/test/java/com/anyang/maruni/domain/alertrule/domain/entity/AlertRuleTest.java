package com.anyang.maruni.domain.alertrule.domain.entity;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlertRule Entity 테스트
 *
 * TDD Red 단계: 실패하는 테스트 작성
 */
@DisplayName("AlertRule Entity 테스트")
class AlertRuleTest {

    private MemberEntity testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = MemberEntity.builder()
                .memberName("테스트회원")
                .memberEmail("test@example.com")
                .memberPassword("password123")
                .build();
    }

    @Test
    @DisplayName("감정 패턴 규칙 생성 테스트")
    void createEmotionPatternRule_shouldCreateValidRule() {
        // Given
        int consecutiveDays = 3;
        AlertLevel alertLevel = AlertLevel.HIGH;

        // When
        AlertRule rule = AlertRule.createEmotionPatternRule(testMember, consecutiveDays, alertLevel);

        // Then
        assertThat(rule.getMember()).isEqualTo(testMember);
        assertThat(rule.getAlertType()).isEqualTo(AlertType.EMOTION_PATTERN);
        assertThat(rule.getRuleName()).isEqualTo("연속 부정감정 감지");
        assertThat(rule.getAlertLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(rule.getIsActive()).isTrue();
        assertThat(rule.getCondition().getConsecutiveDays()).isEqualTo(3);
        assertThat(rule.getCondition().getTargetEmotion()).isEqualTo(EmotionType.NEGATIVE);
    }

    @Test
    @DisplayName("무응답 규칙 생성 테스트")
    void createNoResponseRule_shouldCreateValidRule() {
        // Given
        int noResponseDays = 2;
        AlertLevel alertLevel = AlertLevel.EMERGENCY;

        // When
        AlertRule rule = AlertRule.createNoResponseRule(testMember, noResponseDays, alertLevel);

        // Then
        assertThat(rule.getMember()).isEqualTo(testMember);
        assertThat(rule.getAlertType()).isEqualTo(AlertType.NO_RESPONSE);
        assertThat(rule.getRuleName()).isEqualTo("무응답 감지");
        assertThat(rule.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(rule.getCondition().getConsecutiveDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("키워드 규칙 생성 테스트")
    void createKeywordRule_shouldCreateValidRule() {
        // Given
        String keywords = "도움말,응급상황,아파요";
        AlertLevel alertLevel = AlertLevel.EMERGENCY;

        // When
        AlertRule rule = AlertRule.createKeywordRule(testMember, keywords, alertLevel);

        // Then
        assertThat(rule.getMember()).isEqualTo(testMember);
        assertThat(rule.getAlertType()).isEqualTo(AlertType.KEYWORD_DETECTION);
        assertThat(rule.getRuleName()).isEqualTo("키워드 감지");
        assertThat(rule.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(rule.getCondition().getKeywords()).isEqualTo(keywords);
    }

    @Test
    @DisplayName("연속 부정감정 감지 테스트")
    void shouldTriggerAlert_withConsecutiveNegativeEmotions() {
        // Given
        AlertRule rule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);

        List<MessageEntity> messages = Arrays.asList(
                createMessage("오늘 너무 우울해요", EmotionType.NEGATIVE),
                createMessage("정말 힘들어요", EmotionType.NEGATIVE),
                createMessage("더 이상 견딜 수 없어요", EmotionType.NEGATIVE)
        );

        // When
        boolean shouldTrigger = rule.shouldTriggerAlert(messages);

        // Then
        assertThat(shouldTrigger).isTrue();
    }

    @Test
    @DisplayName("활성화/비활성화 테스트")
    void activateDeactivateRule_shouldToggleStatus() {
        // Given
        AlertRule rule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);
        assertThat(rule.getIsActive()).isTrue();

        // When & Then - 비활성화
        rule.deactivate();
        assertThat(rule.getIsActive()).isFalse();

        // When & Then - 다시 활성화
        rule.activate();
        assertThat(rule.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("규칙 정보 업데이트 테스트")
    void updateRule_shouldUpdateRuleInfo() {
        // Given
        AlertRule rule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);
        String newName = "수정된 규칙 이름";
        String newDescription = "수정된 규칙 설명";
        AlertLevel newLevel = AlertLevel.EMERGENCY;

        // When
        rule.updateRule(newName, newDescription, newLevel);

        // Then
        assertThat(rule.getRuleName()).isEqualTo(newName);
        assertThat(rule.getRuleDescription()).isEqualTo(newDescription);
        assertThat(rule.getAlertLevel()).isEqualTo(newLevel);
    }

    @Test
    @DisplayName("비활성 규칙은 알림을 발생시키지 않음")
    void deactivatedRule_shouldNotTriggerAlert() {
        // Given
        AlertRule rule = AlertRule.createEmotionPatternRule(testMember, 2, AlertLevel.HIGH);
        rule.deactivate();

        List<MessageEntity> messages = Arrays.asList(
                createMessage("우울해요", EmotionType.NEGATIVE),
                createMessage("힘들어요", EmotionType.NEGATIVE)
        );

        // When
        boolean shouldTrigger = rule.shouldTriggerAlert(messages);

        // Then
        assertThat(shouldTrigger).isFalse();
    }

    private MessageEntity createMessage(String content, EmotionType emotion) {
        return MessageEntity.createUserMessage(
                null, // ConversationEntity는 null로 설정 (테스트용)
                content,
                emotion
        );
    }
}