package com.anyang.maruni.domain.dailycheck.application.service;

import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.domain.dailycheck.domain.entity.DailyCheckRecord;
import com.anyang.maruni.domain.dailycheck.domain.entity.RetryRecord;
import com.anyang.maruni.domain.dailycheck.domain.repository.DailyCheckRecordRepository;
import com.anyang.maruni.domain.dailycheck.domain.repository.RetryRecordRepository;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // 상수 정의
    private static final String DAILY_CHECK_TITLE = "안부 메시지";
    private static final String DAILY_CHECK_MESSAGE = "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?";
    private static final int ALLOWED_START_HOUR = 7;
    private static final int ALLOWED_END_HOUR = 21;

    private final MemberRepository memberRepository;
    private final SimpleConversationService conversationService;
    private final NotificationService notificationService;
    private final DailyCheckRecordRepository dailyCheckRecordRepository;
    private final RetryRecordRepository retryRecordRepository;

    /**
     * 매일 오전 9시 안부 메시지 발송
     */
    @Scheduled(cron = "${maruni.scheduling.daily-check.cron}")
    @Transactional
    public void sendDailyCheckMessages() {
        log.info("Starting daily check message sending...");

        List<Long> activeMemberIds = memberRepository.findActiveMemberIds();
        log.info("Found {} active members", activeMemberIds.size());

        for (Long memberId : activeMemberIds) {
            processMemberDailyCheck(memberId);
        }

        log.info("Daily check message sending completed");
    }

    /**
     * 개별 회원 안부 확인 처리
     */
    private void processMemberDailyCheck(Long memberId) {
        try {
            // 중복 발송 체크
            if (isAlreadySentToday(memberId)) {
                log.debug("Already sent to member {} today, skipping", memberId);
                return;
            }

            // 안부 메시지 발송
            String title = DAILY_CHECK_TITLE;
            String message = DAILY_CHECK_MESSAGE;

            boolean success = notificationService.sendPushNotification(memberId, title, message);

            if (success) {
                handleSuccessfulSending(memberId, message);
            } else {
                handleFailedSending(memberId, message);
            }

        } catch (Exception e) {
            log.error("Error sending daily check message to member {}: {}", memberId, e.getMessage());
            scheduleRetry(memberId, DAILY_CHECK_MESSAGE);
        }
    }

    /**
     * 특정 회원에게 오늘 이미 발송했는지 확인
     */
    public boolean isAlreadySentToday(Long memberId) {
        return dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(memberId, LocalDate.now());
    }

    /**
     * 성공적인 발송 처리
     */
    private void handleSuccessfulSending(Long memberId, String message) {
        // 대화 시스템에 시스템 메시지로 기록
        conversationService.processSystemMessage(memberId, message);

        // 성공적인 발송 기록 저장
        saveDailyCheckRecord(memberId, message, true);

        log.debug("Daily check message sent successfully to member {}", memberId);
    }

    /**
     * 실패한 발송 처리
     */
    private void handleFailedSending(Long memberId, String message) {
        // 실패 기록 저장
        saveDailyCheckRecord(memberId, message, false);

        // 실패 시 재시도 스케줄에 등록
        scheduleRetry(memberId, message);
        log.warn("Failed to send daily check message to member {}, scheduled for retry", memberId);
    }

    /**
     * 발송 기록 저장 (성공/실패 공통)
     */
    private void saveDailyCheckRecord(Long memberId, String message, boolean success) {
        DailyCheckRecord record = success
            ? DailyCheckRecord.createSuccessRecord(memberId, message)
            : DailyCheckRecord.createFailureRecord(memberId, message);
        dailyCheckRecordRepository.save(record);
    }

    /**
     * 허용된 발송 시간인지 확인 (오전 7시 ~ 오후 9시)
     */
    public boolean isAllowedSendingTime(LocalTime currentTime) {
        int hour = currentTime.getHour();
        return hour >= ALLOWED_START_HOUR && hour <= ALLOWED_END_HOUR;
    }

    /**
     * 실패한 알림에 대한 재시도 스케줄링
     */
    public void scheduleRetry(Long memberId, String message) {
        RetryRecord retryRecord = RetryRecord.createRetryRecord(memberId, message);
        retryRecordRepository.save(retryRecord);
        log.info("Retry scheduled for member {} at {}", memberId, retryRecord.getScheduledTime());
    }

    /**
     * 재시도 대상 회원 ID 목록 조회
     */
    public List<Long> getPendingRetryMemberIds() {
        return retryRecordRepository.findPendingRetryMemberIds(LocalDateTime.now());
    }

    /**
     * 실패한 알림 재시도 처리
     */
    @Scheduled(cron = "${maruni.scheduling.retry.cron}")
    @Transactional
    public void processRetries() {
        log.info("Starting retry processing...");

        List<RetryRecord> pendingRetries = retryRecordRepository.findPendingRetries(LocalDateTime.now());
        log.info("Found {} pending retries", pendingRetries.size());

        for (RetryRecord retryRecord : pendingRetries) {
            processRetryRecord(retryRecord);
        }

        log.info("Retry processing completed");
    }

    /**
     * 개별 재시도 기록 처리
     */
    private void processRetryRecord(RetryRecord retryRecord) {
        try {
            boolean success = notificationService.sendPushNotification(
                    retryRecord.getMemberId(),
                    DAILY_CHECK_TITLE,
                    retryRecord.getMessage()
            );

            if (success) {
                handleSuccessfulRetry(retryRecord);
            } else {
                handleFailedRetry(retryRecord);
            }

            retryRecordRepository.save(retryRecord);

        } catch (Exception e) {
            log.error("Error during retry for member {}: {}",
                    retryRecord.getMemberId(), e.getMessage());
            retryRecord.incrementRetryCount();
            retryRecordRepository.save(retryRecord);
        }
    }

    /**
     * 성공한 재시도 처리
     */
    private void handleSuccessfulRetry(RetryRecord retryRecord) {
        // 재시도 성공
        retryRecord.markCompleted();

        // 대화 시스템에 기록 (중복 코드 재사용)
        conversationService.processSystemMessage(retryRecord.getMemberId(), retryRecord.getMessage());

        // 성공적인 발송 기록 저장 (중복 코드 재사용)
        saveDailyCheckRecord(retryRecord.getMemberId(), retryRecord.getMessage(), true);

        log.info("Retry successful for member {}", retryRecord.getMemberId());
    }

    /**
     * 실패한 재시도 처리
     */
    private void handleFailedRetry(RetryRecord retryRecord) {
        // 재시도 실패 - 횟수 증가
        retryRecord.incrementRetryCount();
        log.warn("Retry failed for member {}, attempt {} of 3",
                retryRecord.getMemberId(), retryRecord.getRetryCount());
    }
}