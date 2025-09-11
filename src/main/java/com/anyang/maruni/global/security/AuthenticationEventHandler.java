package com.anyang.maruni.global.security;

import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 인증 이벤트 처리를 위한 인터페이스
 * Global 계층과 Auth 도메인 간의 의존성을 분리하여 DDD 원칙 준수
 * 
 * 의존성 방향: Domain → Global Interface (의존성 역전)
 */
public interface AuthenticationEventHandler {
    
    /**
     * 로그인 성공 시 토큰 발급 처리
     * 
     * @param response HTTP 응답 객체 (토큰을 헤더/쿠키에 설정)
     * @param memberInfo 토큰 발급에 필요한 회원 정보
     */
    void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo);
}