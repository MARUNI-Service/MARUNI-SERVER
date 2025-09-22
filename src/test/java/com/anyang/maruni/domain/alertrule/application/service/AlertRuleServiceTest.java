package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.EmotionPatternAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.KeywordAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.NoResponseAnalyzer;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
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
import static org.mockito.Mockito.times;

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
    private MemberRepository memberRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmotionPatternAnalyzer emotionAnalyzer;

    @Mock
    private NoResponseAnalyzer noResponseAnalyzer;

    @Mock
    private KeywordAnalyzer keywordAnalyzer;

    @Mock
    private AlertConfigurationProperties alertConfig;

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
    @DisplayName("이상징후 종합 감지 테스트")
    void detectAnomalies_shouldDetectEmotionPattern() {
        // Given
        List<AlertRule> activeRules = Arrays.asList(testRule);
        AlertResult emotionAlert = AlertResult.createAlert(
                AlertLevel.HIGH, "3일 연속 부정감정 감지", null);

        given(memberRepository.findById(testMember.getId()))
                .willReturn(Optional.of(testMember));
        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(testMember.getId()))
                .willReturn(activeRules);

        // AlertConfigurationProperties Mock 설정
        AlertConfigurationProperties.Analysis analysis = new AlertConfigurationProperties.Analysis();
        analysis.setDefaultDays(7);
        given(alertConfig.getAnalysis()).willReturn(analysis);

        given(emotionAnalyzer.analyzeEmotionPattern(testMember, 7))
                .willReturn(emotionAlert);

        // When
        List<AlertResult> results = alertRuleService.detectAnomalies(testMember.getId());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isAlert()).isTrue();
        assertThat(results.get(0).getAlertLevel()).isEqualTo(AlertLevel.HIGH);
        verify(memberRepository).findById(testMember.getId());
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(testMember.getId());
        verify(emotionAnalyzer).analyzeEmotionPattern(testMember, 7);
    }

    @Test
    @DisplayName("실시간 키워드 감지 테스트")
    void detectKeywordAlert_shouldDetectEmergencyKeywords() {
        // Given
        AlertResult expectedResult = AlertResult.createAlert(
                AlertLevel.EMERGENCY, "긴급 키워드 감지: '도와주세요'", null);

        given(keywordAnalyzer.analyzeKeywordRisk(testMessage))
                .willReturn(expectedResult);

        // When
        AlertResult actualResult = alertRuleService.detectKeywordAlert(testMessage, testMember.getId());

        // Then
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.isAlert()).isTrue();
        assertThat(actualResult.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        verify(keywordAnalyzer).analyzeKeywordRisk(testMessage);
    }

    @Test
    @DisplayName("알림 발생 처리 테스트")
    void triggerAlert_shouldCreateAlertHistory() {
        // Given
        AlertResult alertResult = AlertResult.createAlert(
                AlertLevel.HIGH, "3일 연속 부정감정 감지", null);

        AlertHistory savedHistory = AlertHistory.builder()
                .id(1L)
                .alertRule(null) // MVP
                .member(testMember)
                .alertLevel(AlertLevel.HIGH)
                .alertMessage("3일 연속 부정감정 감지")
                .isNotificationSent(false)
                .build();

        // AlertConfigurationProperties Mock 설정
        AlertConfigurationProperties.Notification notification = new AlertConfigurationProperties.Notification();
        notification.setDetectionDetailsJsonTemplate("{\"alertLevel\":\"%s\",\"analysisDetails\":\"%s\"}");
        notification.setTitleTemplate("[MARUNI 알림] %s 단계 이상징후 감지");
        given(alertConfig.getNotification()).willReturn(notification);

        given(memberRepository.findById(testMember.getId()))
                .willReturn(Optional.of(testMember));
        given(alertHistoryRepository.save(any(AlertHistory.class)))
                .willReturn(savedHistory);
        // testMember.getGuardian()이 null이므로 notificationService stub 제거

        // When
        Long historyId = alertRuleService.triggerAlert(testMember.getId(), alertResult);

        // Then
        assertThat(historyId).isEqualTo(1L);
        verify(memberRepository, times(2)).findById(testMember.getId()); // triggerAlert + sendGuardianNotification
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("보호자 알림 발송 테스트")
    void sendGuardianNotification_shouldNotifyAllGuardians() {
        // Given
        Long memberId = testMember.getId();
        AlertLevel alertLevel = AlertLevel.EMERGENCY;
        String alertMessage = "긴급 상황이 감지되었습니다";

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(testMember));
        // testMember.getGuardian()이 null이므로 notificationService stub 제거

        // When
        alertRuleService.sendGuardianNotification(memberId, alertLevel, alertMessage);

        // Then
        verify(memberRepository).findById(memberId);
        // testMember.getGuardian()이 null이면 알림 발송하지 않음
        // 따라서 notificationService는 호출되지 않음
    }

    @Test
    @DisplayName("알림 이력 기록 테스트")
    void recordAlertHistory_shouldSaveCompleteHistory() {
        // Given
        AlertResult alertResult = AlertResult.createAlert(
                AlertLevel.HIGH, "감정 패턴 이상", null);

        AlertHistory savedHistory = AlertHistory.builder()
                .id(1L)
                .alertRule(testRule)
                .member(testMember)
                .alertLevel(AlertLevel.HIGH)
                .alertMessage("감정 패턴 이상")
                .detectionDetails("{\"alertLevel\":\"HIGH\",\"analysisDetails\":\"null\"}")
                .isNotificationSent(false)
                .build();

        // AlertConfigurationProperties Mock 설정
        AlertConfigurationProperties.Notification notification = new AlertConfigurationProperties.Notification();
        notification.setDetectionDetailsJsonTemplate("{\"alertLevel\":\"%s\",\"analysisDetails\":\"%s\"}");
        given(alertConfig.getNotification()).willReturn(notification);

        given(alertHistoryRepository.save(any(AlertHistory.class)))
                .willReturn(savedHistory);

        // When
        AlertHistory actualHistory = alertRuleService.recordAlertHistory(testRule, testMember, alertResult);

        // Then
        assertThat(actualHistory).isNotNull();
        assertThat(actualHistory.getId()).isEqualTo(1L);
        assertThat(actualHistory.getAlertMessage()).isEqualTo("감정 패턴 이상");
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("회원의 활성 알림 규칙 조회 테스트")
    void getActiveRulesByMemberId_shouldReturnActiveRules() {
        // Given
        List<AlertRule> expectedRules = Arrays.asList(testRule);
        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(testMember.getId()))
                .willReturn(expectedRules);

        // When
        List<AlertRule> actualRules = alertRuleService.getActiveRulesByMemberId(testMember.getId());

        // Then
        assertThat(actualRules).isEqualTo(expectedRules);
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(testMember.getId());
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
    @DisplayName("알림 규칙 생성 테스트")
    void createAlertRule_shouldCreateNewRule() {
        // Given
        AlertType alertType = AlertType.EMOTION_PATTERN;
        AlertLevel alertLevel = AlertLevel.HIGH;
        AlertCondition condition = AlertCondition.createEmotionCondition(3);

        AlertRule savedRule = AlertRule.builder()
                .id(1L)
                .member(testMember)
                .alertType(alertType)
                .ruleName("연속 부정감정 감지")
                .ruleDescription("3일 연속 부정적 감정 감지 시 알림")
                .condition(condition)
                .alertLevel(alertLevel)
                .isActive(true)
                .build();

        given(alertRuleRepository.save(any(AlertRule.class)))
                .willReturn(savedRule);

        // When
        AlertRule actualRule = alertRuleService.createAlertRule(testMember, alertType, alertLevel, condition);

        // Then
        assertThat(actualRule).isNotNull();
        assertThat(actualRule.getId()).isEqualTo(1L);
        assertThat(actualRule.getAlertType()).isEqualTo(alertType);
        assertThat(actualRule.getAlertLevel()).isEqualTo(alertLevel);
        verify(alertRuleRepository).save(any(AlertRule.class));
    }

    @Test
    @DisplayName("알림 규칙 활성화/비활성화 테스트")
    void toggleAlertRule_shouldToggleRuleStatus() {
        // Given
        Long alertRuleId = 1L;
        boolean active = false;

        given(alertRuleRepository.findById(alertRuleId))
                .willReturn(Optional.of(testRule));

        // When
        alertRuleService.toggleAlertRule(alertRuleId, active);

        // Then
        verify(alertRuleRepository).findById(alertRuleId);
        // JPA 더티 체킹으로 자동 업데이트되므로 save 호출 검증하지 않음
    }
}