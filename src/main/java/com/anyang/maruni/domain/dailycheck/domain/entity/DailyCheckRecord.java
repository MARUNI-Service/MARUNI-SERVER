package com.anyang.maruni.domain.dailycheck.domain.entity;

import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 매일 안부 확인 발송 기록
 *
 * 중복 발송 방지를 위한 일일 발송 이력 관리
 */
@Entity
@Table(name = "daily_check_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"memberId", "checkDate"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCheckRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate checkDate;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean success;

    /**
     * 성공적인 발송 기록 생성
     */
    public static DailyCheckRecord createSuccessRecord(Long memberId, String message) {
        return DailyCheckRecord.builder()
                .memberId(memberId)
                .checkDate(LocalDate.now())
                .message(message)
                .success(true)
                .build();
    }

    /**
     * 실패한 발송 기록 생성
     */
    public static DailyCheckRecord createFailureRecord(Long memberId, String message) {
        return DailyCheckRecord.builder()
                .memberId(memberId)
                .checkDate(LocalDate.now())
                .message(message)
                .success(false)
                .build();
    }
}