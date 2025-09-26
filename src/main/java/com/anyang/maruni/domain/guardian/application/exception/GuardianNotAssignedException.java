package com.anyang.maruni.domain.guardian.application.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 회원에게 보호자가 설정되지 않았을 때 발생하는 예외
 */
public class GuardianNotAssignedException extends BaseException {

    public GuardianNotAssignedException() {
        super(ErrorCode.GUARDIAN_NOT_ASSIGNED);
    }
}