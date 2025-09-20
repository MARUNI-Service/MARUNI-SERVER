package com.anyang.maruni.domain.conversation.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화 요청 DTO
 *
 * 사용자가 메시지를 전송할 때 사용하는 요청 객체입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "AI 대화 메시지 전송 요청",
    example = """
        {
          "content": "안녕하세요, 오늘 기분이 좋아요!"
        }
        """
)
public class ConversationRequestDto {

    /**
     * 메시지 내용
     */
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다")
    @Schema(
        description = "사용자가 AI에게 전송할 메시지 내용",
        example = "안녕하세요, 오늘 기분이 좋아요!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 500
    )
    private String content;
}