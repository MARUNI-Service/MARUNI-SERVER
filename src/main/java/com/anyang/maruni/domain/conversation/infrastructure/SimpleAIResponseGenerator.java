package com.anyang.maruni.domain.conversation.infrastructure;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 간단한 AI 응답 생성기 (MVP 버전)
 * 
 * OpenAI GPT API를 사용하여 사용자 메시지에 대한 AI 응답을 생성합니다.
 * MVP에서는 기본적인 프롬프트와 간단한 감정 분석만 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleAIResponseGenerator {

    /**
     * 사용자 메시지에 대한 AI 응답 생성
     * 
     * @param userMessage 사용자 메시지
     * @return AI 응답 내용
     */
    public String generateResponse(String userMessage) {
        // TODO: TDD로 구현 예정
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    /**
     * 간단한 감정 분석 수행
     * 
     * @param message 분석할 메시지
     * @return 감정 타입
     */
    public EmotionType analyzeBasicEmotion(String message) {
        // TODO: TDD로 구현 예정
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}