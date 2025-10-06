package com.anyang.maruni.domain.member.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "JWT 기반 본인 정보 관리 API")
public class MemberApiController {

	private final MemberService memberService;

	// 내 정보 조회
	@Operation(
		summary = "내 정보 조회",
		description = "JWT 토큰을 기반으로 본인의 회원 정보를 조회합니다."
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
		return memberService.findById(userDetails.getMemberId());
	}

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
