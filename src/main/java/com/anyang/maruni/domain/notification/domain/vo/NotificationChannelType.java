package com.anyang.maruni.domain.notification.domain.vo;

/**
 * 알림 채널 타입
 *
 * Phase 2 MVP에서는 PUSH만 구현하고,
 * 추후 확장 시 EMAIL, SMS 등을 추가할 수 있습니다.
 */
public enum NotificationChannelType {
    PUSH("푸시알림"),
    EMAIL("이메일"),
    SMS("문자메시지"),
    IN_APP("인앱알림");

    private final String description;

    NotificationChannelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}