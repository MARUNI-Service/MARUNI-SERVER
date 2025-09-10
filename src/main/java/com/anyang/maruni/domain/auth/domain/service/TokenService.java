package com.anyang.maruni.domain.auth.domain.service;

import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 토큰 발급 및 관리를 위한 인터페이스
 * Auth 도메인과 Global JWT Service 간의 의존성을 분리
 */
public interface TokenService {
    
    /**
     * Access Token과 Refresh Token을 모두 발급
     */
    void issueTokens(HttpServletResponse response, MemberTokenInfo memberInfo);
    
    /**
     * Access Token만 재발급
     */
    void reissueAccessToken(HttpServletResponse response, String memberId, String email);
    
    /**
     * Access Token과 Refresh Token을 모두 재발급
     */
    void reissueAllTokens(HttpServletResponse response, String memberId, String email);
    
    /**
     * Refresh Token 쿠키를 만료시킴
     */
    void expireRefreshCookie(HttpServletResponse response);
}