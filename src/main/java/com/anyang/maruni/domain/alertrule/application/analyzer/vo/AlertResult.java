package com.anyang.maruni.domain.alertrule.application.analyzer.vo;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;

import lombok.Getter;

/**
 * 알림 분석 결과 VO
 *
 * 각종 분석기에서 반환하는 공통 결과 객체입니다.
 */
@Getter
public class AlertResult {

	// Getter 메서드들
	private final boolean isAlert;
    private final AlertLevel alertLevel;
    private final String message;
    private final Object analysisDetails;

    private AlertResult(boolean isAlert, AlertLevel alertLevel, String message, Object analysisDetails) {
        this.isAlert = isAlert;
        this.alertLevel = alertLevel;
        this.message = message;
        this.analysisDetails = analysisDetails;
    }

    /**
     * 알림 발생 결과 생성
     * @param alertLevel 알림 레벨
     * @param message 알림 메시지
     * @param analysisDetails 분석 상세 정보
     * @return AlertResult
     */
    public static AlertResult createAlert(AlertLevel alertLevel, String message, Object analysisDetails) {
        return new AlertResult(true, alertLevel, message, analysisDetails);
    }

    /**
     * 알림 없음 결과 생성
     * @return AlertResult
     */
    public static AlertResult noAlert() {
        return new AlertResult(false, null, null, null);
    }

	@Override
    public String toString() {
        if (!isAlert) {
            return "AlertResult{알림 없음}";
        }
        return String.format("AlertResult{레벨=%s, 메시지='%s'}", alertLevel, message);
    }
}