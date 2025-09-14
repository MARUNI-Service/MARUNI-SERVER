package com.anyang.maruni.domain.dailycheck.application.service;

import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * DailyCheckService TDD Red 테스트
 *
 * Week 5 Day 1-2: 실패하는 테스트 작성
 * - 스케줄링 실행 테스트
 * - 중복 방지 로직 테스트
 * - 시간 기반 필터링 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyCheckService 테스트")
class DailyCheckServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SimpleConversationService conversationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DailyCheckService dailyCheckService;

    @Test
    @DisplayName("매일 9시 안부 메시지를 모든 활성 회원에게 발송한다")
    void sendDailyCheckMessages_shouldSendToAllActiveMembers() {
        // Given
        List<Long> activeMemberIds = Arrays.asList(1L, 2L, 3L);
        given(memberRepository.findActiveMemberIds()).willReturn(activeMemberIds);
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(true);

        // When
        dailyCheckService.sendDailyCheckMessages();

        // Then
        verify(notificationService, times(3))
                .sendPushNotification(anyLong(), eq("안부 메시지"), contains("안녕하세요"));
        verify(conversationService, times(3))
                .processSystemMessage(anyLong(), anyString());
    }

    @Test
    @DisplayName("같은 날 중복 발송을 방지한다")
    void sendDailyCheckMessages_shouldPreventDuplicateOnSameDay() {
        // Given
        List<Long> activeMemberIds = Arrays.asList(1L, 2L);
        given(memberRepository.findActiveMemberIds()).willReturn(activeMemberIds);
        given(dailyCheckService.isAlreadySentToday(1L)).willReturn(true);
        given(dailyCheckService.isAlreadySentToday(2L)).willReturn(false);
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(true);

        // When
        dailyCheckService.sendDailyCheckMessages();

        // Then
        verify(notificationService, times(1))
                .sendPushNotification(eq(2L), anyString(), anyString());
        verify(notificationService, never())
                .sendPushNotification(eq(1L), anyString(), anyString());
    }

    @Test
    @DisplayName("지정된 시간대에만 메시지를 발송한다")
    void sendDailyCheckMessages_shouldOnlySendDuringAllowedHours() {
        // Given
        LocalTime morningTime = LocalTime.of(9, 0);
        LocalTime eveningTime = LocalTime.of(22, 0);

        // When & Then
        assertTrue(dailyCheckService.isAllowedSendingTime(morningTime));
        assertFalse(dailyCheckService.isAllowedSendingTime(eveningTime));
    }

    @Test
    @DisplayName("푸시 알림 실패 시 재시도 스케줄에 등록한다")
    void sendDailyCheckMessages_shouldScheduleRetryOnFailure() {
        // Given
        List<Long> activeMemberIds = Arrays.asList(1L);
        given(memberRepository.findActiveMemberIds()).willReturn(activeMemberIds);
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(false);

        // When
        dailyCheckService.sendDailyCheckMessages();

        // Then
        verify(dailyCheckService).scheduleRetry(eq(1L), anyString());
    }

    @Test
    @DisplayName("일정 시간이 지나면 재시도를 실행한다")
    void processRetries_shouldRetryFailedNotifications() {
        // Given
        List<Long> memberIdsToRetry = Arrays.asList(1L, 2L);
        given(dailyCheckService.getPendingRetryMemberIds()).willReturn(memberIdsToRetry);
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(true);

        // When
        dailyCheckService.processRetries();

        // Then
        verify(notificationService, times(2))
                .sendPushNotification(anyLong(), anyString(), anyString());
    }
}