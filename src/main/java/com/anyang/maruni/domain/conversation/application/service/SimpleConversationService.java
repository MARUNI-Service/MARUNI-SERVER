package com.anyang.maruni.domain.conversation.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertDetectionService;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertNotificationService;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.conversation.application.dto.MessageDto;
import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.mapper.ConversationMapper;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

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
    private final MemberRepository memberRepository;

    // 신규 의존성 (Phase 2: 키워드 감지)
    private final AlertDetectionService alertDetectionService;
    private final AlertNotificationService alertNotificationService;

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

        // 1. 기존 로직: 메시지 저장 + AI 응답
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageExchangeResult result = messageProcessor.processMessage(conversation, content);

        // 2. 신규 로직: 키워드 실시간 감지 (예외 격리)
        detectKeywordInRealtime(result.userMessage(), memberId);

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

    /**
     * 내 대화 전체보기
     *
     * 본인의 대화 내역을 조회합니다.
     *
     * @param memberId 회원 ID
     * @param days 조회 기간 (최근 N일)
     * @return 메시지 목록
     */
    public List<MessageDto> getMyConversationHistory(Long memberId, int days) {
        log.info("Getting conversation history for member {} (days: {})", memberId, days);

        // 최근 N일간 메시지 조회
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<MessageEntity> messages = messageRepository.findConversationHistoryByMemberId(
            memberId, startDate);

        // DTO 변환
        return messages.stream()
            .map(MessageDto::from)
            .toList();
    }

    /**
     * 최신 메시지 조회
     *
     * 본인의 가장 최신 메시지 1개를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 최신 메시지 (없으면 null)
     */
    public MessageDto getLatestMessage(Long memberId) {
        log.info("Getting latest message for member {}", memberId);

        MessageEntity latestMessage = messageRepository.findLatestMessageByMemberId(memberId);

        if (latestMessage == null) {
            log.debug("No messages found for member {}", memberId);
            return null;
        }

        return MessageDto.from(latestMessage);
    }

    /**
     * 실시간 키워드 감지 (private, 예외 격리)
     *
     * 사용자 메시지에서 키워드를 감지하고, EMERGENCY 레벨만 즉시 알림 발송합니다.
     * HIGH 레벨은 로그만 기록합니다.
     *
     * @param message 사용자 메시지
     * @param memberId 회원 ID
     */
    private void detectKeywordInRealtime(MessageEntity message, Long memberId) {
        try {
            AlertResult keywordResult = alertDetectionService.detectKeywordAlert(message, memberId);

            // EMERGENCY 키워드만 즉시 알림 발송
            if (keywordResult.isAlert() && keywordResult.getAlertLevel() == AlertLevel.EMERGENCY) {
                alertNotificationService.triggerAlert(memberId, keywordResult);
                log.warn("⚠️ EMERGENCY keyword detected for member {}: {}",
                         memberId, keywordResult.getMessage());
            } else if (keywordResult.isAlert()) {
                log.info("📌 HIGH keyword detected for member {} (로그만 기록)", memberId);
            }

        } catch (Exception e) {
            // 키워드 감지 실패는 대화 흐름에 영향 없음 (로그만 기록)
            log.error("Keyword detection failed for member {}: {}",
                      memberId, e.getMessage(), e);
        }
    }
}