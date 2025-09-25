package com.anyang.maruni.domain.notification.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * NotificationStatistics 단위 테스트
 *
 * 알림 통계 계산 로직의 정확성을 검증합니다.
 */
@DisplayName("NotificationStatistics 테스트")
class NotificationStatisticsTest {

    @Test
    @DisplayName("정상적인 통계 생성 - 성공/실패 비율 계산")
    void shouldCreateStatisticsWithCorrectRates() {
        // Given
        long total = 1000L;
        long success = 850L;
        long failure = 150L;

        // When
        NotificationStatistics statistics = NotificationStatistics.of(total, success, failure);

        // Then
        assertThat(statistics.getTotalNotifications()).isEqualTo(total);
        assertThat(statistics.getSuccessNotifications()).isEqualTo(success);
        assertThat(statistics.getFailureNotifications()).isEqualTo(failure);
        assertThat(statistics.getSuccessRate()).isEqualTo(0.85);
        assertThat(statistics.getFailureRate()).isEqualTo(0.15);
        assertThat(statistics.getSuccessPercentage()).isEqualTo(85.0);
        assertThat(statistics.getFailurePercentage()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("전체 알림이 0건인 경우 - 성공률과 실패율 모두 0")
    void shouldHandleZeroTotalNotifications() {
        // Given
        long total = 0L;
        long success = 0L;
        long failure = 0L;

        // When
        NotificationStatistics statistics = NotificationStatistics.of(total, success, failure);

        // Then
        assertThat(statistics.getTotalNotifications()).isEqualTo(0L);
        assertThat(statistics.getSuccessNotifications()).isEqualTo(0L);
        assertThat(statistics.getFailureNotifications()).isEqualTo(0L);
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        assertThat(statistics.getFailureRate()).isEqualTo(0.0);
        assertThat(statistics.getSuccessPercentage()).isEqualTo(0.0);
        assertThat(statistics.getFailurePercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("모든 알림이 성공한 경우 - 성공률 100%")
    void shouldHandleAllSuccessNotifications() {
        // Given
        long total = 500L;
        long success = 500L;
        long failure = 0L;

        // When
        NotificationStatistics statistics = NotificationStatistics.of(total, success, failure);

        // Then
        assertThat(statistics.getSuccessRate()).isEqualTo(1.0);
        assertThat(statistics.getFailureRate()).isEqualTo(0.0);
        assertThat(statistics.getSuccessPercentage()).isEqualTo(100.0);
        assertThat(statistics.getFailurePercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("모든 알림이 실패한 경우 - 실패율 100%")
    void shouldHandleAllFailureNotifications() {
        // Given
        long total = 100L;
        long success = 0L;
        long failure = 100L;

        // When
        NotificationStatistics statistics = NotificationStatistics.of(total, success, failure);

        // Then
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        assertThat(statistics.getFailureRate()).isEqualTo(1.0);
        assertThat(statistics.getSuccessPercentage()).isEqualTo(0.0);
        assertThat(statistics.getFailurePercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("통계 요약 문자열 생성")
    void shouldGenerateCorrectSummaryString() {
        // Given
        long total = 1000L;
        long success = 850L;
        long failure = 150L;

        // When
        NotificationStatistics statistics = NotificationStatistics.of(total, success, failure);
        String summary = statistics.getSummary();

        // Then
        String expectedSummary = "전체: 1000건, 성공: 850건(85.0%), 실패: 150건(15.0%)";
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    @DisplayName("소수점 포함 통계 계산")
    void shouldHandleDecimalRates() {
        // Given
        long total = 300L;
        long success = 199L;  // 66.333...%
        long failure = 101L;  // 33.666...%

        // When
        NotificationStatistics statistics = NotificationStatistics.of(total, success, failure);

        // Then
        assertThat(statistics.getSuccessRate()).isCloseTo(0.6633, within(0.01));
        assertThat(statistics.getFailureRate()).isCloseTo(0.3367, within(0.01));
        assertThat(statistics.getSuccessPercentage()).isCloseTo(66.33, within(1.0));
        assertThat(statistics.getFailurePercentage()).isCloseTo(33.67, within(1.0));
    }

    @Test
    @DisplayName("Builder 패턴으로 직접 생성")
    void shouldCreateWithBuilderPattern() {
        // Given & When
        NotificationStatistics statistics = NotificationStatistics.builder()
                .totalNotifications(200L)
                .successNotifications(180L)
                .failureNotifications(20L)
                .successRate(0.9)
                .failureRate(0.1)
                .build();

        // Then
        assertThat(statistics.getTotalNotifications()).isEqualTo(200L);
        assertThat(statistics.getSuccessNotifications()).isEqualTo(180L);
        assertThat(statistics.getFailureNotifications()).isEqualTo(20L);
        assertThat(statistics.getSuccessRate()).isEqualTo(0.9);
        assertThat(statistics.getFailureRate()).isEqualTo(0.1);
    }
}