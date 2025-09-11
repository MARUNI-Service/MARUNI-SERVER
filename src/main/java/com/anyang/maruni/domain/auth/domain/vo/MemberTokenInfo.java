package com.anyang.maruni.domain.auth.domain.vo;

/**
 * 토큰 발급에 필요한 회원 정보를 담는 Value Object
 * 도메인 간 의존성을 분리하여 Auth 도메인의 순수성을 보장
 */
public record MemberTokenInfo(String memberId, String email) {

    /**
     * Long 타입 memberId로 생성
     */
    public static MemberTokenInfo of(Long memberId, String email) {
        return new MemberTokenInfo(memberId.toString(), email);
    }

    /**
     * String 타입 memberId로 생성
     */
    public static MemberTokenInfo of(String memberId, String email) {
        return new MemberTokenInfo(memberId, email);
    }
}
