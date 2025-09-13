package com.anyang.maruni.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.anyang.maruni.domain.auth.domain.repository.TokenBlacklistRepository;
import com.anyang.maruni.domain.auth.domain.repository.RefreshTokenRepository;
import com.anyang.maruni.domain.auth.infrastructure.MockTokenBlacklistRepository;

import org.mockito.Mockito;

/**
 * 테스트 환경용 Redis 설정
 * 실제 Redis 대신 Mock 구현체를 사용
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public TokenBlacklistRepository tokenBlacklistRepository() {
        return new MockTokenBlacklistRepository();
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public RefreshTokenRepository refreshTokenRepository() {
        return Mockito.mock(RefreshTokenRepository.class);
    }
}