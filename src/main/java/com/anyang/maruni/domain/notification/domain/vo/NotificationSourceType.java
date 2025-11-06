package com.anyang.maruni.domain.notification.domain.vo;

/**
 * 알림 출처 타입
 *
 * 알림이 어느 시스템에서 발생했는지를 나타냅니다.
 * MVP 단순화 버전에서 4가지 출처를 지원합니다.
 */
public enum NotificationSourceType {
    DAILY_CHECK("안부 확인 시스템"),
    ALERT_RULE("이상징후 감지 시스템"),
    GUARDIAN_REQUEST("보호자 관리 시스템"),
    SYSTEM("시스템");

    private final String description;

    NotificationSourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
