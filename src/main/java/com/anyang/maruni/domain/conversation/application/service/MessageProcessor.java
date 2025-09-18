package com.anyang.maruni.domain.conversation.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
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
 * 메시지 처리 서비스
 *
 * 사용자 메시지와 AI 응답 생성의 핵심 비즈니스 로직을 담당합니다.
 * SimpleConversationService에서 분리된 단일 책임을 가집니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final MessageRepository messageRepository;
    private final AIResponsePort aiResponsePort;
    private final EmotionAnalysisPort emotionAnalysisPort;

    /**
     * 메시지 처리 및 AI 응답 생성
     *
     * @param conversation 대화 엔티티
     * @param content 사용자 메시지 내용
     * @return 메시지 교환 결과
     */
    @Transactional
    public MessageExchangeResult processMessage(ConversationEntity conversation, String content) {
        log.debug("Processing message for conversation {}: {}", conversation.getId(), content);

        // 1. 사용자 메시지 감정 분석
        EmotionType emotion = emotionAnalysisPort.analyzeEmotion(content);

        // 2. 대화 컨텍스트 구성 (최근 히스토리 포함)
        MemberProfile profile = MemberProfile.createDefault(conversation.getMemberId());
        java.util.List<MessageEntity> recentHistory = conversation.getRecentHistory(5);
        ConversationContext context = ConversationContext.forUserMessage(content, recentHistory, profile, emotion);

        // 3. 사용자 메시지 저장 (도메인 로직 활용)
        MessageEntity userMessage = conversation.addUserMessage(content, emotion);
        messageRepository.save(userMessage);
        log.debug("Saved user message with ID: {}", userMessage.getId());

        // 4. 컨텍스트 기반 AI 응답 생성
        String aiResponse = aiResponsePort.generateResponse(context);

        // 5. AI 응답 메시지 저장 (도메인 로직 활용)
        MessageEntity aiMessage = conversation.addAIMessage(aiResponse);
        messageRepository.save(aiMessage);
        log.debug("Saved AI message with ID: {}", aiMessage.getId());

        return MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();
    }
}