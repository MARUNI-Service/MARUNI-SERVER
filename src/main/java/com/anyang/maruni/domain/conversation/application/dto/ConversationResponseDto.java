package com.anyang.maruni.domain.conversation.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 대화 처리 응답 DTO
 * 
 * 사용자 메시지 처리 결과와 AI 응답을 포함합니다.
 */
@Getter
@Builder
public class ConversationResponseDto {
    
    /**
     * 대화 ID
     */
    private Long conversationId;
    
    /**
     * 사용자 메시지 정보
     */
    private MessageDto userMessage;
    
    /**
     * AI 응답 메시지 정보
     */
    private MessageDto aiMessage;
}