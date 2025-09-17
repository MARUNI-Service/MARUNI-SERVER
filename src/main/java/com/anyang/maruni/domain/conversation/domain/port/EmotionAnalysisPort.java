package com.anyang.maruni.domain.conversation.domain.port;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;

/**
 * 감정 분석 포트 인터페이스
 *
 * AI 모델 변경에 독립적인 감정 분석 기능을 정의합니다.
 * 키워드 기반, ML 기반 등 다양한 감정 분석 방식을 지원할 수 있습니다.
 */
public interface EmotionAnalysisPort {

    /**
     * 메시지 감정 분석
     *
     * @param message 분석할 메시지
     * @return 감정 타입 (POSITIVE, NEGATIVE, NEUTRAL)
     */
    EmotionType analyzeEmotion(String message);
}