package com.anyang.maruni.domain.auth.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.auth.application.dto.response.TokenResponse;
import com.anyang.maruni.domain.auth.domain.service.TokenManager;
import com.anyang.maruni.domain.auth.domain.service.TokenService;
import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.anyang.maruni.global.security.AuthenticationEventHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService implements AuthenticationEventHandler {

	private final TokenManager tokenManager;
	private final TokenService tokenService;

	@Override
	public void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo) {
		// Access Token만 발급 (단순화)
		String accessToken = tokenManager.createAccessToken(
			memberInfo.memberId(),
			memberInfo.email()
		);

		response.setHeader("Authorization", "Bearer " + accessToken);
		response.setContentType("application/json; charset=UTF-8");

		log.info("✅ Access Token 발급 완료 - Member: {}", memberInfo.email());
	}


	public void logout(HttpServletRequest request, HttpServletResponse response) {
		// Access Token은 stateless하므로 서버에서 별도 무효화 처리 불필요
		// 클라이언트에서 토큰 삭제로 로그아웃 처리
		log.info("Logout completed - client should remove token");
	}
}
