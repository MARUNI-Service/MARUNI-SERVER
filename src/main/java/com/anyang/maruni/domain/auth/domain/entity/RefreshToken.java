package com.anyang.maruni.domain.auth.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "refreshToken", timeToLive = 1209600)
public class RefreshToken {

	@Id
	private String memberId;

	private String token;

	public RefreshToken withNewToken(String newToken) {
		return RefreshToken.builder()
			.memberId(this.memberId)
			.token(newToken)
			.build();
	}

	public boolean isValidFor(String memberId) {
		return this.memberId != null && this.memberId.equals(memberId);
	}
}
