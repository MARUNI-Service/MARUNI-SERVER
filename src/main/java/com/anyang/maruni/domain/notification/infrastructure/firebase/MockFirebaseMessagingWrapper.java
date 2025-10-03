package com.anyang.maruni.domain.notification.infrastructure.firebase;

import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트/개발용 Mock Firebase 래퍼
 *
 * 실제 Firebase를 사용하지 않고 로컬에서 테스트할 수 있는 Mock 구현체입니다.
 */
@Component
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockFirebaseMessagingWrapper implements FirebaseMessagingWrapper {

    private final AtomicLong messageIdCounter = new AtomicLong(1);
    private boolean serviceAvailable = true;

    @Override
    public String sendMessage(Message message) throws Exception {
        // Mock 발송 처리
        String messageId = "mock_msg_" + messageIdCounter.getAndIncrement() + "_" +
                          UUID.randomUUID().toString().substring(0, 8);

        log.info("📱 [MOCK FIREBASE] Message sent successfully - ID: {}",
                messageId);

        // 발송 성공 시뮬레이션
        Thread.sleep(10); // 실제 네트워크 지연 시뮬레이션

        return messageId;
    }

    @Override
    public boolean isServiceAvailable() {
        return serviceAvailable;
    }

    @Override
    public String getServiceName() {
        return "MockFirebase";
    }

    /**
     * 테스트를 위한 서비스 가용성 조작 메서드
     */
    public void setServiceAvailable(boolean available) {
        this.serviceAvailable = available;
        log.info("Mock Firebase service availability set to: {}", available);
    }

    /**
     * 테스트를 위한 메시지 카운터 초기화
     */
    public void resetMessageCounter() {
        messageIdCounter.set(1);
        log.debug("Mock Firebase message counter reset");
    }
}