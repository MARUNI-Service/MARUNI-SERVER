package com.anyang.maruni.domain.guardian.application.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * Guardian 도메인 비즈니스 로직 예외
 *
 * 다양한 Guardian 관련 비즈니스 규칙 위반 시 사용되는 예외
 * ErrorCode를 통해 구체적인 오류 상황을 구분
 *
 * 사용 예시:
 * - throw new GuardianBusinessException(ErrorCode.GUARDIAN_ALREADY_ASSIGNED);
 * - throw new GuardianBusinessException(ErrorCode.GUARDIAN_DEACTIVATION_FAILED);
 * - throw new GuardianBusinessException(ErrorCode.GUARDIAN_SELF_ASSIGNMENT_NOT_ALLOWED);
 */
public class GuardianBusinessException extends BaseException {

    public GuardianBusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}