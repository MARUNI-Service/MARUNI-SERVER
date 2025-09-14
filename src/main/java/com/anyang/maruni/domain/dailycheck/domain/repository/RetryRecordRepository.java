package com.anyang.maruni.domain.dailycheck.domain.repository;

import com.anyang.maruni.domain.dailycheck.domain.entity.RetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재시도 기록 저장소
 */
@Repository
public interface RetryRecordRepository extends JpaRepository<RetryRecord, Long> {

    /**
     * 재시도 대상 회원 ID 목록 조회 (완료되지 않고 스케줄 시간이 된 것들)
     */
    @Query("SELECT r.memberId FROM RetryRecord r " +
           "WHERE r.completed = false AND r.scheduledTime <= :currentTime AND r.retryCount < 3")
    List<Long> findPendingRetryMemberIds(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 특정 시간 이전의 미완료 재시도 기록들 조회
     */
    @Query("SELECT r FROM RetryRecord r " +
           "WHERE r.completed = false AND r.scheduledTime <= :currentTime AND r.retryCount < 3")
    List<RetryRecord> findPendingRetries(@Param("currentTime") LocalDateTime currentTime);
}