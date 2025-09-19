package com.anyang.maruni.domain.conversation.application.dto;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 메시지 DTO
 *
 * 사용자 메시지 또는 AI 응답 메시지 정보를 담습니다.
 */
@Getter
@Builder
@Schema(
    description = "대화 메시지 정보",
    example = """
        {
          "id": 1,
          "type": "USER_MESSAGE",
          "content": "안녕하세요, 오늘 기분이 좋아요!",
          "emotion": "POSITIVE",
          "createdAt": "2025-09-18T10:30:00"
        }
        """
)
public class MessageDto {

    /**
     * 메시지 ID
     */
    @Schema(
        description = "메시지의 고유 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    /**
     * 메시지 타입 (USER/AI/SYSTEM)
     */
    @Schema(
        description = "메시지 발신자 유형",
        example = "USER_MESSAGE",
        allowableValues = {"USER_MESSAGE", "AI_RESPONSE", "SYSTEM_MESSAGE"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MessageType type;

    /**
     * 메시지 내용
     */
    @Schema(
        description = "메시지의 텍스트 내용",
        example = "안녕하세요, 오늘 기분이 좋아요!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 500
    )
    private String content;

    /**
     * 감정 분석 결과
     */
    @Schema(
        description = "키워드 기반 감정 분석 결과 (사용자 메시지에만 적용)",
        example = "POSITIVE",
        allowableValues = {"POSITIVE", "NEGATIVE", "NEUTRAL"},
        nullable = true
    )
    private EmotionType emotion;

    /**
     * 메시지 생성 시간
     */
    @Schema(
        description = "메시지가 생성된 일시 (ISO 8601 형식)",
        example = "2025-09-18T10:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime createdAt;

    /**
     * MessageEntity를 MessageDto로 변환하는 정적 팩토리 메서드
     *
     * @param entity 메시지 엔티티
     * @return 메시지 DTO
     */
    public static MessageDto from(MessageEntity entity) {
        return MessageDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .content(entity.getContent())
                .emotion(entity.getEmotion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}