package com.anyang.maruni.global.swagger;

import java.util.Set;

import com.anyang.maruni.global.response.error.ErrorCode;

import lombok.Getter;

@Getter
public enum SwaggerResponseDescription {

	MEMBER_ERROR(ErrorCode.MEMBER_NOT_FOUND, ErrorCode.DUPLICATE_EMAIL),

	MEMBER_JOIN_ERROR(ErrorCode.DUPLICATE_EMAIL, ErrorCode.PARAMETER_VALIDATION_ERROR, ErrorCode.INVALID_INPUT_VALUE),

	AUTH_ERROR(ErrorCode.INVALID_TOKEN, ErrorCode.REFRESH_TOKEN_NOT_FOUND, ErrorCode.LOGIN_FAIL),

	COMMON_ERROR(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INVALID_INPUT_VALUE);

	private final Set<ErrorCode> errorCodeList;

	SwaggerResponseDescription(ErrorCode... errorCodes) {
		this.errorCodeList = Set.of(errorCodes);
	}

}
