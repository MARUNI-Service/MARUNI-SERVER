package com.anyang.maruni.domain.conversation.application.service;

import com.anyang.maruni.domain.conversation.application.dto.ConversationResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SimpleConversationService 테스트 (TDD 재시작)
 * 
 * 가장 간단한 테스트부터 시작합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대화 서비스 테스트")
class SimpleConversationServiceTest {

    @InjectMocks
    private SimpleConversationService simpleConversationService;

    @Test
    @DisplayName("processUserMessage 메서드가 존재한다")
    void processUserMessage_MethodExists() {
        // Given
        Long memberId = 1L;
        String content = "안녕하세요";
        
        // When & Then - 메서드 호출시 UnsupportedOperationException 발생
        assertThatThrownBy(() -> simpleConversationService.processUserMessage(memberId, content))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("아직 구현되지 않았습니다.");
    }
}