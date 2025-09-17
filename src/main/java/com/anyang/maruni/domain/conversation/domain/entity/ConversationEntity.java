package com.anyang.maruni.domain.conversation.domain.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.anyang.maruni.domain.conversation.domain.exception.InvalidMessageException;
import com.anyang.maruni.domain.conversation.domain.exception.MessageLimitExceededException;
import com.anyang.maruni.global.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화 엔티티 (Rich Domain Model)
 *
 * 사용자와 AI 간의 대화 세션을 나타냅니다.
 * 비즈니스 로직이 포함된 풍부한 도메인 모델로 리팩토링되었습니다.
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
     * 대화에 속한 메시지들 (JPA 연관관계)
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MessageEntity> messages = new ArrayList<>();

    // 비즈니스 규칙 상수
    private static final int MAX_DAILY_MESSAGES = 50;
    private static final int MAX_MESSAGE_LENGTH = 500;

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

    /**
     * 대화가 활성 상태인지 확인
     *
     * 비즈니스 규칙: 메시지가 없거나 마지막 메시지가 24시간 이내인 경우 활성
     *
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        if (messages.isEmpty()) {
            return true; // 새 대화는 활성 상태
        }
        LocalDateTime lastMessageTime = getLastMessageTime();
        return lastMessageTime.isAfter(LocalDateTime.now().minusDays(1));
    }

    /**
     * 사용자 메시지 추가
     *
     * @param content 메시지 내용
     * @param emotion 감정 타입
     * @return 생성된 메시지 엔티티
     */
    public MessageEntity addUserMessage(String content, EmotionType emotion) {
        validateMessageContent(content);
        validateCanAddMessage();

        MessageEntity message = MessageEntity.createUserMessage(this.id, content, emotion);
        this.messages.add(message);
        return message;
    }

    /**
     * AI 응답 메시지 추가
     *
     * @param content AI 응답 내용
     * @return 생성된 메시지 엔티티
     */
    public MessageEntity addAIMessage(String content) {
        MessageEntity message = MessageEntity.createAIResponse(this.id, content);
        this.messages.add(message);
        return message;
    }

    /**
     * 메시지 수신 가능 여부 확인
     *
     * @return 메시지 수신 가능 여부
     */
    public boolean canReceiveMessage() {
        return isActive() && getDailyMessageCount() < MAX_DAILY_MESSAGES;
    }

    /**
     * 최근 대화 히스토리 조회
     *
     * @param count 조회할 메시지 수
     * @return 최근 메시지 목록 (최신순)
     */
    public List<MessageEntity> getRecentHistory(int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(message -> message.getCreatedAt() != null)
                .sorted(Comparator.comparing(MessageEntity::getCreatedAt).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 내용 유효성 검증
     */
    private void validateMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new InvalidMessageException();
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidMessageException();
        }
    }

    /**
     * 메시지 추가 가능 여부 검증
     */
    private void validateCanAddMessage() {
        if (!canReceiveMessage()) {
            throw new MessageLimitExceededException();
        }
    }

    /**
     * 마지막 메시지 시간 조회
     */
    private LocalDateTime getLastMessageTime() {
        return messages.stream()
                .map(MessageEntity::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(this.getCreatedAt());
    }

    /**
     * 오늘 작성된 메시지 수 조회
     */
    private long getDailyMessageCount() {
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return messages.stream()
                .filter(m -> m.getCreatedAt().isAfter(today))
                .count();
    }
}