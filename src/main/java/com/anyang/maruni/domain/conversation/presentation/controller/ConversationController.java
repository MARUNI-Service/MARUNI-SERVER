package com.anyang.maruni.domain.conversation.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.conversation.application.dto.request.ConversationRequestDto;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetails;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 대화 API Controller
 *
 * 사용자와 AI 간의 대화 처리를 위한 REST API를 제공합니다.
 * OpenAI GPT-4o 기반 멀티턴 대화 및 감정 분석 기능을 지원합니다.
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "대화 관리 API", description = "AI 대화 및 감정 분석 API")
public class ConversationController {

    private final SimpleConversationService conversationService;

    /**
     * AI 대화 메시지 전송
     *
     * @param userDetails 인증된 사용자 정보
     * @param request 대화 요청 DTO
     * @return 대화 응답 DTO (사용자 메시지 + AI 응답)
     */
    @PostMapping("/messages")
    @Operation(
        summary = "AI 대화 메시지 전송",
        description = "사용자 메시지를 전송하고 OpenAI GPT-4o 기반 AI 응답을 받습니다. " +
                     "키워드 기반 감정 분석 및 멀티턴 대화를 지원합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 처리 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 유효성 실패 (메시지 길이 초과 등)", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "429", description = "일일 메시지 한도 초과 (50개)", content = @Content)
    })
    @CustomExceptionDescription(SwaggerResponseDescription.CONVERSATION_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public ConversationResponseDto sendMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConversationRequestDto request) {

        return conversationService.processUserMessage(userDetails.getMemberId(), request.getContent());
    }
}