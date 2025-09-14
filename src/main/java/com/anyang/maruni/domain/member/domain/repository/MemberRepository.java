package com.anyang.maruni.domain.member.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.oauth2.domain.entity.SocialType;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

	Optional<MemberEntity> findByMemberEmail(String memberEmail);


	Boolean existsByMemberEmail(String memberEmail);

	Optional<MemberEntity> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

	/**
	 * 활성 상태인 모든 회원의 ID 목록 조회
	 * Phase 2: DailyCheckService에서 안부 메시지 발송 대상 조회용
	 */
	@Query("SELECT m.id FROM MemberEntity m WHERE m.memberRole = 'USER'")
	List<Long> findActiveMemberIds();
}