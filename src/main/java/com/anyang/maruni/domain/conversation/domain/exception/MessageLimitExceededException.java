package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 메시지 한도 초과 예외
 *
 * 일일 메시지 한도를 초과하거나 비활성 대화에 메시지를 추가하려 할 때 발생합니다.
 */
public class MessageLimitExceededException extends BaseException {

    public MessageLimitExceededException() {
        super(ErrorCode.DAILY_MESSAGE_LIMIT_EXCEEDED);
    }

    public MessageLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 일일 메시지 한도 초과 시 발생하는 예외
     */
    public static MessageLimitExceededException dailyLimitExceeded() {
        return new MessageLimitExceededException(ErrorCode.DAILY_MESSAGE_LIMIT_EXCEEDED);
    }

    /**
     * 비활성 대화에 메시지 추가 시도 시 발생하는 예외
     */
    public static MessageLimitExceededException inactiveConversation() {
        return new MessageLimitExceededException(ErrorCode.CONVERSATION_INACTIVE);
    }
}