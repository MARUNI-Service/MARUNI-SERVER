package com.anyang.maruni.domain.dailycheck.application.service;

import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * 매일 안부 확인 서비스
 *
 * TDD Red Phase - 실패하는 구현
 * Week 5 Day 1-2: 테스트가 실패하도록 최소한의 구조만 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyCheckService {

    private final MemberRepository memberRepository;
    private final SimpleConversationService conversationService;
    private final NotificationService notificationService;

    /**
     * 매일 오전 9시 안부 메시지 발송
     */
    @Scheduled(cron = "${maruni.scheduling.daily-check.cron}")
    @Transactional
    public void sendDailyCheckMessages() {
        // TDD Red: 빈 구현으로 테스트 실패 유도
        log.info("Daily check messages scheduled - NOT IMPLEMENTED YET");
        throw new UnsupportedOperationException("Not implemented yet - TDD Red phase");
    }

    /**
     * 특정 회원에게 오늘 이미 발송했는지 확인
     */
    public boolean isAlreadySentToday(Long memberId) {
        // TDD Red: 항상 false 반환하여 테스트 실패 유도
        return false;
    }

    /**
     * 허용된 발송 시간인지 확인
     */
    public boolean isAllowedSendingTime(LocalTime currentTime) {
        // TDD Red: 잘못된 로직으로 테스트 실패 유도
        return currentTime.getHour() < 8; // 의도적으로 잘못된 조건
    }

    /**
     * 실패한 알림에 대한 재시도 스케줄링
     */
    public void scheduleRetry(Long memberId, String message) {
        // TDD Red: 빈 구현
        log.info("Retry scheduled for member {} - NOT IMPLEMENTED YET", memberId);
    }

    /**
     * 재시도 대상 회원 ID 목록 조회
     */
    public List<Long> getPendingRetryMemberIds() {
        // TDD Red: 빈 리스트 반환
        return List.of();
    }

    /**
     * 실패한 알림 재시도 처리
     */
    @Scheduled(cron = "${maruni.scheduling.retry.cron}")
    @Transactional
    public void processRetries() {
        // TDD Red: 빈 구현
        log.info("Processing retries - NOT IMPLEMENTED YET");
    }
}