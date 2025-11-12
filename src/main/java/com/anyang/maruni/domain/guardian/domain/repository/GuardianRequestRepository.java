package com.anyang.maruni.domain.guardian.domain.repository;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRequest;
import com.anyang.maruni.domain.guardian.domain.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * GuardianRequest 리포지토리
 *
 * 보호자 요청 데이터 관리를 담당합니다.
 */
public interface GuardianRequestRepository extends JpaRepository<GuardianRequest, Long> {

	/**
	 * 보호자에게 온 요청 목록 조회 (상태별 필터링, 최신순)
	 *
	 * @param guardianId 보호자 회원 ID
	 * @param status 요청 상태 (PENDING, ACCEPTED, REJECTED)
	 * @return 조건에 맞는 GuardianRequest 목록 (최신순)
	 */
	@Query("SELECT gr FROM GuardianRequest gr " +
		"WHERE gr.guardian.id = :guardianId " +
		"AND gr.status = :status " +
		"ORDER BY gr.createdAt DESC")
	List<GuardianRequest> findByGuardianIdAndStatus(
		@Param("guardianId") Long guardianId,
		@Param("status") RequestStatus status
	);

	/**
	 * 특정 요청자가 특정 보호자에게 보낸 특정 상태의 요청 존재 여부 확인
	 *
	 * 중복 요청 방지용: 이미 PENDING 상태의 요청이 있으면 추가 요청 불가
	 *
	 * @param requesterId 요청자 회원 ID
	 * @param guardianId 보호자 회원 ID
	 * @param status 요청 상태
	 * @return 존재하면 true, 없으면 false
	 */
	@Query("SELECT CASE WHEN COUNT(gr) > 0 THEN true ELSE false END " +
		"FROM GuardianRequest gr " +
		"WHERE gr.requester.id = :requesterId " +
		"AND gr.guardian.id = :guardianId " +
		"AND gr.status = :status")
	boolean existsByRequesterIdAndGuardianIdAndStatus(
		@Param("requesterId") Long requesterId,
		@Param("guardianId") Long guardianId,
		@Param("status") RequestStatus status
	);

	/**
	 * 요청자가 보낸 모든 요청 조회 (최신순)
	 *
	 * @param requesterId 요청자 회원 ID
	 * @return 요청자가 보낸 모든 GuardianRequest 목록 (최신순)
	 */
	List<GuardianRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

	/**
	 * ID와 보호자 ID로 요청 조회 (권한 검증용)
	 *
	 * 보호자가 자신에게 온 요청만 조회/수락/거절할 수 있도록 보장합니다.
	 *
	 * @param id GuardianRequest ID
	 * @param guardianId 보호자 회원 ID
	 * @return 조건에 맞는 GuardianRequest (Optional)
	 */
	Optional<GuardianRequest> findByIdAndGuardianId(Long id, Long guardianId);

	/**
	 * 특정 요청자가 특정 보호자에게 보낸 특정 상태의 요청 조회
	 *
	 * 거절된 요청 삭제용: REJECTED 상태의 요청을 찾아서 삭제합니다.
	 *
	 * @param requesterId 요청자 회원 ID
	 * @param guardianId 보호자 회원 ID
	 * @param status 요청 상태
	 * @return 조건에 맞는 GuardianRequest (Optional)
	 */
	Optional<GuardianRequest> findByRequesterIdAndGuardianIdAndStatus(
		Long requesterId,
		Long guardianId,
		RequestStatus status
	);
}
