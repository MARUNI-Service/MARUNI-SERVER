package com.anyang.maruni.domain.alertrule.domain.entity;

import lombok.Getter;

/**
 * 알림 유형 열거형
 *
 * 이상징후 감지 시스템에서 사용하는 알림 유형을 정의합니다.
 * 각 유형에 따라 다른 감지 로직이 적용됩니다.
 */
@Getter
public enum AlertType {
    /**
     * 감정패턴: 연속적인 부정적 감정 감지
     */
    EMOTION_PATTERN("감정패턴", "연속적인 부정적 감정 감지"),

    /**
     * 무응답: 일정 기간 응답 없음
     */
    NO_RESPONSE("무응답", "일정 기간 응답 없음"),

    /**
     * 키워드감지: 위험 키워드 포함된 응답
     */
    KEYWORD_DETECTION("키워드감지", "위험 키워드 포함된 응답"),

    /**
     * 건강우려: 건강 관련 우려사항 감지
     */
    HEALTH_CONCERN("건강우려", "건강 관련 우려사항 감지"),

    /**
     * 긴급상황: 즉시 대응이 필요한 상황
     */
    EMERGENCY("긴급상황", "즉시 대응이 필요한 상황");

    private final String displayName;
    private final String description;

    AlertType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

}