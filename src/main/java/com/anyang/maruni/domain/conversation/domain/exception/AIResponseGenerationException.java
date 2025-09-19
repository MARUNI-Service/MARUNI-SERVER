package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * AI 응답 생성 실패 예외
 *
 * OpenAI API 호출 실패, 네트워크 오류, API 한도 초과 등으로 AI 응답 생성에 실패했을 때 발생합니다.
 */
public class AIResponseGenerationException extends BaseException {

    public AIResponseGenerationException() {
        super(ErrorCode.AI_RESPONSE_GENERATION_FAILED);
    }

    public AIResponseGenerationException(String message) {
        super(ErrorCode.AI_RESPONSE_GENERATION_FAILED);
    }

    public AIResponseGenerationException(Throwable cause) {
        super(ErrorCode.AI_RESPONSE_GENERATION_FAILED);
    }

    public AIResponseGenerationException(String message, Throwable cause) {
        super(ErrorCode.AI_RESPONSE_GENERATION_FAILED);
    }

    /**
     * API 호출 실패로 인한 예외
     */
    public static AIResponseGenerationException apiCallFailed(Throwable cause) {
        return new AIResponseGenerationException("OpenAI API 호출 실패", cause);
    }

    /**
     * API 응답 파싱 실패로 인한 예외
     */
    public static AIResponseGenerationException responseParsingFailed(String response, Throwable cause) {
        return new AIResponseGenerationException("API 응답 파싱 실패: " + response, cause);
    }

    /**
     * API 한도 초과로 인한 예외
     */
    public static AIResponseGenerationException apiLimitExceeded() {
        return new AIResponseGenerationException("OpenAI API 사용 한도 초과");
    }

    /**
     * 네트워크 연결 실패로 인한 예외
     */
    public static AIResponseGenerationException networkError(Throwable cause) {
        return new AIResponseGenerationException("네트워크 연결 실패", cause);
    }
}