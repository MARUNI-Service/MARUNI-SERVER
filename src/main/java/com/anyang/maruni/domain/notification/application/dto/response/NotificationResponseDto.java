package com.anyang.maruni.domain.notification.application.dto.response;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.vo.NotificationType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationSourceType;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO (MVP 추가)
 *
 * 클라이언트에게 반환할 알림 정보를 담는 DTO
 *
 * @param id 알림 ID
 * @param title 알림 제목
 * @param message 알림 내용
 * @param type 알림 타입
 * @param sourceType 알림 출처 타입
 * @param sourceEntityId 출처 엔티티 ID
 * @param isRead 읽음 여부
 * @param readAt 읽은 시각
 * @param createdAt 생성 시각
 */
public record NotificationResponseDto(
        Long id,
        String title,
        String message,
        NotificationType type,
        NotificationSourceType sourceType,
        Long sourceEntityId,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    /**
     * NotificationHistory 엔티티로부터 DTO 생성
     *
     * @param history 알림 이력 엔티티
     * @return NotificationResponseDto
     */
    public static NotificationResponseDto from(NotificationHistory history) {
        return new NotificationResponseDto(
                history.getId(),
                history.getTitle(),
                history.getMessage(),
                history.getNotificationType(),
                history.getSourceType(),
                history.getSourceEntityId(),
                history.getIsRead(),
                history.getReadAt(),
                history.getCreatedAt()
        );
    }
}
