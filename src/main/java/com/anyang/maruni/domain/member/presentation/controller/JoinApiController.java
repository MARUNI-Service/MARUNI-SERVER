package com.anyang.maruni.domain.member.presentation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.member.application.dto.request.MemberSaveRequest;
import com.anyang.maruni.domain.member.application.dto.response.EmailCheckResponse;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/join")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원가입 API", description = "회원 등록 및 이메일 중복 확인 API")
public class JoinApiController {

	private final MemberService memberService;

	// 회원가입 요청
	@Operation(
		summary = "회원가입",
		description = "새로운 회원을 등록합니다. 이메일 중복은 허용되지 않으며, 유효성 검사 실패 시 예외를 반환합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원가입 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력값", content = @Content),
		@ApiResponse(responseCode = "409", description = "이메일 중복", content = @Content)
	})
	@PostMapping
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_JOIN_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_CREATED)
	public MemberResponse join(@RequestBody @Valid MemberSaveRequest req) {
		return memberService.save(req);
	}

	// 이메일 중복 체크
	@Operation(
		summary = "이메일 중복 확인",
		description = "회원가입 전에 이메일 중복 여부를 확인합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 사용 가능"),
		@ApiResponse(responseCode = "409", description = "이메일 중복")
	})
	@GetMapping("/email-check")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_EMAIL_CHECK_OK)
	public EmailCheckResponse emailCheck(@RequestParam String memberEmail) {
		boolean isAvailable = memberService.isEmailAvailable(memberEmail);
		if (!isAvailable) {
			throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
		}
		return EmailCheckResponse.of(memberEmail, isAvailable);
	}
}