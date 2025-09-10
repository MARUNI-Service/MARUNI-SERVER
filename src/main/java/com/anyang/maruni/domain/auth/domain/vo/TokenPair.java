package com.anyang.maruni.domain.auth.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Access Token과 Refresh Token 쌍을 나타내는 값 객체
 * 토큰들의 응집성과 불변성을 보장
 */
@Getter
@RequiredArgsConstructor
public class TokenPair {

	private final String accessToken;
	private final String refreshToken;
	private final long accessTokenExpirySeconds;

	public static TokenPair of(String accessToken, String refreshToken, long accessTokenExpirySeconds) {
		return new TokenPair(accessToken, refreshToken, accessTokenExpirySeconds);
	}

	public static TokenPair accessOnly(String accessToken, long accessTokenExpirySeconds) {
		return new TokenPair(accessToken, null, accessTokenExpirySeconds);
	}

	public boolean hasRefreshToken() {
		return refreshToken != null && !refreshToken.trim().isEmpty();
	}

	public boolean isComplete() {
		return accessToken != null && refreshToken != null 
			&& !accessToken.trim().isEmpty() && !refreshToken.trim().isEmpty();
	}
}