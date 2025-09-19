package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 대화 조회 실패 예외
 *
 * 요청한 대화를 찾을 수 없을 때 발생합니다.
 */
public class ConversationNotFoundException extends BaseException {

    public ConversationNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 일반적인 대화 조회 실패
     */
    public static ConversationNotFoundException notFound() {
        return new ConversationNotFoundException(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    /**
     * 특정 대화 ID로 조회 실패
     */
    public static ConversationNotFoundException byId() {
        return new ConversationNotFoundException(ErrorCode.CONVERSATION_NOT_FOUND_BY_ID);
    }

    /**
     * 회원의 활성 대화 조회 실패
     */
    public static ConversationNotFoundException noActiveConversation() {
        return new ConversationNotFoundException(ErrorCode.ACTIVE_CONVERSATION_NOT_FOUND);
    }
}