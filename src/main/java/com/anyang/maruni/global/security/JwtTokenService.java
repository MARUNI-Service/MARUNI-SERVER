package com.anyang.maruni.global.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.auth.application.service.RefreshTokenService;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

	private final JWTUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;

	public void issueTokens(HttpServletResponse response, MemberEntity member) {
		String memberId = member.getId().toString();
		String accessToken = jwtUtil.createAccessToken(memberId, member.getMemberEmail());
		String refreshToken = jwtUtil.createRefreshToken(memberId, member.getMemberEmail());

		refreshTokenService.saveToken(memberId, refreshToken);

		setAccessToken(response, accessToken);
		setRefreshCookie(response, refreshToken);

		log.info("Access / Refresh 토큰 발급 완료 - Member: {}", member.getMemberEmail());
	}

	public void reissueAccessToken(HttpServletResponse response, String memberId, String email) {
		String accessToken = jwtUtil.createAccessToken(memberId, email);
		setAccessToken(response, accessToken);
		log.info("Access 토큰 재발급 완료 - Member: {}", email);
	}

	public void reissueAllTokens(HttpServletResponse response, String memberId, String email) {
		String accessToken = jwtUtil.createAccessToken(memberId, email);
		String refreshToken = jwtUtil.createRefreshToken(memberId, email);

		refreshTokenService.saveToken(memberId, refreshToken);

		setAccessToken(response, accessToken);
		setRefreshCookie(response, refreshToken);

		log.info("Access / Refresh 토큰 모두 재발급 완료 - Member: {}", email);
	}

	public void expireRefreshCookie(HttpServletResponse response) {
		ResponseCookie expired = jwtUtil.invalidateRefreshToken();
		response.setHeader(HttpHeaders.SET_COOKIE, expired.toString());
	}

	private void setAccessToken(HttpServletResponse response, String accessToken) {
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
	}

	private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = jwtUtil.createRefreshTokenCookie(refreshToken);
		response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
