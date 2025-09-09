package com.anyang.maruni.global.response.error;



import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorType {
	// 4xx Client Error
	INVALID_ROLE("E400", "잘못된 역할(Role)입니다", HttpStatus.BAD_REQUEST.value()),
	TOKEN_MALFORMED("E400", "잘못된 형식의 토큰입니다", HttpStatus.BAD_REQUEST.value()),
	INVALID_INPUT_VALUE("E400", "잘못된 입력값입니다", HttpStatus.BAD_REQUEST.value()),
	PARAMETER_VALIDATION_ERROR("E400", "파라미터 검증에 실패했습니다", HttpStatus.BAD_REQUEST.value()),

	LOGIN_FAIL("E401", "이메일 또는 비밀번호가 틀렸습니다", HttpStatus.UNAUTHORIZED.value()),
	INVALID_TOKEN("E401", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	TOKEN_EXPIRED("E401", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	REFRESH_TOKEN_NOT_FOUND("E401", "리프레시 토큰을 찾을 수 없습니다", HttpStatus.UNAUTHORIZED.value()),
	ACCESS_TOKEN_REQUIRED("E401", "액세스 토큰이 필요합니다", HttpStatus.UNAUTHORIZED.value()),
	TOKEN_BLACKLISTED("E401", "차단된 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	OAUTH2_LOGIN_FAILED("E401", "소셜 로그인에 실패했습니다. 다시 시도해주세요.", HttpStatus.UNAUTHORIZED.value()),

	MEMBER_NOT_FOUND("E404", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),

	TOO_MANY_REQUESTS("E429", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),

	DUPLICATE_EMAIL("E409", "이미 가입된 이메일입니다", HttpStatus.CONFLICT.value()),

	// 5xx Server Error
	INTERNAL_SERVER_ERROR("E500", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value());

	private final String code;
	private final String message;
	private final int status;

	public int getHttpCode() {
		return this.status;
	}
}