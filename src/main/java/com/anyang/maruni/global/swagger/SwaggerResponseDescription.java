package com.anyang.maruni.global.swagger;

import java.util.Set;

import com.anyang.maruni.global.response.error.ErrorCode;

import lombok.Getter;

@Getter
public enum SwaggerResponseDescription {

	MEMBER_ERROR(Set.of(
		ErrorCode.MEMBER_NOT_FOUND,
		ErrorCode.DUPLICATE_EMAIL
	)),

	MEMBER_JOIN_ERROR(Set.of(
		ErrorCode.DUPLICATE_EMAIL,
		ErrorCode.PARAMETER_VALIDATION_ERROR,
		ErrorCode.INVALID_INPUT_VALUE
	)),

	AUTH_ERROR(Set.of(
		ErrorCode.INVALID_TOKEN,
		ErrorCode.REFRESH_TOKEN_NOT_FOUND,
		ErrorCode.LOGIN_FAIL
	)),

	COMMON_ERROR(Set.of(
		ErrorCode.INTERNAL_SERVER_ERROR,
		ErrorCode.INVALID_INPUT_VALUE
	)),

	ALL_ERROR(Set.of(
		ErrorCode.MEMBER_NOT_FOUND,
		ErrorCode.DUPLICATE_EMAIL,
		ErrorCode.INVALID_TOKEN,
		ErrorCode.REFRESH_TOKEN_NOT_FOUND,
		ErrorCode.LOGIN_FAIL,
		ErrorCode.INTERNAL_SERVER_ERROR,
		ErrorCode.INVALID_INPUT_VALUE
	));

	private final Set<ErrorCode> errorCodeList;

	SwaggerResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = errorCodeList;
	}

}
