package com.anyang.maruni.domain.notification.domain.service;

import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationSourceType;

/**
 * 알림 발송 도메인 서비스 인터페이스 (MVP 확장 버전)
 *
 * 다양한 알림 채널(푸시, SMS, 이메일)에 대한 추상화를 제공합니다.
 * DDD 원칙에 따라 도메인 계층에서 인터페이스를 정의하고,
 * Infrastructure 계층에서 구현합니다.
 *
 * MVP 추가: 알림 타입과 출처를 포함한 발송 메서드
 */
public interface NotificationService {

    /**
     * 푸시 알림 발송
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @return 발송 성공 여부
     */
    boolean sendPushNotification(Long memberId, String title, String message);

    /**
     * 타입 정보를 포함한 알림 발송 (MVP 추가)
     *
     * @param memberId 알림 수신 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param notificationType 알림 타입 (DAILY_CHECK, GUARDIAN_REQUEST 등)
     * @param sourceType 알림 출처 타입 (DAILY_CHECK, ALERT_RULE 등)
     * @param sourceEntityId 출처 엔티티 ID (DailyCheckRecord ID, AlertHistory ID 등)
     * @return 발송 성공 여부
     */
    default boolean sendNotificationWithType(
            Long memberId,
            String title,
            String message,
            NotificationType notificationType,
            NotificationSourceType sourceType,
            Long sourceEntityId
    ) {
        // 기본 구현: 기존 sendPushNotification 호출
        // 데코레이터에서 오버라이드하여 타입 정보 저장
        return sendPushNotification(memberId, title, message);
    }

    /**
     * 알림 서비스 사용 가능 여부 확인
     *
     * @return 서비스 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 지원하는 알림 채널 타입
     *
     * @return 알림 채널 타입
     */
    NotificationChannelType getChannelType();
}