package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.EmotionPatternAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.KeywordAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.NoResponseAnalyzer;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AlertRuleService 테스트
 *
 * TDD Red 단계: Service 계층 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService 테스트")
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private EmotionPatternAnalyzer emotionAnalyzer;

    @Mock
    private NoResponseAnalyzer noResponseAnalyzer;

    @Mock
    private KeywordAnalyzer keywordAnalyzer;

    @InjectMocks
    private AlertRuleService alertRuleService;

    private MemberEntity testMember;
    private AlertRule testRule;
    private MessageEntity testMessage;

    @BeforeEach
    void setUp() {
        testMember = MemberEntity.builder()
                .id(1L)
                .memberName("테스트회원")
                .memberEmail("test@example.com")
                .memberPassword("password123")
                .build();

        testRule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);

        testMessage = MessageEntity.createUserMessage(
                null,
                "오늘 정말 우울해요",
                EmotionType.NEGATIVE
        );
    }

    @Test
    @DisplayName("이상징후 종합 감지 테스트 - TDD Red")
    void detectAnomalies_shouldDetectEmotionPattern() {
        // Given - TDD Red 단계이므로 UnsupportedOperationException 발생 예상

        // When & Then
        assertThatThrownBy(() -> alertRuleService.detectAnomalies(testMember.getId()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }

    @Test
    @DisplayName("실시간 키워드 감지 테스트 - TDD Red")
    void detectKeywordAlert_shouldDetectEmergencyKeywords() {
        // Given - TDD Red 단계이므로 UnsupportedOperationException 발생 예상

        // When & Then
        assertThatThrownBy(() ->
                alertRuleService.detectKeywordAlert(testMessage, testMember.getId()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }

    @Test
    @DisplayName("알림 발생 처리 테스트 - TDD Red")
    void triggerAlert_shouldCreateAlertHistory() {
        // Given
        AlertResult alertResult = AlertResult.createAlert(
                AlertLevel.HIGH, "3일 연속 부정감정 감지", null);

        // When & Then - TDD Red 단계
        assertThatThrownBy(() ->
                alertRuleService.triggerAlert(testMember.getId(), alertResult))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }

    @Test
    @DisplayName("보호자 알림 발송 테스트 - TDD Red")
    void sendGuardianNotification_shouldNotifyAllGuardians() {
        // Given
        Long memberId = testMember.getId();
        AlertLevel alertLevel = AlertLevel.EMERGENCY;
        String alertMessage = "긴급 상황이 감지되었습니다";

        // When & Then - TDD Red 단계
        assertThatThrownBy(() ->
                alertRuleService.sendGuardianNotification(memberId, alertLevel, alertMessage))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }

    @Test
    @DisplayName("알림 이력 기록 테스트 - TDD Red")
    void recordAlertHistory_shouldSaveCompleteHistory() {
        // Given
        AlertResult alertResult = AlertResult.createAlert(
                AlertLevel.HIGH, "감정 패턴 이상", null);

        // When & Then - TDD Red 단계
        assertThatThrownBy(() ->
                alertRuleService.recordAlertHistory(testRule, testMember, alertResult))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }

    @Test
    @DisplayName("회원의 활성 알림 규칙 조회 테스트")
    void getActiveRulesByMemberId_shouldReturnActiveRules() {
        // Given
        List<AlertRule> expectedRules = Arrays.asList(testRule);
        given(alertRuleRepository.findActiveRulesByMemberId(testMember.getId()))
                .willReturn(expectedRules);

        // When
        List<AlertRule> actualRules = alertRuleService.getActiveRulesByMemberId(testMember.getId());

        // Then
        assertThat(actualRules).isEqualTo(expectedRules);
        verify(alertRuleRepository).findActiveRulesByMemberId(testMember.getId());
    }

    @Test
    @DisplayName("최근 알림 이력 조회 테스트")
    void getRecentAlertHistory_shouldReturnRecentHistory() {
        // Given
        int days = 7;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(days);

        List<AlertHistory> expectedHistory = Arrays.asList(
                AlertHistory.createAlert(testRule, testMember, "테스트 알림", "{}")
        );

        given(alertHistoryRepository.findByMemberIdAndDateRange(
                eq(testMember.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(expectedHistory);

        // When
        List<AlertHistory> actualHistory = alertRuleService.getRecentAlertHistory(testMember.getId(), days);

        // Then
        assertThat(actualHistory).isEqualTo(expectedHistory);
        verify(alertHistoryRepository).findByMemberIdAndDateRange(
                eq(testMember.getId()), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("알림 규칙 생성 테스트 - TDD Red")
    void createAlertRule_shouldCreateNewRule() {
        // Given
        AlertType alertType = AlertType.EMOTION_PATTERN;
        AlertLevel alertLevel = AlertLevel.HIGH;
        AlertCondition condition = AlertCondition.createEmotionCondition(3);

        // When & Then - TDD Red 단계
        assertThatThrownBy(() ->
                alertRuleService.createAlertRule(testMember, alertType, alertLevel, condition))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }

    @Test
    @DisplayName("알림 규칙 활성화/비활성화 테스트 - TDD Red")
    void toggleAlertRule_shouldToggleRuleStatus() {
        // Given
        Long alertRuleId = 1L;
        boolean active = false;

        // When & Then - TDD Red 단계
        assertThatThrownBy(() ->
                alertRuleService.toggleAlertRule(alertRuleId, active))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("TDD Red 단계: 구현 예정");
    }
}