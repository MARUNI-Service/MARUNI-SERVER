# Phase 2: SMS 시스템 MVP 구현 계획

## 📋 프로젝트 개요

**MARUNI Phase 2**에서는 Twilio API를 활용한 완전한 SMS 시스템을 TDD 방식으로 구축합니다.
이 시스템은 Phase 1의 AI 대화 시스템과 연동되어 실제 노인 돌봄 서비스의 핵심 기능을 제공합니다.

### 🎯 Phase 2 목표
- **양방향 SMS 통신**: 발송 및 수신 완전 지원
- **Twilio API 완전 연동**: 프로덕션 레벨 안정성
- **실시간 상태 추적**: 발송/수신/실패 상태 실시간 모니터링
- **고가용성 시스템**: 재시도 메커니즘 및 장애 복구
- **TDD 완전 적용**: 90% 이상 테스트 커버리지

### 📊 현재 상태 및 연계점
```
✅ Phase 1 완료: AI 대화 시스템 (ConversationService)
🔄 Phase 2 목표: SMS 시스템 ←→ AI 시스템 연동
⏳ Phase 3 예정: 정기 안부 메시지 스케줄링
```

---

## 🏗️ SMS 시스템 요구사항 분석

### 🔄 핵심 비즈니스 플로우

#### 1. 발신 메시지 플로우
```
사용자/관리자 → SMS 발송 요청 → Twilio API → 실제 SMS 발송
                     ↓
              발송 상태 추적 ← Webhook 상태 업데이트 ← Twilio
```

#### 2. 수신 메시지 플로우
```
실제 SMS 수신 → Twilio Webhook → 메시지 파싱 → AI 처리 → 자동 응답
                                      ↓
                              수신 메시지 저장 + 이력 관리
```

#### 3. AI 연동 플로우 (Phase 1과 통합)
```
SMS 수신 → SmsService → ConversationService (Phase 1) → AI 응답 생성
    ↓                                                        ↓
메시지 저장 ←────────── SMS 발송 ←─────────── AI 응답 내용
```

### 📋 상세 요구사항

#### 기능 요구사항
- **SMS 발송**: 개별/대량 발송 지원
- **SMS 수신**: Webhook을 통한 실시간 수신 처리
- **상태 추적**: 발송 → 전달 → 읽음 확인까지 완전 추적
- **재시도 메커니즘**: 발송 실패 시 자동 재시도 (최대 3회)
- **이력 관리**: 모든 SMS 송수신 이력 저장 및 조회
- **AI 연동**: 수신 SMS를 AI가 분석하고 자동 응답

#### 비기능 요구사항
- **성능**: SMS 발송 응답시간 3초 이내
- **가용성**: 99.9% 서비스 가용성
- **확장성**: 동시 1000건 SMS 처리 가능
- **보안**: 전화번호 암호화, Webhook 서명 검증
- **비용 최적화**: 중복 발송 방지, 발송 실패 최소화

---

## 🛠️ 기술 스택 및 아키텍처

### 📚 기술 스택

#### Core Technologies
```yaml
SMS Provider: Twilio API
HTTP Client: Spring WebClient (Twilio SDK 사용)
Message Queue: Redis (재시도 처리용)
Webhook Security: Twilio Signature 검증
Monitoring: Spring Boot Actuator
```

#### 의존성 추가
```gradle
dependencies {
    // Twilio SMS
    implementation 'com.twilio.sdk:twilio:9.14.1'

    // HTTP 클라이언트 강화
    implementation 'org.springframework:spring-webflux'
    implementation 'io.netty:netty-resolver-dns-native-macos:4.1.94.Final:osx-aarch_64'

    // 암호화 (전화번호 보호)
    implementation 'org.springframework.security:spring-security-crypto'

    // Redis Queue (재시도 처리)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // 웹훅 서명 검증
    implementation 'commons-codec:commons-codec:1.15'

    // 테스트 강화
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
    testImplementation 'org.wiremock:wiremock-standalone:3.2.0'
}
```

### 🏛️ DDD 아키텍처 설계

#### SMS 도메인 레이어 구조
```
domain/sms/
├── application/          # Application Layer
│   ├── dto/             # Request/Response DTO
│   │   ├── SmsRequestDto.java
│   │   ├── SmsResponseDto.java
│   │   ├── SmsBatchRequestDto.java
│   │   └── SmsStatusDto.java
│   ├── service/         # Application Service
│   │   ├── SmsService.java           # 핵심 비즈니스 로직
│   │   ├── SmsWebhookService.java    # 웹훅 처리
│   │   └── SmsRetryService.java      # 재시도 로직
│   └── mapper/          # DTO ↔ Entity 매핑
│       └── SmsMapper.java
├── domain/              # Domain Layer
│   ├── entity/         # Domain Entity
│   │   ├── SmsEntity.java            # SMS 메시지 엔티티
│   │   ├── SmsStatusType.java        # 발송 상태 Enum
│   │   ├── SmsDirectionType.java     # 송신/수신 구분 Enum
│   │   └── SmsProviderType.java      # SMS 제공업체 Enum
│   ├── service/        # Domain Service
│   │   ├── SmsValidationService.java # 전화번호/메시지 검증
│   │   └── SmsCostCalculator.java    # 발송 비용 계산
│   ├── repository/     # Repository Interface
│   │   └── SmsRepository.java
│   └── vo/            # Value Object
│       ├── PhoneNumber.java          # 전화번호 VO
│       └── SmsContent.java           # SMS 내용 VO
├── infrastructure/      # Infrastructure Layer
│   ├── client/         # 외부 API 클라이언트
│   │   ├── TwilioSmsClient.java      # Twilio API 클라이언트
│   │   └── SmsProviderClient.java    # SMS 제공업체 인터페이스
│   ├── config/         # 설정
│   │   ├── TwilioConfig.java
│   │   └── SmsRetryConfig.java
│   └── repository/     # Repository 구현체
│       └── JpaSmsRepository.java
└── presentation/        # Presentation Layer
    └── controller/     # REST API Controller
        ├── SmsController.java        # SMS 발송 API
        ├── SmsWebhookController.java # 웹훅 수신 API
        └── SmsHistoryController.java # 이력 조회 API
```

---

## 🗺️ TDD 개발 로드맵 (3주 계획)

### 1주차: SMS 발송 시스템 구축

#### Day 1-2: 🔴 Red Phase - 기본 SMS 발송 테스트
```java
@DisplayName("SMS 발송 도메인 테스트")
class SmsServiceTest {

    @Test
    @DisplayName("유효한 전화번호와 메시지로 SMS를 발송한다")
    void sendSms_WithValidData_SendSuccessfully() {
        // Given
        SmsRequestDto request = SmsRequestDto.builder()
            .phoneNumber("+821012345678")
            .content("안녕하세요, 오늘 기분은 어떠세요?")
            .build();

        // When
        SmsResponseDto response = smsService.sendSms(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SmsStatusType.SENT);
        assertThat(response.getExternalId()).isNotBlank();
    }

    @Test
    @DisplayName("잘못된 전화번호로 SMS 발송 시 예외가 발생한다")
    void sendSms_WithInvalidPhoneNumber_ThrowsException() {
        // Given
        SmsRequestDto request = SmsRequestDto.builder()
            .phoneNumber("invalid-number")
            .content("테스트 메시지")
            .build();

        // When & Then
        assertThatThrownBy(() -> smsService.sendSms(request))
            .isInstanceOf(InvalidPhoneNumberException.class);
    }
}
```

#### Day 3-4: 🟢 Green Phase - 최소 구현
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmsService {

    private final SmsRepository smsRepository;
    private final TwilioSmsClient twilioClient;

    @Transactional
    public SmsResponseDto sendSms(SmsRequestDto request) {
        // 1. 입력 검증
        validateRequest(request);

        // 2. Twilio API 호출
        TwilioResponse twilioResponse = twilioClient.sendSms(
            request.getPhoneNumber(),
            request.getContent()
        );

        // 3. SMS 엔티티 저장
        SmsEntity smsEntity = SmsEntity.builder()
            .phoneNumber(request.getPhoneNumber())
            .content(request.getContent())
            .direction(SmsDirectionType.OUTBOUND)
            .status(SmsStatusType.SENT)
            .externalId(twilioResponse.getSid())
            .build();

        SmsEntity saved = smsRepository.save(smsEntity);

        // 4. 응답 DTO 반환
        return SmsResponseDto.from(saved);
    }
}
```

#### Day 5: 🔵 Refactor Phase - 코드 품질 개선
- PhoneNumber VO 추가로 타입 안정성 확보
- SmsValidationService 추가로 검증 로직 분리
- 예외 처리 강화 및 로깅 추가

### 2주차: SMS 수신 및 상태 추적

#### Day 8-9: 🔴 Red Phase - SMS 수신 웹훅 테스트
```java
@DisplayName("SMS 웹훅 처리 테스트")
class SmsWebhookControllerTest {

    @Test
    @DisplayName("Twilio 웹훅으로 수신된 SMS를 처리한다")
    void handleIncomingSms_WithValidWebhook_ProcessSuccessfully() {
        // Given
        String webhookPayload = createValidTwilioWebhookPayload();
        String twilioSignature = generateTwilioSignature(webhookPayload);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/sms/webhook",
            createWebhookRequest(webhookPayload, twilioSignature),
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // SMS가 데이터베이스에 저장되었는지 확인
        Optional<SmsEntity> savedSms = smsRepository.findByExternalId(extractSid(webhookPayload));
        assertThat(savedSms).isPresent();
        assertThat(savedSms.get().getDirection()).isEqualTo(SmsDirectionType.INBOUND);
    }

    @Test
    @DisplayName("잘못된 서명의 웹훅 요청을 거부한다")
    void handleIncomingSms_WithInvalidSignature_ReturnsUnauthorized() {
        // Given
        String webhookPayload = createValidTwilioWebhookPayload();
        String invalidSignature = "invalid-signature";

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/sms/webhook",
            createWebhookRequest(webhookPayload, invalidSignature),
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

#### Day 10-11: 🟢 Green Phase - 웹훅 처리 구현
```java
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@AutoApiResponse
public class SmsWebhookController {

    private final SmsWebhookService webhookService;
    private final TwilioSignatureValidator signatureValidator;

    @PostMapping("/webhook")
    @Operation(summary = "Twilio SMS 웹훅 처리")
    public ResponseEntity<String> handleSmsWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Twilio-Signature") String signature,
            HttpServletRequest request) {

        // 1. Twilio 서명 검증
        if (!signatureValidator.validate(signature, payload, request.getRequestURL().toString())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 웹훅 처리
        webhookService.processIncomingSms(payload);

        return ResponseEntity.ok("OK");
    }
}
```

#### Day 12: 🔵 Refactor Phase - 보안 및 성능 개선
- 웹훅 서명 검증 로직 강화
- 중복 메시지 처리 방지
- 비동기 처리로 응답 속도 개선

### 3주차: AI 연동 및 고급 기능

#### Day 15-16: 🔴 Red Phase - AI 연동 테스트
```java
@DisplayName("SMS ↔ AI 통합 테스트")
class SmsAiIntegrationTest {

    @Test
    @DisplayName("수신된 SMS를 AI가 분석하고 자동 응답한다")
    void processIncomingSms_WithAiAnalysis_GeneratesAutoReply() {
        // Given
        String incomingSms = "오늘 너무 외로워요";
        String phoneNumber = "+821012345678";

        // Mock: AI 서비스가 적절한 응답 생성
        when(conversationService.processUserMessage(anyLong(), eq(incomingSms)))
            .thenReturn(createAiResponse("괜찮으세요. 항상 당신을 생각하고 있어요."));

        // When
        webhookService.processIncomingSms(createWebhookPayload(phoneNumber, incomingSms));

        // Then
        // 1. 수신 SMS 저장 확인
        assertThat(smsRepository.findByPhoneNumberAndDirection(phoneNumber, INBOUND))
            .hasSize(1);

        // 2. AI 응답 SMS 발송 확인
        assertThat(smsRepository.findByPhoneNumberAndDirection(phoneNumber, OUTBOUND))
            .hasSize(1);

        // 3. AI 서비스 호출 확인
        verify(conversationService).processUserMessage(anyLong(), eq(incomingSms));
    }
}
```

#### Day 17-18: 🟢 Green Phase - AI 연동 구현
```java
@Service
@RequiredArgsConstructor
public class SmsWebhookService {

    private final SmsService smsService;
    private final SimpleConversationService conversationService; // Phase 1 연동
    private final MemberService memberService;

    @Async
    @Transactional
    public void processIncomingSms(String webhookPayload) {
        // 1. 웹훅 파싱
        TwilioWebhookDto webhook = parseWebhookPayload(webhookPayload);

        // 2. 수신 SMS 저장
        SmsEntity incomingSms = saveSmsEntity(webhook, SmsDirectionType.INBOUND);

        // 3. 회원 조회 (전화번호 기반)
        Optional<MemberEntity> member = memberService.findByPhoneNumber(webhook.getFrom());

        if (member.isPresent()) {
            // 4. AI 분석 및 응답 생성
            ConversationResponseDto aiResponse = conversationService.processUserMessage(
                member.get().getId(),
                webhook.getBody()
            );

            // 5. AI 응답을 SMS로 발송
            SmsRequestDto replyRequest = SmsRequestDto.builder()
                .phoneNumber(webhook.getFrom())
                .content(aiResponse.getAiMessage().getContent())
                .build();

            smsService.sendSms(replyRequest);
        }
    }
}
```

#### Day 19-21: 🔵 Refactor Phase - 시스템 완성
- 재시도 메커니즘 구현 (Redis Queue 활용)
- 대량 발송 기능 추가
- 성능 최적화 및 부하 테스트
- 문서화 및 API 테스트 완료

---

## 📊 데이터베이스 설계

### SMS 도메인 테이블 설계

#### SMS 메시지 테이블
```sql
CREATE TABLE sms_messages (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES members(id),
    phone_number VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    status VARCHAR(15) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'UNDELIVERED')),
    provider VARCHAR(10) NOT NULL DEFAULT 'TWILIO',
    external_id VARCHAR(50), -- Twilio SID

    -- 비용 및 메타데이터
    cost_amount DECIMAL(10, 4),
    cost_currency VARCHAR(3) DEFAULT 'USD',
    segments INTEGER DEFAULT 1,

    -- 재시도 관련
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP,

    -- 연동 정보 (AI 응답과의 연결)
    conversation_id BIGINT,
    parent_sms_id BIGINT REFERENCES sms_messages(id), -- 응답 메시지인 경우

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_sms_messages_phone_number ON sms_messages(phone_number);
CREATE INDEX idx_sms_messages_status ON sms_messages(status);
CREATE INDEX idx_sms_messages_direction ON sms_messages(direction);
CREATE INDEX idx_sms_messages_created_at ON sms_messages(created_at);
CREATE INDEX idx_sms_messages_external_id ON sms_messages(external_id);
CREATE INDEX idx_sms_messages_member_id ON sms_messages(member_id);

-- 재시도용 인덱스
CREATE INDEX idx_sms_messages_retry ON sms_messages(status, next_retry_at)
WHERE status = 'FAILED' AND retry_count < max_retries;
```

#### SMS 전송 로그 테이블 (상세 추적용)
```sql
CREATE TABLE sms_delivery_logs (
    id BIGSERIAL PRIMARY KEY,
    sms_id BIGINT NOT NULL REFERENCES sms_messages(id),
    status VARCHAR(15) NOT NULL,
    status_description TEXT,
    provider_response JSONB, -- Twilio 응답 전문
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sms_delivery_logs_sms_id ON sms_delivery_logs(sms_id);
CREATE INDEX idx_sms_delivery_logs_status ON sms_delivery_logs(status);
```

---

## 🔧 환경 설정 및 Configuration

### 환경 변수 (.env)
```bash
# 기존 환경변수들...

# Twilio SMS 설정
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=your_twilio_phone_number
TWILIO_WEBHOOK_URL=https://yourapp.com/api/sms/webhook

# SMS 발송 설정
SMS_MAX_LENGTH=160
SMS_RETRY_MAX_COUNT=3
SMS_RETRY_DELAY_SECONDS=300
SMS_BATCH_SIZE=50

# 비용 관리
SMS_DAILY_LIMIT=1000
SMS_MONTHLY_BUDGET=100.00
SMS_COST_PER_MESSAGE=0.0075

# 보안 설정 (전화번호 암호화)
PHONE_ENCRYPTION_KEY=your_encryption_key_32_characters
PHONE_ENCRYPTION_ALGORITHM=AES/GCB/NoPadding
```

### Twilio Configuration 클래스
```java
@Configuration
@EnableConfigurationProperties(TwilioProperties.class)
public class TwilioConfig {

    @Bean
    public Twilio twilioClient(TwilioProperties properties) {
        Twilio.init(properties.getAccountSid(), properties.getAuthToken());
        return Twilio.getRestClient();
    }

    @Bean
    public TwilioSignatureValidator twilioSignatureValidator(TwilioProperties properties) {
        return new TwilioSignatureValidator(properties.getAuthToken());
    }
}

@ConfigurationProperties(prefix = "twilio")
@Data
public class TwilioProperties {
    private String accountSid;
    private String authToken;
    private String phoneNumber;
    private String webhookUrl;
    private Integer maxRetries = 3;
    private Duration retryDelay = Duration.ofMinutes(5);
}
```

---

## 🧪 테스트 전략

### 테스트 피라미드 (SMS 특화)
```
           /\
          /E2E\
         /____\ (SMS 실제 발송 테스트 5%)
        /      \
       /Integration\ (Twilio Mock 서버 15%)
      /__________\
     /            \
    /Unit Tests    \ (도메인 로직 80%)
   /________________\
```

### Mock 전략

#### Twilio API Mock (WireMock 사용)
```java
@TestConfiguration
public class TwilioTestConfig {

    @Bean
    @Primary
    public WireMockServer twilioMockServer() {
        WireMockServer wireMock = new WireMockServer(8089);

        // SMS 발송 성공 응답
        wireMock.stubFor(post(urlEqualTo("/2010-04-01/Accounts/test/Messages.json"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "sid": "SM1234567890abcdef",
                        "status": "queued",
                        "to": "+821012345678",
                        "from": "+15555551234",
                        "body": "Test message"
                    }
                    """)));

        // SMS 발송 실패 응답
        wireMock.stubFor(post(urlEqualTo("/2010-04-01/Accounts/test/Messages.json"))
            .withRequestBody(containing("invalid-number"))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody("""
                    {
                        "code": 21211,
                        "message": "The 'To' number is not a valid phone number."
                    }
                    """)));

        return wireMock;
    }
}
```

### 통합 테스트 (TestContainers)
```java
@SpringBootTest
@Testcontainers
class SmsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("SMS 발송부터 상태 업데이트까지 전체 플로우가 정상 동작한다")
    void completeSmsFlow_WorksProperly() {
        // 전체 통합 테스트 로직
    }
}
```

---

## 📈 성공 지표 및 완료 기준

### 기술적 지표
- **SMS 발송 성공률**: 99% 이상
- **웹훅 처리 응답시간**: 500ms 이내
- **테스트 커버리지**: SMS 도메인 90% 이상
- **API 응답시간**: 평균 200ms 이내
- **재시도 성공률**: 실패 후 재시도 시 80% 이상 성공

### 기능적 지표
- **양방향 통신**: SMS 발송 및 수신 완전 지원
- **상태 추적**: 발송 → 전달 → 읽음 까지 실시간 추적
- **AI 연동**: 수신 SMS에 대한 AI 자동 응답 생성
- **보안**: 전화번호 암호화 및 웹훅 서명 검증
- **안정성**: 장애 상황에서도 메시지 손실 없음

### Phase 2 완료 체크리스트
- [ ] Twilio API 연동 완료 및 테스트 통과
- [ ] SMS 엔티티 및 Repository 구현 완료
- [ ] SMS 발송 서비스 구현 완료
- [ ] SMS 수신 웹훅 처리 구현 완료
- [ ] 상태 추적 및 재시도 메커니즘 구현 완료
- [ ] AI 대화 시스템 연동 완료 (Phase 1과 통합)
- [ ] 전화번호 암호화 및 보안 기능 구현 완료
- [ ] REST API 엔드포인트 구현 완료
- [ ] 모든 단위 테스트 통과 (커버리지 90% 이상)
- [ ] 통합 테스트 통과 (실제 SMS 발송 포함)
- [ ] API 문서화 완료 (Swagger)
- [ ] 성능 테스트 통과 (응답시간 500ms 이내)

---

## ⚠️ 리스크 및 대응방안

### 기술적 리스크
1. **Twilio API 장애**
   - 대응: 다른 SMS 제공업체 준비 (예: AWS SNS)
   - 대응: 재시도 메커니즘으로 일시적 장애 대응

2. **SMS 발송 비용 급증**
   - 대응: 일일/월별 발송 한도 설정
   - 대응: 실시간 비용 모니터링

3. **웹훅 보안 취약성**
   - 대응: Twilio 서명 검증 필수
   - 대응: IP 화이트리스트 적용

### 비즈니스 리스크
1. **스팸 SMS로 분류**
   - 대응: 메시지 내용 최적화
   - 대응: 발송 빈도 조절

2. **사용자 개인정보 보호**
   - 대응: 전화번호 암호화 저장
   - 대응: GDPR 준수 데이터 처리

---

## 🚀 다음 단계 연계 (Phase 3 준비)

### Phase 3 연동 포인트
1. **스케줄링 시스템**: Phase 2의 SMS 발송 기능 활용
2. **보호자 알림**: 긴급 상황 감지 시 SMS 발송
3. **대량 발송**: 정기 안부 메시지 일괄 발송
4. **분석 시스템**: SMS 응답률 및 패턴 분석

### 확장성 고려사항
1. **멀티 채널**: SMS 외 카카오톡, 이메일 추가
2. **다국어 지원**: 메시지 템플릿 다국어 확장
3. **AI 고도화**: 더 정교한 감정 분석 및 응답 생성

---

**문서 작성일**: 2025-09-14
**최종 수정일**: 2025-09-14
**작성자**: Claude Code
**버전**: v1.0 (Phase 2 SMS System MVP Plan)
**개발 방법론**: Test-Driven Development (TDD)