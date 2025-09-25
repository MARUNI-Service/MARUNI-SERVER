package com.anyang.maruni.domain.notification.infrastructure;

import java.time.LocalDateTime;

import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Mock 알림 발송 기록
 *
 * 테스트 환경에서 알림 발송 이력을 추적하기 위한 VO입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockNotificationRecord {

    private Long memberId;
    private String title;
    private String message;
    private NotificationChannelType channelType;
    private Long timestamp;
    private Boolean success;

    /**
     * 발송 시간을 사람이 읽을 수 있는 형태로 반환
     */
    public LocalDateTime getSentDateTime() {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
        );
    }
}