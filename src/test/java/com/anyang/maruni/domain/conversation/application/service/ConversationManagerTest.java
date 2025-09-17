package com.anyang.maruni.domain.conversation.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.repository.ConversationRepository;

/**
 * ConversationManager 테스트
 *
 * Repository에서 분리된 비즈니스 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대화 관리 도메인 서비스 테스트")
class ConversationManagerTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationManager conversationManager;

    @Test
    @DisplayName("활성 대화 조회: 회원의 가장 최근 대화를 반환한다")
    void findActiveConversation_ExistingConversation_Success() {
        // Given
        Long memberId = 1L;
        ConversationEntity existingConversation = ConversationEntity.builder()
                .id(100L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.of(existingConversation));

        // When
        ConversationEntity result = conversationManager.findActiveConversation(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getMemberId()).isEqualTo(memberId);

        verify(conversationRepository).findTopByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Test
    @DisplayName("활성 대화 조회: 회원의 대화가 없는 경우 null을 반환한다")
    void findActiveConversation_NoConversation_ReturnsNull() {
        // Given
        Long memberId = 2L;

        when(conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.empty());

        // When
        ConversationEntity result = conversationManager.findActiveConversation(memberId);

        // Then
        assertThat(result).isNull();

        verify(conversationRepository).findTopByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Test
    @DisplayName("새 대화 생성: 회원 ID로 새로운 대화를 생성한다")
    void createNewConversation_Success() {
        // Given
        Long memberId = 3L;
        ConversationEntity savedConversation = ConversationEntity.builder()
                .id(200L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenReturn(savedConversation);

        // When
        ConversationEntity result = conversationManager.createNewConversation(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getStartedAt()).isNotNull();

        verify(conversationRepository).save(any(ConversationEntity.class));
    }

    @Test
    @DisplayName("활성 대화 조회 또는 생성: 기존 대화가 있으면 반환한다")
    void findOrCreateActive_ExistingConversation_ReturnsExisting() {
        // Given
        Long memberId = 4L;
        ConversationEntity existingConversation = ConversationEntity.builder()
                .id(300L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now().minusHours(2))
                .build();

        when(conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.of(existingConversation));

        // When
        ConversationEntity result = conversationManager.findOrCreateActive(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(300L);
        assertThat(result.getMemberId()).isEqualTo(memberId);

        // 조회만 했고 새로 생성하지 않았는지 확인
        verify(conversationRepository).findTopByMemberIdOrderByCreatedAtDesc(memberId);
        verify(conversationRepository, never()).save(any(ConversationEntity.class));
    }

    @Test
    @DisplayName("활성 대화 조회 또는 생성: 대화가 없으면 새로 생성한다")
    void findOrCreateActive_NoConversation_CreatesNew() {
        // Given
        Long memberId = 5L;
        ConversationEntity newConversation = ConversationEntity.builder()
                .id(400L)
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenReturn(newConversation);

        // When
        ConversationEntity result = conversationManager.findOrCreateActive(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(400L);
        assertThat(result.getMemberId()).isEqualTo(memberId);

        // 조회 후 생성했는지 확인
        verify(conversationRepository).findTopByMemberIdOrderByCreatedAtDesc(memberId);
        verify(conversationRepository).save(any(ConversationEntity.class));
    }

    @Test
    @DisplayName("비즈니스 로직 분리 검증: Repository는 데이터 접근만 담당한다")
    void businessLogicSeparation_RepositoryOnlyDataAccess() {
        // Given
        Long memberId = 6L;

        when(conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenReturn(ConversationEntity.createNew(memberId));

        // When
        conversationManager.findOrCreateActive(memberId);

        // Then
        // Repository는 순수 데이터 접근 메서드만 호출됨
        verify(conversationRepository).findTopByMemberIdOrderByCreatedAtDesc(memberId);
        verify(conversationRepository).save(any(ConversationEntity.class));

        // 비즈니스 로직 메서드는 호출되지 않음 (이제 제거됨)
        // findActiveByMemberId는 더 이상 존재하지 않음
    }
}