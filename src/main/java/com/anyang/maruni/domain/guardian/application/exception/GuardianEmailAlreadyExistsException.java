package com.anyang.maruni.domain.guardian.application.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 보호자 이메일 중복 예외
 *
 * IllegalArgumentException 대신 BaseException 계열 예외 사용으로
 * 글로벌 예외 처리 시스템과 일관성 확보
 */
public class GuardianEmailAlreadyExistsException extends BaseException {

    public GuardianEmailAlreadyExistsException() {
        super(ErrorCode.GUARDIAN_EMAIL_ALREADY_EXISTS);
    }
}