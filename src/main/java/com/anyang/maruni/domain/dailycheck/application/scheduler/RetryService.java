package com.anyang.maruni.domain.dailycheck.application.scheduler;

import com.anyang.maruni.domain.dailycheck.domain.entity.RetryRecord;
import com.anyang.maruni.domain.dailycheck.domain.repository.RetryRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재시도 전담 서비스
 *
 * 단일 책임: 재시도 스케줄링 및 재시도 기록 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RetryService {

    private final RetryRecordRepository retryRecordRepository;

    /**
     * 실패한 알림에 대한 재시도 스케줄링
     */
    @Transactional
    public void scheduleRetry(Long memberId, String message) {
        RetryRecord retryRecord = RetryRecord.createRetryRecord(memberId, message);
        retryRecordRepository.save(retryRecord);
        log.info("Retry scheduled for member {} at {}", memberId, retryRecord.getScheduledTime());
    }

    /**
     * 재시도 대상 회원 ID 목록 조회
     */
    public List<Long> getPendingRetryMemberIds(LocalDateTime currentTime) {
        return retryRecordRepository.findPendingRetryMemberIds(currentTime);
    }

    /**
     * 재시도 대상 기록 목록 조회
     */
    public List<RetryRecord> getPendingRetries(LocalDateTime currentTime) {
        return retryRecordRepository.findPendingRetries(currentTime);
    }

    /**
     * 재시도 기록 저장
     */
    @Transactional
    public void saveRetryRecord(RetryRecord retryRecord) {
        retryRecordRepository.save(retryRecord);
    }

    /**
     * 재시도 완료 처리
     */
    public void markCompleted(RetryRecord retryRecord) {
        retryRecord.markCompleted();
    }

    /**
     * 실패한 재시도 처리
     */
    public void handleFailedRetry(RetryRecord retryRecord) {
        // 재시도 실패 - 횟수 증가
        retryRecord.incrementRetryCount();
        log.warn("Retry failed for member {}, attempt {} of 3",
                retryRecord.getMemberId(), retryRecord.getRetryCount());
    }

    /**
     * 재시도 횟수 증가 (예외 발생 시)
     */
    @Transactional
    public void incrementRetryCount(RetryRecord retryRecord) {
        retryRecord.incrementRetryCount();
        retryRecordRepository.save(retryRecord);
    }
}