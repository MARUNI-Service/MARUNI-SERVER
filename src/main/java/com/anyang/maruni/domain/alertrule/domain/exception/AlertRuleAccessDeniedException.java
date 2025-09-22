package com.anyang.maruni.domain.alertrule.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 알림 규칙에 대한 접근 권한이 없을 때 발생하는 예외
 */
public class AlertRuleAccessDeniedException extends BaseException {

    public AlertRuleAccessDeniedException(Long alertRuleId, Long memberId) {
        super(ErrorCode.ALERT_RULE_ACCESS_DENIED,
              String.format("회원 ID %d는 알림 규칙 ID %d에 접근할 권한이 없습니다.", memberId, alertRuleId));
    }

    public AlertRuleAccessDeniedException(String message) {
        super(ErrorCode.ALERT_RULE_ACCESS_DENIED, message);
    }
}