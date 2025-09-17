package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 메시지 유효성 검증 실패 예외
 *
 * 메시지 내용이 비어있거나 너무 긴 경우 발생합니다.
 */
public class InvalidMessageException extends BaseException {

    public InvalidMessageException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}