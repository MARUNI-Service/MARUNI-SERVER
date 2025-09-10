package com.anyang.maruni.global.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.anyang.maruni.domain.member.application.dto.request.MemberLoginRequest;
import com.anyang.maruni.domain.auth.application.service.AuthenticationService;
import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

	private final AuthenticationManager authenticationManager;
	private final ObjectMapper objectMapper;
	private final AuthenticationService authenticationService;


	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
		try {
			var loginReq = objectMapper.readValue(request.getInputStream(), MemberLoginRequest.class);

			var authToken = new UsernamePasswordAuthenticationToken(
				loginReq.getMemberEmail(), loginReq.getMemberPassword()
			);

			return authenticationManager.authenticate(authToken);
		} catch (IOException e) {
			throw new RuntimeException("로그인 요청 처리 실패", e);
		}
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
		FilterChain chain, Authentication authentication) {

		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

		MemberTokenInfo memberTokenInfo = MemberTokenInfo.from(customUserDetails.getMember());
		authenticationService.issueTokensOnLogin(response, memberTokenInfo);

		log.info("로그인 성공 - 사용자: {}", customUserDetails.getUsername());

	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
		response.setStatus(ErrorCode.LOGIN_FAIL.getStatus());
		response.setContentType("application/json; charset=UTF-8");

		String json = objectMapper.writeValueAsString(CommonApiResponse.fail(ErrorCode.LOGIN_FAIL));
		response.getWriter().write(json);
	}
}