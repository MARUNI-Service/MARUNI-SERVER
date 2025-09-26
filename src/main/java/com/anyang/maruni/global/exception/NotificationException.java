package com.anyang.maruni.global.exception;

import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 알림 관련 예외 클래스
 *
 * Firebase 연결 실패, 푸시 토큰 오류, 알림 발송 실패 등
 * 알림 시스템에서 발생하는 모든 예외를 처리합니다.
 */
public class NotificationException extends BaseException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode);
        this.initCause(cause); // 원인 예외 설정
    }
}