package com.anyang.maruni.domain.guardian.presentation.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestResponse;
import com.anyang.maruni.domain.guardian.application.service.GuardianRelationService;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetails;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Guardian 관계 관리 API Controller
 *
 * 보호자 요청/수락/거절/해제 REST API를 제공합니다.
 * User Journey 3-4 기반
 */
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Guardian 관계 관리 API", description = "보호자 요청/수락/거절/해제 API")
public class GuardianRelationController {

	private final GuardianRelationService guardianRelationService;

	/**
	 * 보호자 요청 생성
	 *
	 * User Journey 3: 김순자 → 김영희에게 보호자 요청
	 *
	 * @param requester 요청자 (JWT 인증된 회원)
	 * @param request 보호자 요청 정보 (guardianId, relation)
	 * @return GuardianRequestResponse (생성된 요청 정보)
	 */
	@Operation(
		summary = "보호자 요청 생성",
		description = "특정 회원에게 보호자 등록을 요청합니다. 요청자의 JWT 토큰 필요."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 생성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 보호자 있음, 중복 요청 등)"),
		@ApiResponse(responseCode = "404", description = "보호자 회원을 찾을 수 없음")
	})
	@PostMapping("/requests")
	@SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUEST_CREATED)
	public GuardianRequestResponse sendRequest(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody GuardianRequestDto request
	) {
		return guardianRelationService.sendRequest(
			userDetails.getMemberId(),
			request.getGuardianId(),
			request.getRelation()
		);
	}

	/**
	 * 내가 받은 보호자 요청 목록 조회
	 *
	 * User Journey 4 Phase 3: 김영희가 받은 요청 확인
	 *
	 * @param guardian 보호자 (JWT 인증된 회원)
	 * @return GuardianRequestResponse 목록 (PENDING 상태만)
	 */
	@Operation(
		summary = "내가 받은 보호자 요청 목록 조회",
		description = "현재 로그인한 회원에게 온 PENDING 상태의 보호자 요청 목록을 조회합니다."
	)
	@GetMapping("/requests")
	@SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUESTS_VIEW)
	public List<GuardianRequestResponse> getReceivedRequests(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return guardianRelationService.getReceivedRequests(userDetails.getMemberId());
	}

	/**
	 * 보호자 요청 수락
	 *
	 * User Journey 4 Phase 4: 김영희가 요청 수락
	 *
	 * @param requestId 요청 ID
	 * @param guardian 보호자 (JWT 인증된 회원)
	 */
	@Operation(
		summary = "보호자 요청 수락",
		description = "받은 보호자 요청을 수락하고 보호자-회원 관계를 설정합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 수락 성공"),
		@ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음"),
		@ApiResponse(responseCode = "400", description = "이미 처리된 요청")
	})
	@PostMapping("/requests/{requestId}/accept")
	@SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUEST_ACCEPTED)
	public void acceptRequest(
		@PathVariable Long requestId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		guardianRelationService.acceptRequest(requestId, userDetails.getMemberId());
	}

	/**
	 * 보호자 요청 거절
	 *
	 * @param requestId 요청 ID
	 * @param guardian 보호자 (JWT 인증된 회원)
	 */
	@Operation(
		summary = "보호자 요청 거절",
		description = "받은 보호자 요청을 거절합니다."
	)
	@PostMapping("/requests/{requestId}/reject")
	@SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUEST_REJECTED)
	public void rejectRequest(
		@PathVariable Long requestId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		guardianRelationService.rejectRequest(requestId, userDetails.getMemberId());
	}
}
