package com.anyang.maruni.domain.auth.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.auth.application.dto.response.TokenResponse;
import com.anyang.maruni.domain.auth.application.service.AuthenticationService;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "인증 API", description = "JWT 토큰 관리")
public class AuthApiController {

	private final AuthenticationService authenticationService;

	@Operation(summary = "Access Token 재발급")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공"),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
	})
	@PostMapping("/token/refresh")
	@CustomExceptionDescription(SwaggerResponseDescription.AUTH_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_TOKEN_REISSUE_SUCCESS)
	public TokenResponse refreshAccessToken(
		HttpServletRequest request,
		HttpServletResponse response) {

		return authenticationService.refreshAccessToken(request, response);
	}

	@Operation(summary = "Access + Refresh Token 모두 재발급")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공"),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
	})
	@PostMapping("/token/refresh/full")
	@CustomExceptionDescription(SwaggerResponseDescription.AUTH_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_TOKEN_REISSUE_FULL_SUCCESS)
	public TokenResponse refreshAllTokens(
		HttpServletRequest request,
		HttpServletResponse response) {

		return authenticationService.refreshAllTokens(request, response);
	}

	@Operation(summary = "로그아웃")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공")
	})
	@PostMapping("/logout")
	@CustomExceptionDescription(SwaggerResponseDescription.AUTH_ERROR)
	@SuccessCodeAnnotation(SuccessCode.MEMBER_LOGOUT_SUCCESS)
	public CommonApiResponse<Void> logout(
		HttpServletRequest request,
		HttpServletResponse response) {

		authenticationService.logout(request, response);
		return CommonApiResponse.success(SuccessCode.MEMBER_LOGOUT_SUCCESS);
	}
}