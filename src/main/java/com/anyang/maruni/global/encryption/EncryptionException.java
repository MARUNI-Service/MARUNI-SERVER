package com.anyang.maruni.global.encryption;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 암호화/복호화 관련 예외
 */
public class EncryptionException extends BaseException {

    public EncryptionException(String message) {
        super(ErrorCode.ENCRYPTION_ERROR, message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(ErrorCode.ENCRYPTION_ERROR, message, cause);
    }
}