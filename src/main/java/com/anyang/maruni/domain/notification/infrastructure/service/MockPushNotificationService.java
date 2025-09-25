package com.anyang.maruni.domain.notification.infrastructure.service;

import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.infrastructure.vo.MockNotificationRecord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 푸시 알림 서비스
 *
 * 개발 환경에서 사용하는 Mock 구현체입니다.
 * 실제 푸시 알림을 발송하지 않고 로그만 남기며,
 * 테스트를 위한 발송 이력을 저장합니다.
 */
@Service
@Qualifier("originalNotificationService")
@Profile("dev") // 개발 환경에서만 활성화
@Slf4j
public class MockPushNotificationService implements NotificationService {

    // 테스트용 발송 이력 저장
    private final List<MockNotificationRecord> sentNotifications = new ArrayList<>();

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        log.info("🔔 [MOCK] Push notification sent - memberId: {}, title: {}, message: {}",
                memberId, title, message);

        // Mock 발송 이력 저장
        MockNotificationRecord record = MockNotificationRecord.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(NotificationChannelType.PUSH)
                .timestamp(System.currentTimeMillis())
                .success(true)
                .build();

        sentNotifications.add(record);

        // Mock에서는 항상 성공
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true; // Mock은 항상 사용 가능
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.PUSH;
    }

    /**
     * 테스트용 메서드: 발송 이력 조회
     */
    public List<MockNotificationRecord> getSentNotifications() {
        return new ArrayList<>(sentNotifications);
    }

    /**
     * 테스트용 메서드: 발송 이력 초기화
     */
    public void clearSentNotifications() {
        sentNotifications.clear();
    }

    /**
     * 특정 회원에게 발송된 알림 개수 조회 (테스트용)
     */
    public long getNotificationCountForMember(Long memberId) {
        return sentNotifications.stream()
                .filter(record -> record.getMemberId().equals(memberId))
                .count();
    }
}