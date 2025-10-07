package com.anyang.maruni.domain.guardian.application.service;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestResponse;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRequest;
import com.anyang.maruni.domain.guardian.domain.entity.RequestStatus;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRequestRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Guardian 관계 관리 서비스
 *
 * 보호자 요청/수락/거절/해제 비즈니스 로직을 담당합니다.
 * User Journey 3-4 참조
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GuardianRelationService {

	private final GuardianRequestRepository guardianRequestRepository;
	private final MemberRepository memberRepository;
	private final NotificationService notificationService;

	/**
	 * 보호자 요청 생성
	 *
	 * User Journey 3: 김순자 → 김영희에게 보호자 요청
	 *
	 * @param requesterId 요청자 회원 ID (노인)
	 * @param guardianId 보호자 회원 ID
	 * @param relation 관계 (FAMILY, FRIEND 등)
	 * @return GuardianRequestResponse (생성된 요청 정보)
	 * @throws BaseException MEMBER_NOT_FOUND, GUARDIAN_ALREADY_ASSIGNED, GUARDIAN_REQUEST_DUPLICATE
	 */
	@Transactional
	public GuardianRequestResponse sendRequest(
		Long requesterId,
		Long guardianId,
		GuardianRelation relation) {

		// 1. 요청자와 보호자 조회
		MemberEntity requester = findMemberById(requesterId);
		MemberEntity guardian = findMemberById(guardianId);

		// 2. 비즈니스 규칙 검증
		validateCanSendRequest(requester, guardian);

		// 3. GuardianRequest 생성 (상태: PENDING)
		GuardianRequest request = GuardianRequest.createRequest(
			requester, guardian, relation);
		GuardianRequest savedRequest = guardianRequestRepository.save(request);

		// 4. 보호자에게 푸시 알림 발송
		String message = String.format("%s님이 보호자로 등록을 요청했습니다",
			requester.getMemberName());
		notificationService.sendPushNotification(guardianId, "보호자 등록 요청", message);

		log.info("Guardian request sent: requester={}, guardian={}, relation={}",
			requesterId, guardianId, relation);

		return GuardianRequestResponse.from(savedRequest);
	}

	/**
	 * 보호자에게 온 요청 목록 조회
	 *
	 * User Journey 4 Phase 3: 김영희가 받은 요청 확인
	 *
	 * @param guardianId 보호자 회원 ID
	 * @return GuardianRequestResponse 목록 (PENDING 상태만)
	 */
	public List<GuardianRequestResponse> getReceivedRequests(Long guardianId) {
		List<GuardianRequest> requests = guardianRequestRepository
			.findByGuardianIdAndStatus(guardianId, RequestStatus.PENDING);

		return requests.stream()
			.map(GuardianRequestResponse::from)
			.toList();
	}

	/**
	 * 보호자 요청 수락
	 *
	 * User Journey 4 Phase 4: 김영희가 요청 수락
	 *
	 * @param requestId 요청 ID
	 * @param guardianId 보호자 회원 ID
	 * @throws BaseException GUARDIAN_REQUEST_NOT_FOUND, GUARDIAN_REQUEST_ALREADY_PROCESSED
	 */
	@Transactional
	public void acceptRequest(Long requestId, Long guardianId) {
		// 1. 요청 조회 및 권한 검증
		GuardianRequest request = findRequestByIdAndGuardianId(requestId, guardianId);

		// 2. 요청 수락 (상태: ACCEPTED)
		request.accept();

		// 3. Member-Guardian 관계 설정
		MemberEntity requester = request.getRequester();
		MemberEntity guardian = request.getGuardian();
		requester.assignGuardian(guardian, request.getRelation());

		// 4. 요청자에게 푸시 알림 발송
		String message = String.format("%s님이 보호자 요청을 수락했습니다",
			guardian.getMemberName());
		notificationService.sendPushNotification(
			requester.getId(), "보호자 요청 수락", message);

		log.info("Guardian request accepted: requestId={}, guardianId={}",
			requestId, guardianId);
	}

	/**
	 * 보호자 요청 거절
	 *
	 * @param requestId 요청 ID
	 * @param guardianId 보호자 회원 ID
	 * @throws BaseException GUARDIAN_REQUEST_NOT_FOUND, GUARDIAN_REQUEST_ALREADY_PROCESSED
	 */
	@Transactional
	public void rejectRequest(Long requestId, Long guardianId) {
		// 1. 요청 조회 및 권한 검증
		GuardianRequest request = findRequestByIdAndGuardianId(requestId, guardianId);

		// 2. 요청 거절 (상태: REJECTED)
		request.reject();

		// 3. 요청자에게 푸시 알림 발송
		String message = "보호자 요청이 거절되었습니다";
		notificationService.sendPushNotification(
			request.getRequester().getId(), "보호자 요청 거절", message);

		log.info("Guardian request rejected: requestId={}, guardianId={}",
			requestId, guardianId);
	}

	/**
	 * 보호자 관계 해제
	 *
	 * Journey: 김순자가 보호자 관계 해제
	 *
	 * @param memberId 회원 ID
	 * @throws BaseException MEMBER_NOT_FOUND, GUARDIAN_NOT_ASSIGNED
	 */
	@Transactional
	public void removeGuardian(Long memberId) {
		MemberEntity member = findMemberById(memberId);

		if (!member.hasGuardian()) {
			throw new BaseException(ErrorCode.GUARDIAN_NOT_ASSIGNED);
		}

		MemberEntity guardian = member.getGuardian();
		member.removeGuardian();

		// 보호자에게 알림 발송
		String message = String.format("%s님이 보호자 관계를 해제했습니다",
			member.getMemberName());
		notificationService.sendPushNotification(
			guardian.getId(), "보호자 관계 해제", message);

		log.info("Guardian relationship removed: memberId={}, guardianId={}",
			memberId, guardian.getId());
	}

	// ========== Private Helper Methods ==========

	/**
	 * 회원 ID로 회원 조회
	 *
	 * @param memberId 회원 ID
	 * @return MemberEntity
	 * @throws BaseException MEMBER_NOT_FOUND
	 */
	private MemberEntity findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * 요청 ID와 보호자 ID로 요청 조회 (권한 검증)
	 *
	 * @param requestId 요청 ID
	 * @param guardianId 보호자 회원 ID
	 * @return GuardianRequest
	 * @throws BaseException GUARDIAN_REQUEST_NOT_FOUND
	 */
	private GuardianRequest findRequestByIdAndGuardianId(Long requestId, Long guardianId) {
		return guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId)
			.orElseThrow(() -> new BaseException(ErrorCode.GUARDIAN_REQUEST_NOT_FOUND));
	}

	/**
	 * 보호자 요청 가능 여부 검증
	 *
	 * @param requester 요청자 (노인)
	 * @param guardian 보호자
	 * @throws BaseException GUARDIAN_SELF_ASSIGNMENT_NOT_ALLOWED, GUARDIAN_ALREADY_ASSIGNED, GUARDIAN_REQUEST_DUPLICATE
	 */
	private void validateCanSendRequest(MemberEntity requester, MemberEntity guardian) {
		// 1. 자기 자신에게 요청 불가
		if (requester.getId().equals(guardian.getId())) {
			throw new BaseException(ErrorCode.GUARDIAN_SELF_ASSIGNMENT_NOT_ALLOWED);
		}

		// 2. 이미 보호자가 있으면 요청 불가
		if (requester.hasGuardian()) {
			throw new BaseException(ErrorCode.GUARDIAN_ALREADY_ASSIGNED);
		}

		// 3. 이미 PENDING 요청이 있으면 중복 요청 불가
		boolean hasPendingRequest = guardianRequestRepository
			.existsByRequesterIdAndGuardianIdAndStatus(
				requester.getId(), guardian.getId(), RequestStatus.PENDING);

		if (hasPendingRequest) {
			throw new BaseException(ErrorCode.GUARDIAN_REQUEST_DUPLICATE);
		}
	}
}
