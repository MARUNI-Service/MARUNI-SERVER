package com.anyang.maruni.domain.conversation.domain.entity;

import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대화 엔티티 (MVP 버전)
 * 
 * 사용자와 AI 간의 대화 세션을 나타냅니다.
 * MVP에서는 기본적인 정보만 포함하며, 복잡한 상태 관리는 제외합니다.
 */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ConversationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 대화를 시작한 회원 ID
     * Member 도메인과의 연관관계
     */
    @Column(nullable = false)
    private Long memberId;

    /**
     * 대화 시작 시간
     */
    @Column(nullable = false)
    private LocalDateTime startedAt;

    /**
     * 정적 팩토리 메서드: 새 대화 생성
     *
     * @param memberId 회원 ID
     * @return 새로운 ConversationEntity 인스턴스
     */
    public static ConversationEntity createNew(Long memberId) {
        return ConversationEntity.builder()
                .memberId(memberId)
                .startedAt(LocalDateTime.now())
                .build();
    }
}