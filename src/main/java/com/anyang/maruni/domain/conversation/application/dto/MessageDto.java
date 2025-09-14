package com.anyang.maruni.domain.conversation.application.dto;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
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
}