package com.anyang.maruni.domain.conversation.application.dto.response;

import com.anyang.maruni.domain.conversation.application.dto.MessageDto;
import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화 처리 응답 DTO
 *
 * 사용자 메시지 처리 결과와 AI 응답을 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * MessageExchangeResult로부터 ConversationResponseDto 생성
     *
     * @param result 메시지 교환 결과
     * @return 대화 응답 DTO
     */
    public static ConversationResponseDto from(MessageExchangeResult result) {
        return ConversationResponseDto.builder()
                .conversationId(result.getConversation().getId())
                .userMessage(MessageDto.from(result.getUserMessage()))
                .aiMessage(MessageDto.from(result.getAiMessage()))
                .build();
    }

    /**
     * 테스트용 간단한 팩토리 메서드
     *
     * @param conversationId 대화 ID
     * @return 최소한의 ConversationResponseDto
     */
    public static ConversationResponseDto withId(Long conversationId) {
        return ConversationResponseDto.builder()
                .conversationId(conversationId)
                .build();
    }
}