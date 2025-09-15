package com.anyang.maruni.domain.conversation.domain.entity;

import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메시지 엔티티 (MVP 버전)
 * 
 * 대화에서 주고받는 개별 메시지를 나타냅니다.
 * MVP에서는 기본적인 감정 분석 결과만 포함합니다.
 */
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MessageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속된 대화 ID
     */
    @Column(nullable = false)
    private Long conversationId;

    /**
     * 메시지 타입 (사용자 메시지 / AI 응답)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    /**
     * 메시지 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 감정 분석 결과 (MVP: 간단한 3단계)
     */
    @Enumerated(EnumType.STRING)
    private EmotionType emotion;

    /**
     * 정적 팩토리 메서드: 사용자 메시지 생성
     *
     * @param conversationId 대화 ID
     * @param content 메시지 내용
     * @param emotion 감정 분석 결과
     * @return 사용자 MessageEntity 인스턴스
     */
    public static MessageEntity createUserMessage(Long conversationId, String content, EmotionType emotion) {
        return MessageEntity.builder()
                .conversationId(conversationId)
                .type(MessageType.USER_MESSAGE)
                .content(content)
                .emotion(emotion)
                .build();
    }

    /**
     * 정적 팩토리 메서드: AI 응답 메시지 생성
     *
     * @param conversationId 대화 ID
     * @param content AI 응답 내용
     * @return AI MessageEntity 인스턴스
     */
    public static MessageEntity createAIResponse(Long conversationId, String content) {
        return MessageEntity.builder()
                .conversationId(conversationId)
                .type(MessageType.AI_RESPONSE)
                .content(content)
                .emotion(EmotionType.NEUTRAL) // AI 응답은 기본적으로 중립
                .build();
    }
}