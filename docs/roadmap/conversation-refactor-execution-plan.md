# Conversation 도메인 리팩토링 실행 계획 (Claude Code 최적화)

**작성일**: 2025-09-17
**기반 문서**: conversation-refactor-plan-v2.md
**실행 대상**: `src/main/java/com/anyang/maruni/domain/conversation/`
**테스트 경로**: `src/test/java/com/anyang/maruni/domain/conversation/`

---

## 🚀 **Claude Code 즉시 실행 가이드**

### ⚡ **1단계: Repository 비즈니스 로직 분리 (최우선 - 1-2일)**

#### 🎯 **목표**
Clean Architecture 기본 원칙 준수: Repository는 데이터 접근만, 비즈니스 로직은 도메인 서비스로 이동

#### 📂 **수정 대상 파일**
```
src/main/java/com/anyang/maruni/domain/conversation/domain/repository/ConversationRepository.java
src/main/java/com/anyang/maruni/domain/conversation/application/service/ConversationManager.java (신규)
src/main/java/com/anyang/maruni/domain/conversation/application/service/SimpleConversationService.java
```

#### 🔧 **구체적 작업**

**Before (문제 상황)**:
```java
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    // 비즈니스 규칙 하드코딩: "최신 = 활성"
    @Query("SELECT c FROM ConversationEntity c WHERE c.memberId = :memberId ORDER BY c.createdAt DESC LIMIT 1")
    Optional<ConversationEntity> findActiveByMemberId(@Param("memberId") Long memberId);
}
```

**After (개선안)**:
```java
// 1. Repository는 순수 데이터 접근만
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    Optional<ConversationEntity> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<ConversationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

// 2. 비즈니스 로직은 도메인 서비스로 이동
@Service
@RequiredArgsConstructor
public class ConversationManager {
    private final ConversationRepository conversationRepository;

    public ConversationEntity findActiveConversation(Long memberId) {
        return conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
            .filter(ConversationEntity::isActive) // 도메인 로직 활용
            .orElse(null);
    }

    public ConversationEntity createNewConversation(Long memberId) {
        ConversationEntity conversation = ConversationEntity.createNew(memberId);
        return conversationRepository.save(conversation);
    }
}

// 3. SimpleConversationService 수정
@Service
public class SimpleConversationService {
    private final ConversationManager conversationManager; // 추가

    private ConversationEntity findOrCreateActiveConversation(Long memberId) {
        ConversationEntity active = conversationManager.findActiveConversation(memberId);
        return active != null ? active : conversationManager.createNewConversation(memberId);
    }
}
```

#### ✅ **완료 기준**
- [ ] ConversationRepository에서 findActiveByMemberId 메서드 제거
- [ ] ConversationManager 클래스 생성 및 비즈니스 로직 이동
- [ ] SimpleConversationService에서 ConversationManager 사용
- [ ] 기존 테스트 모두 통과
- [ ] 새로운 ConversationManager 테스트 작성

---

### ⚡ **2단계: ConversationEntity 도메인 로직 추가 (2-3일)**

#### 🎯 **목표**
Anemic Domain Model → Rich Domain Model: 엔티티에 비즈니스 로직 추가

#### 📂 **수정 대상 파일**
```
src/main/java/com/anyang/maruni/domain/conversation/domain/entity/ConversationEntity.java
src/main/java/com/anyang/maruni/domain/conversation/domain/entity/MessageEntity.java
```

#### 🔧 **구체적 작업**

**Before (빈혈 모델)**:
```java
@Entity
public class ConversationEntity extends BaseTimeEntity {
    private Long id;
    private Long memberId;
    private LocalDateTime startedAt;
    // 비즈니스 로직 없음
}
```

**After (풍부한 도메인 모델)**:
```java
@Entity
public class ConversationEntity extends BaseTimeEntity {
    private static final int MAX_DAILY_MESSAGES = 50;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private Long id;
    private Long memberId;
    private LocalDateTime startedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageEntity> messages = new ArrayList<>();

    // 도메인 로직 추가
    public boolean isActive() {
        if (messages.isEmpty()) {
            return true; // 새 대화는 활성 상태
        }
        LocalDateTime lastMessageTime = getLastMessageTime();
        return lastMessageTime.isAfter(LocalDateTime.now().minusDays(1));
    }

    public MessageEntity addUserMessage(String content, EmotionType emotion) {
        validateMessageContent(content);
        validateCanAddMessage();

        MessageEntity message = MessageEntity.createUserMessage(this.id, content, emotion);
        this.messages.add(message);
        return message;
    }

    public MessageEntity addAIMessage(String content) {
        MessageEntity message = MessageEntity.createAIResponse(this.id, content);
        this.messages.add(message);
        return message;
    }

    public boolean canReceiveMessage() {
        return isActive() && getDailyMessageCount() < MAX_DAILY_MESSAGES;
    }

    public List<MessageEntity> getRecentHistory(int count) {
        return messages.stream()
            .sorted(Comparator.comparing(MessageEntity::getCreatedAt).reversed())
            .limit(count)
            .collect(Collectors.toList());
    }

    private void validateMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new InvalidMessageException("메시지 내용이 비어있습니다");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidMessageException("메시지가 너무 깁니다");
        }
    }

    private void validateCanAddMessage() {
        if (!canReceiveMessage()) {
            throw new MessageLimitExceededException("메시지 한도를 초과했습니다");
        }
    }

    private LocalDateTime getLastMessageTime() {
        return messages.stream()
            .map(MessageEntity::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(this.getCreatedAt());
    }

    private long getDailyMessageCount() {
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return messages.stream()
            .filter(m -> m.getCreatedAt().isAfter(today))
            .count();
    }
}
```

#### ✅ **완료 기준**
- [ ] ConversationEntity에 도메인 로직 메서드 추가
- [ ] messages 컬렉션 연관관계 설정
- [ ] 비즈니스 규칙 검증 로직 구현
- [ ] 도메인 예외 클래스 생성 (InvalidMessageException 등)
- [ ] 단위 테스트 작성 (도메인 로직 중심)

---

### ⚡ **3단계: ConversationContext 도입 (3-4일)**

#### 🎯 **목표**
멀티턴 대화 지원: AI가 대화 히스토리와 사용자 정보를 활용할 수 있도록 개선

#### 📂 **수정/생성 대상 파일**
```
src/main/java/com/anyang/maruni/domain/conversation/domain/vo/ConversationContext.java (신규)
src/main/java/com/anyang/maruni/domain/conversation/domain/vo/MemberProfile.java (신규)
src/main/java/com/anyang/maruni/domain/conversation/domain/port/AIResponsePort.java
src/main/java/com/anyang/maruni/domain/conversation/infrastructure/ai/OpenAIResponseAdapter.java
```

#### 🔧 **구체적 작업**

**1. ConversationContext 생성**:
```java
@Getter
@Builder
@AllArgsConstructor
public class ConversationContext {
    private String currentMessage;              // 현재 사용자 메시지
    private List<MessageEntity> recentHistory;  // 최근 대화 히스토리 (최대 5턴)
    private MemberProfile memberProfile;        // 사용자 프로필 정보
    private EmotionType currentEmotion;         // 현재 감정 상태
    private Map<String, Object> metadata;       // 추가 컨텍스트 정보

    // 정적 팩토리 메서드
    public static ConversationContext forUserMessage(
            String message,
            List<MessageEntity> history,
            MemberProfile profile,
            EmotionType emotion) {
        return ConversationContext.builder()
                .currentMessage(message)
                .recentHistory(history.stream().limit(5).collect(Collectors.toList()))
                .memberProfile(profile)
                .currentEmotion(emotion)
                .metadata(new HashMap<>())
                .build();
    }

    public static ConversationContext forSystemMessage(String message, MemberProfile profile) {
        return ConversationContext.builder()
                .currentMessage(message)
                .recentHistory(Collections.emptyList())
                .memberProfile(profile)
                .currentEmotion(EmotionType.NEUTRAL)
                .metadata(new HashMap<>())
                .build();
    }
}
```

**2. MemberProfile 생성**:
```java
@Getter
@Builder
@AllArgsConstructor
public class MemberProfile {
    private Long memberId;
    private String ageGroup;        // "60대", "70대", "80대 이상" 등
    private String personalityType; // "활발함", "내성적", "신중함" 등
    private List<String> healthConcerns; // 건강 관심사
    private EmotionType recentEmotionPattern; // 최근 감정 패턴

    public static MemberProfile createDefault(Long memberId) {
        return MemberProfile.builder()
                .memberId(memberId)
                .ageGroup("70대")
                .personalityType("일반")
                .healthConcerns(Collections.emptyList())
                .recentEmotionPattern(EmotionType.NEUTRAL)
                .build();
    }
}
```

**3. AIResponsePort 개선**:
```java
public interface AIResponsePort {
    String generateResponse(ConversationContext context);

    // 기존 메서드 유지 (하위 호환성)
    @Deprecated
    default String generateResponse(String userMessage) {
        ConversationContext context = ConversationContext.forUserMessage(
                userMessage,
                Collections.emptyList(),
                MemberProfile.createDefault(null),
                EmotionType.NEUTRAL
        );
        return generateResponse(context);
    }
}
```

**4. OpenAIResponseAdapter 개선**:
```java
@Component
public class OpenAIResponseAdapter implements AIResponsePort {

    @Override
    public String generateResponse(ConversationContext context) {
        try {
            String enhancedPrompt = buildPromptWithContext(context);
            String response = callSpringAI(enhancedPrompt);
            return truncateResponse(response);
        } catch (Exception e) {
            return handleApiError(e);
        }
    }

    private String buildPromptWithContext(ConversationContext context) {
        StringBuilder prompt = new StringBuilder();

        // 기본 시스템 프롬프트
        prompt.append(properties.getAi().getSystemPrompt());

        // 사용자 프로필 반영
        MemberProfile profile = context.getMemberProfile();
        prompt.append("\n사용자 정보: ").append(profile.getAgeGroup())
              .append(", 성격: ").append(profile.getPersonalityType());

        if (!profile.getHealthConcerns().isEmpty()) {
            prompt.append(", 건강 관심사: ").append(String.join(", ", profile.getHealthConcerns()));
        }

        // 최근 대화 히스토리 반영
        List<MessageEntity> history = context.getRecentHistory();
        if (!history.isEmpty()) {
            prompt.append("\n\n최근 대화:");
            history.forEach(msg ->
                prompt.append("\n").append(msg.getType() == MessageType.USER_MESSAGE ? "사용자" : "AI")
                      .append(": ").append(msg.getContent())
            );
        }

        // 현재 메시지와 감정 상태
        prompt.append("\n\n현재 메시지: ").append(context.getCurrentMessage());
        prompt.append("\n감정 상태: ").append(context.getCurrentEmotion().getDescription());
        prompt.append("\n\nAI 응답:");

        return prompt.toString();
    }
}
```

#### ✅ **완료 기준**
- [ ] ConversationContext, MemberProfile VO 클래스 생성
- [ ] AIResponsePort 인터페이스 개선 (기존 메서드 @Deprecated 처리)
- [ ] OpenAIResponseAdapter에서 컨텍스트 기반 프롬프트 생성 구현
- [ ] SimpleConversationService에서 ConversationContext 활용
- [ ] 멀티턴 대화 테스트 시나리오 작성

---

### ⚡ **4단계: SimpleConversationService 책임 분리 (2-3일)**

#### 🎯 **목표**
단일 책임 원칙 준수: 27줄 거대 메서드를 3-4개 클래스로 적절히 분리

#### 📂 **생성 대상 파일**
```
src/main/java/com/anyang/maruni/domain/conversation/application/service/MessageProcessor.java (신규)
src/main/java/com/anyang/maruni/domain/conversation/application/mapper/ConversationMapper.java (신규)
```

#### 🔧 **구체적 작업**

**1. MessageProcessor 생성**:
```java
@Service
@RequiredArgsConstructor
public class MessageProcessor {
    private final MessageRepository messageRepository;
    private final AIResponsePort aiResponsePort;
    private final EmotionAnalysisPort emotionAnalysisPort;

    @Transactional
    public MessageExchangeResult processMessage(ConversationEntity conversation, String content) {
        // 1. 사용자 메시지 처리
        EmotionType emotion = emotionAnalysisPort.analyzeEmotion(content);
        MessageEntity userMessage = conversation.addUserMessage(content, emotion);
        messageRepository.save(userMessage);

        // 2. 대화 컨텍스트 구성
        MemberProfile profile = MemberProfile.createDefault(conversation.getMemberId());
        List<MessageEntity> history = conversation.getRecentHistory(5);
        ConversationContext context = ConversationContext.forUserMessage(content, history, profile, emotion);

        // 3. AI 응답 생성
        String aiResponse = aiResponsePort.generateResponse(context);
        MessageEntity aiMessage = conversation.addAIMessage(aiResponse);
        messageRepository.save(aiMessage);

        return MessageExchangeResult.builder()
                .conversation(conversation)
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .build();
    }
}

@Getter
@Builder
@AllArgsConstructor
public class MessageExchangeResult {
    private ConversationEntity conversation;
    private MessageEntity userMessage;
    private MessageEntity aiMessage;
}
```

**2. ConversationMapper 생성**:
```java
@Component
public class ConversationMapper {

    public ConversationResponseDto toResponseDto(MessageExchangeResult result) {
        return ConversationResponseDto.builder()
                .conversationId(result.getConversation().getId())
                .userMessage(MessageDto.from(result.getUserMessage()))
                .aiMessage(MessageDto.from(result.getAiMessage()))
                .build();
    }
}
```

**3. MessageDto 개선**:
```java
public class MessageDto {
    // 기존 필드들...

    public static MessageDto from(MessageEntity entity) {
        return MessageDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .content(entity.getContent())
                .emotion(entity.getEmotion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
```

**4. SimpleConversationService 간소화**:
```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleConversationService {
    private final ConversationManager conversationManager;
    private final MessageProcessor messageProcessor;
    private final ConversationMapper mapper;

    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageExchangeResult result = messageProcessor.processMessage(conversation, content);
        return mapper.toResponseDto(result);
    }

    @Transactional
    public void processSystemMessage(Long memberId, String systemMessage) {
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageEntity systemMessageEntity = conversation.addAIMessage(systemMessage);
        messageRepository.save(systemMessageEntity);
    }
}
```

#### ✅ **완료 기준**
- [ ] MessageProcessor 클래스 생성 및 메시지 처리 로직 이동
- [ ] ConversationMapper 클래스 생성 및 DTO 변환 로직 중앙화
- [ ] MessageDto.from() 정적 팩토리 메서드 추가
- [ ] SimpleConversationService를 5줄 내외의 조율 로직으로 간소화
- [ ] 각 클래스별 단위 테스트 작성
- [ ] 기존 통합 테스트 모두 통과

---

## 🔥 **Medium Priority (점진적 개선)**

### 📅 **5단계: DTO 변환 로직 최적화 (1일)**
- MessageDto.from() 메서드 활용으로 중복 코드 제거
- ConversationMapper에서 복잡한 변환 로직 중앙 관리

### 📅 **6단계: Repository 쿼리 최적화 (1-2일)**
- 중복 쿼리 메서드 통합
- Magic String 제거 (@Param 활용)
- 사용하지 않는 메서드 제거 (YAGNI 적용)

---

## 🟢 **Low Priority (선택적 개선)**

### 📅 **7단계: OpenAIResponseAdapter 구조 유지**
**결정 사항**: 현재 121줄 구조가 이미 적절한 수준이므로 분리하지 않음
- AI 응답 생성이라는 단일 책임에 충실
- 불필요한 복잡성 증가 방지

### 📅 **8단계: EmotionAnalysisPort 재검토**
**Phase 3 계획 확인 후 결정**: ML 도입 계획에 따라 포트 유지 또는 단순화

---

## ✅ **실행 시 주의사항**

### 🔒 **기존 기능 보존**
- 현재 동작하는 모든 API는 Breaking Change 없이 유지
- @Deprecated 어노테이션으로 기존 메서드 유지
- 점진적 리팩토링 (Big Bang 방식 지양)

### 🧪 **테스트 전략**
- **도메인 로직**: 단위 테스트 중심 (ConversationEntity, MessageEntity)
- **Application Service**: 통합 테스트 (SimpleConversationService)
- **Infrastructure**: Mock 기반 테스트 (OpenAIResponseAdapter)

### ⚡ **성능 고려사항**
- N+1 쿼리 방지: @OneToMany(fetch = FetchType.LAZY) 설정
- 대화 히스토리 조회 시 적절한 제한 (최대 5턴)
- OpenAI API 호출 최적화 (불필요한 호출 방지)

### 📋 **각 단계별 검증 항목**
1. 모든 기존 테스트 통과
2. 새로운 테스트 코드 작성 및 통과
3. API 동작 확인 (Postman/Swagger 테스트)
4. 성능 영향도 체크 (API 응답 시간)

---

## 🎯 **최종 목표**

**노인 돌봄 챗봇**에 특화된 **멀티턴 대화 지원**과 **깔끔한 도메인 모델**을 통해:
- 지속적인 대화 관계 형성
- 건강 상태 및 감정 패턴 추적
- 응급 상황 감지 및 보호자 알림 연동
- 유지보수 가능한 코드 구조

**실행 완료 후 conversation 도메인은 DDD 모범사례가 되어 다른 도메인의 리팩토링 기준점 역할을 할 것입니다.**