package com.anyang.maruni.domain.notification.domain.vo;

/**
 * 알림 타입
 *
 * 사용자가 받을 수 있는 다양한 알림 유형을 정의합니다.
 * MVP 단순화 버전에서 8가지 타입을 지원합니다.
 */
public enum NotificationType {
    // 안부 확인
    DAILY_CHECK("안부 메시지"),

    // 이상징후 감지
    EMOTION_ALERT("감정 패턴 이상"),
    NO_RESPONSE_ALERT("무응답 이상"),
    KEYWORD_ALERT("키워드 감지"),

    // 보호자 관리
    GUARDIAN_REQUEST("보호자 등록 요청"),
    GUARDIAN_ACCEPT("보호자 요청 수락"),
    GUARDIAN_REJECT("보호자 요청 거절"),

    // 시스템
    SYSTEM("시스템 알림");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
