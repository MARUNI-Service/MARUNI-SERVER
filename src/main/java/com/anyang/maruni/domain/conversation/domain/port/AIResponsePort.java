package com.anyang.maruni.domain.conversation.domain.port;

import com.anyang.maruni.domain.conversation.domain.vo.ConversationContext;

/**
 * AI 응답 생성 포트 인터페이스
 *
 * AI 모델에 독립적인 응답 생성 기능을 정의합니다.
 * OpenAI, Claude, Gemini 등 다양한 AI 모델을 지원할 수 있습니다.
 * 멀티턴 대화와 컨텍스트 기반 응답 생성을 지원합니다.
 */
public interface AIResponsePort {

    /**
     * 대화 컨텍스트를 활용한 AI 응답 생성 (권장)
     *
     * @param context 대화 컨텍스트 (메시지, 히스토리, 사용자 정보 등)
     * @return AI 응답 내용
     */
    String generateResponse(ConversationContext context);


}