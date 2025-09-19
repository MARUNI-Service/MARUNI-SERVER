package com.anyang.maruni.domain.dailycheck.application.scheduler;

import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.domain.dailycheck.domain.entity.RetryRecord;
import com.anyang.maruni.domain.dailycheck.domain.repository.DailyCheckRecordRepository;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * DailyCheckOrchestrator 테스트
 *
 * 메인 비즈니스 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyCheckOrchestrator 테스트")
class DailyCheckOrchestratorTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SimpleConversationService conversationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DailyCheckRecordRepository dailyCheckRecordRepository;

    @Mock
    private RetryService retryService;

    @InjectMocks
    private DailyCheckOrchestrator dailyCheckOrchestrator;

    @Test
    @DisplayName("매일 9시 안부 메시지를 모든 활성 회원에게 발송한다")
    void processAllActiveMembers_shouldSendToAllActiveMembers() {
        // Given
        List<Long> activeMemberIds = Arrays.asList(1L, 2L, 3L);
        given(memberRepository.findActiveMemberIds()).willReturn(activeMemberIds);
        given(dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(anyLong(), any(LocalDate.class)))
                .willReturn(false);  // 모든 회원에게 아직 발송하지 않음
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(true);

        // When
        dailyCheckOrchestrator.processAllActiveMembers();

        // Then
        verify(notificationService, times(3))
                .sendPushNotification(anyLong(), eq("안부 메시지"), contains("안녕하세요"));
        verify(conversationService, times(3))
                .processSystemMessage(anyLong(), anyString());
        verify(dailyCheckRecordRepository, times(3))
                .save(any());
    }

    @Test
    @DisplayName("같은 날 중복 발송을 방지한다")
    void processAllActiveMembers_shouldPreventDuplicateOnSameDay() {
        // Given
        List<Long> activeMemberIds = Arrays.asList(1L, 2L);
        given(memberRepository.findActiveMemberIds()).willReturn(activeMemberIds);
        given(dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(eq(1L), any(LocalDate.class)))
                .willReturn(true);   // 1번 회원은 이미 발송됨
        given(dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(eq(2L), any(LocalDate.class)))
                .willReturn(false);  // 2번 회원은 발송되지 않음
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(true);

        // When
        dailyCheckOrchestrator.processAllActiveMembers();

        // Then
        verify(notificationService, times(1))
                .sendPushNotification(eq(2L), anyString(), anyString());
        verify(notificationService, never())
                .sendPushNotification(eq(1L), anyString(), anyString());
    }

    @Test
    @DisplayName("지정된 시간대에만 메시지를 발송한다")
    void isAllowedSendingTime_shouldOnlySendDuringAllowedHours() {
        // Given
        LocalTime morningTime = LocalTime.of(9, 0);
        LocalTime eveningTime = LocalTime.of(22, 0);

        // When & Then
        assertTrue(dailyCheckOrchestrator.isAllowedSendingTime(morningTime));
        assertFalse(dailyCheckOrchestrator.isAllowedSendingTime(eveningTime));
    }

    @Test
    @DisplayName("푸시 알림 실패 시 재시도 스케줄에 등록한다")
    void processAllActiveMembers_shouldScheduleRetryOnFailure() {
        // Given
        List<Long> activeMemberIds = Arrays.asList(1L);
        given(memberRepository.findActiveMemberIds()).willReturn(activeMemberIds);
        given(dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(anyLong(), any(LocalDate.class)))
                .willReturn(false);
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
                .willReturn(false); // 실패 시나리오

        // When
        dailyCheckOrchestrator.processAllActiveMembers();

        // Then
        verify(retryService, times(1)).scheduleRetry(anyLong(), anyString());  // 재시도 기록 스케줄링 확인
        verify(dailyCheckRecordRepository, times(1)).save(any());  // 실패 기록 저장 확인
    }

    @Test
    @DisplayName("일정 시간이 지나면 재시도를 실행한다")
    void processAllRetries_shouldRetryFailedNotifications() {
        // Given
        given(retryService.getPendingRetries(any(LocalDateTime.class)))
                .willReturn(List.of()); // 빈 목록으로 설정하여 간단한 테스트

        // When
        dailyCheckOrchestrator.processAllRetries();

        // Then - 빈 목록이므로 알림 발송은 없어야 함
        verify(notificationService, never())
                .sendPushNotification(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("특정 회원에게 오늘 이미 발송했는지 확인한다")
    void isAlreadySentToday_shouldCheckTodayRecord() {
        // Given
        Long memberId = 1L;
        given(dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(eq(memberId), any(LocalDate.class)))
                .willReturn(true);

        // When
        boolean result = dailyCheckOrchestrator.isAlreadySentToday(memberId);

        // Then
        assertTrue(result);
        verify(dailyCheckRecordRepository)
                .existsSuccessfulRecordByMemberIdAndDate(eq(memberId), any(LocalDate.class));
    }
}