package com.anyang.maruni.domain.conversation.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.conversation.application.dto.request.ConversationRequestDto;
import com.anyang.maruni.domain.conversation.application.dto.response.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 대화 API Controller (MVP 버전)
 *
 * 사용자와 AI 간의 대화 처리를 위한 REST API를 제공합니다.
 * MVP에서는 기본적인 메시지 전송 및 AI 응답 기능만 제공합니다.
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "대화 API", description = "AI 대화 관련 API")
public class ConversationController {

    private final SimpleConversationService conversationService;

    /**
     * 사용자 메시지 전송 및 AI 응답 받기
     *
     * @param userDetails 인증된 사용자 정보
     * @param request 대화 요청 DTO
     * @return 대화 응답 DTO (사용자 메시지 + AI 응답)
     */
    @PostMapping("/messages")
    @Operation(
        summary = "AI 대화 메시지 전송",
        description = "사용자 메시지를 전송하고 AI 응답을 받습니다. JWT Bearer 토큰 인증이 필요합니다."
    )
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public ConversationResponseDto sendMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConversationRequestDto request) {

        // 대화 서비스를 통해 메시지 처리
        return conversationService.processUserMessage(userDetails.getMemberId(), request.getContent());
    }

}