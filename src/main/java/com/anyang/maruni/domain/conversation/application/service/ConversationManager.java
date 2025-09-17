package com.anyang.maruni.domain.conversation.application.service;

import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 대화 관리 도메인 서비스
 *
 * ConversationEntity와 관련된 비즈니스 로직을 관리합니다.
 * Repository에서 분리된 비즈니스 규칙들을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationManager {

    private final ConversationRepository conversationRepository;

    /**
     * 회원의 활성 대화 조회
     *
     * 비즈니스 규칙: 가장 최근 대화를 활성 대화로 간주
     * 향후 확장: isActive() 도메인 로직 적용 가능
     *
     * @param memberId 회원 ID
     * @return 활성 대화 (없으면 null)
     */
    public ConversationEntity findActiveConversation(Long memberId) {
        log.debug("Finding active conversation for member: {}", memberId);

        return conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElse(null);
    }

    /**
     * 새 대화 생성
     *
     * @param memberId 회원 ID
     * @return 생성된 대화 엔티티
     */
    @Transactional
    public ConversationEntity createNewConversation(Long memberId) {
        log.info("Creating new conversation for member: {}", memberId);

        ConversationEntity conversation = ConversationEntity.createNew(memberId);
        ConversationEntity savedConversation = conversationRepository.save(conversation);

        log.debug("Created conversation with ID: {} for member: {}",
                savedConversation.getId(), memberId);

        return savedConversation;
    }

    /**
     * 활성 대화 조회 또는 새 대화 생성
     *
     * SimpleConversationService에서 사용하는 핵심 비즈니스 로직
     *
     * @param memberId 회원 ID
     * @return 활성 대화 (기존 또는 새로 생성된)
     */
    @Transactional
    public ConversationEntity findOrCreateActive(Long memberId) {
        log.debug("Finding or creating active conversation for member: {}", memberId);

        ConversationEntity activeConversation = findActiveConversation(memberId);

        if (activeConversation != null) {
            log.debug("Found existing active conversation: {} for member: {}",
                    activeConversation.getId(), memberId);
            return activeConversation;
        }

        return createNewConversation(memberId);
    }
}