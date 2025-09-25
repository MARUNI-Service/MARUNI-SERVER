package com.anyang.maruni.domain.notification.infrastructure;

import com.google.firebase.messaging.Message;

/**
 * Firebase 메시징 래퍼 인터페이스
 *
 * Firebase FCM 의존성을 추상화하여 테스트 가능한 아키텍처를 제공합니다.
 * 이 인터페이스를 통해 실제 Firebase 호출과 테스트용 Mock을 분리할 수 있습니다.
 */
public interface FirebaseMessagingWrapper {

    /**
     * FCM 메시지 발송
     *
     * @param message Firebase 메시지 객체
     * @return Firebase 메시지 ID
     * @throws Exception 발송 실패 시
     */
    String sendMessage(Message message) throws Exception;

    /**
     * Firebase 서비스 사용 가능 여부 확인
     *
     * @return 서비스 사용 가능 여부
     */
    boolean isServiceAvailable();

    /**
     * Firebase 서비스 이름 반환
     *
     * @return 서비스 이름 (예: "Firebase", "MockFirebase")
     */
    String getServiceName();
}