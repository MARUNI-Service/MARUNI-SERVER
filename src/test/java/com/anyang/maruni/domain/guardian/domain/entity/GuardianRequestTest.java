package com.anyang.maruni.domain.guardian.domain.entity;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianRequest Entity 테스트")
class GuardianRequestTest {

	@Test
	@DisplayName("보호자 요청 생성 - 정상적인 요청으로 PENDING 상태 생성")
	void createRequest_ValidData_ReturnsPendingStatus() {
		// given
		MemberEntity requester = createMember(1L, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(2L, "guardian@example.com", "김영희");

		// when
		GuardianRequest request = GuardianRequest.createRequest(
			requester, guardian, GuardianRelation.FAMILY);

		// then
		assertThat(request.getRequester()).isEqualTo(requester);
		assertThat(request.getGuardian()).isEqualTo(guardian);
		assertThat(request.getRelation()).isEqualTo(GuardianRelation.FAMILY);
		assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
	}

	@Test
	@DisplayName("요청 수락 - PENDING 상태에서 ACCEPTED로 변경")
	void accept_PendingStatus_ChangesToAccepted() {
		// given
		GuardianRequest request = createPendingRequest();

		// when
		request.accept();

		// then
		assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
	}

	@Test
	@DisplayName("요청 수락 실패 - ACCEPTED 상태에서 예외 발생")
	void accept_AlreadyAccepted_ThrowsException() {
		// given
		GuardianRequest request = createPendingRequest();
		request.accept();  // 이미 수락됨

		// when & then
		assertThatThrownBy(() -> request.accept())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Only PENDING requests can be accepted");
	}

	@Test
	@DisplayName("요청 거절 - PENDING 상태에서 REJECTED로 변경")
	void reject_PendingStatus_ChangesToRejected() {
		// given
		GuardianRequest request = createPendingRequest();

		// when
		request.reject();

		// then
		assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
	}

	@Test
	@DisplayName("요청 거절 실패 - REJECTED 상태에서 예외 발생")
	void reject_AlreadyRejected_ThrowsException() {
		// given
		GuardianRequest request = createPendingRequest();
		request.reject();  // 이미 거절됨

		// when & then
		assertThatThrownBy(() -> request.reject())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Only PENDING requests can be rejected");
	}

	@Test
	@DisplayName("취소 가능 여부 확인 - PENDING 상태이면 true")
	void canBeCancelled_PendingStatus_ReturnsTrue() {
		// given
		GuardianRequest request = createPendingRequest();

		// when
		boolean canCancel = request.canBeCancelled();

		// then
		assertThat(canCancel).isTrue();
	}

	@Test
	@DisplayName("취소 가능 여부 확인 - ACCEPTED 상태이면 false")
	void canBeCancelled_AcceptedStatus_ReturnsFalse() {
		// given
		GuardianRequest request = createPendingRequest();
		request.accept();

		// when
		boolean canCancel = request.canBeCancelled();

		// then
		assertThat(canCancel).isFalse();
	}

	@Test
	@DisplayName("취소 가능 여부 확인 - REJECTED 상태이면 false")
	void canBeCancelled_RejectedStatus_ReturnsFalse() {
		// given
		GuardianRequest request = createPendingRequest();
		request.reject();

		// when
		boolean canCancel = request.canBeCancelled();

		// then
		assertThat(canCancel).isFalse();
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

	private GuardianRequest createPendingRequest() {
		MemberEntity requester = createMember(1L, "requester@example.com", "김순자");
		MemberEntity guardian = createMember(2L, "guardian@example.com", "김영희");
		return GuardianRequest.createRequest(requester, guardian, GuardianRelation.FAMILY);
	}
}
