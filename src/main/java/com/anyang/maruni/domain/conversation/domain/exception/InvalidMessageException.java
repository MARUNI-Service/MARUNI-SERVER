package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 메시지 유효성 검증 실패 예외
 *
 * 메시지 내용이 비어있는 경우 발생합니다.
 */
public class InvalidMessageException extends BaseException {

    public InvalidMessageException() {
        super(ErrorCode.MESSAGE_EMPTY);
    }

    public InvalidMessageException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 메시지가 비어있을 때 발생하는 예외
     */
    public static InvalidMessageException emptyMessage() {
        return new InvalidMessageException(ErrorCode.MESSAGE_EMPTY);
    }

    /**
     * 메시지가 너무 길 때 발생하는 예외
     */
    public static InvalidMessageException messageTooLong() {
        return new InvalidMessageException(ErrorCode.MESSAGE_TOO_LONG);
    }
}