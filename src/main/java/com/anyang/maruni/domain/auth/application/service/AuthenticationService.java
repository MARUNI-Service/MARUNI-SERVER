package com.anyang.maruni.domain.auth.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.auth.application.dto.response.TokenResponse;
import com.anyang.maruni.domain.auth.domain.service.RefreshTokenDomainService;
import com.anyang.maruni.domain.auth.infrastructure.BlacklistTokenStorage;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.security.JWTUtil;
import com.anyang.maruni.global.security.JwtTokenService;
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

	private final JWTUtil jwtUtil;
	private final JwtTokenService jwtTokenService;
	private final TokenValidator tokenValidator;
	private final RefreshTokenDomainService refreshTokenService;
	private final BlacklistTokenStorage blacklistTokenStorage;

	public void issueTokensOnLogin(HttpServletResponse response, MemberEntity member) {
		log.info("Issuing tokens for member: {}", member.getMemberEmail());
		jwtTokenService.issueTokens(response, member);
	}

	public TokenResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		jwtTokenService.reissueAccessToken(response, validation.getMemberId(), validation.getEmail());

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
		jwtTokenService.reissueAllTokens(response, validation.getMemberId(), validation.getEmail());

		log.info("Full token refresh completed for member: {}", validation.getMemberId());
		return TokenResponse.withRefresh(
			extractAccessTokenFromResponse(response),
			getAccessTokenExpirySeconds()
		);
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		jwtUtil.extractRefreshToken(request)
			.filter(jwtUtil::isRefreshToken)
			.flatMap(jwtUtil::getId)
			.ifPresent(memberId -> {
				refreshTokenService.revokeToken(memberId);
				log.info("Refresh token deleted for member: {}", memberId);
			});

		jwtTokenService.expireRefreshCookie(response);

		jwtUtil.extractAccessToken(request)
			.filter(jwtUtil::isAccessToken)
			.ifPresent(accessToken -> {
				jwtUtil.getExpiration(accessToken).ifPresent(expiration ->
					blacklistTokenStorage.addToBlacklist(accessToken, expiration)
				);
			});

		log.info("Logout completed");
	}


	private String extractRefreshTokenFromRequest(HttpServletRequest request) {
		return jwtUtil.extractRefreshToken(request)
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
		return jwtUtil.getAccessTokenExpiration() / 1000;
	}
}
