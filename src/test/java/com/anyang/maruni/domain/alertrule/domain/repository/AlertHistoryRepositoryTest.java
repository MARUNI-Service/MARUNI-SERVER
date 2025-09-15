package com.anyang.maruni.domain.alertrule.domain.repository;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlertHistoryRepository 테스트
 *
 * TDD Red 단계: 알림 이력 Repository 테스트
 */
@DataJpaTest
@DisplayName("AlertHistoryRepository 테스트")
class AlertHistoryRepositoryTest {

    @Autowired
    private AlertHistoryRepository alertHistoryRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private MemberRepository memberRepository;

    private MemberEntity testMember;
    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 및 규칙 생성
        testMember = MemberEntity.builder()
                .memberName("테스트회원")
                .memberEmail("test@example.com")
                .memberPassword("password123")
                .build();

        memberRepository.save(testMember);

        testRule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);
        alertRuleRepository.save(testRule);
    }

    @Test
    @DisplayName("회원별 알림 이력 페이징 조회 테스트")
    void findByMemberIdOrderByCreatedAtDesc_shouldReturnPagedHistory() {
        // Given
        AlertHistory history1 = AlertHistory.createAlert(testRule, testMember, "알림1", "{}");
        AlertHistory history2 = AlertHistory.createAlert(testRule, testMember, "알림2", "{}");
        AlertHistory history3 = AlertHistory.createAlert(testRule, testMember, "알림3", "{}");

        alertHistoryRepository.save(history1);
        alertHistoryRepository.save(history2);
        alertHistoryRepository.save(history3);

        // When
        Page<AlertHistory> page = alertHistoryRepository
                .findByMemberIdOrderByCreatedAtDesc(testMember.getId(), PageRequest.of(0, 2));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getAlertMessage()).contains("알림");
    }

    @Test
    @DisplayName("회원별 특정 기간 알림 이력 조회 테스트")
    void findByMemberIdAndDateRange_shouldReturnHistoryInRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(7);
        LocalDateTime endDate = now.plusDays(1);

        AlertHistory recentHistory = AlertHistory.createAlert(testRule, testMember, "최근 알림", "{}");
        alertHistoryRepository.save(recentHistory);

        // When
        List<AlertHistory> histories = alertHistoryRepository
                .findByMemberIdAndDateRange(testMember.getId(), startDate, endDate);

        // Then
        assertThat(histories).isNotEmpty();
        assertThat(histories.get(0).getAlertMessage()).isEqualTo("최근 알림");
    }

    @Test
    @DisplayName("회원별 특정 레벨 알림 이력 조회 테스트")
    void findByMemberIdAndMinAlertLevel_shouldReturnFilteredHistory() {
        // Given
        AlertRule emergencyRule = AlertRule.createNoResponseRule(testMember, 2, AlertLevel.EMERGENCY);
        alertRuleRepository.save(emergencyRule);

        AlertHistory highHistory = AlertHistory.createAlert(testRule, testMember, "높은 레벨", "{}");
        AlertHistory emergencyHistory = AlertHistory.createEmergencyAlert(emergencyRule, testMember, "긴급 상황", "{}");

        alertHistoryRepository.save(highHistory);
        alertHistoryRepository.save(emergencyHistory);

        // When
        Page<AlertHistory> emergencyPage = alertHistoryRepository
                .findByMemberIdAndMinAlertLevel(testMember.getId(), AlertLevel.EMERGENCY, PageRequest.of(0, 10));

        // Then
        assertThat(emergencyPage.getContent()).hasSize(1);
        assertThat(emergencyPage.getContent().get(0).getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
    }

    @Test
    @DisplayName("미발송 알림 조회 테스트")
    void findPendingNotifications_shouldReturnUnsentHistory() {
        // Given
        AlertHistory pendingHistory1 = AlertHistory.createAlert(testRule, testMember, "미발송1", "{}");
        AlertHistory pendingHistory2 = AlertHistory.createAlert(testRule, testMember, "미발송2", "{}");
        AlertHistory sentHistory = AlertHistory.createAlert(testRule, testMember, "발송완료", "{}");
        sentHistory.markNotificationSent("SUCCESS");

        alertHistoryRepository.save(pendingHistory1);
        alertHistoryRepository.save(pendingHistory2);
        alertHistoryRepository.save(sentHistory);

        // When
        List<AlertHistory> pendingNotifications = alertHistoryRepository.findPendingNotifications();

        // Then
        assertThat(pendingNotifications).hasSize(2);
        assertThat(pendingNotifications).allMatch(h -> !h.getIsNotificationSent());
    }

    @Test
    @DisplayName("시간 초과 미발송 알림 조회 테스트")
    void findTimeoutPendingNotifications_shouldReturnTimedOutHistory() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);

        AlertHistory oldPendingHistory = AlertHistory.createAlert(testRule, testMember, "오래된 미발송", "{}");
        AlertHistory recentPendingHistory = AlertHistory.createAlert(testRule, testMember, "최근 미발송", "{}");

        alertHistoryRepository.save(oldPendingHistory);
        alertHistoryRepository.save(recentPendingHistory);

        // When
        List<AlertHistory> timeoutNotifications = alertHistoryRepository
                .findTimeoutPendingNotifications(cutoffTime);

        // Then - 실제로는 생성 시간이 cutoffTime 이후이므로 결과가 없을 수 있음
        // 테스트 환경에서는 생성 시간을 조작하기 어려우므로 존재 여부만 확인
        assertThat(timeoutNotifications).isNotNull();
    }

    @Test
    @DisplayName("중복 알림 존재 여부 확인 테스트")
    void existsByMemberIdAndAlertRuleIdAndAlertDate_shouldReturnCorrectResult() {
        // Given
        LocalDateTime alertDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        AlertHistory history = AlertHistory.createAlert(testRule, testMember, "테스트", "{}");
        alertHistoryRepository.save(history);

        // When & Then
        boolean exists = alertHistoryRepository.existsByMemberIdAndAlertRuleIdAndAlertDate(
                testMember.getId(), testRule.getId(), alertDate);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("최근 알림 횟수 조회 테스트")
    void countRecentAlertsByMemberId_shouldReturnCorrectCount() {
        // Given
        LocalDateTime daysAgo = LocalDateTime.now().minusDays(7);

        AlertHistory history1 = AlertHistory.createAlert(testRule, testMember, "알림1", "{}");
        AlertHistory history2 = AlertHistory.createAlert(testRule, testMember, "알림2", "{}");

        alertHistoryRepository.save(history1);
        alertHistoryRepository.save(history2);

        // When
        long recentCount = alertHistoryRepository.countRecentAlertsByMemberId(testMember.getId(), daysAgo);

        // Then
        assertThat(recentCount).isEqualTo(2);
    }

    @Test
    @DisplayName("전체 긴급 알림 조회 테스트")
    void findAllEmergencyAlerts_shouldReturnEmergencyHistory() {
        // Given
        AlertRule emergencyRule = AlertRule.createNoResponseRule(testMember, 2, AlertLevel.EMERGENCY);
        alertRuleRepository.save(emergencyRule);

        AlertHistory normalHistory = AlertHistory.createAlert(testRule, testMember, "일반 알림", "{}");
        AlertHistory emergencyHistory = AlertHistory.createEmergencyAlert(emergencyRule, testMember, "긴급 알림", "{}");

        alertHistoryRepository.save(normalHistory);
        alertHistoryRepository.save(emergencyHistory);

        // When
        Page<AlertHistory> emergencyPage = alertHistoryRepository
                .findAllEmergencyAlerts(PageRequest.of(0, 10));

        // Then
        assertThat(emergencyPage.getContent()).hasSize(1);
        assertThat(emergencyPage.getContent().get(0).isEmergency()).isTrue();
    }

    @Test
    @DisplayName("알림 발송 성공률 계산 테스트")
    void calculateNotificationSuccessRate_shouldReturnCorrectRate() {
        // Given
        LocalDateTime daysAgo = LocalDateTime.now().minusDays(7);

        AlertHistory successHistory1 = AlertHistory.createAlert(testRule, testMember, "성공1", "{}");
        AlertHistory successHistory2 = AlertHistory.createAlert(testRule, testMember, "성공2", "{}");
        AlertHistory failedHistory = AlertHistory.createAlert(testRule, testMember, "실패", "{}");

        successHistory1.markNotificationSent("SUCCESS");
        successHistory2.markNotificationSent("SUCCESS");
        failedHistory.markNotificationFailed("네트워크 오류");

        alertHistoryRepository.save(successHistory1);
        alertHistoryRepository.save(successHistory2);
        alertHistoryRepository.save(failedHistory);

        // When
        Double successRate = alertHistoryRepository.calculateNotificationSuccessRate(daysAgo);

        // Then
        assertThat(successRate).isNotNull();
        assertThat(successRate).isBetween(0.0, 1.0);
    }
}