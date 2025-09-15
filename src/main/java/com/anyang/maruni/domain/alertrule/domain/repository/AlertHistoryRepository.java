package com.anyang.maruni.domain.alertrule.domain.repository;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AlertHistory Repository
 *
 * 알림 이력 데이터 액세스를 담당합니다.
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    /**
     * 회원별 알림 이력 조회 (최신순)
     * @param memberId 회원 ID
     * @param pageable 페이징 정보
     * @return 알림 이력 페이지
     */
    @Query("SELECT ah FROM AlertHistory ah " +
           "WHERE ah.member.id = :memberId " +
           "ORDER BY ah.createdAt DESC")
    Page<AlertHistory> findByMemberIdOrderByCreatedAtDesc(
            @Param("memberId") Long memberId, Pageable pageable);

    /**
     * 회원별 특정 기간 알림 이력 조회
     * @param memberId 회원 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 알림 이력 목록
     */
    @Query("SELECT ah FROM AlertHistory ah " +
           "WHERE ah.member.id = :memberId " +
           "AND ah.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ah.createdAt DESC")
    List<AlertHistory> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 회원별 특정 레벨 이상의 알림 이력 조회
     * @param memberId 회원 ID
     * @param alertLevel 최소 알림 레벨
     * @param pageable 페이징 정보
     * @return 알림 이력 페이지
     */
    @Query("SELECT ah FROM AlertHistory ah " +
           "WHERE ah.member.id = :memberId " +
           "AND ah.alertLevel = :alertLevel " +
           "ORDER BY ah.createdAt DESC")
    Page<AlertHistory> findByMemberIdAndMinAlertLevel(
            @Param("memberId") Long memberId,
            @Param("alertLevel") AlertLevel alertLevel,
            Pageable pageable);

    /**
     * 발송되지 않은 알림 이력 조회 (재시도용)
     * @return 미발송 알림 이력 목록
     */
    @Query("SELECT ah FROM AlertHistory ah " +
           "WHERE ah.isNotificationSent = false " +
           "AND (ah.notificationResult IS NULL OR ah.notificationResult LIKE 'FAILED%') " +
           "ORDER BY ah.alertLevel DESC, ah.createdAt ASC")
    List<AlertHistory> findPendingNotifications();

    /**
     * 특정 시간 이전의 미발송 알림 조회 (시간 초과된 알림)
     * @param beforeTime 기준 시간
     * @return 시간 초과된 미발송 알림 목록
     */
    @Query("SELECT ah FROM AlertHistory ah " +
           "WHERE ah.isNotificationSent = false " +
           "AND ah.createdAt < :beforeTime " +
           "ORDER BY ah.createdAt ASC")
    List<AlertHistory> findTimeoutPendingNotifications(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 회원별 일일 알림 이력 존재 여부 확인 (중복 방지용)
     * @param memberId 회원 ID
     * @param alertRuleId 알림 규칙 ID
     * @param alertDate 알림 날짜
     * @return 존재 여부
     */
    boolean existsByMemberIdAndAlertRuleIdAndAlertDate(
            Long memberId, Long alertRuleId, LocalDateTime alertDate);

    /**
     * 회원별 최근 N일간 알림 횟수 조회
     * @param memberId 회원 ID
     * @param daysAgo N일 전
     * @return 알림 횟수
     */
    @Query("SELECT COUNT(ah) FROM AlertHistory ah " +
           "WHERE ah.member.id = :memberId " +
           "AND ah.createdAt >= :daysAgo")
    long countRecentAlertsByMemberId(
            @Param("memberId") Long memberId,
            @Param("daysAgo") LocalDateTime daysAgo);

    /**
     * 전체 긴급 알림 이력 조회 (관리자용)
     * @param pageable 페이징 정보
     * @return 긴급 알림 이력 페이지
     */
    @Query("SELECT ah FROM AlertHistory ah " +
           "WHERE ah.alertLevel = 'EMERGENCY' " +
           "ORDER BY ah.createdAt DESC")
    Page<AlertHistory> findAllEmergencyAlerts(Pageable pageable);

    /**
     * 알림 발송 성공률 통계 (최근 N일)
     * @param daysAgo N일 전
     * @return 발송 성공률 (0.0 ~ 1.0)
     */
    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN ah.isNotificationSent = true THEN 1 END) AS DOUBLE) / " +
           "CAST(COUNT(ah) AS DOUBLE) " +
           "FROM AlertHistory ah " +
           "WHERE ah.createdAt >= :daysAgo")
    Double calculateNotificationSuccessRate(@Param("daysAgo") LocalDateTime daysAgo);
}