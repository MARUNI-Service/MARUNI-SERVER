package com.anyang.maruni.domain.conversation.domain.exception;

import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

/**
 * 감정 분석 실패 예외
 *
 * 키워드 기반 감정 분석 처리 중 오류가 발생했을 때 발생합니다.
 */
public class EmotionAnalysisException extends BaseException {

    public EmotionAnalysisException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 일반적인 감정 분석 실패
     */
    public static EmotionAnalysisException analysisFailure() {
        return new EmotionAnalysisException(ErrorCode.EMOTION_ANALYSIS_FAILED);
    }

    /**
     * 감정 키워드 설정 로드 실패로 인한 예외
     */
    public static EmotionAnalysisException keywordConfigLoadFailed() {
        return new EmotionAnalysisException(ErrorCode.EMOTION_KEYWORD_CONFIG_LOAD_FAILED);
    }

    /**
     * 메시지 전처리 실패로 인한 예외
     */
    public static EmotionAnalysisException messagePreprocessingFailed() {
        return new EmotionAnalysisException(ErrorCode.MESSAGE_PREPROCESSING_FAILED);
    }
}