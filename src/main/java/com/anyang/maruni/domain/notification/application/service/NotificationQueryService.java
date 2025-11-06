package com.anyang.maruni.domain.notification.application.service;

import com.anyang.maruni.domain.notification.application.dto.response.NotificationResponseDto;
import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.repository.NotificationHistoryRepository;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 조회 서비스 (MVP 추가)
 *
 * 알림 목록 조회, 안읽은 알림 개수 조회, 읽음 처리 등을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationHistoryRepository repository;

    /**
     * 회원의 모든 알림 조회 (최신순)
     *
     * @param memberId 회원 ID
     * @return 알림 목록
     */
    public List<NotificationResponseDto> getAllNotifications(Long memberId) {
        return repository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(NotificationResponseDto::from)
                .toList();
    }

    /**
     * 안읽은 알림 개수 조회
     *
     * @param memberId 회원 ID
     * @return 안읽은 알림 개수
     */
    public Long getUnreadCount(Long memberId) {
        return repository.countByMemberIdAndIsReadFalse(memberId);
    }

    /**
     * 알림 읽음 처리
     *
     * JPA dirty checking을 통해 자동으로 DB 업데이트됨
     *
     * @param notificationId 알림 ID
     * @return 업데이트된 알림 정보
     * @throws BaseException 알림을 찾을 수 없는 경우
     */
    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId) {
        NotificationHistory notification = repository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();

        return NotificationResponseDto.from(notification);
    }
}
