package com.anyang.maruni.domain.dailycheck.domain.entity;

import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 안부 메시지 재시도 기록
 *
 * 실패한 알림에 대한 재시도 스케줄 관리
 */
@Entity
@Table(name = "retry_records")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    /**
     * 새 재시도 기록 생성
     */
    public static RetryRecord createRetryRecord(Long memberId, String message) {
        return RetryRecord.builder()
                .memberId(memberId)
                .message(message)
                .scheduledTime(LocalDateTime.now().plusMinutes(5)) // 5분 후 재시도
                .retryCount(0)
                .completed(false)
                .build();
    }

    /**
     * 재시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.scheduledTime = LocalDateTime.now().plusMinutes(5 * retryCount); // 점진적 지연
    }

    /**
     * 재시도 완료 처리
     */
    public void markCompleted() {
        this.completed = true;
    }
}