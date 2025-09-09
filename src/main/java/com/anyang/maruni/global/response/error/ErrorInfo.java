package com.anyang.maruni.global.response.error;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorInfo {
	private String code;
	private String message;
	private Object details;

	public static ErrorInfo of(ErrorCode errorCode) {
		return ErrorInfo.builder()
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.build();
	}

	public static ErrorInfo ofWithDetails(ErrorCode errorCode, Object details) {
		return ErrorInfo.builder()
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.details(details)
			.build();
	}
}
