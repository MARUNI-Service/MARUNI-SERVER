package com.anyang.maruni.global.exception;

import com.anyang.maruni.global.response.error.ErrorType;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
	private final ErrorType errorCode;

	public BaseException(ErrorType errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
