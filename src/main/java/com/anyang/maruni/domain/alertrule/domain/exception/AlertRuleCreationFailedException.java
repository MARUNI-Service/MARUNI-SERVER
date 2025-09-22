package com.anyang.maruni.domain.alertrule.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 알림 규칙 생성에 실패했을 때 발생하는 예외
 */
public class AlertRuleCreationFailedException extends BaseException {

    public AlertRuleCreationFailedException(String alertType, Throwable cause) {
        super(ErrorCode.ALERT_RULE_CREATION_FAILED);
    }

    public AlertRuleCreationFailedException() {
        super(ErrorCode.ALERT_RULE_CREATION_FAILED);
    }
}