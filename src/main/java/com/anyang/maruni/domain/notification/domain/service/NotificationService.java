package com.anyang.maruni.domain.notification.domain.service;

import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;

/**
 * 알림 발송 도메인 서비스 인터페이스
 *
 * 다양한 알림 채널(푸시, SMS, 이메일)에 대한 추상화를 제공합니다.
 * DDD 원칙에 따라 도메인 계층에서 인터페이스를 정의하고,
 * Infrastructure 계층에서 구현합니다.
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