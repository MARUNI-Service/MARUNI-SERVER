package com.anyang.maruni.domain.conversation.application.dto;

import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 메시지 교환 결과 (Value Object)
 *
 * 사용자 메시지와 AI 응답 처리 결과를 캡슐화합니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class MessageExchangeResult {

    /**
     * 대화 엔티티
     */
    private final ConversationEntity conversation;

    /**
     * 사용자 메시지 엔티티
     */
    private final MessageEntity userMessage;

    /**
     * AI 응답 메시지 엔티티
     */
    private final MessageEntity aiMessage;
}