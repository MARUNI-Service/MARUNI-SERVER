package com.anyang.maruni.domain.guardian.domain.entity;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보호자 요청 엔티티
 *
 * 노인 회원이 다른 회원에게 보호자 등록을 요청할 때 생성됩니다.
 * 요청은 PENDING 상태로 시작하며, 보호자가 수락/거절할 수 있습니다.
 *
 * User Journey 3 참조: 김순자 → 김영희에게 보호자 요청
 */
@Entity
@Table(name = "guardian_request",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_requester_guardian",
		columnNames = {"requester_id", "guardian_id"}
	),
	indexes = {
		@Index(name = "idx_guardian_id_status", columnList = "guardian_id, status"),
		@Index(name = "idx_requester_id", columnList = "requester_id"),
		@Index(name = "idx_created_at", columnList = "createdAt")
	})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRequest extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 요청한 사람 (노인)
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id", nullable = false)
	private MemberEntity requester;

	/**
	 * 요청받은 사람 (보호자)
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "guardian_id", nullable = false)
	private MemberEntity guardian;

	/**
	 * 관계 (FAMILY, FRIEND, CAREGIVER 등)
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GuardianRelation relation;

	/**
	 * 요청 상태 (PENDING, ACCEPTED, REJECTED)
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RequestStatus status;

	// ========== 정적 팩토리 메서드 ==========

	/**
	 * 보호자 요청 생성
	 *
	 * @param requester 요청한 사람 (노인)
	 * @param guardian 요청받은 사람 (보호자)
	 * @param relation 관계 (FAMILY, FRIEND 등)
	 * @return 생성된 GuardianRequest (상태: PENDING)
	 */
	public static GuardianRequest createRequest(
		MemberEntity requester,
		MemberEntity guardian,
		GuardianRelation relation) {

		return GuardianRequest.builder()
			.requester(requester)
			.guardian(guardian)
			.relation(relation)
			.status(RequestStatus.PENDING)
			.build();
	}

	// ========== 비즈니스 로직 메서드 ==========

	/**
	 * 요청 수락
	 * PENDING 상태에서만 수락 가능
	 *
	 * @throws IllegalStateException PENDING이 아닌 상태에서 수락 시도 시
	 */
	public void accept() {
		if (this.status != RequestStatus.PENDING) {
			throw new IllegalStateException("Only PENDING requests can be accepted");
		}
		this.status = RequestStatus.ACCEPTED;
	}

	/**
	 * 요청 거절
	 * PENDING 상태에서만 거절 가능
	 *
	 * @throws IllegalStateException PENDING이 아닌 상태에서 거절 시도 시
	 */
	public void reject() {
		if (this.status != RequestStatus.PENDING) {
			throw new IllegalStateException("Only PENDING requests can be rejected");
		}
		this.status = RequestStatus.REJECTED;
	}

	/**
	 * 취소 가능 여부 확인
	 *
	 * @return PENDING 상태이면 true
	 */
	public boolean canBeCancelled() {
		return this.status == RequestStatus.PENDING;
	}
}
