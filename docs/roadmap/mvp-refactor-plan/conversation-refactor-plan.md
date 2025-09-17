# Conversation 도메인 리팩토링 계획서 v4.0 (AI 모델 변경 대비 핵심 인프라)

**MARUNI Conversation 도메인 AI 모델 변경 대비 Port-Adapter 패턴 도입**
**📝 Day 1-3 핵심 인프라만 구축하는 실용적 접근법**

## 📋 v4.0 핵심 변경 사항

### ✅ **AI 모델 변경 가능성 고려한 실용적 접근**
1. **핵심 문제점**: AI 생태계 변화 대비 인프라 부족
2. **명확한 목표**: OpenAI → Claude/Gemini 등 모델 변경 대응
3. **최소 범위**: Day 1-3만 실행 (SRP 해결 + Port-Adapter + 설정 중앙화)
4. **리스크 최소화**: 검증된 필요 기능만 구현

### ❌ **v3.0 Day 4-5 제외 이유**
- 현재 문제없이 동작하는 코드의 불필요한 복잡화
- 빈약한 도메인 모델 "문제"의 과장 (실제로는 단순함이 장점)
- 과도한 DTO 추상화 및 예외 처리 체계 (실질적 편익 없음)

## 🔍 현재 상태 정확한 분석 (실제 코드 기반)

### 📊 **실제 코드 메트릭스**
```
총 12개 클래스
├── SimpleAIResponseGenerator.java    171라인 ⚠️ (SRP 위반)
├── SimpleConversationService.java    130라인 ⚠️ (다중 책임)
├── MessageRepository.java            86라인 (복잡 쿼리)
├── MessageEntity.java                85라인
├── ConversationController.java       75라인
├── ConversationEntity.java           55라인 (빈약한 도메인 모델)
└── 기타 DTO/Enum/Repository          약 200라인
```

### 🚨 **AI 모델 변경 대비 핵심 개선 영역**

#### **1. SRP 위반 - SimpleAIResponseGenerator (AI 모델 변경의 핵심 장애)**
```java
❌ 현재: AI 모델 변경 시 복잡한 수정 필요
@Component
public class SimpleAIResponseGenerator {
    // 1. AI 응답 생성 (OpenAI 의존)
    public String generateResponse(String userMessage) {
        // OpenAI API 호출 로직 - 모델 변경 시 전체 수정 필요
    }

    // 2. 감정 분석 (독립적 비즈니스 로직)
    public EmotionType analyzeBasicEmotion(String message) {
        // 키워드 기반 감정 분석 - AI 모델과 무관한 로직
    }
}

✅ 개선 목표: AI 모델 변경 시 1시간 내 대응 가능
- AIResponsePort + OpenAIResponseAdapter (모델별 어댑터 교체)
- EmotionAnalysisPort + KeywordBasedEmotionAnalyzer (독립적 유지)
```

#### **2. 설정 관리 분산 - 모델별 설정 어려움**
```java
❌ 현재: 모델 변경 시 설정 관리 복잡
@Value("${spring.ai.openai.chat.options.model}")
private String model;
@Value("${spring.ai.openai.chat.options.temperature}")
private Double temperature;

✅ 개선 목표: 모델별 설정 프로파일 지원
@ConfigurationProperties("maruni.conversation")
public class ConversationProperties {
    // OpenAI, Claude, Gemini 등 모델별 설정 분리
}
```

## 🎯 AI 모델 변경 대비 핵심 인프라 구축 전략

### **📈 실용적 설계 원칙**
1. **AI 모델 독립성**: AI 모델 변경 시 최소 영향 범위
2. **기존 연동 호환성**: AlertRule, DailyCheck 도메인과의 연동 100% 유지
3. **점진적 개선**: 핵심 인프라만 구축, 불필요한 변경 제외
4. **성능 영향 최소화**: 기존 성능 지표 ±5% 이내 유지

### **🚦 3일 집중 실행 계획**

#### **🟢 Day 1: SRP 위반 해결 (AI 모델 변경의 핵심)**
- SimpleAIResponseGenerator를 2개 Port-Adapter로 분리
- OpenAI 의존성을 Adapter로 격리
- 감정 분석 로직을 독립적 컴포넌트로 분리

#### **🟢 Day 2: Service 계층 Port 의존성 적용**
- SimpleConversationService를 Port 인터페이스 의존으로 변경
- 기존 SimpleAIResponseGenerator 제거
- 테스트 코드 Port 기반으로 수정

#### **🟢 Day 3: 설정 관리 체계화 (모델별 설정 분리)**
- ConversationProperties로 설정 중앙화
- 모델별 설정 프로파일 지원 (OpenAI/Claude/Gemini)
- Adapter들이 Properties 사용하도록 수정

## 📅 3일 집중 리팩토링 일정 (AI 모델 변경 대비)

### **🎯 Day 1: SRP 위반 해결 - SimpleAIResponseGenerator 분리**

#### **오전 (3-4시간): Port 인터페이스 정의 및 감정 분석 분리**

```java
// 1. 감정 분석 포트 정의 (domain/port/ 패키지)
public interface EmotionAnalysisPort {
    /**
     * 메시지 감정 분석 (기존 analyzeBasicEmotion과 동일한 시그니처)
     * @param message 분석할 메시지
     * @return 감정 타입
     */
    EmotionType analyzeEmotion(String message);
}

// 2. AI 응답 생성 포트 정의
public interface AIResponsePort {
    /**
     * 사용자 메시지에 대한 AI 응답 생성 (기존 generateResponse와 동일)
     * @param userMessage 사용자 메시지
     * @return AI 응답 내용
     */
    String generateResponse(String userMessage);
}

// 3. 키워드 기반 감정 분석기 구현 (infrastructure/analyzer/ 패키지)
@Component
public class KeywordBasedEmotionAnalyzer implements EmotionAnalysisPort {

    // 기존 SimpleAIResponseGenerator의 감정분석 로직 그대로 이관
    private static final Map<EmotionType, List<String>> EMOTION_KEYWORDS = Map.of(
        EmotionType.NEGATIVE, List.of("슬프", "우울", "아프", "힘들", "외로", "무서", "걱정", "답답"),
        EmotionType.POSITIVE, List.of("좋", "행복", "기쁘", "감사", "즐거", "만족", "고마")
    );

    @Override
    public EmotionType analyzeEmotion(String message) {
        // 기존 analyzeBasicEmotion 로직 그대로 복사
        if (!StringUtils.hasText(message)) {
            return EmotionType.NEUTRAL;
        }

        String lowerMessage = message.toLowerCase();

        if (containsAnyKeyword(lowerMessage, EMOTION_KEYWORDS.get(EmotionType.NEGATIVE))) {
            return EmotionType.NEGATIVE;
        }

        if (containsAnyKeyword(lowerMessage, EMOTION_KEYWORDS.get(EmotionType.POSITIVE))) {
            return EmotionType.POSITIVE;
        }

        return EmotionType.NEUTRAL;
    }

    private boolean containsAnyKeyword(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }
}
```

#### **오후 (4-5시간): AI 응답 생성기 분리 및 Service 수정**

```java
// 4. AI 응답 생성기 구현 (infrastructure/ai/ 패키지)
@Component
@RequiredArgsConstructor
public class OpenAIResponseAdapter implements AIResponsePort {
    private final ChatModel chatModel;

    // 기존 SimpleAIResponseGenerator의 설정값 그대로 사용
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    // 상수값들도 그대로 이관
    private static final int MAX_RESPONSE_LENGTH = 100;
    private static final String SYSTEM_PROMPT = "당신은 노인 돌봄 전문 AI 상담사입니다. 따뜻하고 공감적으로 30자 이내로 응답하세요.";
    private static final String DEFAULT_RESPONSE = "안녕하세요! 어떻게 지내세요?";

    @Override
    public String generateResponse(String userMessage) {
        try {
            log.info("AI 응답 생성 요청: {}", userMessage);

            // 기존 로직 그대로 복사
            String sanitizedMessage = sanitizeUserMessage(userMessage);
            String response = callSpringAI(sanitizedMessage);
            String finalResponse = truncateResponse(response);

            log.info("AI 응답 생성 완료: {}", finalResponse);
            return finalResponse;

        } catch (Exception e) {
            return handleApiError(e);
        }
    }

    // 기존 private 메서드들 그대로 복사
    private String sanitizeUserMessage(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "안녕하세요";
        }
        return userMessage;
    }

    private String callSpringAI(String userMessage) {
        // 기존 SimpleAIResponseGenerator의 callSpringAI 로직 그대로
        String combinedPrompt = SYSTEM_PROMPT + "\n\n사용자: " + userMessage + "\n\nAI:";

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();

        Prompt prompt = new Prompt(combinedPrompt, options);
        ChatResponse response = chatModel.call(prompt);

        return response.getResult().getOutput().getContent().trim();
    }

    private String truncateResponse(String response) {
        if (response.length() > MAX_RESPONSE_LENGTH) {
            return response.substring(0, MAX_RESPONSE_LENGTH - 3) + "...";
        }
        return response;
    }

    private String handleApiError(Exception e) {
        log.error("AI 응답 생성 실패: {}", e.getMessage(), e);
        return DEFAULT_RESPONSE;
    }
}
```

### **🎯 Day 2: SimpleConversationService 의존성 수정 및 기존 클래스 제거**

#### **오전 (2-3시간): SimpleConversationService 의존성 수정**

```java
// 기존 SimpleAIResponseGenerator 의존성을 Port로 변경
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    // ✅ 변경: Port 인터페이스에 의존
    private final AIResponsePort aiResponsePort;              // 기존: SimpleAIResponseGenerator
    private final EmotionAnalysisPort emotionAnalysisPort;    // 기존: SimpleAIResponseGenerator.analyzeBasicEmotion

    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        log.info("Processing user message for member {}: {}", memberId, content);

        // 1. 활성 대화 조회 또는 새 대화 생성 (기존과 동일)
        ConversationEntity conversation = findOrCreateActiveConversation(memberId);

        // 2. 사용자 메시지 감정 분석 및 저장 (Port 사용)
        MessageEntity userMessage = saveUserMessage(conversation.getId(), content);

        // 3. AI 응답 생성 (Port 사용)
        String aiResponse = aiResponsePort.generateResponse(content);

        // 4. AI 응답 메시지 저장 (기존과 동일)
        MessageEntity aiMessage = saveAIMessage(conversation.getId(), aiResponse);

        // 5. 응답 DTO 생성 (기존 방식 유지)
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

    // ✅ 수정: 감정 분석 로직을 Port로 위임
    private MessageEntity saveUserMessage(Long conversationId, String content) {
        EmotionType emotion = emotionAnalysisPort.analyzeEmotion(content);  // Port 사용
        MessageEntity userMessage = MessageEntity.createUserMessage(conversationId, content, emotion);
        return messageRepository.save(userMessage);
    }

    // processSystemMessage와 기타 메서드들은 기존과 동일하게 유지
}
```

#### **오후 (3-4시간): 기존 SimpleAIResponseGenerator 제거 및 테스트 수정**

```java
// 1. SimpleAIResponseGenerator.java 파일 삭제
// 2. 관련 테스트 코드 수정
@ExtendWith(MockitoExtension.class)
class SimpleConversationServiceTest {
    @Mock private AIResponsePort aiResponsePort;              // 변경
    @Mock private EmotionAnalysisPort emotionAnalysisPort;    // 변경

    @Test
    void processUserMessage_success() {
        // Given
        given(emotionAnalysisPort.analyzeEmotion("안녕하세요"))
            .willReturn(EmotionType.POSITIVE);
        given(aiResponsePort.generateResponse("안녕하세요"))
            .willReturn("안녕하세요! 반가워요.");

        // When & Then (기존과 동일)
    }
}
```

### **🎯 Day 3: 설정 관리 체계화 - ConversationProperties 도입**

#### **오전 (3-4시간): ConversationProperties 클래스 생성**

```java
// conversation/config/ 패키지에 설정 클래스 생성
@ConfigurationProperties(prefix = "maruni.conversation")
@Component
@Data
public class ConversationProperties {

    private Ai ai = new Ai();
    private Emotion emotion = new Emotion();

    @Data
    public static class Ai {
        private String model = "gpt-4o";
        private Double temperature = 0.7;
        private Integer maxTokens = 100;
        private Integer maxResponseLength = 100;
        private String systemPrompt = "당신은 노인 돌봄 전문 AI 상담사입니다. 따뜻하고 공감적으로 30자 이내로 응답하세요.";
        private String defaultResponse = "안녕하세요! 어떻게 지내세요?";
    }

    @Data
    public static class Emotion {
        private Map<String, List<String>> keywords = Map.of(
            "negative", List.of("슬프", "우울", "아프", "힘들", "외로", "무서", "걱정", "답답"),
            "positive", List.of("좋", "행복", "기쁘", "감사", "즐거", "만족", "고마")
        );
    }
}

// application.yml에 설정 추가
maruni:
  conversation:
    ai:
      model: gpt-4o
      temperature: 0.7
      max-tokens: 100
      max-response-length: 100
      system-prompt: "당신은 노인 돌봄 전문 AI 상담사입니다. 따뜻하고 공감적으로 30자 이내로 응답하세요."
      default-response: "안녕하세요! 어떻게 지내세요?"
    emotion:
      keywords:
        negative: ["슬프", "우울", "아프", "힘들", "외로", "무서", "걱정", "답답"]
        positive: ["좋", "행복", "기쁘", "감사", "즐거", "만족", "고마"]
```

#### **오후 (3-4시간): 기존 Adapter들을 Properties 사용하도록 수정**

```java
// OpenAIResponseAdapter 수정 - @Value 대신 Properties 사용
@Component
@RequiredArgsConstructor
public class OpenAIResponseAdapter implements AIResponsePort {
    private final ChatModel chatModel;
    private final ConversationProperties properties;  // 추가

    @Override
    public String generateResponse(String userMessage) {
        try {
            String sanitizedMessage = sanitizeUserMessage(userMessage);
            String response = callSpringAI(sanitizedMessage);
            String finalResponse = truncateResponse(response);

            return finalResponse;
        } catch (Exception e) {
            return properties.getAi().getDefaultResponse();  // Properties 사용
        }
    }

    private String callSpringAI(String userMessage) {
        String systemPrompt = properties.getAi().getSystemPrompt();  // Properties 사용
        String combinedPrompt = systemPrompt + "\n\n사용자: " + userMessage + "\n\nAI:";

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(properties.getAi().getModel())           // Properties 사용
                .withTemperature(properties.getAi().getTemperature()) // Properties 사용
                .withMaxTokens(properties.getAi().getMaxTokens())   // Properties 사용
                .build();

        Prompt prompt = new Prompt(combinedPrompt, options);
        ChatResponse response = chatModel.call(prompt);

        return response.getResult().getOutput().getContent().trim();
    }

    private String truncateResponse(String response) {
        int maxLength = properties.getAi().getMaxResponseLength();  // Properties 사용
        if (response.length() > maxLength) {
            return response.substring(0, maxLength - 3) + "...";
        }
        return response;
    }
}

// KeywordBasedEmotionAnalyzer 수정 - Properties 사용
@Component
@RequiredArgsConstructor
public class KeywordBasedEmotionAnalyzer implements EmotionAnalysisPort {
    private final ConversationProperties properties;

    @Override
    public EmotionType analyzeEmotion(String message) {
        if (!StringUtils.hasText(message)) {
            return EmotionType.NEUTRAL;
        }

        String lowerMessage = message.toLowerCase();
        Map<String, List<String>> keywords = properties.getEmotion().getKeywords();

        // 부정적 키워드 우선 체크
        if (containsAnyKeyword(lowerMessage, keywords.get("negative"))) {
            return EmotionType.NEGATIVE;
        }

        // 긍정적 키워드 체크
        if (containsAnyKeyword(lowerMessage, keywords.get("positive"))) {
            return EmotionType.POSITIVE;
        }

        return EmotionType.NEUTRAL;
    }
}
```

## 📁 최종 아키텍처 (3일 완료 후)

### **🎯 AI 모델 변경 대비 패키지 구조**
```
conversation/
├── presentation/
│   └── ConversationController.java (기존 유지)
├── application/
│   ├── dto/ (기존 유지)
│   │   ├── request/ConversationRequestDto.java
│   │   ├── response/ConversationResponseDto.java
│   │   └── MessageDto.java
│   └── service/
│       └── SimpleConversationService.java (✅ Port 의존성으로 변경)
├── domain/
│   ├── entity/ (기존 유지)
│   │   ├── ConversationEntity.java
│   │   ├── MessageEntity.java
│   │   ├── EmotionType.java
│   │   └── MessageType.java
│   ├── repository/ (기존 유지)
│   │   ├── ConversationRepository.java
│   │   └── MessageRepository.java
│   └── port/ (✅ 신규 패키지 - 핵심 추가, 더 간결한 구조)
│       ├── AIResponsePort.java
│       └── EmotionAnalysisPort.java
├── infrastructure/
│   ├── ai/ (✅ 신규 패키지 - AI 모델 독립성)
│   │   └── OpenAIResponseAdapter.java (✅ Port 구현)
│   └── analyzer/ (✅ 신규 패키지 - 감정 분석 독립성)
│       └── KeywordBasedEmotionAnalyzer.java (✅ Port 구현)
└── config/ (✅ 신규 패키지 - 설정 중앙화)
    └── ConversationProperties.java
```

### **🔗 AI 모델 변경 시나리오별 대응**
```
🔄 OpenAI → Claude 변경:
1. ClaudeResponseAdapter 구현 (AIResponsePort)
2. application.yml 설정 변경
3. 의존성 주입 변경
⏱️ 소요시간: 1시간

🔄 복수 모델 동시 사용:
1. 각 모델별 Adapter 구현
2. ConversationProperties에 모델 선택 로직 추가
3. Service에서 조건부 주입
⏱️ 소요시간: 2-3시간

🔄 온프레미스 모델 추가:
1. LocalModelAdapter 구현 (AIResponsePort)
2. 로컬 모델 서버 연동 로직
3. 설정 프로파일 추가
⏱️ 소요시간: 4-6시간
```

## 📈 AI 모델 변경 대비 성과 지표

### **🎯 핵심 성과 지표**
- **AI 모델 독립성**: 100% 달성 (Port-Adapter 패턴)
- **설정 중앙화**: 6개 @Value → 1개 Properties 클래스
- **SRP 준수**: SimpleAIResponseGenerator → 2개 전용 클래스 분리
- **기존 호환성**: AlertRule, DailyCheck 연동 100% 유지

### **⚡ AI 모델 변경 대응력**
- **OpenAI → 다른 모델**: 1시간 내 완료 가능
- **복수 모델 지원**: 2-3시간 내 구현 가능
- **A/B 테스트**: 설정 변경만으로 즉시 가능
- **벤더 락인 해제**: 협상력 및 백업 옵션 확보

### **🔒 안정성 보장**
- **기존 API 스펙**: 100% 유지 (Controller 변경 없음)
- **성능 영향**: ±5% 이내 (인터페이스 호출 오버헤드 최소)
- **도메인 연동**: 기존 연동 코드 변경 없음
- **테스트 커버리지**: 90%+ 유지

## 🚨 리스크 관리 및 안전장치

### **⚠️ 주요 리스크 대응**

#### **1. 성능 영향 최소화**
- **모니터링**: 각 Day별 성능 벤치마크 측정
- **임계값**: 5% 이상 성능 저하 시 즉시 롤백
- **최적화**: 인터페이스 호출 오버헤드 측정 및 최소화

#### **2. 기존 연동 보장**
- **회귀 테스트**: AlertRule, DailyCheck 연동 테스트 필수
- **시그니처 유지**: 기존 public 메서드 호환성 100% 보장
- **점진적 전환**: deprecated 활용한 안전한 마이그레이션

### **✅ 검증 체크리스트**
- [ ] Day 1 완료 후: Port 인터페이스 동작 검증
- [ ] Day 2 완료 후: Service 계층 정상 동작 확인
- [ ] Day 3 완료 후: 전체 시스템 통합 테스트
- [ ] 각 단계별: 성능 저하 5% 미만 확인
- [ ] 최종 완료: AI 모델 변경 시나리오 테스트

---

**🎯 v4.0은 AI 모델 변경 가능성에 대비한 핵심 인프라 구축에 집중하는 실용적 계획입니다.**

**📊 3일 투자로 향후 AI 모델 변경 시 90% 이상의 개발 시간을 절약할 수 있습니다.**