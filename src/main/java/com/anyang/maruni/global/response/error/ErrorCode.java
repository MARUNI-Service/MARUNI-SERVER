package com.anyang.maruni.global.response.error;



import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorType {

	// ============ General Validation & Client Errors ============
	TOKEN_MALFORMED("G400", "잘못된 형식의 토큰입니다", HttpStatus.BAD_REQUEST.value()),
	INVALID_INPUT_VALUE("G401", "잘못된 입력값입니다", HttpStatus.BAD_REQUEST.value()),
	PARAMETER_VALIDATION_ERROR("G402", "파라미터 검증에 실패했습니다", HttpStatus.BAD_REQUEST.value()),

	// ============ Authentication & Authorization ============
	LOGIN_FAIL("A401", "이메일 또는 비밀번호가 틀렸습니다", HttpStatus.UNAUTHORIZED.value()),
	INVALID_TOKEN("A402", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	TOKEN_EXPIRED("A403", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	REFRESH_TOKEN_NOT_FOUND("A404", "리프레시 토큰을 찾을 수 없습니다", HttpStatus.UNAUTHORIZED.value()),
	ACCESS_TOKEN_REQUIRED("A405", "액세스 토큰이 필요합니다", HttpStatus.UNAUTHORIZED.value()),
	TOKEN_BLACKLISTED("A406", "차단된 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	OAUTH2_LOGIN_FAILED("A407", "소셜 로그인에 실패했습니다. 다시 시도해주세요.", HttpStatus.UNAUTHORIZED.value()),

	// ============ Member Domain ============
	MEMBER_NOT_FOUND("M404", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	DUPLICATE_EMAIL("M409", "이미 가입된 이메일입니다", HttpStatus.CONFLICT.value()),

	// ============ Guardian Domain ============
	GUARDIAN_NOT_FOUND("GU404", "보호자를 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	GUARDIAN_NOT_ASSIGNED("GU405", "회원에게 보호자가 설정되지 않았습니다", HttpStatus.NOT_FOUND.value()),
	GUARDIAN_EMAIL_ALREADY_EXISTS("GU409", "이미 등록된 보호자 이메일입니다", HttpStatus.CONFLICT.value()),
	GUARDIAN_ALREADY_ASSIGNED("GU410", "이미 보호자가 설정된 회원입니다", HttpStatus.CONFLICT.value()),
	GUARDIAN_DEACTIVATION_FAILED("GU411", "보호자 비활성화에 실패했습니다", HttpStatus.BAD_REQUEST.value()),
	GUARDIAN_SELF_ASSIGNMENT_NOT_ALLOWED("GU412", "자기 자신을 보호자로 설정할 수 없습니다", HttpStatus.BAD_REQUEST.value()),

	// ============ Conversation Domain ============
	CONVERSATION_NOT_FOUND("C404", "대화를 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	CONVERSATION_NOT_FOUND_BY_ID("C405", "해당 ID의 대화를 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	ACTIVE_CONVERSATION_NOT_FOUND("C406", "활성화된 대화가 없습니다. 새 대화를 시작해주세요", HttpStatus.NOT_FOUND.value()),
	MESSAGE_EMPTY("C400", "메시지 내용은 필수입니다", HttpStatus.BAD_REQUEST.value()),
	MESSAGE_TOO_LONG("C401", "메시지는 500자를 초과할 수 없습니다", HttpStatus.BAD_REQUEST.value()),
	DAILY_MESSAGE_LIMIT_EXCEEDED("C429", "일일 메시지 한도를 초과했습니다. 내일 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),
	CONVERSATION_INACTIVE("C430", "비활성 대화입니다. 새 대화를 시작해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),

	// ============ AI Integration ============
	AI_RESPONSE_GENERATION_FAILED("AI500", "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	AI_API_CALL_FAILED("AI501", "AI API 호출에 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	AI_API_LIMIT_EXCEEDED("AI429", "AI API 사용 한도를 초과했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),
	AI_NETWORK_ERROR("AI502", "네트워크 연결에 실패했습니다. 인터넷 연결을 확인해주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	AI_RESPONSE_PARSING_FAILED("AI503", "AI 응답 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	// ============ Emotion Analysis ============
	EMOTION_ANALYSIS_FAILED("EM500", "감정 분석에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	EMOTION_KEYWORD_CONFIG_LOAD_FAILED("EM501", "감정 키워드 설정을 불러오는데 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	MESSAGE_PREPROCESSING_FAILED("EM502", "메시지 전처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	// ============ Rate Limiting ============
	TOO_MANY_REQUESTS("R429", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),

	// ============ Server Errors ============
	ENCRYPTION_ERROR("S500", "암호화 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	INTERNAL_SERVER_ERROR("S501", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value());

	private final String code;
	private final String message;
	private final int status;

	public int getHttpCode() {
		return this.status;
	}
}