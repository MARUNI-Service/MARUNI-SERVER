package com.anyang.maruni.domain.conversation.domain.vo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 대화 컨텍스트 (Value Object)
 *
 * AI가 대화 히스토리와 사용자 정보를 활용할 수 있도록
 * 컨텍스트 정보를 캡슐화합니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class ConversationContext {

    /**
     * 현재 사용자 메시지
     */
    private final String currentMessage;

    /**
     * 최근 대화 히스토리 (최대 5턴)
     */
    private final List<MessageEntity> recentHistory;

    /**
     * 사용자 프로필 정보
     */
    private final MemberProfile memberProfile;

    /**
     * 현재 감정 상태
     */
    private final EmotionType currentEmotion;

    /**
     * 추가 컨텍스트 정보
     */
    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * 사용자 메시지에 대한 컨텍스트 생성
     */
    public static ConversationContext forUserMessage(
            String message,
            List<MessageEntity> history,
            MemberProfile profile,
            EmotionType emotion) {
        return ConversationContext.builder()
                .currentMessage(message)
                .recentHistory(history.stream().limit(5).collect(Collectors.toList()))
                .memberProfile(profile)
                .currentEmotion(emotion)
                .metadata(new HashMap<>())
                .build();
    }

    /**
     * 시스템 메시지에 대한 컨텍스트 생성
     */
    public static ConversationContext forSystemMessage(String message, MemberProfile profile) {
        return ConversationContext.builder()
                .currentMessage(message)
                .recentHistory(Collections.emptyList())
                .memberProfile(profile)
                .currentEmotion(EmotionType.NEUTRAL)
                .metadata(new HashMap<>())
                .build();
    }
}