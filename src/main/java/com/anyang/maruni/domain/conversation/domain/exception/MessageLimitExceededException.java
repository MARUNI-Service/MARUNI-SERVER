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
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}