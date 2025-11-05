package com.anyang.maruni.domain.notification.domain.entity;

import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationSourceType;
import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 발송 이력 엔티티 (MVP 확장 버전)
 *
 * 모든 알림 발송 시도와 결과를 추적하여
 * 디버깅, 통계 분석, 감사(Audit) 목적으로 활용합니다.
 *
 * MVP 추가사항: 알림 타입, 출처, 읽음 상태 추적
 */
@Entity
@Table(name = "notification_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter(AccessLevel.PRIVATE)
public class NotificationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 받는 회원 ID
     */
    @Column(nullable = false)
    private Long memberId;

    /**
     * 알림 제목
     */
    @Column(nullable = false)
    private String title;

    /**
     * 알림 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 알림 채널 타입 (PUSH, EMAIL, SMS 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channelType;

    /**
     * 발송 성공 여부
     */
    @Column(nullable = false)
    private Boolean success;

    /**
     * 실패 시 오류 메시지
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 외부 시스템에서 반환한 메시지 ID
     * (예: 푸시 알림 서비스의 messageId, SMS 발송 ID 등)
     */
    @Column
    private String externalMessageId;

    /**
     * 알림 타입 (DAILY_CHECK, GUARDIAN_REQUEST 등)
     * MVP 추가: 알림의 종류를 구분하기 위한 필드
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    @Builder.Default
    private NotificationType notificationType = NotificationType.SYSTEM;

    /**
     * 알림 출처 타입 (DAILY_CHECK, ALERT_RULE, GUARDIAN_REQUEST 등)
     * MVP 추가: 알림이 어느 시스템에서 발생했는지 추적
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    @Builder.Default
    private NotificationSourceType sourceType = NotificationSourceType.SYSTEM;

    /**
     * 출처 엔티티 ID
     * MVP 추가: 원본 엔티티(DailyCheckRecord, AlertHistory, GuardianRequest 등) 추적
     */
    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    /**
     * 읽음 여부
     * MVP 추가: 사용자가 알림을 읽었는지 추적
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 시각
     * MVP 추가: 사용자가 알림을 읽은 시각 기록
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 성공한 알림 이력 생성
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @return 성공 이력 엔티티
     */
    public static NotificationHistory createSuccess(Long memberId, String title,
                                                   String message, NotificationChannelType channelType) {
        return NotificationHistory.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .success(true)
                .build();
    }

    /**
     * 성공한 알림 이력 생성 (외부 메시지 ID 포함)
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @param externalMessageId 외부 메시지 ID
     * @return 성공 이력 엔티티
     */
    public static NotificationHistory createSuccess(Long memberId, String title,
                                                   String message, NotificationChannelType channelType,
                                                   String externalMessageId) {
        return NotificationHistory.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .success(true)
                .externalMessageId(externalMessageId)
                .build();
    }

    /**
     * 실패한 알림 이력 생성
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @param errorMessage 오류 메시지
     * @return 실패 이력 엔티티
     */
    public static NotificationHistory createFailure(Long memberId, String title,
                                                   String message, NotificationChannelType channelType,
                                                   String errorMessage) {
        return NotificationHistory.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 성공 이력인지 확인
     *
     * @return 성공 여부
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 실패 이력인지 확인
     *
     * @return 실패 여부
     */
    public boolean isFailure() {
        return Boolean.FALSE.equals(success);
    }

    /**
     * 타입 정보를 포함한 성공 알림 이력 생성 (MVP 추가)
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @param notificationType 알림 타입
     * @param sourceType 알림 출처 타입
     * @param sourceEntityId 출처 엔티티 ID
     * @param externalMessageId 외부 메시지 ID
     * @return 성공 이력 엔티티
     */
    public static NotificationHistory createSuccessWithType(
            Long memberId,
            String title,
            String message,
            NotificationChannelType channelType,
            NotificationType notificationType,
            NotificationSourceType sourceType,
            Long sourceEntityId,
            String externalMessageId
    ) {
        return NotificationHistory.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .notificationType(notificationType)
                .sourceType(sourceType)
                .sourceEntityId(sourceEntityId)
                .success(true)
                .externalMessageId(externalMessageId)
                .isRead(false)
                .build();
    }

    /**
     * 타입 정보를 포함한 실패 알림 이력 생성 (MVP 추가)
     *
     * @param memberId 회원 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @param channelType 알림 채널 타입
     * @param notificationType 알림 타입
     * @param sourceType 알림 출처 타입
     * @param sourceEntityId 출처 엔티티 ID
     * @param errorMessage 오류 메시지
     * @return 실패 이력 엔티티
     */
    public static NotificationHistory createFailureWithType(
            Long memberId,
            String title,
            String message,
            NotificationChannelType channelType,
            NotificationType notificationType,
            NotificationSourceType sourceType,
            Long sourceEntityId,
            String errorMessage
    ) {
        return NotificationHistory.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .notificationType(notificationType)
                .sourceType(sourceType)
                .sourceEntityId(sourceEntityId)
                .success(false)
                .errorMessage(errorMessage)
                .isRead(false)
                .build();
    }

    /**
     * 알림을 읽음 상태로 변경 (MVP 추가)
     *
     * JPA dirty checking으로 자동 업데이트됨
     * @Setter(AccessLevel.PRIVATE)를 통해 외부에서 직접 setter 호출 불가
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.setIsRead(true);
            this.setReadAt(LocalDateTime.now());
        }
    }
}