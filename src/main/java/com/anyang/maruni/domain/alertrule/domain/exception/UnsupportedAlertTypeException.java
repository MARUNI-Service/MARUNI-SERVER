package com.anyang.maruni.domain.alertrule.domain.exception;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 지원하지 않는 알림 타입일 때 발생하는 예외
 */
public class UnsupportedAlertTypeException extends BaseException {

    public UnsupportedAlertTypeException(AlertType alertType) {
        super(ErrorCode.UNSUPPORTED_ALERT_TYPE);
    }

    public UnsupportedAlertTypeException() {
        super(ErrorCode.UNSUPPORTED_ALERT_TYPE);
    }
}