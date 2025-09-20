package com.anyang.maruni.global.swagger;

import java.util.Set;

import com.anyang.maruni.global.response.error.ErrorCode;

import lombok.Getter;

@Getter
public enum SwaggerResponseDescription {

	MEMBER_ERROR(ErrorCode.MEMBER_NOT_FOUND, ErrorCode.DUPLICATE_EMAIL),

	MEMBER_JOIN_ERROR(ErrorCode.DUPLICATE_EMAIL, ErrorCode.PARAMETER_VALIDATION_ERROR, ErrorCode.INVALID_INPUT_VALUE),

	AUTH_ERROR(ErrorCode.INVALID_TOKEN, ErrorCode.REFRESH_TOKEN_NOT_FOUND, ErrorCode.LOGIN_FAIL),

	CONVERSATION_ERROR(
	    // Message Validation Errors
	    ErrorCode.MESSAGE_EMPTY, ErrorCode.MESSAGE_TOO_LONG,
	    // Message Limit Errors
	    ErrorCode.DAILY_MESSAGE_LIMIT_EXCEEDED, ErrorCode.CONVERSATION_INACTIVE,
	    // Conversation Not Found Errors
	    ErrorCode.CONVERSATION_NOT_FOUND, ErrorCode.CONVERSATION_NOT_FOUND_BY_ID, ErrorCode.ACTIVE_CONVERSATION_NOT_FOUND,
	    // AI Response Generation Errors
	    ErrorCode.AI_RESPONSE_GENERATION_FAILED, ErrorCode.AI_API_CALL_FAILED, ErrorCode.AI_API_LIMIT_EXCEEDED,
	    ErrorCode.AI_NETWORK_ERROR, ErrorCode.AI_RESPONSE_PARSING_FAILED,
	    // Emotion Analysis Errors
	    ErrorCode.EMOTION_ANALYSIS_FAILED, ErrorCode.EMOTION_KEYWORD_CONFIG_LOAD_FAILED, ErrorCode.MESSAGE_PREPROCESSING_FAILED
	),

	COMMON_ERROR(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INVALID_INPUT_VALUE);

	private final Set<ErrorCode> errorCodeList;

	SwaggerResponseDescription(ErrorCode... errorCodes) {
		this.errorCodeList = Set.of(errorCodes);
	}

}
