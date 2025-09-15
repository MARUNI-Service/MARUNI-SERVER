package com.anyang.maruni.domain.conversation.domain.repository;

import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 대화의 최근 메시지들 조회 (페이징, 최신순)
     * 
     * @param conversationId 대화 ID
     * @param pageable 페이징 정보
     * @return 메시지 페이지 (최신순)
     */
    Page<MessageEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * 특정 감정의 메시지 조회 (분석용)
     *
     * @param emotion 감정 타입
     * @param type 메시지 타입
     * @param pageable 페이징 정보
     * @return 감정별 메시지 목록 (최신순)
     */
    List<MessageEntity> findByEmotionAndTypeOrderByCreatedAtDesc(EmotionType emotion, MessageType type, Pageable pageable);

    /**
     * 특정 회원의 최근 N일간 사용자 메시지 조회 (감정 패턴 분석용)
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
     * 특정 회원의 최근 N일간 사용자 메시지 조회 (일수 기준)
     *
     * @param memberId 회원 ID
     * @param days 조회 일수
     * @return 회원의 최근 메시지 목록 (최신순)
     */
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE m.conversationId IN (SELECT c.id FROM ConversationEntity c WHERE c.memberId = :memberId) " +
           "AND m.type = 'USER_MESSAGE' " +
           "AND m.createdAt >= :startDate " +
           "ORDER BY m.createdAt DESC")
    List<MessageEntity> findRecentUserMessagesByMemberIdAndDays(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate);
}