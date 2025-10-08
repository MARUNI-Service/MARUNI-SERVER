package com.anyang.maruni.domain.conversation.domain.repository;

import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지 Repository 인터페이스 (MVP 버전)
 * 
 * 메시지 엔티티의 데이터 접근을 담당합니다.
 * MVP에서는 기본적인 CRUD와 대화별 조회 기능만 제공합니다.
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    /**
     * 대화의 모든 메시지 조회 (시간순)
     * 
     * @param conversationId 대화 ID
     * @return 메시지 목록 (시간순)
     */
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);


    /**
     * 특정 회원의 최근 메시지 조회 (감정 패턴 분석용)
     *
     * @param memberId 회원 ID
     * @param messageType 메시지 타입
     * @param startDate 조회 시작 날짜
     * @return 회원의 최근 메시지 목록 (최신순)
     */
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE m.conversationId IN (SELECT c.id FROM ConversationEntity c WHERE c.memberId = :memberId) " +
           "AND m.type = :messageType " +
           "AND m.createdAt >= :startDate " +
           "ORDER BY m.createdAt DESC")
    List<MessageEntity> findRecentUserMessagesByMemberId(
            @Param("memberId") Long memberId,
            @Param("messageType") MessageType messageType,
            @Param("startDate") LocalDateTime startDate);

    /**
     * 특정 회원의 대화 내역 조회 (보호자용)
     *
     * @param memberId 회원 ID
     * @param startDate 조회 시작 날짜
     * @return 회원의 대화 메시지 목록 (시간순)
     */
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE m.conversationId IN (SELECT c.id FROM ConversationEntity c WHERE c.memberId = :memberId) " +
           "AND m.createdAt >= :startDate " +
           "ORDER BY m.createdAt ASC")
    List<MessageEntity> findConversationHistoryByMemberId(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate);
}