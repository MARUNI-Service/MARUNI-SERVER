package com.anyang.maruni.domain.guardian.application.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 회원을 찾을 수 없을 때 발생하는 예외 (Guardian 컨텍스트)
 */
public class MemberNotFoundException extends BaseException {

    public MemberNotFoundException(Long memberId) {
        super(ErrorCode.MEMBER_NOT_FOUND);
    }
}