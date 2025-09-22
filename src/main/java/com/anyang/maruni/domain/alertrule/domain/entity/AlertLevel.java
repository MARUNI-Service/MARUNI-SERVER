package com.anyang.maruni.domain.alertrule.domain.entity;

import lombok.Getter;

/**
 * 알림 레벨 열거형
 *
 * 이상징후의 심각도에 따른 알림 레벨을 정의합니다.
 * 숫자가 높을수록 더 긴급한 상황을 의미합니다.
 */
@Getter
public enum AlertLevel implements Comparable<AlertLevel> {
    /**
     * 낮음: 정보성 알림
     */
    LOW("낮음", 1, "정보성 알림"),

    /**
     * 보통: 주의 관찰 필요
     */
    MEDIUM("보통", 2, "주의 관찰 필요"),

    /**
     * 높음: 빠른 확인 필요
     */
    HIGH("높음", 3, "빠른 확인 필요"),

    /**
     * 긴급: 즉시 대응 필요
     */
    EMERGENCY("긴급", 4, "즉시 대응 필요");

    private final String displayName;
    private final int priority;
    private final String description;

    AlertLevel(String displayName, int priority, String description) {
        this.displayName = displayName;
        this.priority = priority;
        this.description = description;
    }

	/**
     * 우선순위가 지정된 레벨보다 높거나 같은지 확인
     */
    public boolean isHigherOrEqualThan(AlertLevel other) {
        return this.priority >= other.priority;
    }

    /**
     * 긴급 레벨인지 확인
     */
    public boolean isEmergency() {
        return this == EMERGENCY;
    }
}