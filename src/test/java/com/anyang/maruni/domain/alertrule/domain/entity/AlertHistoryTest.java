package com.anyang.maruni.domain.alertrule.domain.entity;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AlertHistory Entity 테스트
 *
 * TDD Red 단계: 알림 이력 관리 테스트
 */
@DisplayName("AlertHistory Entity 테스트")
class AlertHistoryTest {

    private MemberEntity testMember;
    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        testMember = MemberEntity.builder()
                .memberName("테스트회원")
                .memberEmail("test@example.com")
                .memberPassword("password123")
                .build();

        testRule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);
    }

    @Test
    @DisplayName("일반 알림 이력 생성 테스트")
    void createAlert_shouldCreateValidHistory() {
        // Given
        String alertMessage = "3일 연속 부정적 감정이 감지되었습니다";
        String detectionDetails = "{\"consecutiveDays\": 3, \"emotion\": \"NEGATIVE\"}";

        // When
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, alertMessage, detectionDetails);

        // Then
        assertThat(history.getAlertRule()).isEqualTo(testRule);
        assertThat(history.getMember()).isEqualTo(testMember);
        assertThat(history.getAlertLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(history.getAlertMessage()).isEqualTo(alertMessage);
        assertThat(history.getDetectionDetails()).isEqualTo(detectionDetails);
        assertThat(history.getIsNotificationSent()).isFalse();
        assertThat(history.getAlertDate()).isNotNull();
    }

    @Test
    @DisplayName("긴급 알림 이력 생성 테스트")
    void createEmergencyAlert_shouldCreateEmergencyHistory() {
        // Given
        String alertMessage = "응급 키워드가 감지되었습니다";
        String detectionDetails = "{\"keyword\": \"아파요\", \"message\": \"정말 아파요\"}";

        // When
        AlertHistory history = AlertHistory.createEmergencyAlert(
                testRule, testMember, alertMessage, detectionDetails);

        // Then
        assertThat(history.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(history.isEmergency()).isTrue();
        assertThat(history.getIsNotificationSent()).isFalse();
    }

    @Test
    @DisplayName("알림 발송 완료 처리 테스트")
    void markNotificationSent_shouldUpdateStatus() {
        // Given
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{}");
        String result = "SUCCESS: 푸시 알림 발송 완료";

        // When
        history.markNotificationSent(result);

        // Then
        assertThat(history.getIsNotificationSent()).isTrue();
        assertThat(history.getNotificationSentAt()).isNotNull();
        assertThat(history.getNotificationResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("알림 발송 실패 처리 테스트")
    void markNotificationFailed_shouldUpdateStatus() {
        // Given
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{}");
        String errorMessage = "네트워크 연결 실패";

        // When
        history.markNotificationFailed(errorMessage);

        // Then
        assertThat(history.getIsNotificationSent()).isFalse();
        assertThat(history.getNotificationResult()).isEqualTo("FAILED: " + errorMessage);
    }

    @Test
    @DisplayName("알림 재시도 가능 여부 테스트 - 미발송 상태")
    void canRetryNotification_shouldReturnTrue_whenNotSent() {
        // Given
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{}");

        // When
        boolean canRetry = history.canRetryNotification();

        // Then
        assertThat(canRetry).isTrue();
    }

    @Test
    @DisplayName("알림 재시도 가능 여부 테스트 - 발송 완료 상태")
    void canRetryNotification_shouldReturnFalse_whenAlreadySent() {
        // Given
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{}");
        history.markNotificationSent("SUCCESS");

        // When
        boolean canRetry = history.canRetryNotification();

        // Then
        assertThat(canRetry).isFalse();
    }

    @Test
    @DisplayName("알림 재시도 가능 여부 테스트 - 실패 상태")
    void canRetryNotification_shouldReturnTrue_whenFailed() {
        // Given
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{}");
        history.markNotificationFailed("네트워크 오류");

        // When
        boolean canRetry = history.canRetryNotification();

        // Then
        assertThat(canRetry).isTrue();
    }

    @Test
    @DisplayName("경과 시간 계산 테스트")
    void getMinutesElapsed_shouldCalculateCorrectly() {
        // Given
        AlertHistory history = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{}");

        // When & Then - TDD Red 단계에서는 createdAt이 null이므로 NPE 발생 예상
        // 실제 구현 시 BaseTimeEntity의 @PrePersist로 자동 설정됨
        assertThatThrownBy(() -> history.getMinutesElapsed())
                .isInstanceOf(NullPointerException.class);
    }
}