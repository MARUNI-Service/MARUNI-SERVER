package com.anyang.maruni.domain.guardian.domain.repository;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Guardian Repository (TDD Red - 기본 구조)
 */
@Repository
public interface GuardianRepository extends JpaRepository<GuardianEntity, Long> {

    // 기본 JPA 메서드들 (자동 구현됨)
    List<GuardianEntity> findByIsActiveTrueOrderByCreatedAtDesc();

    Optional<GuardianEntity> findByGuardianEmailAndIsActiveTrue(String guardianEmail);

    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.guardian.id = :guardianId")
    long countMembersByGuardianId(@Param("guardianId") Long guardianId);
}