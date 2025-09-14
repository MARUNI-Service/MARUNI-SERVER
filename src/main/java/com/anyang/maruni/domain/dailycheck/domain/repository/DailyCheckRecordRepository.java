package com.anyang.maruni.domain.dailycheck.domain.repository;

import com.anyang.maruni.domain.dailycheck.domain.entity.DailyCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 매일 안부 확인 기록 저장소
 */
@Repository
public interface DailyCheckRecordRepository extends JpaRepository<DailyCheckRecord, Long> {

    /**
     * 특정 회원의 특정 날짜 성공적인 발송 기록 존재 여부 확인
     */
    @Query("SELECT COUNT(d) > 0 FROM DailyCheckRecord d " +
           "WHERE d.memberId = :memberId AND d.checkDate = :checkDate AND d.success = true")
    boolean existsSuccessfulRecordByMemberIdAndDate(@Param("memberId") Long memberId,
                                                   @Param("checkDate") LocalDate checkDate);
}