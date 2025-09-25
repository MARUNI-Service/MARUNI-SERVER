package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.repository.NotificationHistoryRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.domain.notification.domain.service.NotificationStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 이력 서비스 구현체
 *
 * 알림 발송 이력의 저장, 조회, 통계 분석을 담당하는 Infrastructure 계층 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationHistoryServiceImpl implements NotificationHistoryService {

    private final NotificationHistoryRepository historyRepository;

    @Override
    @Transactional
    public NotificationHistory recordSuccess(Long memberId, String title, String message,
                                           NotificationChannelType channelType) {
        validateInput(memberId, title, message, channelType);

        NotificationHistory history = NotificationHistory.createSuccess(
                memberId, title, message, channelType);

        NotificationHistory savedHistory = historyRepository.save(history);

        log.info("✅ Notification success recorded - memberId: {}, channel: {}, historyId: {}",
                memberId, channelType, savedHistory.getId());

        return savedHistory;
    }

    @Override
    @Transactional
    public NotificationHistory recordSuccess(Long memberId, String title, String message,
                                           NotificationChannelType channelType, String externalMessageId) {
        validateInput(memberId, title, message, channelType);

        NotificationHistory history = NotificationHistory.createSuccess(
                memberId, title, message, channelType, externalMessageId);

        NotificationHistory savedHistory = historyRepository.save(history);

        log.info("✅ Notification success recorded - memberId: {}, channel: {}, historyId: {}, externalId: {}",
                memberId, channelType, savedHistory.getId(), externalMessageId);

        return savedHistory;
    }

    @Override
    @Transactional
    public NotificationHistory recordFailure(Long memberId, String title, String message,
                                           NotificationChannelType channelType, String errorMessage) {
        validateInput(memberId, title, message, channelType);

        NotificationHistory history = NotificationHistory.createFailure(
                memberId, title, message, channelType, errorMessage);

        NotificationHistory savedHistory = historyRepository.save(history);

        log.warn("❌ Notification failure recorded - memberId: {}, channel: {}, historyId: {}, error: {}",
                memberId, channelType, savedHistory.getId(), errorMessage);

        return savedHistory;
    }

    @Override
    public List<NotificationHistory> getHistoryByMember(Long memberId) {
        validateMemberId(memberId);
        return historyRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Override
    public List<NotificationHistory> getSuccessHistoryByMember(Long memberId) {
        validateMemberId(memberId);
        return historyRepository.findByMemberIdAndSuccessOrderByCreatedAtDesc(memberId, true);
    }

    @Override
    public List<NotificationHistory> getFailureHistoryByMember(Long memberId) {
        validateMemberId(memberId);
        return historyRepository.findByMemberIdAndSuccessOrderByCreatedAtDesc(memberId, false);
    }

    @Override
    public List<NotificationHistory> getRecentHistoryByMember(Long memberId, int limit) {
        validateMemberId(memberId);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive: " + limit);
        }

        return historyRepository.findRecentNotificationsByMemberId(memberId, limit);
    }

    @Override
    public double calculateSuccessRate(LocalDateTime from) {
        validateDateTime(from);

        long totalNotifications = historyRepository.countTotalNotifications(from);
        if (totalNotifications == 0) {
            return 0.0;
        }

        long successNotifications = historyRepository.countSuccessNotifications(from);
        return (double) successNotifications / totalNotifications;
    }

    @Override
    public double calculateSuccessRateByChannel(NotificationChannelType channelType, LocalDateTime from) {
        validateChannelType(channelType);
        validateDateTime(from);

        long totalNotifications = historyRepository.countTotalNotificationsByChannel(channelType, from);
        if (totalNotifications == 0) {
            return 0.0;
        }

        long successNotifications = historyRepository.countSuccessNotificationsByChannel(channelType, from);
        return (double) successNotifications / totalNotifications;
    }

    @Override
    public NotificationStatistics getStatistics(LocalDateTime from) {
        validateDateTime(from);

        long totalNotifications = historyRepository.countTotalNotifications(from);
        long successNotifications = historyRepository.countSuccessNotifications(from);
        long failureNotifications = historyRepository.countFailureNotifications(from);

        NotificationStatistics statistics = NotificationStatistics.of(
                totalNotifications, successNotifications, failureNotifications);

        log.debug("📊 Notification statistics calculated for period from {}: {}",
                from, statistics.getSummary());

        return statistics;
    }

    @Override
    @Transactional
    public long cleanupOldHistory(LocalDateTime before) {
        validateDateTime(before);

        long countBefore = historyRepository.countTotalNotifications(before);
        historyRepository.deleteByCreatedAtBefore(before);

        log.info("🗑️ Cleaned up {} old notification history records before {}",
                countBefore, before);

        return countBefore;
    }

    /**
     * 입력 값 검증
     */
    private void validateInput(Long memberId, String title, String message, NotificationChannelType channelType) {
        validateMemberId(memberId);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        validateChannelType(channelType);
    }

    /**
     * 회원 ID 검증
     */
    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Invalid member ID: " + memberId);
        }
    }

    /**
     * 채널 타입 검증
     */
    private void validateChannelType(NotificationChannelType channelType) {
        if (channelType == null) {
            throw new IllegalArgumentException("Channel type cannot be null");
        }
    }

    /**
     * 날짜시간 검증
     */
    private void validateDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("DateTime cannot be null");
        }

        if (dateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("DateTime cannot be in the future: " + dateTime);
        }
    }
}