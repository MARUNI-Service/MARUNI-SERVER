package com.anyang.maruni.domain.auth.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.auth.domain.entity.RefreshToken;
import com.anyang.maruni.domain.auth.domain.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token 도메인 서비스
 * 단순 CRUD를 넘어선 도메인 로직을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * 새로운 Refresh Token 저장 또는 기존 토큰 갱신
	 */
	public RefreshToken saveOrUpdateToken(String memberId, String token, Long ttlSeconds) {
		RefreshToken refreshToken = new RefreshToken(memberId, token, ttlSeconds);
		RefreshToken saved = refreshTokenRepository.save(refreshToken);
		log.debug("Refresh token saved for member: {} with TTL: {}s", memberId, ttlSeconds);
		return saved;
	}

	/**
	 * 회원의 Refresh Token 조회
	 */
	public Optional<RefreshToken> findTokenByMemberId(String memberId) {
		return refreshTokenRepository.findById(memberId);
	}

	/**
	 * 회원의 Refresh Token 존재 여부 확인
	 */
	public boolean existsTokenForMember(String memberId) {
		return refreshTokenRepository.existsById(memberId);
	}

	/**
	 * 회원의 Refresh Token 삭제
	 */
	public void revokeToken(String memberId) {
		if (refreshTokenRepository.existsById(memberId)) {
			refreshTokenRepository.deleteById(memberId);
			log.info("Refresh token revoked for member: {}", memberId);
		} else {
			log.warn("Attempted to revoke non-existent refresh token for member: {}", memberId);
		}
	}

	/**
	 * 특정 토큰이 회원의 유효한 토큰인지 확인
	 */
	public boolean isValidTokenForMember(String memberId, String token) {
		return findTokenByMemberId(memberId)
			.map(refreshToken -> refreshToken.getToken().equals(token))
			.orElse(false);
	}
}