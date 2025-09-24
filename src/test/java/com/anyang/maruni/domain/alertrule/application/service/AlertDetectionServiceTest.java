package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AnalysisContext;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertDetectionService;
import com.anyang.maruni.domain.alertrule.application.service.orchestrator.AlertAnalysisOrchestrator;
import com.anyang.maruni.domain.alertrule.application.service.util.AlertServiceUtils;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AlertDetectionService 테스트
 *
 * 이상징후 감지 로직 전담 서비스의 독립적인 테스트
 * - 종합 이상징후 감지
 * - 키워드 감지
 * - 활성 규칙 조회
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertDetectionService 테스트")
class AlertDetectionServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertAnalysisOrchestrator analysisOrchestrator;

    @Mock
    private AlertConfigurationProperties alertConfig;

    @Mock
    private AlertServiceUtils alertServiceUtils;

    @InjectMocks
    private AlertDetectionService alertDetectionService;

    private MemberEntity testMember;
    private AlertRule testRule;
    private MessageEntity testMessage;
    private AlertResult testAlertResult;
    private AlertConfigurationProperties.Analysis analysisConfig;

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
                AlertLevel.HIGH, "3일 연속 부정감정 감지", null);

        // AlertConfigurationProperties.AnalysisConfig Mock 설정
        analysisConfig = new AlertConfigurationProperties.Analysis();
        analysisConfig.setDefaultDays(7);
    }

    @Test
    @DisplayName("종합 이상징후 감지 - 알림 발생")
    void detectAnomalies_AlertGenerated() {
        // Given
        Long memberId = 1L;
        List<AlertRule> activeRules = Arrays.asList(testRule);

        given(alertServiceUtils.validateAndGetMember(memberId))
                .willReturn(testMember);
        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(activeRules);
        given(alertConfig.getAnalysis())
                .willReturn(analysisConfig);
        given(analysisOrchestrator.isSupported(AlertType.EMOTION_PATTERN))
                .willReturn(true);
        given(analysisOrchestrator.analyzeByType(eq(AlertType.EMOTION_PATTERN), eq(testMember), any(AnalysisContext.class)))
                .willReturn(testAlertResult);

        // When
        List<AlertResult> results = alertDetectionService.detectAnomalies(memberId);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isAlert()).isTrue();
        assertThat(results.get(0).getAlertLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(results.get(0).getMessage()).isEqualTo("3일 연속 부정감정 감지");

        verify(alertServiceUtils).validateAndGetMember(memberId);
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
        verify(analysisOrchestrator).analyzeByType(eq(AlertType.EMOTION_PATTERN), eq(testMember), any(AnalysisContext.class));
    }

    @Test
    @DisplayName("종합 이상징후 감지 - 알림 미발생")
    void detectAnomalies_NoAlert() {
        // Given
        Long memberId = 1L;
        List<AlertRule> activeRules = Arrays.asList(testRule);

        given(alertServiceUtils.validateAndGetMember(memberId))
                .willReturn(testMember);
        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(activeRules);
        given(alertConfig.getAnalysis())
                .willReturn(analysisConfig);
        given(analysisOrchestrator.isSupported(AlertType.EMOTION_PATTERN))
                .willReturn(true);
        given(analysisOrchestrator.analyzeByType(eq(AlertType.EMOTION_PATTERN), eq(testMember), any(AnalysisContext.class)))
                .willReturn(AlertResult.noAlert());

        // When
        List<AlertResult> results = alertDetectionService.detectAnomalies(memberId);

        // Then
        assertThat(results).isEmpty();

        verify(alertServiceUtils).validateAndGetMember(memberId);
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
        verify(analysisOrchestrator).analyzeByType(eq(AlertType.EMOTION_PATTERN), eq(testMember), any(AnalysisContext.class));
    }

    @Test
    @DisplayName("키워드 감지 - 긴급 키워드 감지")
    void detectKeywordAlert_EmergencyKeywordDetected() {
        // Given
        Long memberId = 1L;
        AlertResult emergencyAlert = AlertResult.createAlert(
                AlertLevel.EMERGENCY, "긴급 키워드 감지: 자살", null);

        given(alertServiceUtils.validateAndGetMember(memberId))
                .willReturn(testMember);
        given(analysisOrchestrator.analyzeByType(eq(AlertType.KEYWORD_DETECTION), eq(testMember), any(AnalysisContext.class)))
                .willReturn(emergencyAlert);

        // When
        AlertResult result = alertDetectionService.detectKeywordAlert(testMessage, memberId);

        // Then
        assertThat(result.isAlert()).isTrue();
        assertThat(result.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(result.getMessage()).contains("긴급 키워드 감지");

        verify(alertServiceUtils).validateAndGetMember(memberId);
        verify(analysisOrchestrator).analyzeByType(eq(AlertType.KEYWORD_DETECTION), eq(testMember), any(AnalysisContext.class));
    }

    @Test
    @DisplayName("키워드 감지 - 키워드 미감지")
    void detectKeywordAlert_NoKeywordDetected() {
        // Given
        Long memberId = 1L;

        given(alertServiceUtils.validateAndGetMember(memberId))
                .willReturn(testMember);
        given(analysisOrchestrator.analyzeByType(eq(AlertType.KEYWORD_DETECTION), eq(testMember), any(AnalysisContext.class)))
                .willReturn(AlertResult.noAlert());

        // When
        AlertResult result = alertDetectionService.detectKeywordAlert(testMessage, memberId);

        // Then
        assertThat(result.isAlert()).isFalse();

        verify(alertServiceUtils).validateAndGetMember(memberId);
        verify(analysisOrchestrator).analyzeByType(eq(AlertType.KEYWORD_DETECTION), eq(testMember), any(AnalysisContext.class));
    }

    @Test
    @DisplayName("활성 규칙 조회 - 성공")
    void getActiveRulesByMemberId_Success() {
        // Given
        Long memberId = 1L;
        List<AlertRule> expectedRules = Arrays.asList(testRule);

        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(expectedRules);

        // When
        List<AlertRule> rules = alertDetectionService.getActiveRulesByMemberId(memberId);

        // Then
        assertThat(rules).isEqualTo(expectedRules);
        assertThat(rules).hasSize(1);
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
    }

    @Test
    @DisplayName("우선순위 정렬된 활성 규칙 조회 - 성공")
    void getActiveRulesByMemberIdOrderedByPriority_Success() {
        // Given
        Long memberId = 1L;
        AlertRule highPriorityRule = AlertRule.createKeywordRule(testMember, "자살", AlertLevel.EMERGENCY);
        AlertRule mediumPriorityRule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.MEDIUM);

        List<AlertRule> unsortedRules = Arrays.asList(mediumPriorityRule, highPriorityRule);

        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(unsortedRules);

        // When
        List<AlertRule> rules = alertDetectionService.getActiveRulesByMemberIdOrderedByPriority(memberId);

        // Then
        assertThat(rules).hasSize(2);
        // EMERGENCY가 MEDIUM보다 우선순위가 높으므로 첫 번째에 위치
        assertThat(rules.get(0).getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(rules.get(1).getAlertLevel()).isEqualTo(AlertLevel.MEDIUM);

        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
    }

    @Test
    @DisplayName("활성 규칙이 없는 경우 - 빈 목록 반환")
    void getActiveRulesByMemberId_EmptyList() {
        // Given
        Long memberId = 1L;

        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(Arrays.asList());

        // When
        List<AlertRule> rules = alertDetectionService.getActiveRulesByMemberId(memberId);

        // Then
        assertThat(rules).isEmpty();
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
    }

    @Test
    @DisplayName("키워드 감지 규칙 제외 - 종합 분석에서는 키워드 제외")
    void detectAnomalies_ExcludeKeywordRules() {
        // Given
        Long memberId = 1L;
        AlertRule keywordRule = AlertRule.createKeywordRule(testMember, "자살", AlertLevel.EMERGENCY);
        List<AlertRule> activeRules = Arrays.asList(keywordRule);

        given(alertServiceUtils.validateAndGetMember(memberId))
                .willReturn(testMember);
        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(activeRules);

        // When
        List<AlertResult> results = alertDetectionService.detectAnomalies(memberId);

        // Then
        // 키워드 규칙은 종합 분석에서 제외되므로 결과가 없어야 함
        assertThat(results).isEmpty();

        verify(alertServiceUtils).validateAndGetMember(memberId);
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
    }

    @Test
    @DisplayName("지원하지 않는 알림 타입 - noAlert 반환")
    void detectAnomalies_UnsupportedAlertType() {
        // Given
        Long memberId = 1L;
        List<AlertRule> activeRules = Arrays.asList(testRule);

        given(alertServiceUtils.validateAndGetMember(memberId))
                .willReturn(testMember);
        given(alertRuleRepository.findActiveRulesWithMemberAndGuardian(memberId))
                .willReturn(activeRules);
        given(analysisOrchestrator.isSupported(AlertType.EMOTION_PATTERN))
                .willReturn(false);

        // When
        List<AlertResult> results = alertDetectionService.detectAnomalies(memberId);

        // Then
        assertThat(results).isEmpty();

        verify(alertServiceUtils).validateAndGetMember(memberId);
        verify(alertRuleRepository).findActiveRulesWithMemberAndGuardian(memberId);
        verify(analysisOrchestrator).isSupported(AlertType.EMOTION_PATTERN);
    }
}