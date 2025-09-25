package com.anyang.maruni.domain.notification.infrastructure;

import com.anyang.maruni.domain.notification.domain.entity.NotificationHistory;
import com.anyang.maruni.domain.notification.domain.repository.NotificationHistoryRepository;
import com.anyang.maruni.domain.notification.domain.vo.NotificationChannelType;
import com.anyang.maruni.domain.notification.domain.vo.NotificationStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * NotificationHistoryServiceImpl 단위 테스트
 *
 * 알림 이력 저장 및 조회 로직의 정확성을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationHistoryServiceImpl 테스트")
class NotificationHistoryServiceImplTest {

    @Mock
    private NotificationHistoryRepository historyRepository;

    @InjectMocks
    private NotificationHistoryServiceImpl historyService;

    @Test
    @DisplayName("성공한 알림 이력 저장 - 기본")
    void shouldRecordSuccessNotification() {
        // Given
        Long memberId = 1L;
        String title = "안부 메시지";
        String message = "오늘 하루는 어떻게 지내고 계신가요?";
        NotificationChannelType channelType = NotificationChannelType.PUSH;

        NotificationHistory expectedHistory = NotificationHistory.createSuccess(
                memberId, title, message, channelType);
        expectedHistory = NotificationHistory.builder()
                .id(1L)
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .success(true)
                .build();

        given(historyRepository.save(any(NotificationHistory.class)))
                .willReturn(expectedHistory);

        // When
        NotificationHistory result = historyService.recordSuccess(
                memberId, title, message, channelType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getMessage()).isEqualTo(message);
        assertThat(result.getChannelType()).isEqualTo(channelType);
        assertThat(result.isSuccess()).isTrue();

        verify(historyRepository).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("성공한 알림 이력 저장 - 외부 메시지 ID 포함")
    void shouldRecordSuccessNotificationWithExternalMessageId() {
        // Given
        Long memberId = 1L;
        String title = "안부 메시지";
        String message = "오늘 하루는 어떻게 지내고 계신가요?";
        NotificationChannelType channelType = NotificationChannelType.PUSH;
        String externalMessageId = "firebase_msg_12345";

        NotificationHistory expectedHistory = NotificationHistory.builder()
                .id(1L)
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .success(true)
                .externalMessageId(externalMessageId)
                .build();

        given(historyRepository.save(any(NotificationHistory.class)))
                .willReturn(expectedHistory);

        // When
        NotificationHistory result = historyService.recordSuccess(
                memberId, title, message, channelType, externalMessageId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExternalMessageId()).isEqualTo(externalMessageId);
        assertThat(result.isSuccess()).isTrue();

        verify(historyRepository).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("실패한 알림 이력 저장")
    void shouldRecordFailureNotification() {
        // Given
        Long memberId = 1L;
        String title = "안부 메시지";
        String message = "오늘 하루는 어떻게 지내고 계신가요?";
        NotificationChannelType channelType = NotificationChannelType.PUSH;
        String errorMessage = "Firebase connection failed";

        NotificationHistory expectedHistory = NotificationHistory.builder()
                .id(1L)
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(channelType)
                .success(false)
                .errorMessage(errorMessage)
                .build();

        given(historyRepository.save(any(NotificationHistory.class)))
                .willReturn(expectedHistory);

        // When
        NotificationHistory result = historyService.recordFailure(
                memberId, title, message, channelType, errorMessage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);

        verify(historyRepository).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("회원별 알림 이력 조회")
    void shouldGetHistoryByMember() {
        // Given
        Long memberId = 1L;
        List<NotificationHistory> expectedHistories = Arrays.asList(
                createSampleHistory(1L, memberId, true),
                createSampleHistory(2L, memberId, false)
        );

        given(historyRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
                .willReturn(expectedHistories);

        // When
        List<NotificationHistory> result = historyService.getHistoryByMember(memberId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedHistories);

        verify(historyRepository).findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Test
    @DisplayName("성공률 계산")
    void shouldCalculateSuccessRate() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        long totalNotifications = 100L;
        long successNotifications = 85L;

        given(historyRepository.countTotalNotifications(from))
                .willReturn(totalNotifications);
        given(historyRepository.countSuccessNotifications(from))
                .willReturn(successNotifications);

        // When
        double successRate = historyService.calculateSuccessRate(from);

        // Then
        assertThat(successRate).isEqualTo(0.85);

        verify(historyRepository).countTotalNotifications(from);
        verify(historyRepository).countSuccessNotifications(from);
    }

    @Test
    @DisplayName("통계 정보 조회")
    void shouldGetStatistics() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        long totalNotifications = 1000L;
        long successNotifications = 950L;
        long failureNotifications = 50L;

        given(historyRepository.countTotalNotifications(from))
                .willReturn(totalNotifications);
        given(historyRepository.countSuccessNotifications(from))
                .willReturn(successNotifications);
        given(historyRepository.countFailureNotifications(from))
                .willReturn(failureNotifications);

        // When
        NotificationStatistics statistics = historyService.getStatistics(from);

        // Then
        assertThat(statistics.getTotalNotifications()).isEqualTo(1000L);
        assertThat(statistics.getSuccessNotifications()).isEqualTo(950L);
        assertThat(statistics.getFailureNotifications()).isEqualTo(50L);
        assertThat(statistics.getSuccessRate()).isEqualTo(0.95);
        assertThat(statistics.getFailureRate()).isEqualTo(0.05);
    }

    @Test
    @DisplayName("잘못된 회원 ID로 이력 조회 시 예외 발생")
    void shouldThrowExceptionForInvalidMemberId() {
        // Given
        Long invalidMemberId = null;

        // When & Then
        assertThatThrownBy(() -> historyService.getHistoryByMember(invalidMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid member ID");
    }

    @Test
    @DisplayName("빈 제목으로 이력 저장 시 예외 발생")
    void shouldThrowExceptionForEmptyTitle() {
        // Given
        Long memberId = 1L;
        String emptyTitle = "";
        String message = "테스트 메시지";
        NotificationChannelType channelType = NotificationChannelType.PUSH;

        // When & Then
        assertThatThrownBy(() -> historyService.recordSuccess(
                memberId, emptyTitle, message, channelType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title cannot be null or empty");
    }

    @Test
    @DisplayName("null 채널 타입으로 이력 저장 시 예외 발생")
    void shouldThrowExceptionForNullChannelType() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";
        NotificationChannelType nullChannelType = null;

        // When & Then
        assertThatThrownBy(() -> historyService.recordSuccess(
                memberId, title, message, nullChannelType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel type cannot be null");
    }

    /**
     * 테스트용 샘플 이력 생성
     */
    private NotificationHistory createSampleHistory(Long id, Long memberId, boolean success) {
        return NotificationHistory.builder()
                .id(id)
                .memberId(memberId)
                .title("테스트 제목")
                .message("테스트 메시지")
                .channelType(NotificationChannelType.PUSH)
                .success(success)
                .build();
    }
}