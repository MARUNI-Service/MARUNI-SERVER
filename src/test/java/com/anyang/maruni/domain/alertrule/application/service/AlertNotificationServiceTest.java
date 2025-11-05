package com.anyang.maruni.domain.alertrule.application.service;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertNotificationService;
import com.anyang.maruni.domain.alertrule.application.service.util.AlertServiceUtils;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertHistory;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

/**
 * AlertNotificationService 테스트
 *
 * 알림 발송 처리 전담 서비스의 독립적인 테스트
 * - 알림 발생 처리 (triggerAlert)
 * - 보호자 알림 발송 (sendGuardianNotification)
 * - 알림 발송 결과 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertNotificationService 테스트")
class AlertNotificationServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private AlertConfigurationProperties alertConfig;

	@Mock
	private AlertServiceUtils alertServiceUtils;

	@Mock
	private AlertHistoryRepository alertHistoryRepository;

	@InjectMocks
	private AlertNotificationService alertNotificationService;

	private MemberEntity testMember;
	private MemberEntity testGuardian;
	private AlertResult testAlertResult;
	private AlertHistory testAlertHistory;

	@BeforeEach
	void setUp() {
		testGuardian = MemberEntity.builder()
			.id(2L)
			.memberName("테스트보호자")
			.memberEmail("guardian@example.com")
			.memberPassword("password123")
			.pushToken("guardian_push_token")
			.build();

		testMember = MemberEntity.builder()
			.id(1L)
			.memberName("테스트회원")
			.memberEmail("test@example.com")
			.memberPassword("password123")
			.build();
		testMember.assignGuardian(testGuardian, GuardianRelation.FAMILY);

		testAlertResult = AlertResult.createAlert(
			AlertLevel.HIGH, AlertType.EMOTION_PATTERN, "3일 연속 부정감정 감지", null);

		testAlertHistory = AlertHistory.builder()
			.id(123L)
			.alertRule(null)
			.member(testMember)
			.alertLevel(AlertLevel.HIGH)
			.alertMessage("테스트 알림")
			.detectionDetails("{\"details\":\"test\"}")
			.isNotificationSent(false)
			.build();
	}

	@Test
	@DisplayName("알림 발생 처리 - 성공")
	void triggerAlert_Success() {
		// Given
		Long memberId = 1L;
		Long expectedHistoryId = 123L;

		AlertConfigurationProperties.Notification notificationConfig =
			new AlertConfigurationProperties.Notification();
		notificationConfig.setTitleTemplate("[%s] 알림");

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(testMember);
		given(alertServiceUtils.createDetectionDetailsJson(testAlertResult))
			.willReturn("{\"alertLevel\":\"HIGH\",\"message\":\"3일 연속 부정감정 감지\"}");
		given(alertHistoryRepository.save(any(AlertHistory.class)))
			.willReturn(testAlertHistory);
		given(alertConfig.getNotification())
			.willReturn(notificationConfig);
		given(notificationService.sendNotificationWithType(anyLong(), anyString(), anyString(), any(), any(), anyLong()))
			.willReturn(true);

		// When
		Long result = alertNotificationService.triggerAlert(memberId, testAlertResult);

		// Then
		assertThat(result).isEqualTo(expectedHistoryId);

		verify(alertServiceUtils, times(2)).validateAndGetMember(memberId);
		verify(alertServiceUtils).createDetectionDetailsJson(testAlertResult);
		verify(alertHistoryRepository).save(any(AlertHistory.class));
		// triggerAlert는 내부적으로 sendGuardianNotificationWithType을 호출하므로 알림 발송도 검증
		verify(notificationService).sendNotificationWithType(
			eq(testGuardian.getId()), eq("[HIGH] 알림"), eq(testAlertResult.getMessage()),
			any(), any(), eq(expectedHistoryId));
	}

	@Test
	@DisplayName("보호자 알림 발송 - 성공")
	void sendGuardianNotification_Success() {
		// Given
		Long memberId = 1L;
		AlertLevel alertLevel = AlertLevel.HIGH;
		String alertMessage = "테스트 알림 메시지";

		AlertConfigurationProperties.Notification notificationConfig =
			new AlertConfigurationProperties.Notification();
		notificationConfig.setTitleTemplate("[%s] 알림");
		notificationConfig.setNotificationFailureLog("알림 발송 실패: 회원 ID %d");
		notificationConfig.setNotificationErrorLog("알림 발송 오류: %s");

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(testMember);
		given(alertConfig.getNotification())
			.willReturn(notificationConfig);
		given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
			.willReturn(true);

		// When
		alertNotificationService.sendGuardianNotification(memberId, alertLevel, alertMessage);

		// Then
		verify(alertServiceUtils).validateAndGetMember(memberId);
		verify(notificationService).sendPushNotification(
			testGuardian.getId(), "[HIGH] 알림", alertMessage);
	}

	@Test
	@DisplayName("보호자 알림 발송 - 보호자 없음")
	void sendGuardianNotification_NoGuardian() {
		// Given
		Long memberId = 1L;
		AlertLevel alertLevel = AlertLevel.HIGH;
		String alertMessage = "테스트 알림 메시지";

		MemberEntity memberWithoutGuardian = MemberEntity.builder()
			.id(1L)
			.memberName("테스트회원")
			.memberEmail("test@example.com")
			.memberPassword("password123")
			.build();

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(memberWithoutGuardian);

		// When
		alertNotificationService.sendGuardianNotification(memberId, alertLevel, alertMessage);

		// Then
		verify(alertServiceUtils).validateAndGetMember(memberId);
		verifyNoInteractions(notificationService);
	}

	@Test
	@DisplayName("보호자 알림 발송 - 발송 실패")
	void sendGuardianNotification_SendFailure() {
		// Given
		Long memberId = 1L;
		AlertLevel alertLevel = AlertLevel.HIGH;
		String alertMessage = "테스트 알림 메시지";

		AlertConfigurationProperties.Notification notificationConfig =
			new AlertConfigurationProperties.Notification();
		notificationConfig.setTitleTemplate("[%s] 알림");
		notificationConfig.setNotificationFailureLog("알림 발송 실패: 회원 ID %d");
		notificationConfig.setNotificationErrorLog("알림 발송 오류: %s");

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(testMember);
		given(alertConfig.getNotification())
			.willReturn(notificationConfig);
		given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
			.willReturn(false);

		// When
		alertNotificationService.sendGuardianNotification(memberId, alertLevel, alertMessage);

		// Then
		verify(alertServiceUtils).validateAndGetMember(memberId);
		verify(notificationService).sendPushNotification(
			testGuardian.getId(), "[HIGH] 알림", alertMessage);
	}

	@Test
	@DisplayName("보호자 알림 발송 - 예외 발생")
	void sendGuardianNotification_Exception() {
		// Given
		Long memberId = 1L;
		AlertLevel alertLevel = AlertLevel.HIGH;
		String alertMessage = "테스트 알림 메시지";

		AlertConfigurationProperties.Notification notificationConfig =
			new AlertConfigurationProperties.Notification();
		notificationConfig.setTitleTemplate("[%s] 알림");
		notificationConfig.setNotificationFailureLog("알림 발송 실패: 회원 ID %d");
		notificationConfig.setNotificationErrorLog("알림 발송 오류: %s");

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(testMember);
		given(alertConfig.getNotification())
			.willReturn(notificationConfig);
		given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
			.willThrow(new RuntimeException("네트워크 오류"));

		// When
		alertNotificationService.sendGuardianNotification(memberId, alertLevel, alertMessage);

		// Then
		verify(alertServiceUtils).validateAndGetMember(memberId);
		verify(notificationService).sendPushNotification(
			testGuardian.getId(), "[HIGH] 알림", alertMessage);
	}

	@Test
	@DisplayName("긴급 알림 발송 - EMERGENCY 레벨")
	void sendGuardianNotification_Emergency() {
		// Given
		Long memberId = 1L;
		AlertLevel alertLevel = AlertLevel.EMERGENCY;
		String alertMessage = "긴급 상황 발생";

		AlertConfigurationProperties.Notification notificationConfig =
			new AlertConfigurationProperties.Notification();
		notificationConfig.setTitleTemplate("[%s] 긴급 알림");
		notificationConfig.setNotificationFailureLog("알림 발송 실패: 회원 ID %d");
		notificationConfig.setNotificationErrorLog("알림 발송 오류: %s");

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(testMember);
		given(alertConfig.getNotification())
			.willReturn(notificationConfig);
		given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
			.willReturn(true);

		// When
		alertNotificationService.sendGuardianNotification(memberId, alertLevel, alertMessage);

		// Then
		verify(alertServiceUtils).validateAndGetMember(memberId);
		verify(notificationService).sendPushNotification(
			testGuardian.getId(), "[EMERGENCY] 긴급 알림", alertMessage);
	}

	@Test
	@DisplayName("알림 발생 처리와 보호자 알림 통합 - 성공")
	void triggerAlert_WithGuardianNotification_Success() {
		// Given
		Long memberId = 1L;
		Long expectedHistoryId = 123L;

		given(alertServiceUtils.validateAndGetMember(memberId))
			.willReturn(testMember);
		given(alertServiceUtils.createDetectionDetailsJson(testAlertResult))
			.willReturn("{\"alertLevel\":\"HIGH\",\"message\":\"3일 연속 부정감정 감지\"}");
		given(alertHistoryRepository.save(any(AlertHistory.class)))
			.willReturn(testAlertHistory);

		AlertConfigurationProperties.Notification notificationConfig =
			new AlertConfigurationProperties.Notification();
		notificationConfig.setTitleTemplate("[%s] 알림");

		given(alertConfig.getNotification())
			.willReturn(notificationConfig);
		given(notificationService.sendNotificationWithType(anyLong(), anyString(), anyString(), any(), any(), anyLong()))
			.willReturn(true);

		// When
		Long result = alertNotificationService.triggerAlert(memberId, testAlertResult);

		// Then
		assertThat(result).isEqualTo(expectedHistoryId);

		// AlertHistory 저장 확인 (validateAndGetMember는 두 번 호출됨: triggerAlert + sendGuardianNotificationWithType)
		verify(alertServiceUtils, times(2)).validateAndGetMember(memberId);
		verify(alertHistoryRepository).save(any(AlertHistory.class));

		// 보호자 알림 발송 확인
		verify(notificationService).sendNotificationWithType(
			eq(testGuardian.getId()), eq("[HIGH] 알림"), eq(testAlertResult.getMessage()),
			any(), any(), eq(expectedHistoryId));
	}
}
