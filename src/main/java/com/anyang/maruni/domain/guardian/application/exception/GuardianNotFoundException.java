package com.anyang.maruni.domain.guardian.application.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 보호자를 찾을 수 없을 때 발생하는 예외
 */
public class GuardianNotFoundException extends BaseException {

    public GuardianNotFoundException() {
        super(ErrorCode.GUARDIAN_NOT_FOUND);
    }
}