package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.service.core.AlertRuleManagementService;
import com.anyang.maruni.domain.alertrule.domain.entity.*;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertRuleRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.alertrule.domain.exception.AlertRuleNotFoundException;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AlertRuleManagementService 테스트
 *
 * 알림 규칙 CRUD 관리 전담 서비스의 독립적인 테스트
 * - 규칙 생성
 * - 규칙 조회 (단일)
 * - 규칙 수정
 * - 규칙 삭제
 * - 규칙 활성화/비활성화
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleManagementService 테스트")
class AlertRuleManagementServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AlertRuleManagementService alertRuleManagementService;

    private MemberEntity testMember;
    private AlertRule testRule;
    private AlertCondition testCondition;

    @BeforeEach
    void setUp() {
        testMember = MemberEntity.builder()
                .id(1L)
                .memberName("테스트회원")
                .memberEmail("test@example.com")
                .memberPassword("password123")
                .build();

        testCondition = AlertCondition.createEmotionCondition(3);
        testRule = AlertRule.createEmotionPatternRule(testMember, 3, AlertLevel.HIGH);
    }

    @Test
    @DisplayName("알림 규칙 생성 - 성공")
    void createAlertRule_Success() {
        // Given
        AlertType alertType = AlertType.EMOTION_PATTERN;
        AlertLevel alertLevel = AlertLevel.HIGH;

        given(alertRuleRepository.save(any(AlertRule.class)))
                .willReturn(testRule);

        // When
        AlertRule result = alertRuleManagementService.createAlertRule(
                testMember, alertType, alertLevel, testCondition);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleRepository).save(any(AlertRule.class));
    }

    @Test
    @DisplayName("알림 규칙 조회 - 성공")
    void getAlertRuleById_Success() {
        // Given
        Long ruleId = 1L;

        given(alertRuleRepository.findByIdWithMemberAndGuardian(ruleId))
                .willReturn(Optional.of(testRule));

        // When
        AlertRule result = alertRuleManagementService.getAlertRuleById(ruleId);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleRepository).findByIdWithMemberAndGuardian(ruleId);
    }

    @Test
    @DisplayName("알림 규칙 조회 - 존재하지 않는 규칙")
    void getAlertRuleById_NotFound() {
        // Given
        Long ruleId = 999L;

        given(alertRuleRepository.findByIdWithMemberAndGuardian(ruleId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> alertRuleManagementService.getAlertRuleById(ruleId))
                .isInstanceOf(AlertRuleNotFoundException.class);

        verify(alertRuleRepository).findByIdWithMemberAndGuardian(ruleId);
    }

    @Test
    @DisplayName("알림 규칙 수정 - 성공")
    void updateAlertRule_Success() {
        // Given
        Long ruleId = 1L;
        String newRuleName = "수정된 규칙";
        String newRuleDescription = "수정된 설명";
        AlertLevel newAlertLevel = AlertLevel.MEDIUM;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.of(testRule));

        // When
        AlertRule result = alertRuleManagementService.updateAlertRule(
                ruleId, newRuleName, newRuleDescription, newAlertLevel);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleRepository).findById(ruleId);
        // JPA 더티 체킹으로 자동 업데이트되므로 save 호출 없음
    }

    @Test
    @DisplayName("알림 규칙 수정 - 존재하지 않는 규칙")
    void updateAlertRule_NotFound() {
        // Given
        Long ruleId = 999L;
        String ruleName = "규칙명";
        String ruleDescription = "설명";
        AlertLevel alertLevel = AlertLevel.HIGH;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> alertRuleManagementService.updateAlertRule(
                ruleId, ruleName, ruleDescription, alertLevel))
                .isInstanceOf(AlertRuleNotFoundException.class);

        verify(alertRuleRepository).findById(ruleId);
    }

    @Test
    @DisplayName("알림 규칙 삭제 - 성공")
    void deleteAlertRule_Success() {
        // Given
        Long ruleId = 1L;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.of(testRule));

        // When
        alertRuleManagementService.deleteAlertRule(ruleId);

        // Then
        verify(alertRuleRepository).findById(ruleId);
        verify(alertRuleRepository).delete(testRule);
    }

    @Test
    @DisplayName("알림 규칙 삭제 - 존재하지 않는 규칙")
    void deleteAlertRule_NotFound() {
        // Given
        Long ruleId = 999L;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> alertRuleManagementService.deleteAlertRule(ruleId))
                .isInstanceOf(AlertRuleNotFoundException.class);

        verify(alertRuleRepository).findById(ruleId);
    }

    @Test
    @DisplayName("알림 규칙 활성화 - 성공")
    void toggleAlertRule_Activate_Success() {
        // Given
        Long ruleId = 1L;
        boolean active = true;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.of(testRule));

        // When
        AlertRule result = alertRuleManagementService.toggleAlertRule(ruleId, active);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleRepository).findById(ruleId);
        // JPA 더티 체킹으로 자동 업데이트되므로 save 호출 없음
    }

    @Test
    @DisplayName("알림 규칙 비활성화 - 성공")
    void toggleAlertRule_Deactivate_Success() {
        // Given
        Long ruleId = 1L;
        boolean active = false;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.of(testRule));

        // When
        AlertRule result = alertRuleManagementService.toggleAlertRule(ruleId, active);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleRepository).findById(ruleId);
        // JPA 더티 체킹으로 자동 업데이트되므로 save 호출 없음
    }

    @Test
    @DisplayName("알림 규칙 활성화/비활성화 - 존재하지 않는 규칙")
    void toggleAlertRule_NotFound() {
        // Given
        Long ruleId = 999L;
        boolean active = true;

        given(alertRuleRepository.findById(ruleId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> alertRuleManagementService.toggleAlertRule(ruleId, active))
                .isInstanceOf(AlertRuleNotFoundException.class);

        verify(alertRuleRepository).findById(ruleId);
    }

    @Test
    @DisplayName("감정 패턴 알림 규칙 생성 - 성공")
    void createAlertRule_EmotionPattern_Success() {
        // Given
        AlertType alertType = AlertType.EMOTION_PATTERN;
        AlertLevel alertLevel = AlertLevel.HIGH;
        AlertCondition emotionCondition = AlertCondition.createEmotionCondition(3);

        given(alertRuleRepository.save(any(AlertRule.class)))
                .willReturn(testRule);

        // When
        AlertRule result = alertRuleManagementService.createAlertRule(
                testMember, alertType, alertLevel, emotionCondition);

        // Then
        assertThat(result).isEqualTo(testRule);
        verify(alertRuleRepository).save(any(AlertRule.class));
    }

    @Test
    @DisplayName("키워드 알림 규칙 생성 - 성공")
    void createAlertRule_Keyword_Success() {
        // Given
        AlertType alertType = AlertType.KEYWORD_DETECTION;
        AlertLevel alertLevel = AlertLevel.EMERGENCY;
        AlertCondition keywordCondition = AlertCondition.createKeywordCondition("자살,죽고싶어");

        AlertRule keywordRule = AlertRule.createKeywordRule(testMember, "자살,죽고싶어", AlertLevel.EMERGENCY);

        given(alertRuleRepository.save(any(AlertRule.class)))
                .willReturn(keywordRule);

        // When
        AlertRule result = alertRuleManagementService.createAlertRule(
                testMember, alertType, alertLevel, keywordCondition);

        // Then
        assertThat(result).isEqualTo(keywordRule);
        verify(alertRuleRepository).save(any(AlertRule.class));
    }

    @Test
    @DisplayName("무응답 알림 규칙 생성 - 성공")
    void createAlertRule_NoResponse_Success() {
        // Given
        AlertType alertType = AlertType.NO_RESPONSE;
        AlertLevel alertLevel = AlertLevel.MEDIUM;
        AlertCondition noResponseCondition = AlertCondition.createNoResponseCondition(2);

        AlertRule noResponseRule = AlertRule.createNoResponseRule(testMember, 2, AlertLevel.MEDIUM);

        given(alertRuleRepository.save(any(AlertRule.class)))
                .willReturn(noResponseRule);

        // When
        AlertRule result = alertRuleManagementService.createAlertRule(
                testMember, alertType, alertLevel, noResponseCondition);

        // Then
        assertThat(result).isEqualTo(noResponseRule);
        verify(alertRuleRepository).save(any(AlertRule.class));
    }
}