package com.anyang.maruni.domain.auth.domain.repository;

/**
 * 토큰 블랙리스트 관리를 위한 도메인 인터페이스
 * Infrastructure 구현체와 도메인 로직을 분리하여 의존성 역전 원칙 적용
 */
public interface TokenBlacklistRepository {
    
    /**
     * Access Token을 블랙리스트에 추가
     * @param accessToken 블랙리스트에 추가할 토큰
     * @param expirationMillis 토큰 만료까지 남은 시간 (밀리초)
     */
    void addToBlacklist(String accessToken, long expirationMillis);
    
    /**
     * Access Token이 블랙리스트에 존재하는지 확인
     * @param accessToken 확인할 토큰
     * @return 블랙리스트 존재 여부
     */
    boolean isTokenBlacklisted(String accessToken);
}