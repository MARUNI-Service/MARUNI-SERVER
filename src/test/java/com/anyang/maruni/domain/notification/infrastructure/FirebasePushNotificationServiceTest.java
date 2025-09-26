package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.exception.NotificationException;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.PushTokenService;
import com.anyang.maruni.domain.notification.infrastructure.firebase.FirebaseMessagingWrapper;
import com.anyang.maruni.domain.notification.infrastructure.service.FirebasePushNotificationService;
import com.anyang.maruni.global.config.properties.FirebaseProperties;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FirebasePushNotificationService 단위 테스트 (근본적 개선)
 *
 * Firebase 래퍼 인터페이스를 통해 외부 의존성을 완전히 제거하고
 * 실제 비즈니스 로직만 테스트할 수 있는 구조로 개선되었습니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FirebasePushNotificationService 테스트")
class FirebasePushNotificationServiceTest {

    @Mock
    private FirebaseMessagingWrapper firebaseMessagingWrapper;

    @Mock
    private PushTokenService pushTokenService;

    @Mock
    private FirebaseProperties firebaseProperties;

    @InjectMocks
    private FirebasePushNotificationService notificationService;

    @Test
    @DisplayName("푸시 알림 발송 성공 - 모든 의존성이 정상 동작")
    void shouldSendPushNotificationSuccessfully() throws Exception {
        // Given
        Long memberId = 1L;
        String title = "안부 메시지";
        String message = "오늘 하루는 어떻게 지내고 계신가요?";
        String pushToken = "valid_fcm_token_12345";
        String expectedMessageId = "firebase_msg_67890";

        given(pushTokenService.getPushTokenByMemberId(memberId))
                .willReturn(pushToken);
        given(firebaseMessagingWrapper.sendMessage(any(Message.class)))
                .willReturn(expectedMessageId);
        given(firebaseMessagingWrapper.getServiceName())
                .willReturn("Firebase");

        // When
        boolean result = notificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(pushTokenService).getPushTokenByMemberId(memberId);
        verify(firebaseMessagingWrapper).sendMessage(any(Message.class));
    }

    @Test
    @DisplayName("푸시 토큰 조회 실패 시 NotificationException 발생")
    void shouldThrowNotificationExceptionWhenPushTokenNotFound() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(pushTokenService.getPushTokenByMemberId(memberId))
                .willThrow(new NotificationException(ErrorCode.PUSH_TOKEN_INVALID));

        // When & Then
        assertThatThrownBy(() -> notificationService.sendPushNotification(memberId, title, message))
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .extracting("code")
                .isEqualTo("N400");
    }

    @Test
    @DisplayName("Firebase 메시징 실패 시 적절한 예외 처리")
    void shouldHandleFirebaseMessagingException() throws Exception {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        String pushToken = "valid_token";

        given(pushTokenService.getPushTokenByMemberId(memberId))
                .willReturn(pushToken);
        given(firebaseMessagingWrapper.sendMessage(any(Message.class)))
                .willThrow(new RuntimeException("Firebase connection failed"));
        given(firebaseMessagingWrapper.getServiceName())
                .willReturn("Firebase");

        // When & Then
        assertThatThrownBy(() -> notificationService.sendPushNotification(memberId, title, message))
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .extracting("code")
                .isEqualTo("N501"); // NOTIFICATION_SEND_FAILED
    }

    @Test
    @DisplayName("서비스 가용성 확인 - 정상 상태")
    void shouldReturnTrueWhenServiceIsAvailable() {
        // Given
        given(firebaseMessagingWrapper.isServiceAvailable())
                .willReturn(true);
        given(firebaseMessagingWrapper.getServiceName())
                .willReturn("Firebase");

        // When
        boolean isAvailable = notificationService.isAvailable();

        // Then
        assertThat(isAvailable).isTrue();
        verify(firebaseMessagingWrapper).isServiceAvailable();
    }

    @Test
    @DisplayName("서비스 가용성 확인 - 서비스 장애")
    void shouldReturnFalseWhenServiceIsUnavailable() {
        // Given
        given(firebaseMessagingWrapper.isServiceAvailable())
                .willReturn(false);
        given(firebaseMessagingWrapper.getServiceName())
                .willReturn("Firebase");

        // When
        boolean isAvailable = notificationService.isAvailable();

        // Then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("알림 채널 타입 확인")
    void shouldReturnPushChannelType() {
        // When
        NotificationChannelType channelType = notificationService.getChannelType();

        // Then
        assertThat(channelType).isEqualTo(NotificationChannelType.PUSH);
    }
}