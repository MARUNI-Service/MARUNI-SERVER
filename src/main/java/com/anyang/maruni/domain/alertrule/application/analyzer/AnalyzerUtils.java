package com.anyang.maruni.domain.alertrule.application.analyzer;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;

/**
 * Analyzer 공통 유틸리티
 *
 * 각 Analyzer에서 사용하는 공통 로직을 제공합니다.
 */
public final class AnalyzerUtils {

    // 생성자 private으로 유틸리티 클래스임을 명시
    private AnalyzerUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 연속 일수 기반 알림 메시지 생성
     * @param consecutiveDays 연속 일수
     * @param ratio 비율 (0.0 ~ 1.0)
     * @param patternType 패턴 유형 (예: "부정감정", "무응답")
     * @return 포매팅된 알림 메시지
     */
    public static String createConsecutiveDaysMessage(int consecutiveDays, double ratio, String patternType) {
        return String.format("%d일 연속 %s 감지 (%s비율: %.1f%%)",
                consecutiveDays, patternType, patternType, ratio * 100);
    }

    /**
     * 키워드 감지 알림 메시지 생성
     * @param alertLevel 알림 레벨
     * @param keyword 감지된 키워드
     * @return 포매팅된 알림 메시지
     */
    public static String createKeywordDetectionMessage(AlertLevel alertLevel, String keyword) {
        String alertType = alertLevel == AlertLevel.EMERGENCY ? "긴급" : "위험";
        return String.format("%s 키워드 감지: '%s'", alertType, keyword);
    }

    /**
     * 백분율 형태로 비율 표시
     * @param ratio 비율 (0.0 ~ 1.0)
     * @return 백분율 문자열
     */
    public static String formatPercentage(double ratio) {
        return String.format("%.1f%%", ratio * 100);
    }
}