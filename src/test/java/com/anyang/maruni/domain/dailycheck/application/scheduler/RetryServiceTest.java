package com.anyang.maruni.domain.dailycheck.application.scheduler;

import com.anyang.maruni.domain.dailycheck.domain.entity.RetryRecord;
import com.anyang.maruni.domain.dailycheck.domain.repository.RetryRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * RetryService 테스트
 *
 * 재시도 로직 전담 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryService 테스트")
class RetryServiceTest {

    @Mock
    private RetryRecordRepository retryRecordRepository;

    @InjectMocks
    private RetryService retryService;

    @Test
    @DisplayName("실패한 알림에 대한 재시도를 스케줄링한다")
    void scheduleRetry_shouldSaveRetryRecord() {
        // Given
        Long memberId = 1L;
        String message = "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?";

        // When
        retryService.scheduleRetry(memberId, message);

        // Then
        ArgumentCaptor<RetryRecord> captor = ArgumentCaptor.forClass(RetryRecord.class);
        verify(retryRecordRepository).save(captor.capture());

        RetryRecord savedRecord = captor.getValue();
        assertEquals(memberId, savedRecord.getMemberId());
        assertEquals(message, savedRecord.getMessage());
        assertEquals(0, savedRecord.getRetryCount());
        assertFalse(savedRecord.getCompleted());
        assertNotNull(savedRecord.getScheduledTime());
    }

    @Test
    @DisplayName("재시도 대상 회원 ID 목록을 조회한다")
    void getPendingRetryMemberIds_shouldReturnMemberIds() {
        // Given
        LocalDateTime currentTime = LocalDateTime.now();
        List<Long> expectedMemberIds = Arrays.asList(1L, 2L, 3L);
        given(retryRecordRepository.findPendingRetryMemberIds(currentTime))
                .willReturn(expectedMemberIds);

        // When
        List<Long> result = retryService.getPendingRetryMemberIds(currentTime);

        // Then
        assertEquals(expectedMemberIds, result);
        verify(retryRecordRepository).findPendingRetryMemberIds(currentTime);
    }

    @Test
    @DisplayName("재시도 대상 기록 목록을 조회한다")
    void getPendingRetries_shouldReturnRetryRecords() {
        // Given
        LocalDateTime currentTime = LocalDateTime.now();
        RetryRecord retryRecord1 = RetryRecord.createRetryRecord(1L, "message1");
        RetryRecord retryRecord2 = RetryRecord.createRetryRecord(2L, "message2");
        List<RetryRecord> expectedRecords = Arrays.asList(retryRecord1, retryRecord2);
        given(retryRecordRepository.findPendingRetries(currentTime))
                .willReturn(expectedRecords);

        // When
        List<RetryRecord> result = retryService.getPendingRetries(currentTime);

        // Then
        assertEquals(expectedRecords, result);
        verify(retryRecordRepository).findPendingRetries(currentTime);
    }

    @Test
    @DisplayName("재시도 기록을 저장한다")
    void saveRetryRecord_shouldSaveRecord() {
        // Given
        RetryRecord retryRecord = RetryRecord.createRetryRecord(1L, "test message");

        // When
        retryService.saveRetryRecord(retryRecord);

        // Then
        verify(retryRecordRepository).save(retryRecord);
    }

    @Test
    @DisplayName("재시도 완료 처리를 한다")
    void markCompleted_shouldMarkRecordAsCompleted() {
        // Given
        RetryRecord retryRecord = RetryRecord.createRetryRecord(1L, "test message");
        assertFalse(retryRecord.getCompleted()); // 초기 상태 확인

        // When
        retryService.markCompleted(retryRecord);

        // Then
        assertTrue(retryRecord.getCompleted());
    }

    @Test
    @DisplayName("실패한 재시도 처리 시 횟수를 증가시킨다")
    void handleFailedRetry_shouldIncrementRetryCount() {
        // Given
        RetryRecord retryRecord = RetryRecord.createRetryRecord(1L, "test message");
        int initialCount = retryRecord.getRetryCount();

        // When
        retryService.handleFailedRetry(retryRecord);

        // Then
        assertEquals(initialCount + 1, retryRecord.getRetryCount());
    }

    @Test
    @DisplayName("예외 발생 시 재시도 횟수를 증가시키고 저장한다")
    void incrementRetryCount_shouldIncrementAndSave() {
        // Given
        RetryRecord retryRecord = RetryRecord.createRetryRecord(1L, "test message");
        int initialCount = retryRecord.getRetryCount();

        // When
        retryService.incrementRetryCount(retryRecord);

        // Then
        assertEquals(initialCount + 1, retryRecord.getRetryCount());
        verify(retryRecordRepository).save(retryRecord);
    }
}