package com.anyang.maruni.domain.auth.application.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String BLACKLIST_PREFIX = "blacklist:";

	/**
	 * Access Token을 블랙리스트에 추가
	 */
	public void addToBlacklist(String accessToken, long expirationMillis) {
		String key = BLACKLIST_PREFIX + accessToken;
		redisTemplate.opsForValue().set(key, "logout", Duration.ofMillis(expirationMillis));
		log.info("블랙리스트 등록: {}, 유효 시간(ms): {}", key, expirationMillis);
	}

	/**
	 * Access Token이 블랙리스트에 존재하는지 확인
	 */
	public boolean isBlacklisted(String accessToken) {
		String key = BLACKLIST_PREFIX + accessToken;
		boolean result = redisTemplate.hasKey(key);
		log.debug("블랙리스트 조회: {}, 결과: {}", key, result);
		return result;
	}
}