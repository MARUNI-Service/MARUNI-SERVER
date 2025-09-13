package com.anyang.maruni.domain.conversation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 사용자 메시지 처리 및 AI 응답 생성
     * 
     * @param memberId 회원 ID
     * @param content 메시지 내용
     * @return 대화 응답 DTO
     */
    @Transactional
    public Object processUserMessage(Long memberId, String content) {
        // TODO: TDD로 구현 예정
        // 1. 대화 조회/생성
        // 2. 사용자 메시지 저장
        // 3. AI 응답 생성
        // 4. AI 응답 저장
        // 5. 응답 DTO 반환
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}