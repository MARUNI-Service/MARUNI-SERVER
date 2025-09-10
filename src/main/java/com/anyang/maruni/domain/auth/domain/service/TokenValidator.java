package com.anyang.maruni.domain.auth.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.auth.domain.repository.TokenBlacklistRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 검증을 담당하는 컴포넌트
 * 재사용 가능한 토큰 검증 로직을 중앙화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidator {

	private final TokenManager tokenManager;
	private final RefreshTokenService refreshTokenService;
	private final TokenBlacklistRepository tokenBlacklistRepository;

	/**
	 * Refresh Token의 종합적 검증
	 */
	public TokenValidationResult validateRefreshToken(String refreshToken) {
		// 1. JWT 형식 및 만료시간 검증
		if (!tokenManager.isRefreshToken(refreshToken)) {
			log.warn("Invalid refresh token format or expired");
			return TokenValidationResult.invalid("Invalid or expired refresh token");
		}

		// 2. 토큰에서 사용자 ID 추출
		Optional<String> memberIdOpt = tokenManager.getId(refreshToken);
		if (memberIdOpt.isEmpty()) {
			log.warn("Cannot extract member ID from refresh token");
			return TokenValidationResult.invalid("Invalid token payload");
		}

		String memberId = memberIdOpt.get();

		// 3. 도메인 서비스를 통한 토큰 일치 여부 확인
		if (!refreshTokenService.isValidTokenForMember(memberId, refreshToken)) {
			log.warn("Refresh token not found or mismatched for member: {}", memberId);
			return TokenValidationResult.invalid("Token not found or mismatched");
		}

		// 4. 토큰에서 추가 정보 추출
		Optional<String> emailOpt = tokenManager.getEmail(refreshToken);

		if (emailOpt.isEmpty()) {
			log.warn("Cannot extract email or role from refresh token");
			return TokenValidationResult.invalid("Invalid token claims");
		}

		log.debug("Refresh token validation successful for member: {}", memberId);
		return TokenValidationResult.valid(memberId, emailOpt.get());
	}

	/**
	 * Access Token의 블랙리스트 및 만료 검증
	 */
	public boolean isValidAccessToken(String accessToken) {
		// 1. JWT 형식 및 만료 시간 검증
		if (!tokenManager.isAccessToken(accessToken)) {
			return false;
		}
		
		// 2. 블랙리스트 확인
		return !tokenBlacklistRepository.isTokenBlacklisted(accessToken);
	}

	/**
	 * 토큰 검증 결과를 캡슐화하는 클래스
	 */
	@Getter
	public static class TokenValidationResult {
		private final boolean valid;
		private final String memberId;
		private final String email;
		private final String errorMessage;

		private TokenValidationResult(boolean valid, String memberId, String email, String errorMessage) {
			this.valid = valid;
			this.memberId = memberId;
			this.email = email;
			this.errorMessage = errorMessage;
		}

		public static TokenValidationResult valid(String memberId, String email) {
			return new TokenValidationResult(true, memberId, email, null);
		}

		public static TokenValidationResult invalid(String errorMessage) {
			return new TokenValidationResult(false, null, null, errorMessage);
		}

	}
}
