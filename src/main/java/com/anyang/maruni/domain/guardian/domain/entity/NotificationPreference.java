package com.anyang.maruni.domain.guardian.domain.entity;

/**
 * 알림 채널 선호도
 */
public enum NotificationPreference {
    PUSH("푸시알림"),
    EMAIL("이메일"),
    SMS("SMS"),
    ALL("모든알림");

    private final String description;

    NotificationPreference(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}