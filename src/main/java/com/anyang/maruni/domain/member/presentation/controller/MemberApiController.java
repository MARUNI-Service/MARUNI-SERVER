package com.anyang.maruni.domain.member.presentation.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.member.application.dto.request.DailyCheckUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.request.MemberUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetails;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "JWT 기반 본인 정보 관리 및 검색 API")
public class MemberApiController {

	private final MemberService memberService;

	// ========== 신규 API (Phase 1) ==========

	// 회원 검색 (이메일 기반)
	@Operation(
		summary = "회원 검색",
		description = "이메일 주소로 회원을 검색합니다. 보호자 등록 시 사용합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "검색 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "해당 이메일의 사용자가 존재하지 않음", content = @Content)
	})
	@GetMapping("/search")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
	public MemberResponse searchByEmail(
			@Parameter(description = "검색할 이메일 주소", required = true, example = "elderly@example.com")
			@RequestParam String email,
			@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return memberService.searchByEmail(email);
	}

	// 내 정보 조회 (역할 정보 포함)
	@Operation(
		summary = "내 정보 조회",
		description = "JWT 토큰을 기반으로 본인의 회원 정보를 조회합니다. 보호자 정보 및 내가 돌보는 사람들 정보를 포함합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자가 존재하지 않음", content = @Content)
	})
	@GetMapping("/me")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
	public MemberResponse getMyInfo(
			@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return memberService.getMyProfile(userDetails.getMemberId());
	}

	// 내가 돌보는 사람들 목록 조회
	@Operation(
		summary = "내가 돌보는 사람들 목록 조회",
		description = "내가 보호자로 등록된 회원들의 목록을 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자가 존재하지 않음", content = @Content)
	})
	@GetMapping("/me/managed-members")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
	public List<MemberResponse> getManagedMembers(
			@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return memberService.getManagedMembers(userDetails.getMemberId());
	}

	// 안부 메시지 설정 변경
	@Operation(
		summary = "안부 메시지 설정 변경",
		description = "매일 안부 메시지 수신 여부를 변경합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "설정 변경 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 실패", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자가 존재하지 않음", content = @Content)
	})
	@PatchMapping("/me/daily-check")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
	public MemberResponse updateDailyCheckEnabled(
			@Valid @RequestBody DailyCheckUpdateRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return memberService.updateDailyCheckEnabled(
				userDetails.getMemberId(),
				request.getDailyCheckEnabled()
		);
	}

	// ========== 기존 API ==========

	// 내 정보 수정
	@Operation(
		summary = "내 정보 수정",
		description = "JWT 토큰을 기반으로 본인의 회원 정보를 수정합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 실패", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자가 존재하지 않음", content = @Content)
	})
	@PutMapping("/me")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
	public MemberResponse updateMyInfo(
			@RequestBody MemberUpdateRequest req,
			@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		req.setId(userDetails.getMemberId());
		memberService.update(req);
		return memberService.findById(userDetails.getMemberId());
	}

	// 내 계정 삭제
	@Operation(
		summary = "내 계정 삭제",
		description = "JWT 토큰을 기반으로 본인의 계정을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자가 존재하지 않음", content = @Content)
	})
	@DeleteMapping("/me")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_DELETED)
	public void deleteMyAccount(
			@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		memberService.deleteById(userDetails.getMemberId());
	}

}
