package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 대화 조회 실패 예외
 *
 * 요청한 대화를 찾을 수 없을 때 발생합니다.
 */
public class ConversationNotFoundException extends BaseException {

    public ConversationNotFoundException() {
        super(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    public ConversationNotFoundException(Long conversationId) {
        super(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    public ConversationNotFoundException(Long memberId, String context) {
        super(ErrorCode.CONVERSATION_NOT_FOUND);
    }
}