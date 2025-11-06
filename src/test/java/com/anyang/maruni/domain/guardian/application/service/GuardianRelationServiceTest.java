package com.anyang.maruni.domain.guardian.application.service;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestResponse;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRequest;
import com.anyang.maruni.domain.guardian.domain.entity.RequestStatus;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRequestRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationHistoryService;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * GuardianRelationService 테스트
 *
 * TDD Red-Green-Blue 사이클 적용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianRelationService 테스트")
class GuardianRelationServiceTest {

	@Mock
	private GuardianRequestRepository guardianRequestRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private NotificationHistoryService notificationHistoryService;

	@InjectMocks
	private GuardianRelationService guardianRelationService;

	@Test
	@DisplayName("보호자 요청 생성 - 정상적인 요청으로 GuardianRequest 생성 및 알림 발송")
	void sendRequest_ValidData_CreatesRequestAndSendsNotification() {
		// given
		Long requesterId = 1L;
		Long guardianId = 2L;
		MemberEntity requester = createMember(requesterId, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(guardianId, "guardian@example.com", "김영희");

		given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
		given(memberRepository.findById(guardianId)).willReturn(Optional.of(guardian));
		given(guardianRequestRepository.existsByRequesterIdAndGuardianIdAndStatus(
			requesterId, guardianId, RequestStatus.PENDING)).willReturn(false);
		given(guardianRequestRepository.save(any(GuardianRequest.class)))
			.willAnswer(invocation -> {
				GuardianRequest request = invocation.getArgument(0);
				return GuardianRequest.builder()
					.id(1L)
					.requester(request.getRequester())
					.guardian(request.getGuardian())
					.relation(request.getRelation())
					.status(request.getStatus())
					.build();
			});

		// when
		GuardianRequestResponse response = guardianRelationService.sendRequest(
			requesterId, guardianId, GuardianRelation.FAMILY);

		// then
		verify(guardianRequestRepository).save(any(GuardianRequest.class));
		verify(notificationHistoryService).recordNotificationWithType(
			eq(guardianId), anyString(), anyString(), any(), any(), anyLong());
		assertThat(response.getRelation()).isEqualTo(GuardianRelation.FAMILY);
		assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
	}

	@Test
	@DisplayName("보호자 요청 생성 실패 - 이미 보호자가 있으면 예외 발생")
	void sendRequest_AlreadyHasGuardian_ThrowsException() {
		// given
		Long requesterId = 1L;
		Long guardianId = 2L;
		MemberEntity requester = createMember(requesterId, "requester@example.com", "김순자");
		MemberEntity existingGuardian = createMember(3L, "existing@example.com", "기존보호자");
		requester.assignGuardian(existingGuardian, GuardianRelation.FAMILY);  // 이미 보호자 있음

		MemberEntity newGuardian = createMember(guardianId, "guardian@example.com", "김영희");

		given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
		given(memberRepository.findById(guardianId)).willReturn(Optional.of(newGuardian));

		// when & then
		assertThatThrownBy(() -> guardianRelationService.sendRequest(requesterId, guardianId, GuardianRelation.FAMILY))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUARDIAN_ALREADY_ASSIGNED);
	}

	@Test
	@DisplayName("보호자 요청 생성 실패 - 자기 자신에게 요청하면 예외 발생")
	void sendRequest_SelfAssignment_ThrowsException() {
		// given
		Long memberId = 1L;
		MemberEntity member = createMember(memberId, "member@example.com", "김순자");

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

		// when & then
		assertThatThrownBy(() -> guardianRelationService.sendRequest(memberId, memberId, GuardianRelation.FAMILY))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUARDIAN_SELF_ASSIGNMENT_NOT_ALLOWED);
	}

	@Test
	@DisplayName("보호자 요청 생성 실패 - 이미 PENDING 요청이 있으면 중복 방지")
	void sendRequest_DuplicatePendingRequest_ThrowsException() {
		// given
		Long requesterId = 1L;
		Long guardianId = 2L;
		MemberEntity requester = createMember(requesterId, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(guardianId, "guardian@example.com", "김영희");

		given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
		given(memberRepository.findById(guardianId)).willReturn(Optional.of(guardian));
		given(guardianRequestRepository.existsByRequesterIdAndGuardianIdAndStatus(
			requesterId, guardianId, RequestStatus.PENDING)).willReturn(true);  // 이미 PENDING 요청 있음

		// when & then
		assertThatThrownBy(() -> guardianRelationService.sendRequest(requesterId, guardianId, GuardianRelation.FAMILY))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUARDIAN_REQUEST_DUPLICATE);
	}

	@Test
	@DisplayName("보호자 요청 목록 조회 - PENDING 상태 요청만 반환")
	void getReceivedRequests_ReturnsOnlyPendingRequests() {
		// given
		Long guardianId = 2L;
		MemberEntity requester = createMember(1L, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(guardianId, "guardian@example.com", "김영희");

		GuardianRequest request1 = createRequest(1L, requester, guardian, RequestStatus.PENDING);
		GuardianRequest request2 = createRequest(2L, requester, guardian, RequestStatus.PENDING);

		given(guardianRequestRepository.findByGuardianIdAndStatus(guardianId, RequestStatus.PENDING))
			.willReturn(List.of(request1, request2));

		// when
		List<GuardianRequestResponse> responses = guardianRelationService.getReceivedRequests(guardianId);

		// then
		assertThat(responses).hasSize(2);
		assertThat(responses).allMatch(r -> r.getStatus() == RequestStatus.PENDING);
	}

	@Test
	@DisplayName("보호자 요청 수락 - PENDING 요청을 수락하고 관계 설정")
	void acceptRequest_PendingRequest_AcceptsAndAssignsGuardian() {
		// given
		Long requestId = 1L;
		Long guardianId = 2L;
		MemberEntity requester = createMember(1L, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(guardianId, "guardian@example.com", "김영희");
		GuardianRequest request = createRequest(requestId, requester, guardian, RequestStatus.PENDING);

		given(guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId))
			.willReturn(Optional.of(request));

		// when
		guardianRelationService.acceptRequest(requestId, guardianId);

		// then
		assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
		assertThat(requester.getGuardian()).isEqualTo(guardian);
		verify(notificationHistoryService).recordNotificationWithType(
			eq(requester.getId()), anyString(), anyString(), any(), any(), anyLong());
	}

	@Test
	@DisplayName("보호자 요청 수락 실패 - 요청을 찾을 수 없으면 예외 발생")
	void acceptRequest_RequestNotFound_ThrowsException() {
		// given
		Long requestId = 999L;
		Long guardianId = 2L;

		given(guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> guardianRelationService.acceptRequest(requestId, guardianId))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUARDIAN_REQUEST_NOT_FOUND);
	}

	@Test
	@DisplayName("보호자 요청 거절 - PENDING 요청을 거절")
	void rejectRequest_PendingRequest_RejectsRequest() {
		// given
		Long requestId = 1L;
		Long guardianId = 2L;
		MemberEntity requester = createMember(1L, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(guardianId, "guardian@example.com", "김영희");
		GuardianRequest request = createRequest(requestId, requester, guardian, RequestStatus.PENDING);

		given(guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId))
			.willReturn(Optional.of(request));

		// when
		guardianRelationService.rejectRequest(requestId, guardianId);

		// then
		assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
		verify(notificationHistoryService).recordNotificationWithType(
			eq(requester.getId()), anyString(), anyString(), any(), any(), anyLong());
	}

	@Test
	@DisplayName("보호자 관계 해제 - 기존 보호자 관계를 해제")
	void removeGuardian_ExistingGuardian_RemovesGuardian() {
		// given
		Long memberId = 1L;
		MemberEntity member = createMember(memberId, "member@example.com", "김순자");
		MemberEntity guardian = createMember(2L, "guardian@example.com", "김영희");
		member.assignGuardian(guardian, GuardianRelation.FAMILY);

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

		// when
		guardianRelationService.removeGuardian(memberId);

		// then
		assertThat(member.getGuardian()).isNull();
		verify(notificationHistoryService).recordNotification(
			eq(guardian.getId()), anyString(), anyString());
	}

	@Test
	@DisplayName("보호자 관계 해제 실패 - 보호자가 없으면 예외 발생")
	void removeGuardian_NoGuardian_ThrowsException() {
		// given
		Long memberId = 1L;
		MemberEntity member = createMember(memberId, "member@example.com", "김순자");

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

		// when & then
		assertThatThrownBy(() -> guardianRelationService.removeGuardian(memberId))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUARDIAN_NOT_ASSIGNED);
	}

	// ========== Helper Methods ==========

	private MemberEntity createMember(Long id, String email, String name) {
		return MemberEntity.builder()
			.id(id)
			.memberEmail(email)
			.memberName(name)
			.memberPassword("password")
			.dailyCheckEnabled(false)
			.build();
	}

	private GuardianRequest createRequest(Long id, MemberEntity requester, MemberEntity guardian, RequestStatus status) {
		return GuardianRequest.builder()
			.id(id)
			.requester(requester)
			.guardian(guardian)
			.relation(GuardianRelation.FAMILY)
			.status(status)
			.build();
	}
}
