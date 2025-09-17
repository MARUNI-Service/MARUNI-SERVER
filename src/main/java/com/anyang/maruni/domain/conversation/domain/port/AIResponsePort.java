package com.anyang.maruni.domain.conversation.domain.port;

import java.util.Collections;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.vo.ConversationContext;
import com.anyang.maruni.domain.conversation.domain.vo.MemberProfile;

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

    /**
     * 단순 메시지 기반 AI 응답 생성 (하위 호환성)
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답 내용
     * @deprecated ConversationContext를 사용하는 generateResponse(ConversationContext) 메서드를 권장합니다
     */
    @Deprecated
    default String generateResponse(String userMessage) {
        ConversationContext context = ConversationContext.forUserMessage(
                userMessage,
                Collections.emptyList(),
                MemberProfile.createDefault(null),
                EmotionType.NEUTRAL
        );
        return generateResponse(context);
    }
}