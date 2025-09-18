package com.anyang.maruni.domain.conversation.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.mapper.ConversationMapper;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 간단한 대화 서비스 (리팩토링 완료)
 *
 * 대화 흐름을 조율하는 얇은 서비스 계층입니다.
 * 실제 비즈니스 로직은 MessageProcessor와 ConversationMapper에 위임합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleConversationService {

    private final ConversationManager conversationManager;
    private final MessageProcessor messageProcessor;
    private final ConversationMapper mapper;
    private final MessageRepository messageRepository;

    /**
     * 사용자 메시지 처리 및 AI 응답 생성 (간소화됨)
     *
     * @param memberId 회원 ID
     * @param content 메시지 내용
     * @return 대화 응답 DTO
     */
    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        log.info("Processing user message for member {}: {}", memberId, content);

        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageExchangeResult result = messageProcessor.processMessage(conversation, content);
        return mapper.toResponseDto(result);
    }

    /**
     * 시스템 메시지 처리 (Phase 2 스케줄링 시스템에서 사용)
     *
     * 정기 안부 메시지 등 시스템에서 보내는 메시지를 대화 시스템에 기록합니다.
     * 사용자가 이 메시지에 응답할 수 있도록 AI 메시지 형태로 저장됩니다.
     *
     * @param memberId 회원 ID
     * @param systemMessage 시스템 메시지 내용
     */
    @Transactional
    public void processSystemMessage(Long memberId, String systemMessage) {
        log.info("Processing system message for member {}: {}", memberId, systemMessage);

        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageEntity systemMessageEntity = conversation.addAIMessage(systemMessage);
        messageRepository.save(systemMessageEntity);

        log.debug("System message saved as AI message for conversation {}", conversation.getId());
    }
}