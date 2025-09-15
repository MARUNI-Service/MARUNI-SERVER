package com.anyang.maruni.domain.member.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
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
	 * 현재는 모든 회원을 활성 상태로 간주
	 */
	@Query("SELECT m.id FROM MemberEntity m")
	List<Long> findActiveMemberIds();

	// Guardian 관련 메서드 추가
	List<MemberEntity> findByGuardian(GuardianEntity guardian);

	List<MemberEntity> findByGuardianIsNull();

	@Query("SELECT m.id FROM MemberEntity m WHERE m.guardian.id = :guardianId")
	List<Long> findMemberIdsByGuardianId(@Param("guardianId") Long guardianId);
}