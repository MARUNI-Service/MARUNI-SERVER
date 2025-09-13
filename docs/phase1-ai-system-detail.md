# Phase 1: AI 대화 시스템 구축 상세 구현 계획

## 📋 문서 개요

**MARUNI Phase 1**에서는 OpenAI GPT-4o를 활용한 AI 대화 시스템을 TDD 기반으로 구축합니다. 
노인들의 문자 응답을 AI가 분석하고, 자연스러운 대화를 통해 감정 상태 및 이상징후를 감지하는 핵심 시스템을 완성합니다.

**구현 기간**: 3주 (TDD 방식)  
**핵심 목표**: OpenAI API 연동 → 대화 맥락 관리 → 감정 분석 시스템

---

## 🏗️ 1. 기술 아키텍처 설계

### 1.1 도메인 모델 설계 (DDD 구조)

#### Core Entities
```java
// 대화 세션 (Aggregate Root)
ConversationEntity {
    Long conversationId
    Long memberId           // Member 도메인 참조
    ConversationType type   // DAILY_CHECK, EMERGENCY, CASUAL
    ConversationStatus status // ACTIVE, PAUSED, COMPLETED
    LocalDateTime startedAt
    LocalDateTime lastActivityAt
    ConversationSummary summary // VO: 대화 요약 정보
}

// 개별 메시지
MessageEntity {
    Long messageId
    Long conversationId
    MessageType type        // USER_MESSAGE, AI_RESPONSE
    String content
    MessageMetadata metadata // VO: 감정점수, 위험도 등
    LocalDateTime createdAt
}

// 대화 맥락 (Value Object)
ConversationContext {
    String shortTermMemory   // 최근 5개 메시지 요약
    String longTermMemory    // 누적 대화 패턴
    EmotionState currentEmotion
    List<String> importantTopics
}
```

#### Domain Services
```java
// AI 응답 생성 도메인 서비스
AIResponseGenerator {
    + generateResponse(ConversationContext, String userMessage): AIResponse
    + analyzeEmotionalState(String message): EmotionAnalysis
    + detectAnomalies(ConversationHistory): AnomalyDetectionResult
}

// 대화 맥락 관리 도메인 서비스  
ConversationContextManager {
    + updateContext(Conversation, Message): ConversationContext
    + summarizeConversation(List<Message>): ConversationSummary
    + extractImportantTopics(ConversationHistory): List<Topic>
}
```

### 1.2 Infrastructure 레이어 설계

#### OpenAI API 연동 구조
```java
// OpenAI API 클라이언트 (Infrastructure)
@Component
public class OpenAIApiClient implements AIApiClient {
    private final OpenAI openAI;
    
    public AIResponse generateChatResponse(ChatRequest request) {
        // GPT-4o API 호출 로직
    }
    
    public EmotionAnalysis analyzeEmotion(String text) {
        // 감정 분석 전용 프롬프트 처리
    }
}

// AI 응답 생성기 구현체
@Component  
public class OpenAIResponseGenerator implements AIResponseGenerator {
    private final OpenAIApiClient apiClient;
    private final PromptTemplateService promptService;
    
    @Override
    public AIResponse generateResponse(ConversationContext context, String userMessage) {
        String prompt = promptService.buildConversationPrompt(context, userMessage);
        return apiClient.generateChatResponse(new ChatRequest(prompt));
    }
}
```

#### 프롬프트 템플릿 시스템
```java
@Service
public class PromptTemplateService {
    
    public String buildConversationPrompt(ConversationContext context, String userMessage) {
        return """
            당신은 노인 돌봄 전문 AI 상담사입니다.
            
            # 대화 맥락
            - 이전 대화 요약: %s
            - 현재 감정 상태: %s
            - 주요 관심사: %s
            
            # 사용자 메시지
            "%s"
            
            # 응답 가이드라인
            1. 따뜻하고 공감적인 톤으로 응답
            2. 이상징후 감지 시 자연스럽게 더 깊이 질문
            3. 긍정적 방향으로 대화 유도
            4. 30자 이내로 간결하게 (SMS 특성 고려)
            
            응답:
            """.formatted(context.getShortTermMemory(), 
                         context.getCurrentEmotion(),
                         String.join(", ", context.getImportantTopics()),
                         userMessage);
    }
    
    public String buildEmotionAnalysisPrompt(String message) {
        return """
            다음 메시지의 감정 상태를 분석해주세요:
            
            메시지: "%s"
            
            다음 JSON 형식으로 응답:
            {
                "emotion": "POSITIVE|NEUTRAL|NEGATIVE|CONCERNING",
                "intensity": 1-10,
                "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                "keywords": ["키워드1", "키워드2"],
                "reason": "분석 근거"
            }
            """.formatted(message);
    }
}
```

### 1.3 Application 레이어 설계

#### 핵심 Application Service
```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIResponseGenerator aiResponseGenerator;
    private final ConversationContextManager contextManager;
    private final ConversationCacheService cacheService;
    
    @Transactional
    public ConversationResponseDto processUserMessage(ProcessMessageRequestDto request) {
        // 1. 대화 조회 또는 새 대화 생성
        Conversation conversation = findOrCreateConversation(request.getMemberId());
        
        // 2. 사용자 메시지 저장
        Message userMessage = saveUserMessage(conversation, request.getContent());
        
        // 3. 대화 맥락 업데이트
        ConversationContext context = contextManager.updateContext(conversation, userMessage);
        
        // 4. AI 응답 생성
        AIResponse aiResponse = aiResponseGenerator.generateResponse(context, request.getContent());
        
        // 5. AI 응답 저장
        Message aiMessage = saveAIMessage(conversation, aiResponse);
        
        // 6. 캐시 업데이트
        cacheService.updateConversationCache(conversation.getId(), context);
        
        return ConversationResponseDto.from(aiMessage);
    }
    
    @Transactional
    public EmotionAnalysisDto analyzeUserEmotion(Long memberId, String message) {
        EmotionAnalysis analysis = aiResponseGenerator.analyzeEmotionalState(message);
        
        // 위험도 높은 경우 별도 처리
        if (analysis.getRiskLevel() == RiskLevel.HIGH || analysis.getRiskLevel() == RiskLevel.CRITICAL) {
            // 이벤트 발행: 보호자 알림 등
            eventPublisher.publishEvent(new HighRiskDetectedEvent(memberId, analysis));
        }
        
        return EmotionAnalysisDto.from(analysis);
    }
}
```

### 1.4 Presentation 레이어 설계

#### REST API 컨트롤러
```java
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@AutoApiResponse
@CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
public class ConversationController {
    
    private final ConversationService conversationService;
    
    @PostMapping("/message")
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    @Operation(summary = "사용자 메시지 처리 및 AI 응답 생성")
    public ConversationResponseDto processMessage(
            @Valid @RequestBody ProcessMessageRequestDto request) {
        return conversationService.processUserMessage(request);
    }
    
    @PostMapping("/emotion-analysis")
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    @Operation(summary = "감정 분석 수행")
    public EmotionAnalysisDto analyzeEmotion(
            @Valid @RequestBody EmotionAnalysisRequestDto request) {
        return conversationService.analyzeUserEmotion(request.getMemberId(), request.getMessage());
    }
    
    @GetMapping("/{conversationId}/history")
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    @Operation(summary = "대화 히스토리 조회")
    public ConversationHistoryDto getConversationHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return conversationService.getConversationHistory(conversationId, page, size);
    }
}
```

---

## ⚙️ 2. 환경 설정 및 의존성

### 2.1 OpenAI Java SDK 설정

#### build.gradle 의존성 추가
```gradle
dependencies {
    // 기존 의존성들...
    
    // OpenAI Java SDK
    implementation 'com.theokanning.openai-gpt3-java:service:0.18.2'
    
    // HTTP 클라이언트 (OpenAI SDK 내부 사용)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-jackson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // JSON 처리 강화
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    
    // 테스트용 Mock 서버
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
}
```

### 2.2 환경변수 설정 (.env)

```bash
# 기존 환경변수들...

# OpenAI API 설정
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=150
OPENAI_TEMPERATURE=0.7
OPENAI_TIMEOUT_SECONDS=30

# AI 응답 설정
AI_RESPONSE_MAX_LENGTH=100
AI_CONVERSATION_CONTEXT_LIMIT=10
AI_EMOTION_ANALYSIS_THRESHOLD=0.8

# Redis 캐싱 설정 (대화 맥락용)
CONVERSATION_CACHE_TTL=3600
CONTEXT_CACHE_TTL=1800
```

### 2.3 Configuration 클래스 설계

#### OpenAI 설정
```java
@Configuration
@EnableConfigurationProperties(OpenAIProperties.class)
public class OpenAIConfig {
    
    @Bean
    public OpenAiService openAiService(OpenAIProperties properties) {
        return new OpenAiService(properties.getApiKey(), Duration.ofSeconds(properties.getTimeoutSeconds()));
    }
    
    @Bean
    @ConditionalOnProperty(name = "openai.mock.enabled", havingValue = "true")
    public OpenAiService mockOpenAiService() {
        // 테스트용 Mock 서비스
        return Mockito.mock(OpenAiService.class);
    }
}

@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIProperties {
    private String apiKey;
    private String model = "gpt-4o";
    private Integer maxTokens = 150;
    private Double temperature = 0.7;
    private Integer timeoutSeconds = 30;
    
    @NestedConfigurationProperty
    private MockProperties mock = new MockProperties();
    
    @Data
    public static class MockProperties {
        private boolean enabled = false;
        private String responseDelay = "1s";
    }
}
```

#### 대화 캐싱 설정
```java
@Configuration
@EnableCaching
public class ConversationCacheConfig {
    
    @Bean("conversationCacheManager")
    public CacheManager conversationCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

---

## 🗺️ 3. TDD 구현 로드맵

### 3.1 AI 대화 기본 시스템 (1주차)

#### 🔴 Red Phase: 실패하는 테스트 작성
**Day 1-2: Core Domain 테스트**
```java
@DisplayName("AI 응답 생성 테스트")
class AIResponseGeneratorTest {
    
    @Test
    @DisplayName("사용자 메시지에 대한 AI 응답을 생성한다")
    void generateResponse_WithUserMessage_ReturnsAIResponse() {
        // Given
        ConversationContext context = ConversationContext.builder()
            .shortTermMemory("어제 기분이 좋지 않다고 하셨어요")
            .currentEmotion(EmotionState.NEUTRAL)
            .build();
        String userMessage = "오늘은 조금 나아진 것 같아요";
        
        // When
        AIResponse response = aiResponseGenerator.generateResponse(context, userMessage);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getContent().length()).isLessThanOrEqualTo(100);
        assertThat(response.getEmotionalTone()).isEqualTo(EmotionalTone.SUPPORTIVE);
    }
}
```

**Day 3-4: Infrastructure Layer 테스트**
```java
@DisplayName("OpenAI API 클라이언트 테스트")
class OpenAIApiClientTest {
    
    @Test
    @DisplayName("OpenAI API 호출이 성공적으로 수행된다")
    void callOpenAI_WithValidRequest_ReturnsResponse() {
        // MockWebServer를 사용한 API 테스트
    }
    
    @Test
    @DisplayName("API 호출 실패 시 적절한 예외가 발생한다")
    void callOpenAI_WithInvalidKey_ThrowsException() {
        // 실패 시나리오 테스트
    }
}
```

#### 🟢 Green Phase: 테스트 통과 최소 구현
**Day 5-6: 기본 구현**
- `ConversationEntity`, `MessageEntity` 구현
- `OpenAIApiClient` 기본 구현
- `AIResponseGenerator` 기본 구현
- Repository 인터페이스 및 JPA 구현체

#### 🔵 Refactor Phase: 코드 품질 개선
**Day 7: 리팩토링**
- 코드 중복 제거
- 성능 최적화
- 예외 처리 강화

### 3.2 대화 맥락 관리 (2주차)

#### 🔴 Red Phase: 맥락 관리 테스트
**Day 8-9: Context Management 테스트**
```java
@DisplayName("대화 맥락 관리 테스트")
class ConversationContextManagerTest {
    
    @Test
    @DisplayName("새로운 메시지로 대화 맥락을 업데이트한다")
    void updateContext_WithNewMessage_UpdatesContext() {
        // Given
        Conversation conversation = createSampleConversation();
        Message newMessage = createUserMessage("오늘 병원에 다녀왔어요");
        
        // When
        ConversationContext updatedContext = contextManager.updateContext(conversation, newMessage);
        
        // Then
        assertThat(updatedContext.getImportantTopics()).contains("병원");
        assertThat(updatedContext.getShortTermMemory()).contains("병원에 다녀왔다");
    }
}
```

**Day 10-11: Redis 캐싱 테스트**
```java
@DisplayName("대화 맥락 캐싱 테스트")
class ConversationCacheServiceTest {
    
    @TestContainer
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Test
    @DisplayName("대화 맥락이 Redis에 캐싱된다")
    void cacheConversationContext_WithValidData_StoresInRedis() {
        // Redis 캐싱 로직 테스트
    }
}
```

#### 🟢 Green Phase: 맥락 관리 구현
**Day 12-13: 구현**
- `ConversationContextManager` 구현
- Redis 기반 `ConversationCacheService` 구현
- 대화 요약 알고리즘 구현

#### 🔵 Refactor Phase: 최적화
**Day 14: 리팩토링**
- 캐싱 전략 최적화
- 메모리 사용량 개선

### 3.3 감정 분석 및 이상징후 감지 (3주차)

#### 🔴 Red Phase: 감정 분석 테스트
**Day 15-16: Emotion Analysis 테스트**
```java
@DisplayName("감정 분석 테스트")
class EmotionAnalysisTest {
    
    @Test
    @DisplayName("위험 신호가 포함된 메시지를 감지한다")
    void analyzeEmotion_WithRiskSignals_ReturnsHighRisk() {
        // Given
        String riskMessage = "요즘 너무 외롭고 아무도 나를 찾지 않아요";
        
        // When
        EmotionAnalysis analysis = aiResponseGenerator.analyzeEmotionalState(riskMessage);
        
        // Then
        assertThat(analysis.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(analysis.getKeywords()).contains("외롭", "찾지 않");
        assertThat(analysis.getIntensity()).isGreaterThan(7);
    }
}
```

#### 🟢 Green Phase: 감정 분석 구현
**Day 17-18: 구현**
- 감정 분석 프롬프트 템플릿 구현
- `EmotionAnalysis` 도메인 모델 구현
- 위험도 점수 계산 알고리즘

#### 🔵 Refactor Phase: 정확도 개선
**Day 19-21: 최적화 및 완성**
- 분석 정확도 개선
- 성능 최적화
- 통합 테스트 및 문서화

---

## 🗄️ 4. 데이터베이스 설계

### 4.1 테이블 스키마 설계

#### 대화 관련 테이블
```sql
-- 대화 세션 테이블
CREATE TABLE conversations (
    conversation_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(member_id),
    conversation_type VARCHAR(20) NOT NULL CHECK (conversation_type IN ('DAILY_CHECK', 'EMERGENCY', 'CASUAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED')),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    summary_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 메시지 테이블
CREATE TABLE messages (
    message_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(conversation_id),
    message_type VARCHAR(20) NOT NULL CHECK (message_type IN ('USER_MESSAGE', 'AI_RESPONSE')),
    content TEXT NOT NULL,
    
    -- 메타데이터 (JSON 형태로 저장)
    emotion_score DECIMAL(3,2),
    risk_level VARCHAR(20) CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    keywords TEXT[], -- PostgreSQL 배열 타입
    analysis_result JSONB,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 대화 맥락 히스토리 (필요시)
CREATE TABLE conversation_contexts (
    context_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(conversation_id),
    short_term_memory TEXT,
    long_term_memory TEXT,
    current_emotion VARCHAR(20),
    important_topics TEXT[],
    context_version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 추가
CREATE INDEX idx_conversations_member_id ON conversations(member_id);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_risk_level ON messages(risk_level);
```

### 4.2 Redis 캐싱 전략

#### 캐시 키 설계
```
# 대화 맥락 캐시
conversation:context:{conversationId} -> ConversationContext JSON

# 사용자별 활성 대화 캐시  
member:active_conversation:{memberId} -> conversationId

# AI 응답 캐시 (같은 입력에 대한 중복 호출 방지)
ai:response:hash:{messageHash} -> AIResponse JSON

# 감정 분석 결과 캐시
emotion:analysis:{messageHash} -> EmotionAnalysis JSON
```

#### 캐시 TTL 전략
```java
@Component
public class ConversationCacheService {
    
    private static final String CONTEXT_CACHE_KEY = "conversation:context:%d";
    private static final String ACTIVE_CONVERSATION_KEY = "member:active_conversation:%d";
    private static final Duration CONTEXT_TTL = Duration.ofHours(1);
    private static final Duration ACTIVE_CONVERSATION_TTL = Duration.ofHours(24);
    
    @Cacheable(value = "conversationContext", key = "#conversationId")
    public ConversationContext getContext(Long conversationId) {
        // 캐시 미스 시 DB에서 조회
    }
    
    @CachePut(value = "conversationContext", key = "#conversationId")
    public ConversationContext updateContext(Long conversationId, ConversationContext context) {
        // 캐시 업데이트
    }
}
```

---

## 🧪 5. 테스트 전략 및 Mock 설계

### 5.1 테스트 피라미드 구조

```
        /\
       /  \
      / E2E\ (5%)
     /______\
    /        \
   /Integration\ (15%)
  /__________  \
 /              \
/   Unit Tests   \ (80%)
\________________/
```

### 5.2 Unit Test 전략

#### Repository Layer 테스트
```java
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class ConversationRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Test
    @DisplayName("회원 ID로 활성 대화를 조회한다")
    void findActiveConversationByMemberId_WithValidMember_ReturnsConversation() {
        // Given
        MemberEntity member = createTestMember();
        ConversationEntity conversation = createActiveConversation(member);
        entityManager.persistAndFlush(member);
        entityManager.persistAndFlush(conversation);
        
        // When
        Optional<ConversationEntity> result = conversationRepository
            .findByMemberIdAndStatus(member.getId(), ConversationStatus.ACTIVE);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getMemberId()).isEqualTo(member.getId());
    }
}
```

#### Service Layer 테스트 (Mock 사용)
```java
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private AIResponseGenerator aiResponseGenerator;
    
    @Mock
    private ConversationContextManager contextManager;
    
    @InjectMocks
    private ConversationService conversationService;
    
    @Test
    @DisplayName("사용자 메시지 처리 시 AI 응답이 생성된다")
    void processUserMessage_WithValidMessage_GeneratesAIResponse() {
        // Given
        ProcessMessageRequestDto request = createMessageRequest();
        ConversationEntity conversation = createMockConversation();
        AIResponse mockAIResponse = createMockAIResponse();
        
        when(conversationRepository.findByMemberIdAndStatus(anyLong(), any()))
            .thenReturn(Optional.of(conversation));
        when(aiResponseGenerator.generateResponse(any(), anyString()))
            .thenReturn(mockAIResponse);
        
        // When
        ConversationResponseDto result = conversationService.processUserMessage(request);
        
        // Then
        assertThat(result.getContent()).isEqualTo(mockAIResponse.getContent());
        verify(aiResponseGenerator).generateResponse(any(), anyString());
    }
}
```

### 5.3 Integration Test 전략

#### OpenAI API Mock 서버
```java
@TestConfiguration
public class MockOpenAIConfig {
    
    @Bean
    @Primary
    public OpenAiService mockOpenAiService() {
        return new MockOpenAiService();
    }
    
    private static class MockOpenAiService extends OpenAiService {
        @Override
        public ChatCompletionResult createChatCompletion(ChatCompletionRequest request) {
            return ChatCompletionResult.builder()
                .choices(List.of(
                    ChatCompletionChoice.builder()
                        .message(new ChatMessage(ChatMessageRole.ASSISTANT.value(), 
                               "안녕하세요! 오늘 기분은 어떠신가요?"))
                        .finishReason("stop")
                        .build()
                ))
                .usage(new Usage(50L, 20L, 70L))
                .build();
        }
    }
}
```

#### Redis 통합 테스트
```java
@SpringBootTest
@Testcontainers
class ConversationIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }
    
    @Test
    @DisplayName("대화 맥락이 Redis에 정상적으로 캐싱된다")
    void conversationContext_IsCachedProperly() {
        // 통합 테스트 로직
    }
}
```

### 5.4 E2E Test 시나리오

#### 전체 대화 플로우 테스트
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/test-data/conversation-test-data.sql")
class ConversationE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("사용자 메시지 전송부터 AI 응답까지 전체 플로우가 정상 동작한다")
    void completeConversationFlow_WorksProperly() {
        // 1. 사용자 메시지 전송
        ProcessMessageRequestDto request = new ProcessMessageRequestDto(1L, "오늘 기분이 좋지 않아요");
        
        ResponseEntity<CommonApiResponse<ConversationResponseDto>> response = 
            restTemplate.postForEntity("/api/conversations/message", request, 
                new ParameterizedTypeReference<CommonApiResponse<ConversationResponseDto>>() {});
        
        // 2. 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isNotBlank();
        
        // 3. 감정 분석 결과 확인
        // 4. 대화 히스토리 확인
    }
}
```

---

## 📊 성공 지표 및 완료 기준

### 기술적 지표
- **테스트 커버리지**: 90% 이상 (도메인 로직 95% 이상)
- **AI 응답 시간**: 평균 3초 이내, 95th percentile 5초 이내
- **대화 맥락 유지율**: 95% 이상 (연속 대화에서)
- **감정 분석 정확도**: 85% 이상 (테스트 데이터셋 기준)

### 기능적 지표
- **AI 응답 품질**: 자연스럽고 공감적인 응답 생성
- **이상징후 감지**: 위험 키워드 포함 시 적절한 위험도 책정
- **대화 연속성**: 이전 대화 내용 기반 자연스러운 응답

### Phase 1 완료 체크리스트
- [ ] OpenAI API 연동 완료 및 테스트 통과
- [ ] 대화 엔티티 및 Repository 구현 완료
- [ ] AI 응답 생성 서비스 구현 완료
- [ ] 대화 맥락 관리 시스템 구현 완료
- [ ] 감정 분석 및 위험도 감지 구현 완료
- [ ] Redis 캐싱 시스템 구현 완료
- [ ] REST API 엔드포인트 구현 완료
- [ ] 모든 단위 테스트 통과 (커버리지 90% 이상)
- [ ] 통합 테스트 통과
- [ ] API 문서화 완료 (Swagger)
- [ ] 성능 테스트 통과 (응답시간 5초 이내)

---

## 🚀 다음 단계 준비사항

### Phase 2 (SMS 시스템) 연동 준비
1. **메시지 인터페이스 표준화**: AI 응답을 SMS로 전송할 수 있는 인터페이스 설계
2. **비동기 처리 준비**: SMS 수신 → AI 처리 → SMS 응답의 비동기 플로우 고려
3. **이벤트 기반 아키텍처**: 도메인 이벤트를 통한 느슨한 결합 구조

### 확장성 고려사항
1. **다중 AI 모델 지원**: GPT-4o 외 다른 모델 추가 가능한 구조
2. **다국어 지원**: 프롬프트 템플릿의 다국어 확장 가능성
3. **개인화**: 사용자별 대화 스타일 학습 및 적용

---

**문서 작성일**: 2025-09-13  
**최종 수정일**: 2025-09-13  
**작성자**: Claude Code  
**버전**: v1.0