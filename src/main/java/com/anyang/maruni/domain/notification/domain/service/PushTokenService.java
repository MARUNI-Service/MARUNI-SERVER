package com.anyang.maruni.domain.notification.domain.service;

/**
 * 푸시 토큰 조회 도메인 서비스 인터페이스
 *
 * 회원별 푸시 토큰 조회 로직을 캡슐화합니다.
 * DDD 원칙에 따라 도메인 계층에서 인터페이스를 정의합니다.
 */
public interface PushTokenService {

    /**
     * 회원 ID로 푸시 토큰 조회
     *
     * @param memberId 회원 ID
     * @return 푸시 토큰 (토큰이 없으면 null)
     * @throws IllegalArgumentException 유효하지 않은 회원 ID
     */
    String getPushTokenByMemberId(Long memberId);

    /**
     * 회원이 유효한 푸시 토큰을 가지고 있는지 확인
     *
     * @param memberId 회원 ID
     * @return 유효한 푸시 토큰 보유 여부
     */
    boolean hasPushToken(Long memberId);
}