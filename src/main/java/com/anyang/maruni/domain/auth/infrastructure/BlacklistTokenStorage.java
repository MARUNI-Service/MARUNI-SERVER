package com.anyang.maruni.domain.auth.infrastructure;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.auth.domain.repository.TokenBlacklistRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 블랙리스트 토큰 저장소 - Infrastructure 계층
 * Redis를 통한 토큰 블랙리스트 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistTokenStorage implements TokenBlacklistRepository {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String BLACKLIST_PREFIX = "blacklist:token:";

	/**
	 * Access Token을 블랙리스트에 추가
	 * @param accessToken 블랙리스트에 추가할 토큰
	 * @param expirationMillis 토큰 만료까지 남은 시간 (밀리초)
	 */
	public void addToBlacklist(String accessToken, long expirationMillis) {
		String key = BLACKLIST_PREFIX + accessToken;
		redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(expirationMillis));
		log.info("Token added to blacklist with expiry {}ms: {}", expirationMillis, maskToken(accessToken));
	}

	/**
	 * Access Token이 블랙리스트에 존재하는지 확인
	 * @param accessToken 확인할 토큰
	 * @return 블랙리스트 존재 여부
	 */
	public boolean isTokenBlacklisted(String accessToken) {
		String key = BLACKLIST_PREFIX + accessToken;
		boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(key));
		log.debug("Blacklist check for token: {} - Result: {}", maskToken(accessToken), isBlacklisted);
		return isBlacklisted;
	}

	/**
	 * 토큰을 마스킹하여 로그에 안전하게 출력
	 */
	private String maskToken(String token) {
		if (token == null || token.length() < 10) {
			return "***";
		}
		return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
	}
}