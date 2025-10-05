package com.anyang.maruni.global.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.auth.domain.service.TokenService;
import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService implements TokenService {

	private final JWTUtil jwtUtil;

	public void issueTokens(HttpServletResponse response, MemberTokenInfo memberInfo) {
		// Access Token만 발급 (단순화)
		String accessToken = jwtUtil.createAccessToken(memberInfo.memberId(), memberInfo.email());
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		log.info("✅ Access Token 발급 완료 - Member: {}", memberInfo.email());
	}

	public void reissueAccessToken(HttpServletResponse response, String memberId, String email) {
		// 단순화: Access Token만 재발급
		String accessToken = jwtUtil.createAccessToken(memberId, email);
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		log.info("✅ Access Token 재발급 완료 - Member: {}", email);
	}

	public void reissueAllTokens(HttpServletResponse response, String memberId, String email) {
		// reissueAccessToken과 동일하게 동작
		reissueAccessToken(response, memberId, email);
	}

	public void expireRefreshCookie(HttpServletResponse response) {
		// Refresh Token 제거됨 - 아무 작업 불필요
		log.debug("Refresh cookie expiration called (no-op)");
	}
}
