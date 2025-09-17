package com.anyang.maruni.domain.conversation.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.conversation.application.dto.MessageDto;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.conversation.domain.port.AIResponsePort;
import com.anyang.maruni.domain.conversation.domain.port.EmotionAnalysisPort;
import com.anyang.maruni.domain.conversation.domain.vo.ConversationContext;
import com.anyang.maruni.domain.conversation.domain.vo.MemberProfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private final ConversationManager conversationManager;
    private final MessageRepository messageRepository;

    private final AIResponsePort aiResponsePort;
    private final EmotionAnalysisPort emotionAnalysisPort;

    /**
     * 사용자 메시지 처리 및 AI 응답 생성 (컨텍스트 기반)
     *
     * @param memberId 회원 ID
     * @param content 메시지 내용
     * @return 대화 응답 DTO
     */
    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        log.info("Processing user message for member {}: {}", memberId, content);

        // 1. 활성 대화 조회 또는 새 대화 생성
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);

        // 2. 사용자 메시지 감정 분석
        EmotionType emotion = emotionAnalysisPort.analyzeEmotion(content);

        // 3. 대화 컨텍스트 구성 (최근 히스토리 포함)
        ConversationContext context = buildConversationContext(conversation, content, memberId, emotion);

        // 4. 컨텍스트 기반 AI 응답 생성
        String aiResponse = aiResponsePort.generateResponse(context);

        // 5. 사용자 메시지 저장 (도메인 로직 활용)
        MessageEntity userMessage = conversation.addUserMessage(content, emotion);
        messageRepository.save(userMessage);

        // 6. AI 응답 메시지 저장 (도메인 로직 활용)
        MessageEntity aiMessage = conversation.addAIMessage(aiResponse);
        messageRepository.save(aiMessage);

        // 7. 응답 DTO 생성
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
     * 대화 컨텍스트 구성 (멀티턴 대화 지원)
     */
    private ConversationContext buildConversationContext(
            ConversationEntity conversation,
            String currentMessage,
            Long memberId,
            EmotionType emotion) {

        // 사용자 프로필 생성 (MVP에서는 기본값 사용)
        MemberProfile profile = MemberProfile.createDefault(memberId);

        // 최근 대화 히스토리 조회 (최대 5턴)
        java.util.List<MessageEntity> recentHistory = conversation.getRecentHistory(5);

        return ConversationContext.forUserMessage(currentMessage, recentHistory, profile, emotion);
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

        // 1. 활성 대화 조회 또는 새 대화 생성
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);

        // 2. 시스템 메시지를 AI 메시지로 저장 (도메인 로직 활용)
        MessageEntity systemMessageEntity = conversation.addAIMessage(systemMessage);
        messageRepository.save(systemMessageEntity);

        log.debug("System message saved as AI message for conversation {}", conversation.getId());
    }
}