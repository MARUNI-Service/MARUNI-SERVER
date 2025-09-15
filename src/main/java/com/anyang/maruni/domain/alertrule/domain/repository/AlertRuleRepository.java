package com.anyang.maruni.domain.alertrule.domain.repository;

import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
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
     * 회원의 모든 활성 알림 규칙 조회
     * @param memberId 회원 ID
     * @return 활성 알림 규칙 목록
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "WHERE ar.member.id = :memberId " +
           "AND ar.isActive = true " +
           "ORDER BY " +
           "CASE ar.alertLevel " +
           "  WHEN 'EMERGENCY' THEN 4 " +
           "  WHEN 'HIGH' THEN 3 " +
           "  WHEN 'MEDIUM' THEN 2 " +
           "  WHEN 'LOW' THEN 1 " +
           "  ELSE 0 " +
           "END DESC, ar.createdAt DESC")
    List<AlertRule> findActiveRulesByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 특정 유형 활성 알림 규칙 조회
     * @param memberId 회원 ID
     * @param alertType 알림 유형
     * @return 활성 알림 규칙 목록
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "WHERE ar.member.id = :memberId " +
           "AND ar.alertType = :alertType " +
           "AND ar.isActive = true " +
           "ORDER BY ar.createdAt DESC")
    List<AlertRule> findActiveRulesByMemberIdAndType(
            @Param("memberId") Long memberId,
            @Param("alertType") AlertType alertType);

    /**
     * 특정 레벨 이상의 활성 알림 규칙 조회
     * @param alertLevel 최소 알림 레벨
     * @return 활성 알림 규칙 목록
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "WHERE ar.alertLevel = :alertLevel " +
           "AND ar.isActive = true " +
           "ORDER BY ar.createdAt DESC")
    List<AlertRule> findActiveRulesByMinLevel(@Param("alertLevel") AlertLevel alertLevel);

    /**
     * 모든 활성 감정 패턴 규칙 조회 (일일 분석용)
     * @return 감정 패턴 규칙 목록
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "WHERE ar.alertType = 'EMOTION_PATTERN' " +
           "AND ar.isActive = true " +
           "ORDER BY ar.member.id")
    List<AlertRule> findAllActiveEmotionPatternRules();

    /**
     * 모든 활성 무응답 규칙 조회 (일일 분석용)
     * @return 무응답 규칙 목록
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "WHERE ar.alertType = 'NO_RESPONSE' " +
           "AND ar.isActive = true " +
           "ORDER BY ar.member.id")
    List<AlertRule> findAllActiveNoResponseRules();

    /**
     * 회원별 알림 규칙 수 조회
     * @param memberId 회원 ID
     * @return 활성 규칙 수
     */
    @Query("SELECT COUNT(ar) FROM AlertRule ar " +
           "WHERE ar.member.id = :memberId " +
           "AND ar.isActive = true")
    long countActiveRulesByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 특정 유형 알림 규칙 존재 여부 확인
     * @param memberId 회원 ID
     * @param alertType 알림 유형
     * @return 존재 여부
     */
    boolean existsByMemberIdAndAlertTypeAndIsActiveTrue(Long memberId, AlertType alertType);

    /**
     * 회원의 활성 알림 규칙을 알림 레벨별로 그룹핑하여 조회
     * @param memberId 회원 ID
     * @return 알림 규칙 목록
     */
    @Query("SELECT ar FROM AlertRule ar " +
           "WHERE ar.member.id = :memberId " +
           "AND ar.isActive = true " +
           "ORDER BY " +
           "CASE ar.alertLevel " +
           "  WHEN 'EMERGENCY' THEN 4 " +
           "  WHEN 'HIGH' THEN 3 " +
           "  WHEN 'MEDIUM' THEN 2 " +
           "  WHEN 'LOW' THEN 1 " +
           "  ELSE 0 " +
           "END DESC, ar.alertType, ar.createdAt DESC")
    List<AlertRule> findActiveRulesByMemberIdOrderedByLevel(@Param("memberId") Long memberId);
}