package com.anyang.maruni.domain.conversation.application.dto;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 메시지 DTO
 * 
 * 사용자 메시지 또는 AI 응답 메시지 정보를 담습니다.
 */
@Getter
@Builder
public class MessageDto {
    
    /**
     * 메시지 ID
     */
    private Long id;
    
    /**
     * 메시지 타입 (USER/AI)
     */
    private MessageType type;
    
    /**
     * 메시지 내용
     */
    private String content;
    
    /**
     * 감정 분석 결과
     */
    private EmotionType emotion;
    
    /**
     * 메시지 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * MessageEntity를 MessageDto로 변환하는 정적 팩토리 메서드
     *
     * @param entity 메시지 엔티티
     * @return 메시지 DTO
     */
    public static MessageDto from(MessageEntity entity) {
        return MessageDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .content(entity.getContent())
                .emotion(entity.getEmotion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}