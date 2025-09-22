package com.anyang.maruni.domain.alertrule.domain.repository;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AlertRule Repository
 *
 * 알림 규칙 데이터 액세스를 담당합니다.
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {


    /**
     * 특정 알림 규칙을 Member, Guardian과 함께 조회 (N+1 방지)
     * @param alertRuleId 알림 규칙 ID
     * @return 알림 규칙 (Member, Guardian 포함)
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "JOIN FETCH ar.member m " +
           "LEFT JOIN FETCH m.guardian " +
           "WHERE ar.id = :alertRuleId")
    Optional<AlertRule> findByIdWithMemberAndGuardian(@Param("alertRuleId") Long alertRuleId);

    /**
     * Member, Guardian을 JOIN FETCH하여 조회 (정렬 없음)
     * Service 계층에서 Java로 정렬 처리
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록 (정렬 없음)
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "JOIN FETCH ar.member m " +
           "LEFT JOIN FETCH m.guardian " +
           "WHERE ar.member.id = :memberId AND ar.isActive = true")
    List<AlertRule> findActiveRulesWithMemberAndGuardian(@Param("memberId") Long memberId);
}