package com.anyang.maruni.domain.dailycheck.domain.repository;

import com.anyang.maruni.domain.dailycheck.domain.entity.DailyCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * 특정 회원의 기간별 DailyCheck 기록 조회 (무응답 패턴 분석용)
     * @param memberId 회원 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return DailyCheck 기록 목록 (최신순)
     */
    @Query("SELECT d FROM DailyCheckRecord d " +
           "WHERE d.memberId = :memberId " +
           "AND d.checkDate BETWEEN :startDate AND :endDate " +
           "ORDER BY d.checkDate DESC")
    List<DailyCheckRecord> findByMemberIdAndDateRangeOrderByCheckDateDesc(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}