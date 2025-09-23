package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.util.AlertServiceUtils;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AlertHistoryService 테스트
 *
 * 알림 이력 관리 전담 서비스의 독립적인 테스트
 * - 알림 이력 기록 (recordAlertHistory)
 * - 최근 알림 이력 조회 (getRecentAlertHistory)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertHistoryService 테스트")
class AlertHistoryServiceTest {

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AlertServiceUtils alertServiceUtils;

    @InjectMocks
    private AlertHistoryService alertHistoryService;

    private MemberEntity testMember;
    private AlertRule testRule;
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

        testAlertResult = AlertResult.createAlert(
                AlertLevel.HIGH, "3일 연속 부정감정 감지", null);

        testAlertHistory = AlertHistory.builder()
                .id(1L)
                .alertRule(testRule)
                .member(testMember)
                .alertLevel(AlertLevel.HIGH)
                .alertMessage("3일 연속 부정감정 감지")
                .detectionDetails("{\"alertLevel\":\"HIGH\",\"message\":\"3일 연속 부정감정 감지\"}")
                .isNotificationSent(false)
                .build();
    }

    @Test
    @DisplayName("알림 이력 기록 - 성공")
    void recordAlertHistory_Success() {
        // Given
        String expectedDetailsJson = "{\"alertLevel\":\"HIGH\",\"message\":\"3일 연속 부정감정 감지\"}";

        given(alertServiceUtils.createDetectionDetailsJson(testAlertResult))
                .willReturn(expectedDetailsJson);
        given(alertHistoryRepository.save(any(AlertHistory.class)))
                .willReturn(testAlertHistory);

        // When
        AlertHistory result = alertHistoryService.recordAlertHistory(testRule, testMember, testAlertResult);

        // Then
        assertThat(result).isEqualTo(testAlertHistory);
        assertThat(result.getAlertMessage()).isEqualTo("3일 연속 부정감정 감지");
        assertThat(result.getAlertLevel()).isEqualTo(AlertLevel.HIGH);

        verify(alertServiceUtils).createDetectionDetailsJson(testAlertResult);
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("최근 알림 이력 조회 - 성공")
    void getRecentAlertHistory_Success() {
        // Given
        Long memberId = 1L;
        int days = 30;
        List<AlertHistory> expectedHistory = Arrays.asList(testAlertHistory);

        given(alertHistoryRepository.findByMemberIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(expectedHistory);

        // When
        List<AlertHistory> result = alertHistoryService.getRecentAlertHistory(memberId, days);

        // Then
        assertThat(result).isEqualTo(expectedHistory);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAlertMessage()).isEqualTo("3일 연속 부정감정 감지");

        verify(alertHistoryRepository).findByMemberIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("최근 알림 이력 조회 - 빈 목록")
    void getRecentAlertHistory_EmptyList() {
        // Given
        Long memberId = 1L;
        int days = 7;

        given(alertHistoryRepository.findByMemberIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(Arrays.asList());

        // When
        List<AlertHistory> result = alertHistoryService.getRecentAlertHistory(memberId, days);

        // Then
        assertThat(result).isEmpty();

        verify(alertHistoryRepository).findByMemberIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("알림 이력 기록 - 긴급 알림")
    void recordAlertHistory_Emergency() {
        // Given
        AlertResult emergencyResult = AlertResult.createAlert(
                AlertLevel.EMERGENCY, "긴급 키워드 감지: 자살", null);

        AlertHistory emergencyHistory = AlertHistory.builder()
                .id(2L)
                .alertRule(testRule)
                .member(testMember)
                .alertLevel(AlertLevel.EMERGENCY)
                .alertMessage("긴급 키워드 감지: 자살")
                .detectionDetails("{\"alertLevel\":\"EMERGENCY\",\"message\":\"긴급 키워드 감지: 자살\"}")
                .isNotificationSent(false)
                .build();

        String expectedDetailsJson = "{\"alertLevel\":\"EMERGENCY\",\"message\":\"긴급 키워드 감지: 자살\"}";

        given(alertServiceUtils.createDetectionDetailsJson(emergencyResult))
                .willReturn(expectedDetailsJson);
        given(alertHistoryRepository.save(any(AlertHistory.class)))
                .willReturn(emergencyHistory);

        // When
        AlertHistory result = alertHistoryService.recordAlertHistory(testRule, testMember, emergencyResult);

        // Then
        assertThat(result).isEqualTo(emergencyHistory);
        assertThat(result.getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        assertThat(result.getAlertMessage()).contains("긴급 키워드 감지");

        verify(alertServiceUtils).createDetectionDetailsJson(emergencyResult);
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("알림 이력 기록 - 여러 유형의 알림")
    void recordAlertHistory_MultipleTypes() {
        // Given - 감정 패턴 알림
        AlertResult emotionResult = AlertResult.createAlert(
                AlertLevel.MEDIUM, "감정 패턴 이상 감지", null);

        AlertHistory emotionHistory = AlertHistory.builder()
                .id(3L)
                .alertRule(testRule)
                .member(testMember)
                .alertLevel(AlertLevel.MEDIUM)
                .alertMessage("감정 패턴 이상 감지")
                .detectionDetails("{\"alertLevel\":\"MEDIUM\",\"message\":\"감정 패턴 이상 감지\"}")
                .isNotificationSent(false)
                .build();

        given(alertServiceUtils.createDetectionDetailsJson(emotionResult))
                .willReturn("{\"alertLevel\":\"MEDIUM\",\"message\":\"감정 패턴 이상 감지\"}");
        given(alertHistoryRepository.save(any(AlertHistory.class)))
                .willReturn(emotionHistory);

        // When
        AlertHistory result = alertHistoryService.recordAlertHistory(testRule, testMember, emotionResult);

        // Then
        assertThat(result.getAlertLevel()).isEqualTo(AlertLevel.MEDIUM);
        assertThat(result.getAlertMessage()).isEqualTo("감정 패턴 이상 감지");

        verify(alertServiceUtils).createDetectionDetailsJson(emotionResult);
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("기간별 알림 이력 조회 - 7일")
    void getRecentAlertHistory_SevenDays() {
        // Given
        Long memberId = 1L;
        int days = 7;

        AlertHistory recentHistory = AlertHistory.builder()
                .id(4L)
                .alertRule(testRule)
                .member(testMember)
                .alertLevel(AlertLevel.LOW)
                .alertMessage("최근 알림")
                .detectionDetails("{\"alertLevel\":\"LOW\",\"message\":\"최근 알림\"}")
                .isNotificationSent(true)
                .build();

        List<AlertHistory> expectedHistory = Arrays.asList(recentHistory);

        given(alertHistoryRepository.findByMemberIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(expectedHistory);

        // When
        List<AlertHistory> result = alertHistoryService.getRecentAlertHistory(memberId, days);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAlertLevel()).isEqualTo(AlertLevel.LOW);
        assertThat(result.get(0).getAlertMessage()).isEqualTo("최근 알림");

        verify(alertHistoryRepository).findByMemberIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("AlertRule 없이 이력 기록 - NullPointerException 예상")
    void recordAlertHistory_WithoutAlertRule_ThrowsException() {
        // Given
        AlertResult mvpResult = AlertResult.createAlert(
                AlertLevel.HIGH, "MVP 테스트 알림", null);

        given(alertServiceUtils.createDetectionDetailsJson(mvpResult))
                .willReturn("{\"alertLevel\":\"HIGH\",\"message\":\"MVP 테스트 알림\"}");

        // When & Then - AlertRule이 null이면 NullPointerException 발생
        assertThatThrownBy(() -> alertHistoryService.recordAlertHistory(null, testMember, mvpResult))
                .isInstanceOf(NullPointerException.class);

        verify(alertServiceUtils).createDetectionDetailsJson(mvpResult);
    }
}