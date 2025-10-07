package com.anyang.maruni.domain.member.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

	// ========== 기존 메서드 (재사용) ==========

	Optional<MemberEntity> findByMemberEmail(String memberEmail);

	Boolean existsByMemberEmail(String memberEmail);

	// ========== 신규 메서드 (Phase 1) ==========

	/**
	 * 안부 메시지 수신자 ID 목록 조회 (DailyCheck용)
	 */
	@Query("SELECT m.id FROM MemberEntity m WHERE m.dailyCheckEnabled = true")
	List<Long> findDailyCheckEnabledMemberIds();

	/**
	 * 보호자의 돌봄 대상 조회 (자기 참조)
	 * @param guardian 보호자 MemberEntity
	 * @return 돌봄 대상 목록
	 */
	@Query("SELECT m FROM MemberEntity m WHERE m.guardian = :guardian")
	List<MemberEntity> findByGuardian(@Param("guardian") MemberEntity guardian);

	/**
	 * 회원 검색 (이메일 기반)
	 * @param email 검색할 이메일
	 * @return 회원 정보
	 */
	@Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :email")
	Optional<MemberEntity> searchByEmail(@Param("email") String email);

	/**
	 * 안부 메시지 수신자 전체 조회 (엔티티)
	 */
	@Query("SELECT m FROM MemberEntity m WHERE m.dailyCheckEnabled = true")
	List<MemberEntity> findAllByDailyCheckEnabled();

	/**
	 * 보호자 ID로 돌봄 대상 조회 (성능 최적화)
	 */
	@Query("SELECT m FROM MemberEntity m WHERE m.guardian.id = :guardianId")
	List<MemberEntity> findManagedMembersByGuardianId(@Param("guardianId") Long guardianId);

	/**
	 * 보호자가 없는 회원 조회
	 */
	List<MemberEntity> findByGuardianIsNull();
}