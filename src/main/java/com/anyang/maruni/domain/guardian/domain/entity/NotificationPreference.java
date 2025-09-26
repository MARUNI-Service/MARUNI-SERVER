package com.anyang.maruni.domain.guardian.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 채널 선호도
 */
@Schema(description = "보호자 알림 수신 설정")
public enum NotificationPreference {
    @Schema(description = "푸시 알림만")
    PUSH("푸시알림"),

    @Schema(description = "이메일 알림만")
    EMAIL("이메일"),

    @Schema(description = "SMS 알림만")
    SMS("SMS"),

    @Schema(description = "모든 알림 채널")
    ALL("모든알림");

    private final String description;

    NotificationPreference(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}