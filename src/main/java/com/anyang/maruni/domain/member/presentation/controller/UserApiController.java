package com.anyang.maruni.domain.member.presentation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.member.application.dto.request.MemberUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "사용자 조회, 수정, 삭제 관련 API")
public class UserApiController {

	private final MemberService memberService;

	// 전체 사용자 목록 조회
	@Operation(
		summary = "전체 사용자 목록 조회",
		description = "가입된 모든 사용자의 정보를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
	public List<MemberResponse> findAll() {
		return memberService.findAll();
	}

	// 특정 사용자 조회
	@Operation(
		summary = "특정 사용자 조회",
		description = "사용자 ID를 기준으로 회원 정보를 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "해당 사용자가 존재하지 않음", content = @Content)
	})
	@GetMapping("/{id}")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
	public MemberResponse findById(@PathVariable Long id) {
		return memberService.findById(id);
	}

	// 사용자 정보 수정

	@Operation(
		summary = "사용자 정보 수정",
		description = "요청 본문의 정보로 사용자 정보를 수정합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "해당 사용자가 존재하지 않음", content = @Content)
	})
	@PutMapping("/{id}")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
	public void update(@PathVariable Long id, @RequestBody MemberUpdateRequest req) {
		req.setId(id);
		memberService.update(req);
	}

	// 사용자 삭제
	@Operation(
		summary = "사용자 삭제",
		description = "지정한 사용자 ID에 해당하는 회원을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "해당 사용자가 존재하지 않음", content = @Content)
	})
	@DeleteMapping("/{id}")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_DELETED)
	public void delete(@PathVariable Long id) {
		memberService.deleteById(id);
	}
}
