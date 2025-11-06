package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertDetectionService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertHistoryService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertNotificationService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertRuleManagementService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertRuleService;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AlertRuleService 테스트 (Facade 패턴)
 *
 * 리팩토링 후: 새로 분리된 4개 서비스를 Mock하여 위임 패턴 검증
 * ✅ 100% API 호환성 검증
 * ✅ 기존 테스트 로직 유지 + Mock 대상만 변경
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService 테스트 (Facade 패턴)")
class AlertRuleServiceTest {

    // 새로 분리된 서비스들을 Mock으로 주입
    @Mock
    private AlertDetectionService alertDetectionService;

    @Mock
    private AlertRuleManagementService alertRuleManagementService;

    @Mock
    private AlertNotificationService alertNotificationService;

    @Mock
    private AlertHistoryService alertHistoryService;

    @InjectMocks
    private AlertRuleService alertRuleService; // Facade 패턴

    private MemberEntity testMember;
    private AlertRule testRule;
    private MessageEntity testMessage;
    private AlertResult testAlertResult;
    private AlertHistory testAlertHistory;

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

        testAlertResult = AlertResult.createAlert(
                AlertLevel.HIGH, AlertType.EMOTION_PATTERN, "3일 연속 부정감정 감지", null);

        testAlertHistory = AlertHistory.createAlert(
                testRule, testMember, "테스트 알림", "{\"details\":\"test\"}");
    }

    // ========== 이상징후 감지 관련 테스트 (AlertDetectionService 위임) ==========

    @Test
    @DisplayName("이상징후 종합 감지 - 성공")
    void detectAnomalies_Success() {
        // Given
        Long memberId = 1L;
        List<AlertResult> expectedResults = Arrays.asList(testAlertResult);

        // AlertDetectionService의 응답을 Mock
        given(alertDetectionService.detectAnomalies(memberId))
                .willReturn(expectedResults);

        // When
        List<AlertResult> results = alertRuleService.detectAnomalies(memberId);

        // Then
        assertThat(results).isEqualTo(expectedResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isAlert()).isTrue();
        assertThat(results.get(0).getAlertLevel()).isEqualTo(AlertLevel.HIGH);
        verify(alertDetectionService).detectAnomalies(memberId);
    }

    @Test
    @DisplayName("키워드 감지 - 성공")
    void detectKeywordAlert_Success() {
        // Given
        Long memberId = 1L;
        AlertResult expectedResult = AlertResult.createAlert(
                AlertLevel.EMERGENCY, AlertType.KEYWORD_DETECTION, "긴급 키워드 감지", null);

        // AlertDetectionService의 응답을 Mock
        given(alertDetectionService.detectKeywordAlert(testMessage, memberId))
                .willReturn(expectedResult);

        // When
        AlertResult result = alertRuleService.detectKeywordAlert(testMessage, memberId);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isAlert()).isTrue();
        assertThat(result.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        verify(alertDetectionService).detectKeywordAlert(testMessage, memberId);
    }

    @Test
    @DisplayName("활성 알림 규칙 조회 - 성공")
    void getActiveRulesByMemberId_Success() {
        // Given
        Long memberId = 1L;
        List<AlertRule> expectedRules = Arrays.asList(testRule);

        // AlertDetectionService의 응답을 Mock
        given(alertDetectionService.getActiveRulesByMemberId(memberId))
                .willReturn(expectedRules);

        // When
        List<AlertRule> rules = alertRuleService.getActiveRulesByMemberId(memberId);

        // Then
        assertThat(rules).isEqualTo(expectedRules);
        assertThat(rules).hasSize(1);
        verify(alertDetectionService).getActiveRulesByMemberId(memberId);
    }

    @Test
    @DisplayName("우선순위 정렬된 활성 알림 규칙 조회 - 성공")
    void getActiveRulesByMemberIdOrderedByPriority_Success() {
        // Given
        Long memberId = 1L;
        List<AlertRule> expectedRules = Arrays.asList(testRule);

        // AlertDetectionService의 응답을 Mock
        given(alertDetectionService.getActiveRulesByMemberIdOrderedByPriority(memberId))
                .willReturn(expectedRules);

        // When
        List<AlertRule> rules = alertRuleService.getActiveRulesByMemberIdOrderedByPriority(memberId);

        // Then
        assertThat(rules).isEqualTo(expectedRules);
        assertThat(rules).hasSize(1);
        verify(alertDetectionService).getActiveRulesByMemberIdOrderedByPriority(memberId);
    }

    // ========== 알림 규칙 CRUD 관련 테스트 (AlertRuleManagementService 위임) ==========

    @Test
    @DisplayName("알림 규칙 생성 - 성공")
    void createAlertRule_Success() {
        // Given
        AlertCondition condition = AlertCondition.createEmotionCondition(3);

        // AlertRuleManagementService의 응답을 Mock
        given(alertRuleManagementService.createAlertRule(
                testMember, AlertType.EMOTION_PATTERN, AlertLevel.HIGH, condition))
                .willReturn(testRule);

        // When
        AlertRule result = alertRuleService.createAlertRule(
                testMember, AlertType.EMOTION_PATTERN, AlertLevel.HIGH, condition);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleManagementService).createAlertRule(
                testMember, AlertType.EMOTION_PATTERN, AlertLevel.HIGH, condition);
    }

    @Test
    @DisplayName("알림 규칙 조회 - 성공")
    void getAlertRuleById_Success() {
        // Given
        Long ruleId = 1L;

        // AlertRuleManagementService의 응답을 Mock
        given(alertRuleManagementService.getAlertRuleById(ruleId))
                .willReturn(testRule);

        // When
        AlertRule result = alertRuleService.getAlertRuleById(ruleId);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleManagementService).getAlertRuleById(ruleId);
    }

    @Test
    @DisplayName("알림 규칙 수정 - 성공")
    void updateAlertRule_Success() {
        // Given
        Long ruleId = 1L;
        String ruleName = "수정된 규칙";
        String ruleDescription = "수정된 설명";
        AlertLevel alertLevel = AlertLevel.MEDIUM;

        // AlertRuleManagementService의 응답을 Mock
        given(alertRuleManagementService.updateAlertRule(ruleId, ruleName, ruleDescription, alertLevel))
                .willReturn(testRule);

        // When
        AlertRule result = alertRuleService.updateAlertRule(ruleId, ruleName, ruleDescription, alertLevel);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleManagementService).updateAlertRule(ruleId, ruleName, ruleDescription, alertLevel);
    }

    @Test
    @DisplayName("알림 규칙 삭제 - 성공")
    void deleteAlertRule_Success() {
        // Given
        Long ruleId = 1L;

        // When
        alertRuleService.deleteAlertRule(ruleId);

        // Then
        verify(alertRuleManagementService).deleteAlertRule(ruleId);
    }

    @Test
    @DisplayName("알림 규칙 활성화/비활성화 - 성공")
    void toggleAlertRule_Success() {
        // Given
        Long ruleId = 1L;
        boolean active = false;

        // AlertRuleManagementService의 응답을 Mock
        given(alertRuleManagementService.toggleAlertRule(ruleId, active))
                .willReturn(testRule);

        // When
        AlertRule result = alertRuleService.toggleAlertRule(ruleId, active);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleManagementService).toggleAlertRule(ruleId, active);
    }

    // ========== 알림 발송 관련 테스트 (AlertNotificationService 위임) ==========

    @Test
    @DisplayName("알림 발생 처리 - 성공")
    void triggerAlert_Success() {
        // Given
        Long memberId = 1L;
        Long expectedHistoryId = 123L;

        // AlertNotificationService의 응답을 Mock
        given(alertNotificationService.triggerAlert(memberId, testAlertResult))
                .willReturn(expectedHistoryId);

        // When
        Long result = alertRuleService.triggerAlert(memberId, testAlertResult);

        // Then
        assertThat(result).isEqualTo(expectedHistoryId);
        verify(alertNotificationService).triggerAlert(memberId, testAlertResult);
    }

    @Test
    @DisplayName("보호자 알림 발송 - 성공")
    void sendGuardianNotification_Success() {
        // Given
        Long memberId = 1L;
        AlertLevel alertLevel = AlertLevel.HIGH;
        String alertMessage = "테스트 알림 메시지";

        // When
        alertRuleService.sendGuardianNotification(memberId, alertLevel, alertMessage);

        // Then
        verify(alertNotificationService).sendGuardianNotification(memberId, alertLevel, alertMessage);
    }

    // ========== 알림 이력 관련 테스트 (AlertHistoryService 위임) ==========

    @Test
    @DisplayName("알림 이력 기록 - 성공")
    void recordAlertHistory_Success() {
        // Given
        // AlertHistoryService의 응답을 Mock
        given(alertHistoryService.recordAlertHistory(testRule, testMember, testAlertResult))
                .willReturn(testAlertHistory);

        // When
        AlertHistory result = alertRuleService.recordAlertHistory(testRule, testMember, testAlertResult);

        // Then
        assertThat(result).isEqualTo(testAlertHistory);
        verify(alertHistoryService).recordAlertHistory(testRule, testMember, testAlertResult);
    }

    @Test
    @DisplayName("최근 알림 이력 조회 - 성공")
    void getRecentAlertHistory_Success() {
        // Given
        Long memberId = 1L;
        int days = 30;
        List<AlertHistory> expectedHistory = Arrays.asList(testAlertHistory);

        // AlertHistoryService의 응답을 Mock
        given(alertHistoryService.getRecentAlertHistory(memberId, days))
                .willReturn(expectedHistory);

        // When
        List<AlertHistory> result = alertRuleService.getRecentAlertHistory(memberId, days);

        // Then
        assertThat(result).isEqualTo(expectedHistory);
        assertThat(result).hasSize(1);
        verify(alertHistoryService).getRecentAlertHistory(memberId, days);
    }
}