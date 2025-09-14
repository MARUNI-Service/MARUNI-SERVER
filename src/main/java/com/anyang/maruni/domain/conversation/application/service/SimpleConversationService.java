package com.anyang.maruni.domain.conversation.application.service;

import com.anyang.maruni.domain.conversation.application.dto.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.dto.MessageDto;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import com.anyang.maruni.domain.conversation.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.conversation.infrastructure.SimpleAIResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 간단한 대화 서비스 (MVP 버전)
 * 
 * 사용자 메시지 처리와 AI 응답 생성의 전체 플로우를 관리합니다.
 * MVP에서는 기본적인 메시지 처리 기능만 제공합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpleAIResponseGenerator aiResponseGenerator;

    /**
     * 사용자 메시지 처리 및 AI 응답 생성
     *
     * @param memberId 회원 ID
     * @param content 메시지 내용
     * @return 대화 응답 DTO
     */
    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        log.info("Processing user message for member {}: {}", memberId, content);

        // 1. 활성 대화 조회 또는 새 대화 생성
        ConversationEntity conversation = findOrCreateActiveConversation(memberId);

        // 2. 사용자 메시지 감정 분석 및 저장
        MessageEntity userMessage = saveUserMessage(conversation.getId(), content);

        // 3. AI 응답 생성
        String aiResponse = aiResponseGenerator.generateResponse(content);

        // 4. AI 응답 메시지 저장
        MessageEntity aiMessage = saveAIMessage(conversation.getId(), aiResponse);

        // 5. 응답 DTO 생성
        return ConversationResponseDto.builder()
                .conversationId(conversation.getId())
                .userMessage(MessageDto.builder()
                        .id(userMessage.getId())
                        .type(userMessage.getType())
                        .content(userMessage.getContent())
                        .emotion(userMessage.getEmotion())
                        .createdAt(userMessage.getCreatedAt())
                        .build())
                .aiMessage(MessageDto.builder()
                        .id(aiMessage.getId())
                        .type(aiMessage.getType())
                        .content(aiMessage.getContent())
                        .emotion(aiMessage.getEmotion())
                        .createdAt(aiMessage.getCreatedAt())
                        .build())
                .build();
    }

    /**
     * 활성 대화 조회 또는 새 대화 생성
     */
    private ConversationEntity findOrCreateActiveConversation(Long memberId) {
        return conversationRepository.findActiveByMemberId(memberId)
                .orElseGet(() -> {
                    log.info("Creating new conversation for member {}", memberId);
                    ConversationEntity newConversation = ConversationEntity.createNew(memberId);
                    return conversationRepository.save(newConversation);
                });
    }

    /**
     * 사용자 메시지 저장 (감정 분석 포함)
     */
    private MessageEntity saveUserMessage(Long conversationId, String content) {
        // AI 응답 생성기를 통해 감정 분석
        EmotionType emotion = aiResponseGenerator.analyzeBasicEmotion(content);

        MessageEntity userMessage = MessageEntity.createUserMessage(conversationId, content, emotion);
        return messageRepository.save(userMessage);
    }

    /**
     * AI 응답 메시지 저장
     */
    private MessageEntity saveAIMessage(Long conversationId, String content) {
        MessageEntity aiMessage = MessageEntity.createAIResponse(conversationId, content);
        return messageRepository.save(aiMessage);
    }
}