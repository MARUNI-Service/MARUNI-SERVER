package com.anyang.maruni.domain.conversation.domain.repository;

import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 대화 Repository 인터페이스 (MVP 버전)
 * 
 * 대화 엔티티의 데이터 접근을 담당합니다.
 * MVP에서는 기본적인 CRUD와 회원별 조회 기능만 제공합니다.
 */
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    /**
     * 회원의 가장 최근 대화 조회
     * 
     * @param memberId 회원 ID
     * @return 가장 최근 대화 (Optional)
     */
    Optional<ConversationEntity> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 회원의 모든 대화 조회 (최신순)
     * 
     * @param memberId 회원 ID
     * @return 대화 목록 (최신순)
     */
    List<ConversationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}