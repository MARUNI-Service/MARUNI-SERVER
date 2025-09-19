package com.anyang.maruni.domain.conversation.infrastructure.analyzer;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.anyang.maruni.domain.conversation.config.ConversationProperties;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.exception.EmotionAnalysisException;
import com.anyang.maruni.domain.conversation.domain.port.EmotionAnalysisPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 키워드 기반 감정 분석기
 *
 * 기존 SimpleAIResponseGenerator의 감정 분석 로직을 독립적인 컴포넌트로 분리했습니다.
 * AI 모델과 무관하게 키워드 매칭을 통한 감정 분석을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordBasedEmotionAnalyzer implements EmotionAnalysisPort {

    private final ConversationProperties properties;

    /**
     * 키워드 기반 감정 분석 수행 (예외 처리 강화)
     *
     * @param message 분석할 메시지
     * @return 감정 타입
     * @throws EmotionAnalysisException 감정 분석 실패 시
     */
    @Override
    public EmotionType analyzeEmotion(String message) {
        try {
            log.debug("감정 분석 시작: {}", message);

            // null 또는 빈 문자열 처리
            if (!StringUtils.hasText(message)) {
                log.debug("빈 메시지, 중립적 감정 반환");
                return EmotionType.NEUTRAL;
            }

            // 메시지 전처리
            String processedMessage = preprocessMessage(message);

            // 키워드 설정 검증
            Map<String, List<String>> keywords = validateAndGetKeywords();

            // 부정적 키워드 체크 (우선 순위 높음)
            if (containsAnyKeyword(processedMessage, keywords.get("negative"))) {
                log.debug("부정적 감정 감지: NEGATIVE");
                return EmotionType.NEGATIVE;
            }

            // 긍정적 키워드 체크
            if (containsAnyKeyword(processedMessage, keywords.get("positive"))) {
                log.debug("긍정적 감정 감지: POSITIVE");
                return EmotionType.POSITIVE;
            }

            // 기본값: 중립
            log.debug("중립적 감정: NEUTRAL");
            return EmotionType.NEUTRAL;

        } catch (Exception e) {
            log.error("감정 분석 중 오류 발생: {}", e.getMessage(), e);
            throw EmotionAnalysisException.messagePreprocessingFailed(message, e);
        }
    }

    /**
     * 메시지 전처리 (소문자 변환, 특수문자 정리 등)
     */
    private String preprocessMessage(String message) {
        try {
            return message.toLowerCase().trim();
        } catch (Exception e) {
            throw EmotionAnalysisException.messagePreprocessingFailed(message, e);
        }
    }

    /**
     * 키워드 설정 검증 및 조회
     */
    private Map<String, List<String>> validateAndGetKeywords() {
        try {
            Map<String, List<String>> keywords = properties.getEmotion().getKeywords();

            if (keywords == null || keywords.isEmpty()) {
                throw EmotionAnalysisException.keywordConfigLoadFailed(
                    new IllegalStateException("감정 키워드 설정이 비어있습니다"));
            }

            if (!keywords.containsKey("negative") || !keywords.containsKey("positive")) {
                throw EmotionAnalysisException.keywordConfigLoadFailed(
                    new IllegalStateException("필수 키워드 카테고리(positive/negative)가 누락되었습니다"));
            }

            return keywords;

        } catch (Exception e) {
            if (e instanceof EmotionAnalysisException) {
                throw e;
            }
            throw EmotionAnalysisException.keywordConfigLoadFailed(e);
        }
    }

    /**
     * 메시지에 키워드 목록 중 하나라도 포함되는지 확인
     */
    private boolean containsAnyKeyword(String message, List<String> keywords) {
        return keywords.stream()
                .anyMatch(message::contains);
    }
}