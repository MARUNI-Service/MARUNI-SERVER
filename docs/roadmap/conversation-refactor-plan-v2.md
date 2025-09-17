# Conversation 도메인 리팩토링 분석 보고서 (v2.2)

**작성일**: 2025-09-17
**분석 대상**: conversation 도메인 전체 구조
**분석 관점**: DDD 구조화, 책임 분리, 오버 엔지니어링 여부
**업데이트**: 노인 돌봄 도메인 특성 반영 (멀티턴 대화 필수)

### 📝 **v2.2 주요 변경사항 (도메인 특성 재검토)**
- **🎯 도메인 우선**: 노인 돌봄 챗봇 = 지속적 대화가 핵심 요구사항
- **🔥 멀티턴 대화**: YAGNI 대상이 아닌 **즉시 필요한 핵심 기능**
- **📈 ConversationContext**: Medium → **High Priority**로 승격
- **🏥 도메인 특화**: 건강 상태, 감정 패턴, 증상 추적 등 전문 요구사항 반영

---

## 📋 **Executive Summary (v2.2 - 도메인 특성 반영)**

conversation 도메인은 **전반적으로 잘 설계**되어 있으나, **노인 돌봄 챗봇**이라는 도메인 특성을 고려할 때 **멀티턴 대화**와 **도메인 특화 기능**이 즉시 필요합니다. **도메인 전문성**이 **일반적인 YAGNI 원칙**보다 우선합니다.

**🔥 즉시 해결 필요 (도메인 핵심 요구사항)**:
- **멀티턴 대화 지원** (ConversationContext 도입) - 노인 돌봄의 핵심
- **Anemic Domain Model** → Rich Domain Model (최우선)
- **Repository 비즈니스 로직 분리** (Clean Architecture 기본 원칙)

**🏥 도메인 특화 요구사항**:
- 건강 상태 추적 (증상, 약물, 병원 방문 등)
- 감정 패턴 분석 (우울감, 외로움 감지)
- 보호자 알림 연동 (응급 상황 감지)

**권장 방향**: **도메인 전문성 우선** + **지속적 돌봄 관점**

---

## 🏗️ **1. DDD 구조 분석**

### ✅ **잘 구현된 부분**

#### 계층 분리
```
✓ Presentation Layer: ConversationController
✓ Application Layer: SimpleConversationService, DTOs
✓ Domain Layer: Entities, Repositories, Ports
✓ Infrastructure Layer: OpenAIResponseAdapter, KeywordBasedEmotionAnalyzer
```

#### 의존성 방향
```
✓ Presentation → Application → Domain ← Infrastructure
✓ 도메인이 중심이 되는 올바른 구조
✓ Port-Adapter 패턴으로 외부 의존성 격리
```

### ⚠️ **문제점 및 개선 필요 사항**

#### 1) **Anemic Domain Model (빈혈 모델)**

**현재 문제**:
```java
@Entity
public class ConversationEntity extends BaseTimeEntity {
    private Long id;
    private Long memberId;
    private LocalDateTime startedAt;
    // 비즈니스 로직 없음 - 단순한 데이터 홀더
}
```

**문제점**:
- 엔티티가 상태만 가지고 행동이 없음
- 비즈니스 로직이 Service 계층에 집중
- OOP 원칙 위반 (Tell, Don't Ask)

**개선안**:
```java
@Entity
public class ConversationEntity extends BaseTimeEntity {
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<MessageEntity> messages = new ArrayList<>();

    // 도메인 로직 추가
    public boolean isActive() {
        return messages.isEmpty() ||
               getLastMessageTime().isAfter(LocalDateTime.now().minusDays(1));
    }

    public Message addUserMessage(String content, EmotionType emotion) {
        validateMessageContent(content);
        Message message = MessageEntity.createUserMessage(this.id, content, emotion);
        this.messages.add(message);
        return message;
    }

    public boolean canReceiveMessage() {
        return isActive() && messages.size() < MAX_DAILY_MESSAGES;
    }

    private void validateMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new InvalidMessageException("메시지 내용이 비어있습니다");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidMessageException("메시지가 너무 깁니다");
        }
    }
}
```

#### 2) **Aggregate 경계 모호함**

**현재 문제**:
- `Conversation`과 `Message`의 관계가 느슨함
- `Message`가 독립적으로 생성/수정 가능
- 불변성 보장 어려움

**개선안**:
```java
// Conversation을 Aggregate Root로 명확히 설정
@Entity
public class ConversationEntity { // Aggregate Root
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageEntity> messages = new ArrayList<>();

    // Message는 반드시 Conversation을 통해서만 생성/수정
    public Message sendMessage(String content, MessageType type, EmotionType emotion) {
        // 비즈니스 규칙 검증
        // 메시지 생성 및 추가
    }
}

// MessageRepository는 조회 전용으로 제한
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    // 생성/수정 메서드 제거, 조회만 허용
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
```

---

## 🎯 **2. 책임 분리 분석**

### ⚠️ **주요 문제점**

#### 1) **SimpleConversationService - 과도한 책임 (SRP 위반)**

**현재 문제**:
```java
@Transactional
public ConversationResponseDto processUserMessage(Long memberId, String content) {
    // 1. 대화 세션 관리 (5줄)
    ConversationEntity conversation = findOrCreateActiveConversation(memberId);

    // 2. 메시지 저장 (3줄)
    MessageEntity userMessage = saveUserMessage(conversation.getId(), content);

    // 3. AI 응답 생성 조율 (2줄)
    String aiResponse = aiResponsePort.generateResponse(content);

    // 4. AI 메시지 저장 (2줄)
    MessageEntity aiMessage = saveAIMessage(conversation.getId(), aiResponse);

    // 5. DTO 변환 (15줄의 빌더 패턴 코드)
    return ConversationResponseDto.builder()
        .conversationId(conversation.getId())
        .userMessage(MessageDto.builder()
            .id(userMessage.getId())
            .type(userMessage.getType())
            .content(userMessage.getContent())
            .emotion(userMessage.getEmotion())
            .createdAt(userMessage.getCreatedAt())
            .build())
        .aiMessage(MessageDto.builder()
            .id(aiMessage.getId())
            .type(aiMessage.getType())
            .content(aiMessage.getContent())
            .emotion(aiMessage.getEmotion())
            .createdAt(aiMessage.getCreatedAt())
            .build())
        .build();
}
```

**문제점**:
- 하나의 메서드가 5가지 서로 다른 책임을 가짐
- 27줄의 긴 메서드 (Clean Code 기준 위반)
- 변경 사유가 5가지 (SRP 위반)

**개선안**:
```java
// 1. 도메인 서비스 도입
@Service
public class ConversationManager {
    public ConversationEntity findOrCreateActive(Long memberId) { /* 대화 관리 전담 */ }
}

@Service
public class MessageProcessor {
    public MessageExchangeResult processMessage(ConversationEntity conversation, String content) {
        // 메시지 처리 전담
    }
}

// 2. 매퍼 클래스 도입
@Component
public class ConversationMapper {
    public ConversationResponseDto toResponseDto(MessageExchangeResult result) { /* 변환 전담 */ }
}

// 3. Application Service는 조율만 담당
@Service
public class ConversationService {
    private final ConversationManager conversationManager;
    private final MessageProcessor messageProcessor;
    private final ConversationMapper mapper;

    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageExchangeResult result = messageProcessor.processMessage(conversation, content);
        return mapper.toResponseDto(result);
    }
}
```

#### 2) **OpenAIResponseAdapter - 현재 구조 유지 권장** ✅ **적절한 수준**

**현재 구조 분석**:
```java
public class OpenAIResponseAdapter implements AIResponsePort {
    public String generateResponse(String userMessage) {
        // 1. 입력 검증 (sanitizeUserMessage)
        // 2. API 호출 (callSpringAI)
        // 3. 응답 처리 (truncateResponse)
        // 4. 에러 처리 (handleApiError)
        // 5. 설정 관리 (@Value 어노테이션들)
    }
}
```

**현재 구조 유지 근거**:
1. **적절한 응집도**: 모든 메서드가 "AI 응답 생성"이라는 단일 목적에 집중
2. **명확한 책임**: OpenAI API 관련 모든 로직이 한 곳에 응집
3. **현실적 복잡도**: 121줄로 관리 가능한 수준의 클래스 크기
4. **변경 빈도**: AI 관련 로직은 함께 변경되는 경우가 많음

**과도한 분리의 문제점**:
- **불필요한 복잡성**: 3개 클래스로 분리 시 오히려 추적 어려움
- **인위적 책임 분할**: sanitize와 truncate는 본질적으로 하나의 플로우
- **YAGNI 위반**: 현재 요구사항에 비해 과도한 설계

**최종 권장사항**: **현재 OpenAIResponseAdapter 구조 유지**
```java
// 현재 구조가 이미 적절함 - 분리하지 않고 유지
@Component
public class OpenAIResponseAdapter implements AIResponsePort {
    // 입력 검증, API 호출, 응답 처리, 에러 처리를 하나의 클래스에서 관리
    // → AI 응답 생성이라는 단일 책임에 충실한 구조
}
```

#### 3) **Repository의 비즈니스 로직 포함** 🔥 **타협 불가 (Gemini 의견과 다름)**

**현재 문제**:
```java
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    // 명확한 안티패턴: Repository에 비즈니스 규칙 하드코딩
    default Optional<ConversationEntity> findActiveByMemberId(Long memberId) {
        return findTopByMemberIdOrderByCreatedAtDesc(memberId); // "최신 = 활성"이라는 비즈니스 규칙
    }
}
```

**문제점**:
- "가장 최근 대화 = 활성 대화"라는 비즈니스 규칙이 Repository에 하드코딩됨
- Clean Architecture 기본 원칙 위반 (Repository는 데이터 접근만 담당)
- 활성 상태 정의 변경 시 Repository 수정 필요 (OCP 위반)

**개선안 (Clean Architecture 준수)**:
```java
// Repository는 순수 데이터 접근만
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    Optional<ConversationEntity> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<ConversationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

// 비즈니스 로직은 도메인 서비스로 이동
@Service
public class ConversationManager {
    public ConversationEntity findActiveConversation(Long memberId) {
        return conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
            .filter(ConversationEntity::isActive) // 도메인 로직 활용
            .orElse(null);
    }
}
```

---

## 🔌 **3. Port-Adapter 패턴 적절성 분석**

### ✅ **잘된 부분**

#### Port-Adapter 패턴 적용
```
✓ 의존성 역전: 도메인이 인프라에 의존하지 않음
✓ 인터페이스 분리: AIResponsePort, EmotionAnalysisPort
✓ 확장성: 다른 AI 모델로 교체 가능
```

#### 실제 활용 가능성
```
✓ OpenAI → Claude → Gemini 교체 시나리오 실존
✓ 키워드 기반 → ML 기반 감정분석 전환 계획 있음
✓ 설정 중앙화로 운영 편의성 확보
```

### ⚠️ **개선 필요 사항**

#### 1) **포트 인터페이스가 너무 단순함**

**현재 문제**:
```java
public interface AIResponsePort {
    String generateResponse(String userMessage); // 단순한 String → String
}
```

**문제점**:
- 대화 히스토리를 활용할 수 없음
- 사용자 정보(나이, 상황 등)를 반영할 수 없음
- 시스템 메시지와 일반 메시지 구분 불가
- 멀티턴 대화 지원 어려움

**개선안**:
```java
public interface AIResponsePort {
    String generateResponse(ConversationContext context);
}

public class ConversationContext {
    private String currentMessage;              // 현재 메시지
    private List<Message> recentHistory;        // 최근 대화 히스토리 (N턴)
    private MemberProfile memberProfile;        // 사용자 정보 (나이, 특성 등)
    private MessageType expectedResponseType;   // 기대하는 응답 타입
    private Map<String, Object> metadata;       // 추가 컨텍스트 정보

    // 정적 팩토리 메서드들
    public static ConversationContext forUserMessage(String message, List<Message> history, MemberProfile profile);
    public static ConversationContext forSystemMessage(String message, MemberProfile profile);
}
```

#### 2) **EmotionAnalysisPort의 과도한 추상화 의심**

**현재 상황**:
```java
// 현재: 단순한 키워드 매칭만 수행
public class KeywordBasedEmotionAnalyzer implements EmotionAnalysisPort {
    public EmotionType analyzeEmotion(String message) {
        // List.contains() 호출만 함
        return keywords.get("negative").stream().anyMatch(message::contains)
            ? EmotionType.NEGATIVE : EmotionType.POSITIVE;
    }
}
```

**판단 기준**:
- ✅ **포트 유지 권장**: ML 모델 도입 계획이 있다면
- ⚠️ **단순화 고려**: 단순 키워드 매칭만 계속한다면

**개선안 (ML 도입 시)**:
```java
public interface EmotionAnalysisPort {
    EmotionAnalysisResult analyzeEmotion(EmotionAnalysisRequest request);
}

public class EmotionAnalysisRequest {
    private String message;
    private String previousContext;  // 이전 대화 맥락
    private MemberProfile profile;   // 개인별 감정 패턴
}

public class EmotionAnalysisResult {
    private EmotionType primaryEmotion;
    private Map<EmotionType, Double> emotionScores;  // 각 감정별 점수
    private double confidence;                       // 신뢰도
    private List<String> detectedKeywords;          // 감지된 키워드들
}
```

---

## 🔍 **4. 코드 품질 및 확장성 분석**

### ⚠️ **주요 문제점**

#### 1) **중복 코드**

**DTO 변환 로직 중복**:
```java
// SimpleConversationService에서 15줄이 인라인으로 반복
.userMessage(MessageDto.builder()
    .id(userMessage.getId())
    .type(userMessage.getType())
    .content(userMessage.getContent())
    .emotion(userMessage.getEmotion())
    .createdAt(userMessage.getCreatedAt())
    .build())
.aiMessage(MessageDto.builder()
    .id(aiMessage.getId())
    .type(aiMessage.getType())
    .content(aiMessage.getContent())
    .emotion(aiMessage.getEmotion())
    .createdAt(aiMessage.getCreatedAt())
    .build())
```

**개선안**:
```java
// MessageDto에 정적 팩토리 메서드 추가
public class MessageDto {
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

// 사용부 간소화
return ConversationResponseDto.builder()
    .conversationId(conversation.getId())
    .userMessage(MessageDto.from(userMessage))
    .aiMessage(MessageDto.from(aiMessage))
    .build();
```

#### 2) **Repository 쿼리 중복 및 최적화 문제**

**현재 문제**:
```java
// 비슷한 패턴의 쿼리 중복
@Query("SELECT m FROM MessageEntity m WHERE m.conversationId IN ...")
List<MessageEntity> findRecentUserMessagesByMemberId(...);

@Query("SELECT m FROM MessageEntity m WHERE m.conversationId IN ...")
List<MessageEntity> findRecentUserMessagesByMemberIdAndDays(...);
```

**개선안**:
```java
// 하나의 유연한 메서드로 통합
@Query("SELECT m FROM MessageEntity m " +
       "JOIN ConversationEntity c ON m.conversationId = c.id " +
       "WHERE c.memberId = :memberId " +
       "AND m.type = :messageType " +
       "AND (:startDate IS NULL OR m.createdAt >= :startDate) " +
       "ORDER BY m.createdAt DESC")
List<MessageEntity> findMessagesByMemberIdAndCriteria(
    @Param("memberId") Long memberId,
    @Param("messageType") MessageType messageType,
    @Param("startDate") LocalDateTime startDate);
```

#### 3) **Magic String 및 하드코딩**

**현재 문제**:
```java
@Query("... AND m.type = 'USER_MESSAGE' ...") // 하드코딩된 Enum 값
List<MessageEntity> findRecentUserMessagesByMemberIdAndDays(...);
```

**개선안**:
```java
@Query("... AND m.type = :messageType ...")  // 파라미터로 전달
List<MessageEntity> findRecentUserMessagesByMemberIdAndDays(
    @Param("messageType") MessageType messageType, ...);
```

### 📈 **확장성 문제**

#### 1) **대화 히스토리 활용 불가**

**현재 제약**:
- AI에게 이전 대화 내용을 전달할 수 없음
- 멀티턴 대화 지원 어려움
- 사용자별 대화 패턴 학습 불가

**개선안**:
```java
// ConversationContext를 통한 히스토리 전달
public class OpenAIResponseAdapter implements AIResponsePort {
    public String generateResponse(ConversationContext context) {
        String prompt = buildPromptWithHistory(
            context.getCurrentMessage(),
            context.getRecentHistory(),
            context.getMemberProfile()
        );
        return chatModel.call(prompt);
    }

    private String buildPromptWithHistory(String message, List<Message> history, MemberProfile profile) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(systemPrompt);

        // 사용자 프로필 반영
        prompt.append("\n사용자 정보: ").append(profile.getAgeGroup()).append(", ")
              .append(profile.getPersonalityType());

        // 최근 대화 히스토리 반영 (최대 5턴)
        prompt.append("\n최근 대화:");
        history.stream().limit(5).forEach(msg ->
            prompt.append("\n").append(msg.getType()).append(": ").append(msg.getContent()));

        // 현재 메시지
        prompt.append("\n사용자: ").append(message);
        prompt.append("\nAI:");

        return prompt.toString();
    }
}
```

#### 2) **메시지 타입 확장성 부족**

**현재 제약**:
```java
public enum MessageType {
    USER_MESSAGE,    // 사용자 메시지
    AI_RESPONSE      // AI 응답
    // 향후 필요할 수 있는 타입들이 없음
}
```

**개선안**:
```java
public enum MessageType {
    USER_MESSAGE,        // 사용자 일반 메시지
    AI_RESPONSE,         // AI 일반 응답
    SYSTEM_MESSAGE,      // 시스템 알림 (스케줄링)
    EMERGENCY_ALERT,     // 긴급 알림
    HEALTH_CHECK,        // 건강 상태 확인
    MOOD_INQUIRY,        // 기분 문의
    FOLLOW_UP           // 후속 질문
}
```

---

## ⚖️ **5. 오버 엔지니어링 여부 판단**

### ✅ **적절한 설계 (유지 권장)**

#### 1) **Port-Adapter 패턴**
```
판단: 적절함 ✅
이유:
- AI 모델 교체 가능성이 실제로 있음 (OpenAI → Claude → Gemini)
- 외부 API 의존성이 높은 도메인 특성상 필수
- 테스트 시 Mock 객체 활용 가능
```

#### 2) **설정 중앙화 (ConversationProperties)**
```
판단: 적절함 ✅
이유:
- AI 파라미터 (temperature, max-tokens)는 운영 중 조정 필요
- 감정 키워드는 언어/문화별 차이로 변경 필요
- 응답 길이 제한은 채널별로 다를 수 있음
```

#### 3) **DDD 계층 분리**
```
판단: 적절함 ✅
이유:
- 복잡한 비즈니스 로직을 가진 도메인 (AI 대화)
- 외부 시스템 의존성이 높음 (OpenAI, 감정분석)
- 향후 기능 확장 계획이 명확함
```

### ⚠️ **오버 엔지니어링 의심 부분**

#### 1) **과도한 Repository 메서드**
```
의심 대상:
- findByEmotionAndTypeOrderByCreatedAtDesc()
- findRecentUserMessagesByMemberIdAndDays()

판단 기준: YAGNI (You Aren't Gonna Need It)
권장: 실제 사용하지 않는 메서드는 제거
```

#### 2) **EmotionAnalysisPort의 필요성**
```
현재 구현: 단순한 키워드 매칭 (List.contains)
포트 필요성:
- ✅ ML 도입 계획이 확실하다면 유지
- ⚠️ 단순 키워드만 계속한다면 도메인 서비스로 단순화 고려
```

#### 3) **과도한 DTO 계층**
```
현재: ConversationRequestDto, ConversationResponseDto, MessageDto
판단: 적절함 (API 계약과 도메인 모델 분리를 위해 필요)
```

### 🎯 **최종 판단: 적절한 설계**

**전체 평가**: 오버 엔지니어링보다는 **적절한 수준의 아키텍처 설계**

**근거**:
1. **실제 요구사항 존재**: AI 모델 교체, 감정분석 고도화 등
2. **복잡성 대비 적절**: 외부 시스템 의존성이 높은 도메인 특성
3. **확장 계획 명확**: Phase 3에서 고급 AI 기능 도입 예정

---

## 🎯 **최종 리팩토링 우선순위 권고**

### 🔥 **High Priority (즉시 개선 권장)**

#### 1) **SimpleConversationService 책임 분리**
**예상 소요 시간**: 2-3일

**Before**:
```java
// 27줄의 거대한 메서드, 5가지 책임
public ConversationResponseDto processUserMessage(Long memberId, String content) {
    // 대화 관리 + 메시지 저장 + AI 호출 + DTO 변환 + 트랜잭션 관리
}
```

**After**:
```java
// 책임별 클래스 분리
@Service public class ConversationManager { /* 대화 관리 */ }
@Service public class MessageProcessor { /* 메시지 처리 */ }
@Component public class ConversationMapper { /* DTO 변환 */ }

@Service
public class ConversationService {
    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        // 5줄 내외의 조율 로직만
    }
}
```

**기대 효과**:
- 단일 책임 원칙 준수
- 테스트 용이성 향상
- 코드 가독성 개선
- 변경 영향 최소화

#### 2) **도메인 모델 강화 (Anemic → Rich Domain Model)**
**예상 소요 시간**: 3-4일

**Before**:
```java
// 빈혈 모델: 단순 데이터 홀더
@Entity
public class ConversationEntity extends BaseTimeEntity {
    private Long id;
    private Long memberId;
    private LocalDateTime startedAt;
    // 비즈니스 로직 없음
}
```

**After**:
```java
// 풍부한 도메인 모델: 비즈니스 로직 포함
@Entity
public class ConversationEntity extends BaseTimeEntity {
    @OneToMany(cascade = CascadeType.ALL)
    private List<MessageEntity> messages = new ArrayList<>();

    // 도메인 로직
    public boolean isActive() { /* 활성 상태 판단 */ }
    public Message addUserMessage(String content, EmotionType emotion) { /* 메시지 추가 */ }
    public boolean canReceiveMessage() { /* 수신 가능 여부 */ }
    public List<Message> getRecentHistory(int count) { /* 최근 히스토리 */ }
}
```

**기대 효과**:
- 도메인 로직의 응집도 향상
- 불변성 보장
- 비즈니스 규칙의 중앙화
- Service 계층 복잡도 감소

### 🔥 **High Priority (도메인 핵심 요구사항)**

#### 3) **AIResponsePort 개선 - 멀티턴 대화 지원** 🏥 **도메인 특성 (Medium → High 승격)**
**예상 소요 시간**: 3-4일

**노인 돌봄 대화 시나리오**:
```
👵 "오늘 기분이 좀 안 좋아요"
🤖 "어떤 일이 있으셨나요? 말씀해보세요"
👵 "무릎이 아파서 산책을 못 갔어요"
🤖 "무릎이 아프시는군요. 언제부터 아프셨나요?" (이전 맥락 필요)
👵 "어제부터요. 계단 오르내릴 때 특히 아파요"
🤖 "병원에 가보시는 것이 좋겠어요. 가족분께 연락해드릴까요?" (증상 종합 판단)
```

**필수 개선안**:
```java
// 기존: 단순한 String → String (맥락 상실)
public interface AIResponsePort {
    String generateResponse(String userMessage);
}

// 개선: 노인 돌봄 특화 컨텍스트 지원
public interface AIResponsePort {
    String generateResponse(ConversationContext context);
}

public class ConversationContext {
    private String currentMessage;
    private List<Message> recentHistory;        // 대화 맥락 유지 (필수)
    private MemberProfile memberProfile;        // 나이, 건강상태 (필수)
    private HealthContext healthContext;        // 증상, 약물 정보 (도메인 특화)
    private EmotionPattern recentEmotions;      // 감정 변화 추적 (돌봄 핵심)
    private Set<String> mentionedSymptoms;      // 언급된 증상들
    private AlertLevel currentAlertLevel;       // 위험도 평가
}
```

**도메인 필요성**:
- **지속적 돌봄**: 단발성 대화가 아닌 장기간 관계 형성 필요
- **건강 상태 추적**: 증상 변화, 약물 복용, 병원 방문 등 연속성 중요
- **감정 패턴 분석**: 우울감, 외로움 등 장기적 변화 모니터링

### 🟡 **Medium Priority (점진적 개선)**

#### 4) **DTO 변환 로직 개선**
**예상 소요 시간**: 1일

**개선 내용**:
```java
// MessageDto에 팩토리 메서드 추가
public static MessageDto from(MessageEntity entity) { /* 변환 로직 */ }

// ConversationMapper 클래스 신설
@Component
public class ConversationMapper {
    public ConversationResponseDto toResponseDto(MessageExchangeResult result) { /* */ }
}
```

**기대 효과**:
- 코드 중복 제거 (15줄 → 1줄)
- 변환 로직 중앙화
- 유지보수성 향상

### 🟢 **Low Priority (선택적 개선)**

#### 5) **Repository 쿼리 최적화**
**예상 소요 시간**: 1-2일

**개선 내용**:
- 중복 쿼리 메서드 통합
- Magic String 제거
- 사용하지 않는 메서드 제거 (YAGNI 적용)

#### 6) **OpenAIResponseAdapter 과도한 분리 지양** ✅ **현재 구조 유지**
**결정 사항**: 현재 121줄의 단일 클래스 구조가 이미 적절한 수준
- AI 응답 생성이라는 단일 책임에 충실
- 불필요한 복잡성 증가 방지
- YAGNI 원칙에 부합

#### 7) **EmotionAnalysisPort 재검토** ⚠️ **Phase 3 계획 확인 후 결정**
**예상 소요 시간**: Phase 3 로드맵 확인 후 결정

**현재 상황**:
```java
// 단순한 키워드 매칭만 수행 (List.contains 수준)
public class KeywordBasedEmotionAnalyzer implements EmotionAnalysisPort {
    public EmotionType analyzeEmotion(String message) {
        return keywords.get("negative").stream().anyMatch(message::contains)
            ? EmotionType.NEGATIVE : EmotionType.POSITIVE;
    }
}
```

**판단 기준 (Gemini 리뷰 반영)**:
- **옵션 1**: Phase 3에서 6개월 내 ML 감정분석 도입 계획 있음 → 포트 유지
- **옵션 2**: 단순 키워드 매칭만 계속 사용 → 도메인 서비스로 단순화
- **결정 방법**: `docs/roadmap/phase3.md` 확인 후 실용적 판단

---

## 📊 **리팩토링 로드맵 (v2.2 - 도메인 특성 반영)**

### **🔥 Week 1: 핵심 안티패턴 해결 + 도메인 모델 강화**
- [ ] **Repository 비즈니스 로직 분리** (최우선 - Clean Architecture 기본 원칙)
- [ ] **ConversationEntity 도메인 로직 추가** (Anemic → Rich Domain Model)
- [ ] **도메인 특화 엔티티 설계** (HealthContext, EmotionPattern 등)
- [ ] 기본 테스트 코드 작성

### **🏥 Week 2: 멀티턴 대화 지원 (노인 돌봄 핵심)**
- [ ] **ConversationContext 도입** (건강상태, 감정패턴, 대화맥락 포함)
- [ ] **AIResponsePort 인터페이스 개선** (단순 String → Context 기반)
- [ ] **OpenAIResponseAdapter 개선** (멀티턴 대화 지원)
- [ ] **대화 히스토리 관리** 로직 구현

### **🟡 Week 3: 적정 수준 책임 분리 (YAGNI 적용)**
- [ ] **SimpleConversationService 책임 분리** (3-4개 클래스로 적절히)
- [ ] **ConversationManager, MessageProcessor 도입**
- [ ] **도메인 서비스 계층** 구성 (HealthTracker, EmotionAnalyzer 등)
- [ ] **OpenAIResponseAdapter 구조 유지** (과도한 분리 지양)
- [ ] **EmotionAnalysisPort 재검토** (Phase 3 ML 계획 확인 후)

### **🟢 Week 4: 도메인 특화 기능 + 품질 개선**
- [ ] **AlertRule 도메인 연동** 강화 (건강 이상 징후 감지)
- [ ] **Guardian 도메인 연동** (응급 상황 알림)
- [ ] DTO 변환 로직 최적화 (MessageDto.from() 등)
- [ ] 중복 코드 제거 및 통합 테스트 강화

---

## 📝 **리팩토링 시 주의사항**

### **기존 기능 보존**
- 현재 동작하는 모든 API는 Breaking Change 없이 유지
- 점진적 리팩토링 (Big Bang 방식 지양)
- 각 단계별 테스트 코드 필수

### **성능 고려사항**
- N+1 쿼리 문제 주의 (Conversation ↔ Message 관계)
- 대화 히스토리 조회 시 적절한 페이징 적용
- OpenAI API 호출 최적화 (불필요한 호출 방지)

### **테스트 전략**
- 도메인 로직: 단위 테스트 중심
- Application Service: 통합 테스트
- Infrastructure: Mock 기반 테스트

---

## 🎯 **최종 결론 (v2.2 - 도메인 전문성 우선)**

conversation 도메인은 **전반적으로 적절한 수준의 아키텍처 설계**가 이루어져 있으나, **노인 돌봄 챗봇**이라는 도메인 특성을 고려할 때 **멀티턴 대화와 지속적 관계 형성**이 핵심 요구사항임을 재확인했습니다.

**🏥 도메인 특성 재평가**:
- **지속적 돌봄**: 단발성 대화가 아닌 장기간 관계 형성이 핵심
- **건강 상태 추적**: 증상 변화, 감정 패턴, 응급 상황 감지 필요
- **멀티턴 대화**: YAGNI 대상이 아닌 **도메인 본질적 요구사항**

**🔥 즉시 해결 (도메인 핵심)**:
- 🏥 **멀티턴 대화 지원** (ConversationContext 도입) - 돌봄의 핵심
- 🚨 **Repository 비즈니스 로직 분리** (Clean Architecture 기본 원칙)
- 🔧 **Anemic Domain Model → Rich Domain Model** (도메인 로직 강화)

**📋 핵심 교훈**:
- **도메인 전문성** > **일반적인 개발 원칙** (YAGNI 등)
- 노인 돌봄 = 지속적 관계 형성 + 건강 상태 모니터링
- 기술적 완성도보다는 **실제 돌봄 효과**에 집중

**최종 방향**: **도메인 전문성 우선** + **지속적 돌봄 관점**으로 노인분들께 실질적 도움이 되는 conversation 도메인 구축

---

### 🎭 **Gemini vs 도메인 전문가 관점 비교**

| 관점 | Gemini (일반 개발) | 도메인 전문가 (노인 돌봄) |
|------|-----------------|----------------------|
| 멀티턴 대화 | YAGNI 적용 대상 | **핵심 요구사항** |
| ConversationContext | 선제적 설계 | **즉시 필요** |
| 우선순위 | 기술적 완성도 | **돌봄 효과** |
| 결론 | 신중한 접근 | **도메인 특성 우선** |

**결론**: 도메인 전문성이 일반적인 개발 원칙보다 우선하는 좋은 사례