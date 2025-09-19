package com.anyang.maruni.global.response.error;



import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorType {
	// 4xx Client Error
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
	GUARDIAN_NOT_FOUND("E404", "보호자를 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	CONVERSATION_NOT_FOUND("E404", "대화를 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),

	TOO_MANY_REQUESTS("E429", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),
	DAILY_MESSAGE_LIMIT_EXCEEDED("E429", "일일 메시지 한도를 초과했습니다. 내일 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),
	CONVERSATION_INACTIVE("E429", "비활성 대화입니다. 새 대화를 시작해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),

	DUPLICATE_EMAIL("E409", "이미 가입된 이메일입니다", HttpStatus.CONFLICT.value()),

	// Conversation Domain Specific Errors
	MESSAGE_EMPTY("E400", "메시지 내용은 필수입니다", HttpStatus.BAD_REQUEST.value()),
	MESSAGE_TOO_LONG("E400", "메시지는 500자를 초과할 수 없습니다", HttpStatus.BAD_REQUEST.value()),

	// AI Response Generation Errors (구체적 분류)
	AI_RESPONSE_GENERATION_FAILED("E500", "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	AI_API_CALL_FAILED("E500", "AI API 호출에 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	AI_API_LIMIT_EXCEEDED("E429", "AI API 사용 한도를 초과했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),
	AI_NETWORK_ERROR("E500", "네트워크 연결에 실패했습니다. 인터넷 연결을 확인해주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	AI_RESPONSE_PARSING_FAILED("E500", "AI 응답 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	EMOTION_ANALYSIS_FAILED("E500", "감정 분석에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	// 5xx Server Error
	ENCRYPTION_ERROR("E500", "암호화 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	INTERNAL_SERVER_ERROR("E500", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value());

	private final String code;
	private final String message;
	private final int status;

	public int getHttpCode() {
		return this.status;
	}
}