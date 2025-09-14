# Phase 1: AI 대화 시스템 MVP 구현 계획

## 📋 문서 개요

**MARUNI Phase 1 MVP**에서는 OpenAI GPT-4o를 활용한 기본 AI 대화 시스템을 구축합니다.
핵심 기능에 집중하여 빠르게 동작하는 시스템을 완성한 후, 단계적으로 고급 기능을 추가해나가는 접근을 취합니다.

**🎯 MVP 전략**: 기본 기능 우선 → 점진적 개선  
**구현 기간**: 2주 (실용적 접근)  
**핵심 목표**: OpenAI API 연동 → 기본 대화 저장 → 간단 감정 분석

### MVP 범위 정의
**✅ 포함 기능 (핵심)**
- 사용자 메시지 → AI 응답 생성
- 대화 데이터 저장 (PostgreSQL)
- 기본 감정 분석 (긍정/중립/부정)
- REST API 제공 (SMS 연동 준비)

**⏳ 차후 추가 (고급)**
- 복잡한 대화 맥락 관리 (Redis 캐싱)
- 정교한 이상징후 감지 시스템
- 이벤트 기반 보호자 알림
- 성능 최적화 및 확장성 개선

---

## 🏗️ 1. MVP 아키텍처 설계 (간소화)

### 1.1 핵심 도메인 모델 (MVP 버전)

#### Core Entities (MVP 최소화)
```java
// 기본 대화 세션
@Entity
@Table(name = "conversations")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long memberId;           // Member 도메인 참조
    
    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
}

// 기본 메시지
@Entity
@Table(name = "messages")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long conversationId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;        // USER_MESSAGE, AI_RESPONSE
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    // MVP: 간단한 감정 분석만
    @Enumerated(EnumType.STRING)
    private EmotionType emotion;     // POSITIVE, NEUTRAL, NEGATIVE
}

// 간단한 Enum 들
public enum MessageType { USER_MESSAGE, AI_RESPONSE }
public enum EmotionType { POSITIVE, NEUTRAL, NEGATIVE }
```

#### MVP 도메인 서비스 (기본 기능)
```java
// 기본 AI 응답 생성자 (MVP)
@Component
@RequiredArgsConstructor
public class SimpleAIResponseGenerator {
    
    private final OpenAiService openAiService;
    
    // 핵심 기능: 사용자 메시지에 대한 AI 응답 생성
    public String generateResponse(String userMessage) {
        String prompt = buildSimplePrompt(userMessage);
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4o")
            .messages(List.of(
                new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                    "당신은 노인 돌봄 전문 AI 상담사입니다. 따뜻하고 공감적으로 30자 이내로 응답하세요."),
                new ChatMessage(ChatMessageRole.USER.value(), userMessage)
            ))
            .maxTokens(100)
            .temperature(0.7)
            .build();
            
        ChatCompletionResult result = openAiService.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent().trim();
    }
    
    // 기본 기능: 간단한 감정 분석
    public EmotionType analyzeBasicEmotion(String message) {
        // 간단한 키워드 기반 감정 분석 (MVP)
        String lowerMessage = message.toLowerCase();
        
        // 부정적 키워드 체크
        if (lowerMessage.contains("슬프") || lowerMessage.contains("우울") || 
            lowerMessage.contains("아프") || lowerMessage.contains("힘들")) {
            return EmotionType.NEGATIVE;
        }
        
        // 긍정적 키워드 체크  
        if (lowerMessage.contains("좋") || lowerMessage.contains("행복") ||
            lowerMessage.contains("기쁘") || lowerMessage.contains("감사")) {
            return EmotionType.POSITIVE;
        }
        
        return EmotionType.NEUTRAL;
    }
    
    private String buildSimplePrompt(String userMessage) {
        return String.format("""
            사용자: "%s"
            
            위 메시지에 따뜻하고 공감적으로 응답해주세요. 
            SMS 특성을 고려하여 30자 이내로 간결하게 답변하세요.
            """, userMessage);
    }
}

// 기본 대화 서비스 (MVP)
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleConversationService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpleAIResponseGenerator aiResponseGenerator;
    
    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        // 1. 대화 조회/생성
        ConversationEntity conversation = findOrCreateConversation(memberId);
        
        // 2. 사용자 메시지 저장
        MessageEntity userMessage = MessageEntity.builder()
            .conversationId(conversation.getId())
            .type(MessageType.USER_MESSAGE)
            .content(content)
            .emotion(aiResponseGenerator.analyzeBasicEmotion(content))
            .build();
        messageRepository.save(userMessage);
        
        // 3. AI 응답 생성
        String aiResponseContent = aiResponseGenerator.generateResponse(content);
        
        // 4. AI 응답 저장
        MessageEntity aiMessage = MessageEntity.builder()
            .conversationId(conversation.getId())
            .type(MessageType.AI_RESPONSE)
            .content(aiResponseContent)
            .emotion(EmotionType.NEUTRAL) // AI 응답은 중립
            .build();
        messageRepository.save(aiMessage);
        
        return ConversationResponseDto.from(aiMessage);
    }
    
    private ConversationEntity findOrCreateConversation(Long memberId) {
        return conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
            .orElseGet(() -> conversationRepository.save(
                ConversationEntity.builder()
                    .memberId(memberId)
                    .startedAt(LocalDateTime.now())
                    .build()
            ));
    }
}
```

### 1.2 Repository 레이어 (기본)

```java
// 기본 대화 Repository
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    
    // 회원의 가장 최근 대화 조회
    Optional<ConversationEntity> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
    
    // 회원의 모든 대화 조회
    List<ConversationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

// 기본 메시지 Repository
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    
    // 대화의 모든 메시지 조회 (시간순)
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    // 대화의 최근 메시지들 조회 (페이징)
    Page<MessageEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
    
    // 특정 감정의 메시지 조회 (분석용)
    List<MessageEntity> findByEmotionAndTypeOrderByCreatedAtDesc(EmotionType emotion, MessageType type, Pageable pageable);
}
```

### 1.3 REST API 레이어

```java
// 기본 대화 컨트롤러
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@AutoApiResponse
@CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
public class ConversationController {
    
    private final SimpleConversationService conversationService;
    
    @PostMapping("/message")
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    @Operation(summary = "사용자 메시지 처리 및 AI 응답 생성")
    public ConversationResponseDto processMessage(
            @Valid @RequestBody ProcessMessageRequestDto request) {
        return conversationService.processUserMessage(
            request.getMemberId(), 
            request.getContent()
        );
    }
    
    @GetMapping("/{conversationId}/messages")
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    @Operation(summary = "대화 메시지 히스토리 조회")
    public MessageHistoryDto getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return conversationService.getMessageHistory(conversationId, page, size);
    }
    
    @GetMapping("/member/{memberId}")
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    @Operation(summary = "회원의 대화 목록 조회")
    public List<ConversationSummaryDto> getMemberConversations(@PathVariable Long memberId) {
        return conversationService.getMemberConversations(memberId);
    }
}
```

### 1.4 DTO 설계 (기본)

```java
// 메시지 처리 요청 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMessageRequestDto {
    
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;
    
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다")
    private String content;
}

// 대화 응답 DTO
@Data
@Builder
public class ConversationResponseDto {
    private Long messageId;
    private Long conversationId;
    private String content;
    private EmotionType emotion;
    private LocalDateTime createdAt;
    
    public static ConversationResponseDto from(MessageEntity message) {
        return ConversationResponseDto.builder()
            .messageId(message.getId())
            .conversationId(message.getConversationId())
            .content(message.getContent())
            .emotion(message.getEmotion())
            .createdAt(message.getCreatedAt())
            .build();
    }
}
```

---

## ⚙️ 2. 환경 설정 및 의존성 (MVP)

### 2.1 필수 의존성만 추가 (build.gradle)

```gradle
dependencies {
    // 기존 의존성들...
    
    // OpenAI Java SDK (핵심)
    implementation 'com.theokanning.openai-gpt3-java:service:0.18.2'
    
    // HTTP 클라이언트 (OpenAI SDK 의존)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-jackson:2.9.0'
    
    // 테스트용 Mock 서버
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
}
```

### 2.2 환경변수 설정 (.env)

```bash
# 기존 환경변수들...

# OpenAI API 설정 (필수)
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=100
OPENAI_TEMPERATURE=0.7
```

### 2.3 OpenAI 설정 클래스

```java
@Configuration
@EnableConfigurationProperties(OpenAIProperties.class)
public class OpenAIConfig {
    
    @Bean
    public OpenAiService openAiService(@Value("${openai.api.key}") String apiKey) {
        return new OpenAiService(apiKey, Duration.ofSeconds(30));
    }
}

@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIProperties {
    private String apiKey;
    private String model = "gpt-4o";
    private Integer maxTokens = 100;
    private Double temperature = 0.7;
}
```

---

## 🗺️ 3. MVP TDD 구현 로드맵 (2주)

### Week 1: 핵심 기능 구현

#### 🔴 Day 1-2: Red Phase - 기본 테스트 작성
```java
@DisplayName("AI 응답 생성 기본 테스트")
class SimpleAIResponseGeneratorTest {
    
    @Test
    @DisplayName("사용자 메시지에 대한 AI 응답을 생성한다")
    void generateResponse_WithUserMessage_ReturnsResponse() {
        // Given
        String userMessage = "안녕하세요";
        
        // When
        String response = aiResponseGenerator.generateResponse(userMessage);
        
        // Then
        assertThat(response).isNotBlank();
        assertThat(response.length()).isLessThanOrEqualTo(100);
    }
    
    @Test
    @DisplayName("기본 감정 분석을 수행한다")
    void analyzeBasicEmotion_WithMessage_ReturnsEmotion() {
        // Given & When & Then
        assertThat(aiResponseGenerator.analyzeBasicEmotion("기분이 좋아요"))
            .isEqualTo(EmotionType.POSITIVE);
        assertThat(aiResponseGenerator.analyzeBasicEmotion("슬픕니다"))
            .isEqualTo(EmotionType.NEGATIVE);
        assertThat(aiResponseGenerator.analyzeBasicEmotion("그냥 그래요"))
            .isEqualTo(EmotionType.NEUTRAL);
    }
}

@DisplayName("대화 서비스 기본 테스트")
class SimpleConversationServiceTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock 
    private MessageRepository messageRepository;
    
    @Mock
    private SimpleAIResponseGenerator aiResponseGenerator;
    
    @InjectMocks
    private SimpleConversationService conversationService;
    
    @Test
    @DisplayName("사용자 메시지 처리 시 AI 응답이 생성되고 저장된다")
    void processUserMessage_WithValidInput_CreatesAndSavesMessages() {
        // Given
        Long memberId = 1L;
        String content = "안녕하세요";
        
        ConversationEntity conversation = ConversationEntity.builder()
            .id(1L)
            .memberId(memberId)
            .build();
            
        when(conversationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
            .thenReturn(Optional.of(conversation));
        when(aiResponseGenerator.analyzeBasicEmotion(content))
            .thenReturn(EmotionType.NEUTRAL);
        when(aiResponseGenerator.generateResponse(content))
            .thenReturn("안녕하세요! 오늘 기분은 어떠세요?");
        when(messageRepository.save(any(MessageEntity.class)))
            .thenAnswer(invocation -> {
                MessageEntity message = invocation.getArgument(0);
                return message.toBuilder().id(1L).build();
            });
        
        // When
        ConversationResponseDto result = conversationService.processUserMessage(memberId, content);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("안녕하세요! 오늘 기분은 어떠세요?");
        verify(messageRepository, times(2)).save(any(MessageEntity.class)); // 사용자 + AI 메시지
    }
}
```

#### 🟢 Day 3-4: Green Phase - 기본 구현
- `SimpleAIResponseGenerator` 구현
- `ConversationEntity`, `MessageEntity` 구현
- Repository 인터페이스 및 기본 구현

#### 🔵 Day 5: Refactor Phase - 기본 리팩토링
- 코드 정리 및 예외 처리 추가
- 기본적인 방어적 코딩

#### Day 6-7: 통합 및 REST API
- `SimpleConversationService` 구현
- Controller 및 DTO 구현
- 기본 통합 테스트

### Week 2: 안정화 및 완성

#### Day 8-9: 안정성 강화
```java
// AI API 호출 실패 시 방어적 처리
@Component
public class SafeAIResponseGenerator {
    
    public String generateResponse(String userMessage) {
        try {
            return aiResponseGenerator.generateResponse(userMessage);
        } catch (Exception e) {
            log.error("AI 응답 생성 실패: {}", e.getMessage());
            return "죄송합니다. 잠시 후 다시 시도해주세요."; // 기본 응답
        }
    }
    
    // 응답 길이 검증
    private String validateAndTrimResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "안녕하세요! 어떻게 지내세요?";
        }
        return response.length() > 100 ? response.substring(0, 97) + "..." : response;
    }
}
```

#### Day 10-11: 테스트 완성
- 통합 테스트 추가
- Mock 기반 외부 API 테스트
- 예외 상황 테스트

#### Day 12-14: 문서화 및 버퍼
- API 문서화 (Swagger)
- 배포 준비
- 버퍼 시간 (예상치 못한 이슈 해결)

---

## 🗄️ 4. 간소화된 데이터베이스 설계

### 4.1 테이블 스키마 (MVP)

```sql
-- 대화 테이블 (간소화)
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 메시지 테이블 (간소화)
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('USER_MESSAGE', 'AI_RESPONSE')),
    content TEXT NOT NULL,
    emotion VARCHAR(20) CHECK (emotion IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 기본 인덱스
CREATE INDEX idx_conversations_member_id ON conversations(member_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
```

---

## 🧪 5. 간소화된 테스트 전략

### 5.1 MVP 테스트 범위
- **단위 테스트 (70%)**: 핵심 비즈니스 로직
- **통합 테스트 (25%)**: Repository + Service
- **E2E 테스트 (5%)**: 주요 API 엔드포인트

### 5.2 핵심 테스트 케이스

```java
@SpringBootTest
@AutoConfigureMockMvc
class ConversationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OpenAiService openAiService;
    
    @Test
    @DisplayName("사용자 메시지 전송 시 AI 응답이 정상 반환된다")
    void processMessage_ReturnsAIResponse() throws Exception {
        // Given
        ChatCompletionResult mockResult = createMockChatCompletion();
        when(openAiService.createChatCompletion(any())).thenReturn(mockResult);
        
        ProcessMessageRequestDto request = new ProcessMessageRequestDto(1L, "안녕하세요");
        
        // When & Then
        mockMvc.perform(post("/api/conversations/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpected(jsonPath("$.data.content").isNotEmpty())
            .andExpect(jsonPath("$.data.emotion").exists());
    }
    
    private ChatCompletionResult createMockChatCompletion() {
        return ChatCompletionResult.builder()
            .choices(List.of(
                ChatCompletionChoice.builder()
                    .message(new ChatMessage(ChatMessageRole.ASSISTANT.value(), 
                           "안녕하세요! 오늘 기분은 어떠신가요?"))
                    .finishReason("stop")
                    .build()
            ))
            .usage(new Usage(30L, 15L, 45L))
            .build();
    }
}

@DataJpaTest
class ConversationRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Test
    @DisplayName("회원 ID로 최근 대화를 조회한다")
    void findTopByMemberIdOrderByCreatedAtDesc_WithValidMember_ReturnsLatestConversation() {
        // Given
        ConversationEntity conversation1 = ConversationEntity.builder()
            .memberId(1L)
            .startedAt(LocalDateTime.now().minusDays(1))
            .build();
        ConversationEntity conversation2 = ConversationEntity.builder()
            .memberId(1L)
            .startedAt(LocalDateTime.now())
            .build();
            
        entityManager.persistAndFlush(conversation1);
        entityManager.persistAndFlush(conversation2);
        
        // When
        Optional<ConversationEntity> result = conversationRepository
            .findTopByMemberIdOrderByCreatedAtDesc(1L);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(conversation2.getId());
    }
}
```

---

## 📊 6. MVP 성공 지표 및 완료 기준

### 기술적 지표 (최소 요구사항)
- **기본 기능 동작**: 사용자 메시지 → AI 응답 전체 플로우 정상 동작
- **응답 시간**: 평균 5초 이내 (OpenAI API 응답 시간 포함)
- **테스트 커버리지**: 핵심 비즈니스 로직 80% 이상
- **API 안정성**: 예외 상황에서도 기본 응답 제공

### MVP 완료 체크리스트
- [ ] OpenAI API 연동 완료 (기본 프롬프트)
- [ ] 대화/메시지 Entity 및 Repository 구현 완료
- [ ] 간단한 감정 분석 기능 구현 완료
- [ ] REST API 엔드포인트 구현 완료 (3개 이상)
- [ ] 기본 예외 처리 및 방어적 코딩 완료
- [ ] 단위 테스트 작성 완료 (핵심 로직)
- [ ] 통합 테스트 작성 완료 (주요 API)
- [ ] API 문서화 완료 (Swagger)

---

## 🚀 7. Phase 1.1+ 확장 계획

### Phase 1.1: 맥락 관리 추가 (1주)
- Redis 캐싱 도입
- 이전 대화 맥락을 고려한 AI 응답
- 대화 요약 기능

### Phase 1.2: 감정 분석 고도화 (1주)  
- OpenAI 기반 정교한 감정 분석
- 위험도 점수 시스템
- 이상징후 감지 알고리즘

### Phase 1.3: 이벤트 시스템 (1주)
- 보호자 알림 이벤트 발행
- 비동기 처리 시스템
- 알림 시스템과의 연동 준비

### Phase 2 연동 준비사항
- SMS 도메인과의 인터페이스 설계
- 메시지 큐를 통한 비동기 처리
- 통합 테스트 환경 구축

---

**장점 요약**
- ✅ **빠른 MVP 완성**: 2주 내 동작하는 시스템
- ✅ **리스크 최소화**: 복잡한 기능 제외로 안정성 확보  
- ✅ **점진적 개선**: 기본 → 고급 기능 단계적 추가
- ✅ **SMS 연동 준비**: Phase 2와 자연스러운 연결
- ✅ **철저한 TDD**: Red-Green-Refactor 사이클 엄수

---

---

## 🔄 **Phase 1 MVP 실제 구현 진행 상황** (2025-09-14 업데이트)

### 📊 **전체 진행률: 70%** (MVP 기준)

#### ✅ **완료된 구성요소 (100%)**

##### 1. **SimpleAIResponseGenerator** - 완전 구현 완료 ✅
**위치**: `src/main/java/.../conversation/infrastructure/SimpleAIResponseGenerator.java`

```java
// ✅ 실제 구현된 주요 메서드들
@Component
public class SimpleAIResponseGenerator {

    // OpenAI GPT-4o API 완전 연동
    public String generateResponse(String userMessage) {
        // ✅ 구현 완료: API 호출, 예외 처리, 응답 길이 제한
    }

    // 키워드 기반 감정 분석 완성
    public EmotionType analyzeBasicEmotion(String message) {
        // ✅ 구현 완료: 8개 부정, 7개 긍정 키워드 기반 분석
    }
}
```

**✅ 구현된 기능들:**
- OpenAI API 연동 (GPT-4o 모델)
- 방어적 코딩 (예외 처리, 입력 검증, 응답 길이 제한)
- 감정 분석 (POSITIVE/NEGATIVE/NEUTRAL)
- 상수 관리 및 설정 외부화 (`@Value` 활용)

**✅ 테스트 완성도:**
```java
// 5개 테스트 케이스 모두 작성 및 통과
SimpleAIResponseGeneratorTest.java:
├── generateResponse_WithUserMessage_ReturnsAIResponse ✅
├── generateResponse_WithEmptyMessage_ReturnsDefaultResponse ✅
├── analyzeBasicEmotion_WithPositiveMessage_ReturnsPositive ✅
├── analyzeBasicEmotion_WithNegativeMessage_ReturnsNegative ✅
└── analyzeBasicEmotion_WithNullMessage_ReturnsNeutral ✅
```

##### 2. **Entity 설계** - DDD 구조 완성 ✅
**위치**: `src/main/java/.../conversation/domain/entity/`

```java
// ✅ ConversationEntity - 완전 구현
@Entity
@Table(name = "conversations")
public class ConversationEntity extends BaseTimeEntity {
    // ✅ 정적 팩토리 메서드 포함
    public static ConversationEntity createNew(Long memberId) { ... }
}

// ✅ MessageEntity - 완전 구현
@Entity
@Table(name = "messages")
public class MessageEntity extends BaseTimeEntity {
    // ✅ 타입별 정적 팩토리 메서드
    public static MessageEntity createUserMessage(...) { ... }
    public static MessageEntity createAIResponse(...) { ... }
}

// ✅ Enum 타입들
public enum MessageType { USER_MESSAGE, AI_RESPONSE }
public enum EmotionType { POSITIVE, NEUTRAL, NEGATIVE }
```

##### 3. **DTO 계층** - 완성 ✅
```java
// ✅ ConversationResponseDto, MessageDto 완성
@Getter @Builder
public class ConversationResponseDto {
    private Long conversationId;
    private MessageDto userMessage;
    private MessageDto aiMessage;
}
```

##### 4. **Repository 인터페이스** - 설계 완성 ✅
```java
// ✅ JPA Repository 인터페이스 정의
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    Optional<ConversationEntity> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<ConversationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
```

---

#### ⚠️ **미완성 구성요소 (30%)**

##### 1. **SimpleConversationService** - 핵심 로직 미구현 🔴
**현재 상태**: 더미 구현 (하드코딩)

```java
// ❌ 현재 임시 구현 상태
@Transactional
public ConversationResponseDto processUserMessage(Long memberId, String content) {
    // 🔴 하드코딩된 더미 응답
    return ConversationResponseDto.builder()
            .conversationId(memberId)  // memberId를 conversationId로 사용 (임시)
            .build();
}
```

**❌ 누락된 핵심 기능들:**
- AI 응답 생성기 연동 (`SimpleAIResponseGenerator` 호출)
- 대화/메시지 Entity 저장 로직
- 실제 데이터베이스 연동
- 트랜잭션 처리

**⚠️ 테스트 상태**: 3개 테스트 작성되었으나 의미 없는 테스트 (더미 구현 기반)

##### 2. **Controller 계층** - 구현 여부 미확인 🟡
- REST API 엔드포인트 존재 여부 확인 필요
- Swagger 문서화 상태 미확인

##### 3. **데이터베이스 스키마** - 생성 여부 미확인 🟡
- `conversations`, `messages` 테이블 생성 여부 확인 필요

---

### 🎯 **계획서 대비 실제 구현 비교**

| 구성요소 | 계획서 목표 | 실제 구현 | 완성도 | 상태 |
|----------|------------|-----------|---------|------|
| **AI 응답 생성** | SimpleAIResponseGenerator | ✅ **완전 구현** | 100% | 🟢 |
| **감정 분석** | 키워드 기반 3단계 | ✅ **완전 구현** | 100% | 🟢 |
| **Entity 설계** | Conversation/Message | ✅ **완전 구현** | 100% | 🟢 |
| **Service 핵심 로직** | processUserMessage | ❌ **더미 구현** | 20% | 🔴 |
| **Repository** | JPA 인터페이스 | ✅ **인터페이스만** | 80% | 🟡 |
| **REST API** | 3개 엔드포인트 | ❓ **미확인** | ? | 🟡 |
| **DB 스키마** | conversations/messages | ❓ **미확인** | ? | 🟡 |

---

### 📋 **즉시 수행해야 할 작업 (우선순위)**

#### **Phase 1 (긴급)**
1. **SimpleConversationService 실제 구현** 🔴
   ```java
   // 구현해야 할 핵심 로직
   @Transactional
   public ConversationResponseDto processUserMessage(Long memberId, String content) {
       // 1. 대화 조회/생성
       ConversationEntity conversation = findOrCreateConversation(memberId);

       // 2. 사용자 메시지 저장
       EmotionType emotion = aiResponseGenerator.analyzeBasicEmotion(content);
       MessageEntity userMessage = MessageEntity.createUserMessage(
           conversation.getId(), content, emotion);
       messageRepository.save(userMessage);

       // 3. AI 응답 생성
       String aiResponse = aiResponseGenerator.generateResponse(content);

       // 4. AI 응답 저장
       MessageEntity aiMessage = MessageEntity.createAIResponse(
           conversation.getId(), aiResponse);
       messageRepository.save(aiMessage);

       // 5. 응답 DTO 구성
       return ConversationResponseDto.builder()
           .conversationId(conversation.getId())
           .userMessage(MessageDto.from(userMessage))
           .aiMessage(MessageDto.from(aiMessage))
           .build();
   }
   ```

#### **Phase 2 (후속)**
2. **Controller 구현 확인/추가**
3. **데이터베이스 스키마 생성 확인**
4. **통합 테스트 작성**

---

### 🧪 **현재 TDD 상태**

#### **Red-Green-Refactor 진행 상황**
- **SimpleAIResponseGenerator**: 🟢 **Green 단계 완료** (테스트 통과)
- **SimpleConversationService**: 🔴 **Red 단계** (가짜 구현으로 테스트 통과, 실제로는 미구현)

#### **다음 TDD 사이클**
1. 🔴 **Red**: SimpleConversationService 실제 테스트 작성 (실패하는 테스트)
2. 🟢 **Green**: 테스트를 통과하는 최소 구현
3. 🔵 **Refactor**: 코드 품질 개선

---

### ✅ **MVP 성공 지표 현재 상태**

| 지표 | 목표 | 현재 상태 | 달성 여부 |
|------|------|-----------|-----------|
| OpenAI API 연동 | 기본 프롬프트 | ✅ **완성** | ✅ |
| 대화/메시지 Entity | Repository 구현 | 🟡 **인터페이스만** | ⚠️ |
| 감정 분석 | 3단계 분석 | ✅ **완성** | ✅ |
| REST API | 3개 이상 | ❓ **미확인** | ❓ |
| 예외 처리 | 기본 응답 제공 | ✅ **완성** | ✅ |
| 단위 테스트 | 핵심 로직 80% | 🟡 **부분 완성** | ⚠️ |

**현재 MVP 완료도**: **5/8 = 62.5%**

---

**문서 작성일**: 2025-09-13
**최종 수정일**: 2025-09-14
**작성자**: Claude Code
**버전**: MVP v1.1 (진행 상황 업데이트)