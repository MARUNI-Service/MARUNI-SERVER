package com.anyang.maruni.domain.dailycheck.application.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * DailyCheckScheduler 테스트
 *
 * 스케줄링 트리거 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyCheckScheduler 테스트")
class DailyCheckSchedulerTest {

    @Mock
    private DailyCheckOrchestrator dailyCheckOrchestrator;

    @InjectMocks
    private DailyCheckScheduler dailyCheckScheduler;

    @Test
    @DisplayName("일일 체크 트리거가 오케스트레이터의 processAllActiveMembers를 호출한다")
    void triggerDailyCheck_shouldCallOrchestratorProcessAllActiveMembers() {
        // When
        dailyCheckScheduler.triggerDailyCheck();

        // Then
        verify(dailyCheckOrchestrator, times(1)).processAllActiveMembers();
    }

    @Test
    @DisplayName("재시도 트리거가 오케스트레이터의 processAllRetries를 호출한다")
    void triggerRetryProcess_shouldCallOrchestratorProcessAllRetries() {
        // When
        dailyCheckScheduler.triggerRetryProcess();

        // Then
        verify(dailyCheckOrchestrator, times(1)).processAllRetries();
    }
}