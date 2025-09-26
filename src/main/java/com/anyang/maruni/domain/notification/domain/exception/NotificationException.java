package com.anyang.maruni.domain.notification.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorType;

/**
 * 알림 도메인 예외
 *
 * 알림 관련 비즈니스 예외를 처리합니다.
 * BaseException을 상속하여 MARUNI 프로젝트의 예외 처리 패턴을 따릅니다.
 */
public class NotificationException extends BaseException {

    /**
     * 알림 예외 생성
     *
     * @param errorCode 에러 코드
     */
    public NotificationException(ErrorType errorCode) {
        super(errorCode);
    }

    /**
     * 알림 예외 생성 (원인 예외 포함)
     *
     * @param errorCode 에러 코드
     * @param cause 원인 예외
     */
    public NotificationException(ErrorType errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}