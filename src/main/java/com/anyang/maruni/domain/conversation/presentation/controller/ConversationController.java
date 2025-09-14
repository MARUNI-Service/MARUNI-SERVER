package com.anyang.maruni.domain.conversation.presentation.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.conversation.application.dto.ConversationRequestDto;
import com.anyang.maruni.domain.conversation.application.dto.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.application.service.SimpleConversationService;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
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
     * @param authentication 인증된 사용자 정보
     * @param request 대화 요청 DTO
     * @return 대화 응답 DTO (사용자 메시지 + AI 응답)
     */
    @PostMapping("/messages")
    @Operation(
        summary = "AI 대화 메시지 전송",
        description = "사용자 메시지를 전송하고 AI 응답을 받습니다. 인증된 사용자만 사용할 수 있습니다."
    )
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public ConversationResponseDto sendMessage(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody ConversationRequestDto request) {

        // 인증된 사용자의 ID 추출
        Long memberId = extractMemberIdFromAuth(authentication);

        // 대화 서비스를 통해 메시지 처리
        ConversationResponseDto response = conversationService.processUserMessage(memberId, request.getContent());

        return response;
    }

    /**
     * Authentication 객체에서 회원 ID 추출
     *
     * @param authentication Spring Security Authentication 객체
     * @return 회원 ID
     */
    private Long extractMemberIdFromAuth(Authentication authentication) {
        // CustomUserDetails에서 회원 ID 추출
        // 실제 구현에서는 authentication.getPrincipal()을 CustomUserDetails로 캐스팅하여 사용
        // MVP에서는 간단하게 authentication name을 Long으로 변환 (실제로는 username이 memberId)
        return Long.parseLong(authentication.getName());
    }
}