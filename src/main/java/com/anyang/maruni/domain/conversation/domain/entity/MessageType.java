package com.anyang.maruni.domain.conversation.domain.entity;

/**
 * 메시지 타입 열거형
 * 
 * 대화에서 발생하는 메시지의 종류를 구분합니다.
 * - USER_MESSAGE: 사용자가 보낸 메시지
 * - AI_RESPONSE: AI가 생성한 응답 메시지
 */
public enum MessageType {
    /**
     * 사용자 메시지
     */
    USER_MESSAGE,
    
    /**
     * AI 응답 메시지
     */
    AI_RESPONSE
}