package com.anyang.maruni.domain.notification.domain.repository;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 발송 이력 리포지토리
 *
 * 알림 이력 저장, 조회 및 통계 분석을 위한 데이터 액세스 인터페이스입니다.
 * DDD 원칙에 따라 도메인 계층에서 인터페이스를 정의합니다.
 */
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    /**
     * 특정 회원의 알림 이력을 최신순으로 조회
     *
     * @param memberId 회원 ID
     * @return 알림 이력 목록 (최신순)
     */
    List<NotificationHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 특정 회원의 성공/실패 알림 이력을 최신순으로 조회
     *
     * @param memberId 회원 ID
     * @param success 성공 여부
     * @return 알림 이력 목록 (최신순)
     */
    List<NotificationHistory> findByMemberIdAndSuccessOrderByCreatedAtDesc(Long memberId, Boolean success);

    /**
     * 특정 회원의 특정 채널 알림 이력을 최신순으로 조회
     *
     * @param memberId 회원 ID
     * @param channelType 알림 채널 타입
     * @return 알림 이력 목록 (최신순)
     */
    List<NotificationHistory> findByMemberIdAndChannelTypeOrderByCreatedAtDesc(Long memberId, NotificationChannelType channelType);

    /**
     * 특정 기간 이후 성공한 알림 건수 조회
     *
     * @param from 기준 시점
     * @return 성공한 알림 건수
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.success = true AND h.createdAt >= :from")
    long countSuccessNotifications(@Param("from") LocalDateTime from);

    /**
     * 특정 기간 이후 전체 알림 건수 조회
     *
     * @param from 기준 시점
     * @return 전체 알림 건수
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.createdAt >= :from")
    long countTotalNotifications(@Param("from") LocalDateTime from);

    /**
     * 특정 기간 이후 실패한 알림 건수 조회
     *
     * @param from 기준 시점
     * @return 실패한 알림 건수
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.success = false AND h.createdAt >= :from")
    long countFailureNotifications(@Param("from") LocalDateTime from);

    /**
     * 채널별 성공률 조회 (특정 기간)
     *
     * @param channelType 알림 채널 타입
     * @param from 기준 시점
     * @return 성공한 알림 건수
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.channelType = :channelType AND h.success = true AND h.createdAt >= :from")
    long countSuccessNotificationsByChannel(@Param("channelType") NotificationChannelType channelType, @Param("from") LocalDateTime from);

    /**
     * 채널별 전체 알림 건수 조회 (특정 기간)
     *
     * @param channelType 알림 채널 타입
     * @param from 기준 시점
     * @return 전체 알림 건수
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.channelType = :channelType AND h.createdAt >= :from")
    long countTotalNotificationsByChannel(@Param("channelType") NotificationChannelType channelType, @Param("from") LocalDateTime from);

    /**
     * 특정 시점 이전의 오래된 이력 데이터 삭제
     * (데이터 정리용 - 운영 시 배치에서 사용)
     *
     * @param before 기준 시점
     */
    void deleteByCreatedAtBefore(LocalDateTime before);

    /**
     * 특정 회원의 최근 알림 이력 조회 (제한된 개수)
     *
     * @param memberId 회원 ID
     * @param limit 조회할 개수
     * @return 최근 알림 이력 목록
     */
    @Query("SELECT h FROM NotificationHistory h WHERE h.memberId = :memberId ORDER BY h.createdAt DESC LIMIT :limit")
    List<NotificationHistory> findRecentNotificationsByMemberId(@Param("memberId") Long memberId, @Param("limit") int limit);

    /**
     * 특정 회원의 안읽은 알림 개수 조회 (MVP 추가)
     *
     * @param memberId 회원 ID
     * @return 안읽은 알림 개수
     */
    Long countByMemberIdAndIsReadFalse(Long memberId);
}