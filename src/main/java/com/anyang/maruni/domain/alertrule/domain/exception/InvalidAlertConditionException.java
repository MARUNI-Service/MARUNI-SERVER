package com.anyang.maruni.domain.alertrule.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 유효하지 않은 알림 조건일 때 발생하는 예외
 */
public class InvalidAlertConditionException extends BaseException {

    public InvalidAlertConditionException(String condition, String reason) {
        super(ErrorCode.INVALID_ALERT_CONDITION,
              String.format("유효하지 않은 알림 조건입니다. 조건: %s, 사유: %s", condition, reason));
    }

    public InvalidAlertConditionException(String message) {
        super(ErrorCode.INVALID_ALERT_CONDITION, message);
    }
}