package com.anyang.maruni.domain.auth.domain.vo;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 토큰 발급에 필요한 회원 정보를 담는 Value Object
 * Member 도메인과 Auth 도메인 간의 의존성을 분리
 */
@Getter
@RequiredArgsConstructor
public class MemberTokenInfo {
    
    private final String memberId;
    private final String email;
    
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
    
    /**
     * MemberEntity로부터 생성 (편의 메소드)
     */
    public static MemberTokenInfo from(MemberEntity member) {
        return new MemberTokenInfo(
            member.getId().toString(),
            member.getMemberEmail()
        );
    }
}