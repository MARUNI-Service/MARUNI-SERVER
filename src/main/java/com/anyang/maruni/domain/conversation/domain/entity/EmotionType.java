package com.anyang.maruni.domain.conversation.domain.entity;

/**
 * 감정 타입 열거형 (MVP 버전)
 * 
 * 메시지에서 분석된 감정 상태를 나타냅니다.
 * MVP에서는 간단한 3단계 감정 분석만 제공합니다.
 * 
 * - POSITIVE: 긍정적 감정 (기쁨, 행복, 감사 등)
 * - NEUTRAL: 중립적 감정 (평상시, 보통 등)  
 * - NEGATIVE: 부정적 감정 (슬픔, 우울, 힘듦 등)
 */
public enum EmotionType {
    /**
     * 긍정적 감정
     */
    POSITIVE,
    
    /**
     * 중립적 감정
     */
    NEUTRAL,
    
    /**
     * 부정적 감정
     */
    NEGATIVE
}