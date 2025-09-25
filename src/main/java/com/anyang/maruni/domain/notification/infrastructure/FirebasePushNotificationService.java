package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.exception.NotificationException;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.domain.service.PushTokenService;
import com.anyang.maruni.global.config.properties.FirebaseProperties;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Firebase FCM 푸시 알림 서비스 (리팩토링)
 *
 * Firebase 래퍼 인터페이스를 통해 테스트 가능한 구조로 재설계된 서비스입니다.
 * 실제 푸시 토큰 조회와 Firebase 메시징을 분리하여 단위 테스트가 가능합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirebasePushNotificationService implements NotificationService {

    private final FirebaseMessagingWrapper firebaseMessagingWrapper;
    private final PushTokenService pushTokenService;
    private final FirebaseProperties firebaseProperties;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String messageContent) {
        try {
            // 1. 푸시 토큰 조회
            String pushToken = pushTokenService.getPushTokenByMemberId(memberId);

            // 2. Firebase 메시지 구성
            Message firebaseMessage = buildFirebaseMessage(pushToken, title, messageContent);

            // 3. Firebase 메시지 발송
            String messageId = firebaseMessagingWrapper.sendMessage(firebaseMessage);

            log.info("🚀 [{}] Push notification sent successfully - memberId: {}, messageId: {}",
                    firebaseMessagingWrapper.getServiceName(), memberId, messageId);

            return true;

        } catch (FirebaseMessagingException e) {
            log.error("❌ [{}] Firebase messaging error - memberId: {}, errorCode: {}, message: {}",
                    firebaseMessagingWrapper.getServiceName(), memberId, e.getErrorCode(), e.getMessage());

            throw new NotificationException(ErrorCode.FIREBASE_SEND_FAILED, e);

        } catch (NotificationException e) {
            // 이미 NotificationException인 경우 그대로 재던지기
            throw e;

        } catch (Exception e) {
            log.error("❌ [{}] Unexpected error - memberId: {}, error: {}",
                    firebaseMessagingWrapper.getServiceName(), memberId, e.getMessage());

            throw new NotificationException(ErrorCode.NOTIFICATION_SEND_FAILED, e);
        }
    }

    @Override
    public boolean isAvailable() {
        boolean available = firebaseMessagingWrapper.isServiceAvailable();
        log.debug("{} service availability: {}",
                firebaseMessagingWrapper.getServiceName(), available);
        return available;
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.PUSH;
    }

    /**
     * Firebase FCM 메시지 구성
     *
     * @param token 푸시 토큰
     * @param title 알림 제목
     * @param messageContent 알림 내용
     * @return Firebase 메시지 객체
     */
    private Message buildFirebaseMessage(String token, String title, String messageContent) {
        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(messageContent)
                        .build())
                .setToken(token)
                // 추가 메타데이터
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .putData("source", "MARUNI")
                .putData("service", firebaseMessagingWrapper.getServiceName())
                .build();
    }
}