package com.anyang.maruni.domain.conversation.application.mapper;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.conversation.application.dto.MessageDto;
import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;

/**
 * 대화 도메인 매퍼
 *
 * Entity ↔ DTO 변환 로직을 중앙 관리합니다.
 * SimpleConversationService에서 분리된 단일 책임을 가집니다.
 */
@Component
public class ConversationMapper {

    /**
     * MessageExchangeResult를 ConversationResponseDto로 변환
     *
     * @param result 메시지 교환 결과
     * @return 대화 응답 DTO
     */
    public ConversationResponseDto toResponseDto(MessageExchangeResult result) {
        return ConversationResponseDto.from(result);
    }
}