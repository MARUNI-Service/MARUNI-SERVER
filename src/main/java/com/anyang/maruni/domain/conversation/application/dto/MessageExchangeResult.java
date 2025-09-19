package com.anyang.maruni.domain.conversation.application.dto;

import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;

import lombok.Builder;

/**
 * 메시지 교환 결과 (Value Object)
 *
 * 사용자 메시지와 AI 응답 처리 결과를 캡슐화합니다.
 * @param conversation
대화 엔티티
 * @param userMessage
사용자 메시지 엔티티
 * @param aiMessage
AI 응답 메시지 엔티티
 */
@Builder
public record MessageExchangeResult(ConversationEntity conversation, MessageEntity userMessage,
                                    MessageEntity aiMessage) {

    /**
     * 메시지 교환 결과 생성
     *
     * @param conversation 대화 엔티티
     * @param userMessage 사용자 메시지 엔티티
     * @param aiMessage AI 응답 메시지 엔티티
     * @return 메시지 교환 결과
     */
    public static MessageExchangeResult of(ConversationEntity conversation,
                                          MessageEntity userMessage,
                                          MessageEntity aiMessage) {
        return MessageExchangeResult.builder()
            .conversation(conversation)
            .userMessage(userMessage)
            .aiMessage(aiMessage)
            .build();
    }
}