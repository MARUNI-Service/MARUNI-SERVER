package com.anyang.maruni.domain.auth.infrastructure;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.test.context.TestComponent;

import com.anyang.maruni.domain.auth.domain.repository.TokenBlacklistRepository;

/**
 * 테스트용 Mock 토큰 블랙리스트 저장소
 * Redis 대신 메모리를 사용한 간단한 구현
 */
@TestComponent
public class MockTokenBlacklistRepository implements TokenBlacklistRepository {

    private final ConcurrentMap<String, String> blacklistedTokens = new ConcurrentHashMap<>();

    @Override
    public void addToBlacklist(String accessToken, long expirationMillis) {
        blacklistedTokens.put(accessToken, "revoked");
        System.out.println("Token added to mock blacklist: " + maskToken(accessToken));
    }

    @Override
    public boolean isTokenBlacklisted(String accessToken) {
        boolean isBlacklisted = blacklistedTokens.containsKey(accessToken);
        System.out.println("Mock blacklist check for token: " + maskToken(accessToken) + " - Result: " + isBlacklisted);
        return isBlacklisted;
    }

    /**
     * 테스트용 헬퍼 메소드: 블랙리스트 초기화
     */
    public void clear() {
        blacklistedTokens.clear();
        System.out.println("Mock blacklist cleared");
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