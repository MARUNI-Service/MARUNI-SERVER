package com.anyang.maruni.domain.conversation.domain.port;

/**
 * AI 응답 생성 포트 인터페이스
 *
 * AI 모델에 독립적인 응답 생성 기능을 정의합니다.
 * OpenAI, Claude, Gemini 등 다양한 AI 모델을 지원할 수 있습니다.
 */
public interface AIResponsePort {

    /**
     * 사용자 메시지에 대한 AI 응답 생성
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답 내용
     */
    String generateResponse(String userMessage);
}