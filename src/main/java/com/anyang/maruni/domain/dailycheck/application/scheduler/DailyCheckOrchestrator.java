package com.anyang.maruni.domain.dailycheck.application.scheduler;

import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.domain.dailycheck.domain.entity.DailyCheckRecord;
import com.anyang.maruni.domain.dailycheck.domain.entity.RetryRecord;
import com.anyang.maruni.domain.dailycheck.domain.repository.DailyCheckRecordRepository;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.domain.notification.domain.vo.NotificationType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DailyCheck 오케스트레이터
 *
 * 단일 책임: 메인 비즈니스 로직 조정 및 회원별 안부 확인 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyCheckOrchestrator {

    // 상수 정의
    private static final String DAILY_CHECK_TITLE = "안부 메시지";
    private static final String DAILY_CHECK_MESSAGE = "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?";
    private static final int ALLOWED_START_HOUR = 7;
    private static final int ALLOWED_END_HOUR = 21;

    private final MemberRepository memberRepository;
    private final SimpleConversationService conversationService;
    private final NotificationService notificationService;
    private final DailyCheckRecordRepository dailyCheckRecordRepository;
    private final RetryService retryService;

    /**
     * 모든 활성 회원에게 안부 메시지 발송 - 실제 비즈니스 로직
     */
    @Transactional
    public void processAllActiveMembers() {
        List<Long> activeMemberIds = memberRepository.findDailyCheckEnabledMemberIds();
        log.info("Found {} daily check enabled members", activeMemberIds.size());

        for (Long memberId : activeMemberIds) {
            processMemberDailyCheck(memberId);
        }

        log.info("Daily check message sending completed");
    }

    /**
     * 모든 재시도 대상 처리 - 실제 비즈니스 로직
     */
    @Transactional
    public void processAllRetries() {
        List<RetryRecord> pendingRetries = retryService.getPendingRetries(LocalDateTime.now());
        log.info("Found {} pending retries", pendingRetries.size());

        for (RetryRecord retryRecord : pendingRetries) {
            processRetryRecord(retryRecord);
        }

        log.info("Retry processing completed");
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

            // 안부 메시지 발송 (MVP: 타입 정보 포함)
            String title = DAILY_CHECK_TITLE;
            String message = DAILY_CHECK_MESSAGE;

            boolean success = notificationService.sendNotificationWithType(
                memberId,
                title,
                message,
                NotificationType.DAILY_CHECK,
                NotificationSourceType.DAILY_CHECK,
                null  // MVP: DailyCheckRecord ID는 발송 후 생성되므로 null
            );

            if (success) {
                handleSuccessfulSending(memberId, message);
            } else {
                handleFailedSending(memberId, message);
            }

        } catch (Exception e) {
            log.error("Error sending daily check message to member {}: {}", memberId, e.getMessage());
            retryService.scheduleRetry(memberId, DAILY_CHECK_MESSAGE);
        }
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
                retryService.handleFailedRetry(retryRecord);
            }

            retryService.saveRetryRecord(retryRecord);

        } catch (Exception e) {
            log.error("Error during retry for member {}: {}",
                    retryRecord.getMemberId(), e.getMessage());
            retryService.incrementRetryCount(retryRecord);
        }
    }

    /**
     * 특정 회원에게 오늘 이미 발송했는지 확인
     */
    public boolean isAlreadySentToday(Long memberId) {
        return dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(memberId, LocalDate.now());
    }

    /**
     * 허용된 발송 시간인지 확인 (오전 7시 ~ 오후 9시)
     */
    public boolean isAllowedSendingTime(LocalTime currentTime) {
        int hour = currentTime.getHour();
        return hour >= ALLOWED_START_HOUR && hour <= ALLOWED_END_HOUR;
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
        retryService.scheduleRetry(memberId, message);
        log.warn("Failed to send daily check message to member {}, scheduled for retry", memberId);
    }

    /**
     * 성공한 재시도 처리
     */
    private void handleSuccessfulRetry(RetryRecord retryRecord) {
        // 재시도 성공
        retryService.markCompleted(retryRecord);

        // 대화 시스템에 기록 (중복 코드 재사용)
        conversationService.processSystemMessage(retryRecord.getMemberId(), retryRecord.getMessage());

        // 성공적인 발송 기록 저장 (중복 코드 재사용)
        saveDailyCheckRecord(retryRecord.getMemberId(), retryRecord.getMessage(), true);

        log.info("Retry successful for member {}", retryRecord.getMemberId());
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
}