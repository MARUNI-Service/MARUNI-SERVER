package com.anyang.maruni.domain.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "토큰 응답")
public class TokenResponse {

	@Schema(description = "Access Token", example = "Bearer eyJhbGciOiJIUzI1NiIs...")
	private final String accessToken;

	@Schema(description = "토큰 타입", example = "Bearer")
	private final String tokenType;

	@Schema(description = "Access Token 만료 시간 (초)", example = "3600")
	private final Long expiresIn;

	@Schema(description = "Refresh Token 포함 여부", example = "true")
	private final boolean refreshTokenIncluded;

	public static TokenResponse accessOnly(String accessToken, Long expiresIn) {
		return TokenResponse.builder()
			.accessToken(accessToken)
			.tokenType("Bearer")
			.expiresIn(expiresIn)
			.refreshTokenIncluded(false)
			.build();
	}

	public static TokenResponse withRefresh(String accessToken, Long expiresIn) {
		return TokenResponse.builder()
			.accessToken(accessToken)
			.tokenType("Bearer")
			.expiresIn(expiresIn)
			.refreshTokenIncluded(true)
			.build();
	}
}
