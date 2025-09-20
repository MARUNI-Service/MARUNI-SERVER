package com.anyang.maruni.domain.conversation.application.dto.response;

import com.anyang.maruni.domain.conversation.application.dto.MessageDto;
import com.anyang.maruni.domain.conversation.application.dto.MessageExchangeResult;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화 처리 응답 DTO
 *
 * 사용자 메시지 처리 결과와 AI 응답을 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "AI 대화 처리 응답",
    example = """
        {
          "conversationId": 1,
          "userMessage": {
            "id": 1,
            "type": "USER_MESSAGE",
            "content": "안녕하세요, 오늘 기분이 좋아요!",
            "emotion": "POSITIVE",
            "createdAt": "2025-09-18T10:30:00"
          },
          "aiMessage": {
            "id": 2,
            "type": "AI_RESPONSE",
            "content": "안녕하세요! 기분이 좋으시다니 정말 다행이에요. 오늘 특별한 일이 있으셨나요?",
            "emotion": "NEUTRAL",
            "createdAt": "2025-09-18T10:30:03"
          }
        }
        """
)
public class ConversationResponseDto {

    /**
     * 대화 ID
     */
    @Schema(
        description = "대화 세션의 고유 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long conversationId;

    /**
     * 사용자 메시지 정보
     */
    @Schema(
        description = "사용자가 전송한 메시지의 상세 정보",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MessageDto userMessage;

    /**
     * AI 응답 메시지 정보
     */
    @Schema(
        description = "AI가 생성한 응답 메시지의 상세 정보",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MessageDto aiMessage;

    /**
     * MessageExchangeResult로부터 ConversationResponseDto 생성
     *
     * @param result 메시지 교환 결과
     * @return 대화 응답 DTO
     */
    public static ConversationResponseDto from(MessageExchangeResult result) {
        return ConversationResponseDto.builder()
                .conversationId(result.conversation().getId())
                .userMessage(MessageDto.from(result.userMessage()))
                .aiMessage(MessageDto.from(result.aiMessage()))
                .build();
    }

    /**
     * 테스트용 간단한 팩토리 메서드
     *
     * @param conversationId 대화 ID
     * @return 최소한의 ConversationResponseDto
     */
    public static ConversationResponseDto withId(Long conversationId) {
        return ConversationResponseDto.builder()
                .conversationId(conversationId)
                .build();
    }
}