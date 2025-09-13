package com.anyang.maruni.domain.conversation.application.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.conversation.application.dto.ConversationResponseDto;
import com.anyang.maruni.domain.conversation.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.conversation.domain.repository.MessageRepository;
import com.anyang.maruni.domain.conversation.infrastructure.SimpleAIResponseGenerator;

/**
 * SimpleConversationService 테스트 (TDD 재시작)
 * 
 * 가장 간단한 테스트부터 시작합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대화 서비스 테스트")
class SimpleConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private SimpleAIResponseGenerator aiResponseGenerator;

    @InjectMocks
    private SimpleConversationService simpleConversationService;

    @Test
    @DisplayName("processUserMessage 메서드가 존재하고 호출 가능하다")
    void processUserMessage_MethodExists() {
        // Given
        Long memberId = 1L;
        String content = "안녕하세요";
        
        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, content);
        
        // Then
        assertThat(result).isNotNull();
    }
    
    @Test
    @DisplayName("사용자 메시지를 처리하면 ConversationResponseDto를 반환한다")
    void processUserMessage_ReturnsConversationResponseDto() {
        // Given
        Long memberId = 1L;
        String content = "안녕하세요";
        
        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, content);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isNotNull();
    }
    
    @Test
    @DisplayName("다른 회원ID로 호출하면 해당 memberId를 conversationId로 반환한다")
    void processUserMessage_DifferentMemberId_ReturnsMemberIdAsConversationId() {
        // Given
        Long memberId = 999L;  // 다른 회원 ID
        String content = "테스트 메시지";
        
        // When
        ConversationResponseDto result = simpleConversationService.processUserMessage(memberId, content);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isNotNull();
        // 리팩토링 후: memberId가 conversationId로 사용됨
        assertThat(result.getConversationId()).isEqualTo(999L);
    }
}