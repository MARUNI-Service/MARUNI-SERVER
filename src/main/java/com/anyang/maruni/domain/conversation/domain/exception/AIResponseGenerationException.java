package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * AI 응답 생성 실패 예외
 *
 * OpenAI API 호출 실패, 네트워크 오류, API 한도 초과 등으로 AI 응답 생성에 실패했을 때 발생합니다.
 */
public class AIResponseGenerationException extends BaseException {

    public AIResponseGenerationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 일반적인 AI 응답 생성 실패
     */
    public static AIResponseGenerationException generalFailure() {
        return new AIResponseGenerationException(ErrorCode.AI_RESPONSE_GENERATION_FAILED);
    }

    /**
     * API 호출 실패로 인한 예외
     */
    public static AIResponseGenerationException apiCallFailed() {
        return new AIResponseGenerationException(ErrorCode.AI_API_CALL_FAILED);
    }

    /**
     * API 응답 파싱 실패로 인한 예외
     */
    public static AIResponseGenerationException responseParsingFailed() {
        return new AIResponseGenerationException(ErrorCode.AI_RESPONSE_PARSING_FAILED);
    }

    /**
     * API 한도 초과로 인한 예외
     */
    public static AIResponseGenerationException apiLimitExceeded() {
        return new AIResponseGenerationException(ErrorCode.AI_API_LIMIT_EXCEEDED);
    }

    /**
     * 네트워크 연결 실패로 인한 예외
     */
    public static AIResponseGenerationException networkError() {
        return new AIResponseGenerationException(ErrorCode.AI_NETWORK_ERROR);
    }
}