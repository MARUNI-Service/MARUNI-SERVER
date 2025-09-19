package com.anyang.maruni.domain.alertrule.domain.entity;

import java.util.Arrays;
import java.util.List;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 조건 임베디드 엔티티
 *
 * 이상징후 감지를 위한 구체적인 조건들을 정의합니다.
 * AlertRule 엔티티에 임베디드되어 사용됩니다.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertCondition {

    /**
     * 연속 일수 (감정 패턴, 무응답 감지에 사용)
     */
    @Column(name = "consecutive_days")
    private Integer consecutiveDays;

    /**
     * 임계값 개수 (특정 조건 발생 횟수)
     */
    @Column(name = "threshold_count")
    private Integer thresholdCount;

    /**
     * 대상 감정 타입 (감정 패턴 감지에 사용)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_emotion")
    private EmotionType targetEmotion;

    /**
     * 키워드 목록 (JSON 형태로 저장)
     */
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 감정 패턴 조건 생성
     * @param consecutiveDays 연속 부정감정 일수
     * @return AlertCondition
     */
    public static AlertCondition createEmotionCondition(int consecutiveDays) {
        return AlertCondition.builder()
                .consecutiveDays(consecutiveDays)
                .targetEmotion(EmotionType.NEGATIVE)
                .thresholdCount(1)
                .build();
    }

    /**
     * 무응답 조건 생성
     * @param noResponseDays 무응답 일수
     * @return AlertCondition
     */
    public static AlertCondition createNoResponseCondition(int noResponseDays) {
        return AlertCondition.builder()
                .consecutiveDays(noResponseDays)
                .thresholdCount(0)
                .build();
    }

    /**
     * 키워드 감지 조건 생성
     * @param keywords 감지할 키워드 문자열 (쉼표 구분)
     * @return AlertCondition
     */
    public static AlertCondition createKeywordCondition(String keywords) {
        return AlertCondition.builder()
                .keywords(keywords)
                .thresholdCount(1)
                .build();
    }

    // ========== 비즈니스 로직: 조건 평가 ==========

    /**
     * 주어진 메시지들이 알림 조건을 만족하는지 평가
     * @param recentMessages 최근 메시지 목록
     * @param alertType 알림 유형
     * @return 조건 만족 여부
     */
    public boolean evaluate(List<MessageEntity> recentMessages, AlertType alertType) {
		return switch (alertType) {
			case EMOTION_PATTERN -> evaluateEmotionPattern(recentMessages);
			case NO_RESPONSE -> evaluateNoResponsePattern(recentMessages);
			case KEYWORD_DETECTION -> evaluateKeywordPattern(recentMessages);
			default -> false;
		};
    }

    /**
     * 감정 패턴 평가 (연속적인 부정 감정)
     */
    private boolean evaluateEmotionPattern(List<MessageEntity> messages) {
        if (consecutiveDays == null || messages.isEmpty()) {
            return false;
        }

        int consecutiveNegativeDays = 0;
        for (MessageEntity message : messages) {
            if (message.getEmotion() == EmotionType.NEGATIVE) {
                consecutiveNegativeDays++;
                if (consecutiveNegativeDays >= this.consecutiveDays) {
                    return true;
                }
            } else {
                consecutiveNegativeDays = 0;
            }
        }
        return false;
    }

    /**
     * 무응답 패턴 평가
     */
    private boolean evaluateNoResponsePattern(List<MessageEntity> messages) {
        if (consecutiveDays == null) {
            return false;
        }

        // 기대되는 응답 수보다 실제 응답이 적으면 무응답으로 판단
        return messages.size() < this.consecutiveDays;
    }

    /**
     * 키워드 패턴 평가
     */
    private boolean evaluateKeywordPattern(List<MessageEntity> messages) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return false;
        }

        String[] keywordArray = keywords.split(",");

        return messages.stream()
                .anyMatch(message ->
                        Arrays.stream(keywordArray)
                                .anyMatch(keyword ->
                                        message.getContent().toLowerCase()
                                                .contains(keyword.trim().toLowerCase())
                                )
                );
    }
}