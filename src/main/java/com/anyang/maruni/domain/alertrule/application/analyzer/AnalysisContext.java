package com.anyang.maruni.domain.alertrule.application.analyzer;

import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이상징후 분석에 필요한 컨텍스트 정보를 담는 Value Object
 *
 * 분석기별로 필요한 데이터를 캡슐화하여 전달합니다.
 *
 * Phase 2 리팩토링: Strategy Pattern의 컨텍스트 객체
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisContext {

    /**
     * 분석 기간 (일 단위)
     * EmotionPatternAnalyzer와 NoResponseAnalyzer에서 사용
     */
    private int analysisDays;

    /**
     * 실시간 키워드 분석 대상 메시지
     * KeywordAnalyzer에서 사용
     */
    private MessageEntity targetMessage;

    /**
     * 감정 패턴 분석용 팩토리 메서드
     *
     * @param analysisDays 분석 기간
     * @return 감정 패턴 분석용 컨텍스트
     */
    public static AnalysisContext forEmotionPattern(int analysisDays) {
        return AnalysisContext.builder()
                .analysisDays(analysisDays)
                .build();
    }

    /**
     * 무응답 분석용 팩토리 메서드
     *
     * @param analysisDays 분석 기간
     * @return 무응답 분석용 컨텍스트
     */
    public static AnalysisContext forNoResponse(int analysisDays) {
        return AnalysisContext.builder()
                .analysisDays(analysisDays)
                .build();
    }

    /**
     * 키워드 분석용 팩토리 메서드
     *
     * @param message 분석 대상 메시지
     * @return 키워드 분석용 컨텍스트
     */
    public static AnalysisContext forKeyword(MessageEntity message) {
        return AnalysisContext.builder()
                .targetMessage(message)
                .build();
    }
}