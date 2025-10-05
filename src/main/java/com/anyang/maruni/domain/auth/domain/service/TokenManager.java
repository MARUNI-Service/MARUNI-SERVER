package com.anyang.maruni.domain.auth.domain.service;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

/**
 * JWT 토큰 관리를 위한 인터페이스
 * Auth 도메인과 Global JWT 구현체 간의 의존성을 분리
 * (단순화: Access Token만 사용)
 */
public interface TokenManager {

    // 토큰 생성
    String createAccessToken(String memberId, String email);

    // 토큰 추출
    Optional<String> extractAccessToken(HttpServletRequest request);

    // 토큰 정보 추출
    Optional<String> getEmail(String token);

    // 토큰 검증
    boolean isAccessToken(String token);
}