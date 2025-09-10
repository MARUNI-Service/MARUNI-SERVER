package com.anyang.maruni.domain.auth.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.auth.application.dto.response.TokenResponse;
import com.anyang.maruni.domain.auth.domain.service.RefreshTokenService;
import com.anyang.maruni.domain.auth.domain.service.TokenManager;
import com.anyang.maruni.domain.auth.domain.service.TokenService;
import com.anyang.maruni.domain.auth.domain.service.TokenValidator;
import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;
import com.anyang.maruni.domain.auth.domain.repository.TokenBlacklistRepository;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

	private final TokenManager tokenManager;
	private final TokenService tokenService;
	private final TokenValidator tokenValidator;
	private final RefreshTokenService refreshTokenService;
	private final TokenBlacklistRepository tokenBlacklistRepository;

	public void issueTokensOnLogin(HttpServletResponse response, MemberTokenInfo memberInfo) {
		log.info("Issuing tokens for member: {}", memberInfo.email());
		tokenService.issueTokens(response, memberInfo);
	}

	public TokenResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		tokenService.reissueAccessToken(response, validation.getMemberId(), validation.getEmail());

		log.info("Access token refreshed for member: {}", validation.getMemberId());
		return TokenResponse.accessOnly(
			extractAccessTokenFromResponse(response),
			getAccessTokenExpirySeconds()
		);
	}

	public TokenResponse refreshAllTokens(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		refreshTokenService.revokeToken(validation.getMemberId());
		tokenService.reissueAllTokens(response, validation.getMemberId(), validation.getEmail());

		log.info("Full token refresh completed for member: {}", validation.getMemberId());
		return TokenResponse.withRefresh(
			extractAccessTokenFromResponse(response),
			getAccessTokenExpirySeconds()
		);
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		tokenManager.extractRefreshToken(request)
			.filter(tokenManager::isRefreshToken)
			.flatMap(tokenManager::getId)
			.ifPresent(memberId -> {
				refreshTokenService.revokeToken(memberId);
				log.info("Refresh token deleted for member: {}", memberId);
			});

		tokenService.expireRefreshCookie(response);

		tokenManager.extractAccessToken(request)
			.filter(tokenManager::isAccessToken)
			.ifPresent(accessToken -> {
				tokenManager.getExpiration(accessToken).ifPresent(expiration ->
					tokenBlacklistRepository.addToBlacklist(accessToken, expiration)
				);
			});

		log.info("Logout completed");
	}


	private String extractRefreshTokenFromRequest(HttpServletRequest request) {
		return tokenManager.extractRefreshToken(request)
			.orElseThrow(() -> new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
	}

	private String extractAccessTokenFromResponse(HttpServletResponse response) {
		String authHeader = response.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		return null;
	}

	private Long getAccessTokenExpirySeconds() {
		return tokenManager.getAccessTokenExpiration() / 1000;
	}
}
