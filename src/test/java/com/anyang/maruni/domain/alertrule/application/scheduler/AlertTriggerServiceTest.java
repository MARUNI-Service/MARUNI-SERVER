package com.anyang.maruni.domain.alertrule.application.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertDetectionService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertNotificationService;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

/**
 * AlertTriggerService 테스트
 *
 * 핵심 시나리오:
 * 1. 전체 회원 감지 성공
 * 2. 일부 회원 실패해도 나머지 처리
 */
@ExtendWith(MockitoExtension.class)
class AlertTriggerServiceTest {

    @Mock
    private AlertDetectionService alertDetectionService;

    @Mock
    private AlertNotificationService alertNotificationService;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AlertTriggerService alertTriggerService;

    @Test
    @DisplayName("전체 회원 감지 성공")
    void detectAnomaliesForAllMembers_Success() {
        // Given: 3명의 회원
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L, 2L, 3L));

        AlertResult highAlert = AlertResult.createAlert(
            AlertLevel.HIGH, AlertType.NO_RESPONSE, "3일 무응답", null
        );
        when(alertDetectionService.detectAnomalies(anyLong()))
            .thenReturn(List.of(highAlert));

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then: 3명 모두 감지 시도
        verify(alertDetectionService, times(3)).detectAnomalies(anyLong());
        // 3명 모두 알림 발송
        verify(alertNotificationService, times(3)).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("일부 회원 실패해도 나머지 처리 계속")
    void detectAnomaliesForAllMembers_PartialFailure() {
        // Given
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L, 2L, 3L));

        AlertResult alert = AlertResult.createAlert(
            AlertLevel.HIGH, AlertType.NO_RESPONSE, "무응답", null
        );

        // Member 1: 성공
        when(alertDetectionService.detectAnomalies(1L))
            .thenReturn(List.of(alert));

        // Member 2: 예외 발생
        when(alertDetectionService.detectAnomalies(2L))
            .thenThrow(new RuntimeException("Database error"));

        // Member 3: 성공
        when(alertDetectionService.detectAnomalies(3L))
            .thenReturn(List.of(alert));

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then: 3명 모두 감지 시도됨
        verify(alertDetectionService, times(3)).detectAnomalies(anyLong());

        // Member 1, 3만 알림 발송 (Member 2는 실패)
        verify(alertNotificationService, times(2)).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("위험 신호 없을 때 알림 미발송")
    void detectAnomaliesForAllMembers_NoAlerts() {
        // Given
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L));

        // 빈 결과 (알림 없음)
        when(alertDetectionService.detectAnomalies(1L))
            .thenReturn(List.of());

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then: 감지는 시도
        verify(alertDetectionService, times(1)).detectAnomalies(1L);

        // 알림은 발송하지 않음
        verify(alertNotificationService, never()).triggerAlert(anyLong(), any());
    }
}
