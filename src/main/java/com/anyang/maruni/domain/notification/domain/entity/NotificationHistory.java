package com.anyang.maruni.domain.notification.domain.entity;

import com.anyang.maruni.domain.notification.domain.service.NotificationChannelType;
import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 이력 엔티티
 *
 * 모든 알림 발송 시도와 결과를 추적하여
 * 디버깅, 통계 분석, 감사(Audit) 목적으로 활용합니다.
 */
@Entity
@Table(name = "notification_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
     * (예: Firebase의 messageId, SMS 발송 ID 등)
     */
    @Column
    private String externalMessageId;

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
}