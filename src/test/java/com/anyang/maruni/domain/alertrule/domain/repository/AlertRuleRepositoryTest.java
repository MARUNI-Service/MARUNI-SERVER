package com.anyang.maruni.domain.alertrule.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

/**
 * AlertRuleRepository 테스트
 *
 * TDD Red 단계: Repository 쿼리 메서드 테스트
 */
@DataJpaTest
@DisplayName("AlertRuleRepository 테스트")
class AlertRuleRepositoryTest {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private MemberRepository memberRepository;

    private MemberEntity testMember1;
    private MemberEntity testMember2;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성 및 저장
        testMember1 = MemberEntity.builder()
                .memberName("회원1")
                .memberEmail("member1@example.com")
                .memberPassword("password123")
                .build();

        testMember2 = MemberEntity.builder()
                .memberName("회원2")
                .memberEmail("member2@example.com")
                .memberPassword("password123")
                .build();

        memberRepository.save(testMember1);
        memberRepository.save(testMember2);
    }

    @Test
    @DisplayName("회원별 활성 알림 규칙 조회 테스트")
    void findActiveRulesByMemberId_shouldReturnActiveRules() {
        // Given
        AlertRule activeRule = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        AlertRule inactiveRule = AlertRule.createNoResponseRule(testMember1, 2, AlertLevel.EMERGENCY);
        inactiveRule.deactivate();

        alertRuleRepository.save(activeRule);
        alertRuleRepository.save(inactiveRule);

        // When
        List<AlertRule> activeRules = alertRuleRepository.findActiveRulesByMemberId(testMember1.getId());

        // Then
        assertThat(activeRules).hasSize(1);
        assertThat(activeRules.get(0).getIsActive()).isTrue();
        assertThat(activeRules.get(0).getAlertType()).isEqualTo(AlertType.EMOTION_PATTERN);
    }

    @Test
    @DisplayName("회원별 특정 유형 활성 규칙 조회 테스트")
    void findActiveRulesByMemberIdAndType_shouldReturnFilteredRules() {
        // Given
        AlertRule emotionRule = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        AlertRule noResponseRule = AlertRule.createNoResponseRule(testMember1, 2, AlertLevel.EMERGENCY);

        alertRuleRepository.save(emotionRule);
        alertRuleRepository.save(noResponseRule);

        // When
        List<AlertRule> emotionRules = alertRuleRepository
                .findActiveRulesByMemberIdAndType(testMember1.getId(), AlertType.EMOTION_PATTERN);

        // Then
        assertThat(emotionRules).hasSize(1);
        assertThat(emotionRules.get(0).getAlertType()).isEqualTo(AlertType.EMOTION_PATTERN);
    }

    @Test
    @DisplayName("특정 레벨 이상 활성 규칙 조회 테스트")
    void findActiveRulesByMinLevel_shouldReturnHighLevelRules() {
        // Given
        AlertRule highRule = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        AlertRule emergencyRule = AlertRule.createNoResponseRule(testMember2, 2, AlertLevel.EMERGENCY);
        AlertRule lowRule = AlertRule.createKeywordRule(testMember1, "테스트", AlertLevel.LOW);

        alertRuleRepository.save(highRule);
        alertRuleRepository.save(emergencyRule);
        alertRuleRepository.save(lowRule);

        // When
        List<AlertRule> emergencyRules = alertRuleRepository.findActiveRulesByMinLevel(AlertLevel.EMERGENCY);

        // Then
        assertThat(emergencyRules).hasSize(1);
        assertThat(emergencyRules.get(0).getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
    }

    @Test
    @DisplayName("모든 활성 감정 패턴 규칙 조회 테스트")
    void findAllActiveEmotionPatternRules_shouldReturnEmotionRules() {
        // Given
        AlertRule emotionRule1 = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        AlertRule emotionRule2 = AlertRule.createEmotionPatternRule(testMember2, 2, AlertLevel.MEDIUM);
        AlertRule noResponseRule = AlertRule.createNoResponseRule(testMember1, 2, AlertLevel.EMERGENCY);

        alertRuleRepository.save(emotionRule1);
        alertRuleRepository.save(emotionRule2);
        alertRuleRepository.save(noResponseRule);

        // When
        List<AlertRule> emotionRules = alertRuleRepository.findAllActiveEmotionPatternRules();

        // Then
        assertThat(emotionRules).hasSize(2);
        assertThat(emotionRules).allMatch(rule -> rule.getAlertType() == AlertType.EMOTION_PATTERN);
    }

    @Test
    @DisplayName("회원별 활성 규칙 수 조회 테스트")
    void countActiveRulesByMemberId_shouldReturnCorrectCount() {
        // Given
        AlertRule activeRule1 = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        AlertRule activeRule2 = AlertRule.createNoResponseRule(testMember1, 2, AlertLevel.EMERGENCY);
        AlertRule inactiveRule = AlertRule.createKeywordRule(testMember1, "테스트", AlertLevel.LOW);
        inactiveRule.deactivate();

        alertRuleRepository.save(activeRule1);
        alertRuleRepository.save(activeRule2);
        alertRuleRepository.save(inactiveRule);

        // When
        long activeCount = alertRuleRepository.countActiveRulesByMemberId(testMember1.getId());

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 유형 규칙 존재 여부 확인 테스트")
    void existsByMemberIdAndAlertTypeAndIsActiveTrue_shouldReturnCorrectResult() {
        // Given
        AlertRule emotionRule = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        alertRuleRepository.save(emotionRule);

        // When & Then
        assertThat(alertRuleRepository.existsByMemberIdAndAlertTypeAndIsActiveTrue(
                testMember1.getId(), AlertType.EMOTION_PATTERN)).isTrue();

        assertThat(alertRuleRepository.existsByMemberIdAndAlertTypeAndIsActiveTrue(
                testMember1.getId(), AlertType.NO_RESPONSE)).isFalse();
    }

    @Test
    @DisplayName("회원별 알림 레벨순 정렬 조회 테스트")
    void findActiveRulesByMemberIdOrderedByLevel_shouldReturnOrderedRules() {
        // Given
        AlertRule lowRule = AlertRule.createKeywordRule(testMember1, "테스트", AlertLevel.LOW);
        AlertRule highRule = AlertRule.createEmotionPatternRule(testMember1, 3, AlertLevel.HIGH);
        AlertRule emergencyRule = AlertRule.createNoResponseRule(testMember1, 2, AlertLevel.EMERGENCY);

        alertRuleRepository.save(lowRule);
        alertRuleRepository.save(highRule);
        alertRuleRepository.save(emergencyRule);

        // When
        List<AlertRule> orderedRules = alertRuleRepository
                .findActiveRulesByMemberIdOrderedByLevel(testMember1.getId());

        // Then
        assertThat(orderedRules).hasSize(3);
        assertThat(orderedRules.get(0).getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(orderedRules.get(1).getAlertLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(orderedRules.get(2).getAlertLevel()).isEqualTo(AlertLevel.LOW);
    }
}