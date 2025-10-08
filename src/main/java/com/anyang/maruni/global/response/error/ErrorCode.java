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
	ACCESS_TOKEN_REQUIRED("A405", "액세스 토큰이 필요합니다", HttpStatus.UNAUTHORIZED.value()),

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

	// Guardian Request 관련
	GUARDIAN_REQUEST_NOT_FOUND("GR404", "보호자 요청을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	GUARDIAN_REQUEST_ALREADY_PROCESSED("GR400", "이미 처리된 요청입니다", HttpStatus.BAD_REQUEST.value()),
	GUARDIAN_REQUEST_DUPLICATE("GR409", "이미 대기 중인 요청이 있습니다", HttpStatus.CONFLICT.value()),
	GUARDIAN_ACCESS_DENIED("GR403", "보호자 권한이 없습니다", HttpStatus.FORBIDDEN.value()),

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

	// ============ AlertRule Domain ============
	ALERT_RULE_NOT_FOUND("AR404", "알림 규칙을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	ALERT_RULE_ACCESS_DENIED("AR403", "알림 규칙에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN.value()),
	INVALID_ALERT_CONDITION("AR400", "유효하지 않은 알림 조건입니다", HttpStatus.BAD_REQUEST.value()),
	ALERT_RULE_CREATION_FAILED("AR500", "알림 규칙 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	UNSUPPORTED_ALERT_TYPE("AR401", "지원하지 않는 알림 타입입니다", HttpStatus.BAD_REQUEST.value()),

	// ============ Emotion Analysis ============
	EMOTION_ANALYSIS_FAILED("EM500", "감정 분석에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	EMOTION_KEYWORD_CONFIG_LOAD_FAILED("EM501", "감정 키워드 설정을 불러오는데 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	MESSAGE_PREPROCESSING_FAILED("EM502", "메시지 전처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	// ============ Rate Limiting ============
	TOO_MANY_REQUESTS("R429", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value()),

	// ============ Notification Domain ============
	FIREBASE_CONNECTION_FAILED("N500", "Firebase 연결에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	PUSH_TOKEN_INVALID("N400", "유효하지 않은 푸시 토큰입니다", HttpStatus.BAD_REQUEST.value()),
	NOTIFICATION_SEND_FAILED("N501", "알림 발송에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	TEMPLATE_NOT_FOUND("N404", "알림 템플릿을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	FIREBASE_SEND_FAILED("N502", "Firebase 메시지 발송에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	NOTIFICATION_SERVICE_UNAVAILABLE("N503", "알림 서비스를 사용할 수 없습니다", HttpStatus.SERVICE_UNAVAILABLE.value()),
	FIREBASE_CONFIG_ERROR("N504", "Firebase 설정 오류입니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	// Phase 1 Week 4-5: Stability Enhancement errors
	FIREBASE_INVALID_TOKEN("N401", "Firebase 토큰이 유효하지 않습니다", HttpStatus.BAD_REQUEST.value()),
	PUSH_TOKEN_NOT_FOUND("N405", "푸시 토큰을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	TEMPLATE_VARIABLE_MISSING("N402", "템플릿 변수가 누락되었습니다", HttpStatus.BAD_REQUEST.value()),
	NOTIFICATION_HISTORY_SAVE_FAILED("N505", "알림 이력 저장에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),
	NOTIFICATION_FALLBACK_FAILED("N506", "모든 알림 서비스가 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),

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