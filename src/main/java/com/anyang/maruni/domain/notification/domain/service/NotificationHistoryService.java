package com.anyang.maruni.domain.notification.domain.service;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationStatistics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 이력 도메인 서비스 인터페이스
 *
 * 알림 발송 이력의 저장, 조회, 통계 분석을 담당합니다.
 * DDD 원칙에 따라 도메인 계층에서 인터페이스를 정의합니다.
 */
public interface NotificationHistoryService {

    /**
     * 성공한 알림 이력 저장
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @return 저장된 이력 엔티티
     */
    NotificationHistory recordSuccess(Long memberId, String title, String message,
                                     NotificationChannelType channelType);

    /**
     * 성공한 알림 이력 저장 (외부 메시지 ID 포함)
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @param externalMessageId 외부 메시지 ID
     * @return 저장된 이력 엔티티
     */
    NotificationHistory recordSuccess(Long memberId, String title, String message,
                                     NotificationChannelType channelType, String externalMessageId);

    /**
     * 실패한 알림 이력 저장
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @param errorMessage 오류 메시지
     * @return 저장된 이력 엔티티
     */
    NotificationHistory recordFailure(Long memberId, String title, String message,
                                     NotificationChannelType channelType, String errorMessage);

    /**
     * 특정 회원의 알림 이력 조회
     *
     * @param memberId 회원 ID
     * @return 알림 이력 목록 (최신순)
     */
    List<NotificationHistory> getHistoryByMember(Long memberId);

    /**
     * 특정 회원의 성공한 알림 이력 조회
     *
     * @param memberId 회원 ID
     * @return 성공한 알림 이력 목록 (최신순)
     */
    List<NotificationHistory> getSuccessHistoryByMember(Long memberId);

    /**
     * 특정 회원의 실패한 알림 이력 조회
     *
     * @param memberId 회원 ID
     * @return 실패한 알림 이력 목록 (최신순)
     */
    List<NotificationHistory> getFailureHistoryByMember(Long memberId);

    /**
     * 특정 회원의 최근 알림 이력 조회 (제한된 개수)
     *
     * @param memberId 회원 ID
     * @param limit 조회할 개수
     * @return 최근 알림 이력 목록
     */
    List<NotificationHistory> getRecentHistoryByMember(Long memberId, int limit);

    /**
     * 알림 성공률 계산 (특정 기간)
     *
     * @param from 기준 시점
     * @return 성공률 (0.0 ~ 1.0)
     */
    double calculateSuccessRate(LocalDateTime from);

    /**
     * 채널별 알림 성공률 계산 (특정 기간)
     *
     * @param channelType 알림 채널 타입
     * @param from 기준 시점
     * @return 성공률 (0.0 ~ 1.0)
     */
    double calculateSuccessRateByChannel(NotificationChannelType channelType, LocalDateTime from);

    /**
     * 알림 발송 통계 조회 (특정 기간)
     *
     * @param from 기준 시점
     * @return 알림 통계 정보
     */
    NotificationStatistics getStatistics(LocalDateTime from);

    /**
     * 오래된 이력 데이터 정리
     *
     * @param before 기준 시점
     * @return 삭제된 이력 개수
     */
    long cleanupOldHistory(LocalDateTime before);
}