package com.anyang.maruni.domain.alertrule.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 알림 규칙을 찾을 수 없을 때 발생하는 예외
 */
public class AlertRuleNotFoundException extends BaseException {

    public AlertRuleNotFoundException(Long alertRuleId) {
        super(ErrorCode.ALERT_RULE_NOT_FOUND, "알림 규칙을 찾을 수 없습니다. ID: " + alertRuleId);
    }

    public AlertRuleNotFoundException(String message) {
        super(ErrorCode.ALERT_RULE_NOT_FOUND, message);
    }
}