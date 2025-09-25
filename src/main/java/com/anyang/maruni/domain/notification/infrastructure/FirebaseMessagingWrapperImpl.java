package com.anyang.maruni.domain.notification.infrastructure;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 Firebase FCM 래퍼 구현체
 *
 * 실제 Firebase Admin SDK를 사용하여 FCM 메시지를 발송합니다.
 */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FirebaseMessagingWrapperImpl implements FirebaseMessagingWrapper {

    private final FirebaseApp firebaseApp;

    @Override
    public String sendMessage(Message message) throws Exception {
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
        return messaging.send(message);
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
            return messaging != null && firebaseApp != null;
        } catch (Exception e) {
            log.warn("Firebase service check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getServiceName() {
        return "Firebase";
    }
}