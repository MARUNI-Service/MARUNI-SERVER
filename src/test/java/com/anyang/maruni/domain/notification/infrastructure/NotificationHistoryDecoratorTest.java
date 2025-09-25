package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.infrastructure.decorator.NotificationHistoryDecorator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * NotificationHistoryDecorator 단위 테스트
 *
 * Decorator 패턴의 정확한 동작과 이력 저장 기능을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationHistoryDecorator 테스트")
class NotificationHistoryDecoratorTest {

    @Mock
    private NotificationService delegate;

    @Mock
    private NotificationHistoryService historyService;

    @Mock
    private NotificationHistory mockHistory;

    private NotificationHistoryDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new NotificationHistoryDecorator(delegate, historyService);
    }

    @Test
    @DisplayName("성공한 알림 발송 시 성공 이력 저장")
    void shouldRecordSuccessWhenNotificationSentSuccessfully() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        NotificationChannelType channelType = NotificationChannelType.PUSH;

        given(delegate.sendPushNotification(memberId, title, message)).willReturn(true);
        given(delegate.getChannelType()).willReturn(channelType);
        given(historyService.recordSuccess(memberId, title, message, channelType))
                .willReturn(mockHistory);
        given(mockHistory.getId()).willReturn(100L);

        // When
        boolean result = decorator.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(delegate).sendPushNotification(memberId, title, message);
        verify(historyService).recordSuccess(memberId, title, message, channelType);
    }

    @Test
    @DisplayName("실패한 알림 발송 시 실패 이력 저장")
    void shouldRecordFailureWhenNotificationSendFailed() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        NotificationChannelType channelType = NotificationChannelType.PUSH;

        given(delegate.sendPushNotification(memberId, title, message)).willReturn(false);
        given(delegate.getChannelType()).willReturn(channelType);
        given(historyService.recordFailure(eq(memberId), eq(title), eq(message),
                eq(channelType), eq("Notification service returned false")))
                .willReturn(mockHistory);
        given(mockHistory.getId()).willReturn(101L);

        // When
        boolean result = decorator.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isFalse();
        verify(delegate).sendPushNotification(memberId, title, message);
        verify(historyService).recordFailure(memberId, title, message, channelType,
                "Notification service returned false");
    }

    @Test
    @DisplayName("예외 발생 시 예외 정보와 함께 실패 이력 저장")
    void shouldRecordFailureWithExceptionWhenExceptionOccurs() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        NotificationChannelType channelType = NotificationChannelType.PUSH;
        RuntimeException exception = new RuntimeException("Firebase connection failed");

        given(delegate.sendPushNotification(memberId, title, message)).willThrow(exception);
        given(delegate.getChannelType()).willReturn(channelType);
        given(historyService.recordFailure(eq(memberId), eq(title), eq(message),
                eq(channelType), eq("Exception occurred: Firebase connection failed")))
                .willReturn(mockHistory);
        given(mockHistory.getId()).willReturn(102L);

        // When
        boolean result = decorator.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isFalse();
        verify(delegate).sendPushNotification(memberId, title, message);
        verify(historyService).recordFailure(memberId, title, message, channelType,
                "Exception occurred: Firebase connection failed");
    }

    @Test
    @DisplayName("가용성 확인은 델리게이트에 위임")
    void shouldDelegateAvailabilityCheck() {
        // Given
        given(delegate.isAvailable()).willReturn(true);

        // When
        boolean result = decorator.isAvailable();

        // Then
        assertThat(result).isTrue();
        verify(delegate).isAvailable();
        verifyNoInteractions(historyService); // 이력 저장은 호출되지 않아야 함
    }

    @Test
    @DisplayName("채널 타입 조회는 델리게이트에 위임")
    void shouldDelegateChannelTypeRetrieval() {
        // Given
        NotificationChannelType expectedType = NotificationChannelType.PUSH;
        given(delegate.getChannelType()).willReturn(expectedType);

        // When
        NotificationChannelType result = decorator.getChannelType();

        // Then
        assertThat(result).isEqualTo(expectedType);
        verify(delegate).getChannelType();
        verifyNoInteractions(historyService); // 이력 저장은 호출되지 않아야 함
    }

    @Test
    @DisplayName("델리게이트 반환 메서드")
    void shouldReturnDelegate() {
        // When
        NotificationService result = decorator.getDelegate();

        // Then
        assertThat(result).isSameAs(delegate);
    }

    @Test
    @DisplayName("타입별 서비스 언래핑 - 현재 데코레이터 반환")
    void shouldUnwrapToCurrentDecoratorWhenTypeMatches() {
        // When
        NotificationHistoryDecorator result = decorator.unwrap(NotificationHistoryDecorator.class);

        // Then
        assertThat(result).isSameAs(decorator);
    }

    @Test
    @DisplayName("타입별 서비스 언래핑 - 델리게이트 반환")
    void shouldUnwrapToDelegateWhenDelegateTypeMatches() {
        // Given
        NotificationService mockSpecificService = new MockSpecificService();
        NotificationHistoryDecorator decoratorWithSpecificService =
                new NotificationHistoryDecorator(mockSpecificService, historyService);

        // When
        MockSpecificService result = decoratorWithSpecificService.unwrap(MockSpecificService.class);

        // Then
        assertThat(result).isSameAs(mockSpecificService);
    }

    @Test
    @DisplayName("타입별 서비스 언래핑 - 중첩 데코레이터 처리")
    void shouldUnwrapNestedDecorators() {
        // Given
        NotificationService innerService = new MockSpecificService();
        NotificationHistoryDecorator innerDecorator =
                new NotificationHistoryDecorator(innerService, historyService);
        NotificationHistoryDecorator outerDecorator =
                new NotificationHistoryDecorator(innerDecorator, historyService);

        // When
        MockSpecificService result = outerDecorator.unwrap(MockSpecificService.class);

        // Then
        assertThat(result).isSameAs(innerService);
    }

    @Test
    @DisplayName("타입별 서비스 언래핑 - 매치되지 않는 타입은 null 반환")
    void shouldReturnNullWhenNoMatchingTypeFound() {
        // Given - NotificationService를 상속하지 않는 클래스는 테스트할 수 없으므로
        // 실제로는 존재하지 않는 NotificationService 하위 타입을 시뮬레이션
        class NonExistentNotificationService implements NotificationService {
            @Override
            public boolean sendPushNotification(Long memberId, String title, String message) {
                return false;
            }
            @Override
            public boolean isAvailable() { return false; }
            @Override
            public NotificationChannelType getChannelType() { return NotificationChannelType.PUSH; }
        }

        // When
        NonExistentNotificationService result = decorator.unwrap(NonExistentNotificationService.class);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("이력 저장 서비스 예외 발생 시에도 원본 결과 반환")
    void shouldReturnOriginalResultEvenWhenHistoryServiceFails() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(delegate.sendPushNotification(memberId, title, message)).willReturn(true);
        given(delegate.getChannelType()).willReturn(NotificationChannelType.PUSH);
        given(historyService.recordSuccess(any(), any(), any(), any()))
                .willThrow(new RuntimeException("Database connection failed"));

        // When
        boolean result = decorator.sendPushNotification(memberId, title, message);

        // Then - 이력 저장 실패해도 원본 알림 결과는 그대로 반환
        assertThat(result).isTrue();
        verify(delegate).sendPushNotification(memberId, title, message);
        verify(historyService).recordSuccess(memberId, title, message, NotificationChannelType.PUSH);
    }

    /**
     * 테스트용 Mock 서비스 클래스
     */
    private static class MockSpecificService implements NotificationService {
        @Override
        public boolean sendPushNotification(Long memberId, String title, String message) {
            return true;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public NotificationChannelType getChannelType() {
            return NotificationChannelType.PUSH;
        }
    }
}