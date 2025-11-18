# 🚨 자동 위험 감지 트리거 통합 구현 계획서

**작성일**: 2025-11-12
**최종 수정**: 2025-11-12 (데모 목적 단순화 반영)
**상태**: 📋 계획 단계 (리뷰 완료)
**담당 도메인**: AlertRule (자체 스케줄러) ↔ DailyCheck, AlertRule ↔ Conversation
**개발 방식**: 선 구현 후 테스트 작성
**목적**: 데모용 (과한 복잡성 제거)

---

## 📋 1. 배경 및 목표

### 현재 상황
- ✅ AlertRule 도메인: 3종 감지 알고리즘 완성 (EmotionPattern, NoResponse, Keyword)
- ✅ DailyCheck 도메인: 매일 오전 9시 안부 메시지 자동 발송 완성
- ✅ Conversation 도메인: OpenAI GPT-4o 대화 시스템 완성
- ❌ **문제점**: 도메인 간 연동 미구현으로 자동 감지 트리거 없음

### 목표
DailyCheck와 Conversation 도메인에서 AlertRule 도메인을 호출하여 **자동 위험 감지 시스템** 완성

#### 📌 구체적 목표
1. **DailyCheck 연동**: 안부 메시지 발송 후 무응답 패턴 자동 분석
2. **Conversation 연동**: 사용자 메시지 수신 시 위험 키워드 실시간 감지
3. **알림 자동화**: 위험 감지 시 보호자에게 즉시 알림 발송e

---

## 🎯 2. 구현 범위

### 2.1. DailyCheck → AlertRule 연동

#### 트리거 시점
- **일 1회**: 매일 오후 10시 (하루 응답 데이터 집계 후)
- **스케줄러**: 새로운 `AlertScheduler` 추가

#### 감지 알고리즘
- ✅ **NoResponseAnalyzer**: 무응답 패턴 분석
- ✅ **EmotionPatternAnalyzer**: 감정 패턴 분석 (대화 데이터 활용)

#### 구현 위치
```
alertrule/
└── application/
    └── scheduler/
        ├── (신규) AlertScheduler.java       # 스케줄링 트리거
        └── (신규) AlertTriggerService.java  # Alert 호출 전담
```

**패키지 위치 선정 이유 (DDD 원칙):**
- AlertRule이 감지의 주체이므로 스케줄러도 AlertRule 도메인에 위치
- DailyCheck는 단순히 "데이터 제공자" 역할 (메시지 응답 데이터)
- 재사용성: Guardian이나 다른 도메인에서도 감지 트리거 가능

---

### 2.2. Conversation → AlertRule 연동

#### 트리거 시점
- **실시간**: 사용자 메시지 수신 즉시

#### 감지 알고리즘
- ✅ **KeywordAnalyzer**: 위험 키워드 실시간 감지
  - **EMERGENCY** ("죽고싶다", "자살" 등): 즉시 알림 발송 ✅
  - **HIGH** ("우울", "외롭다" 등): 로그만 기록 (Phase 3에서 누적 분석 추가 예정) ⚠️

#### 구현 위치
```
conversation/
└── application/
    └── service/
        └── (수정) SimpleConversationService.java  # processUserMessage() 확장
```

---

## 🏗️ 3. 아키텍처 설계

### 3.1. 전체 데이터 플로우

```
┌─────────────────────────────────────────────────────────────┐
│                    자동 트리거 시스템                          │
└─────────────────────────────────────────────────────────────┘

[DailyCheck 트리거 - 일 1회]
┌──────────────────┐
│ AlertScheduler   │ (매일 오후 10시)
│ @Scheduled       │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ AlertTrigger     │ (전체 회원 순회 + 예외 격리)
│ Service          │
└────────┬─────────┘
         ↓
┌──────────────────────────────────────┐
│ AlertDetectionService                │
│ • detectAnomalies(memberId)          │
│   - NoResponseAnalyzer               │
│   - EmotionPatternAnalyzer           │
└────────┬─────────────────────────────┘
         ↓
┌──────────────────────────────────────┐
│ AlertNotificationService             │
│ • triggerAlert(memberId, result)     │
│   - AlertHistory 저장                │
│   - 보호자 알림 발송                  │
└──────────────────────────────────────┘


[Conversation 트리거 - 실시간]
┌──────────────────┐
│ POST /api/       │
│ conversations/   │
│ messages         │
└────────┬─────────┘
         ↓
┌──────────────────────────────────────┐
│ SimpleConversationService            │
│ • processUserMessage()               │
│   1. 메시지 저장                      │
│   2. AI 응답 생성                     │
│   3. ⭐ 키워드 감지 (신규 + 예외 격리) │
└────────┬─────────────────────────────┘
         ↓
┌──────────────────────────────────────┐
│ AlertDetectionService                │
│ • detectKeywordAlert(message, id)    │
│   - KeywordAnalyzer                  │
└────────┬─────────────────────────────┘
         ↓
┌──────────────────────────────────────┐
│ AlertNotificationService             │
│ • triggerAlert() (EMERGENCY만)       │
│   - 긴급 키워드 즉시 알림             │
└──────────────────────────────────────┘
```

---

### 3.2. 계층 간 의존성

```
Presentation Layer
    ↓
Application Layer
    ConversationService → AlertDetectionService (신규 의존성)
    AlertTriggerService → AlertDetectionService (신규 서비스)
    ↓
Domain Layer
    AlertRule (감지 로직)
    Conversation (메시지 데이터)
    DailyCheck (응답 데이터)
```

**DDD 계층 원칙 준수**:
- ✅ Application → Domain 의존 (허용)
- ✅ 같은 Application 계층 간 의존 (허용)
- ❌ Domain → Application 의존 금지

---

## 🧪 4. 테스트하기 좋은 코드 설계 원칙

### 4.1. 의존성 주입 (Constructor Injection)

**원칙**: 모든 외부 의존성은 생성자로 주입받아 모킹 가능하게 설계

#### ✅ Good Example
```java
@Service
@RequiredArgsConstructor  // 생성자 자동 생성
public class AlertTriggerService {

    private final AlertDetectionService alertDetectionService;  // 모킹 가능
    private final AlertNotificationService alertNotificationService;  // 모킹 가능
    private final MemberRepository memberRepository;  // 모킹 가능

    // 비즈니스 로직
}
```

**테스트 시**:
```java
@Test
void testDetectAnomalies() {
    // Given: 모킹된 의존성 주입
    AlertDetectionService mockDetection = mock(AlertDetectionService.class);
    AlertTriggerService service = new AlertTriggerService(
        mockDetection, mockNotification, mockRepository
    );

    // When & Then
}
```

---

#### ❌ Bad Example
```java
@Service
public class AlertTriggerService {

    @Autowired
    private AlertDetectionService alertDetectionService;  // 필드 주입 → 테스트 어려움

    // new로 직접 생성 → 모킹 불가
    private MemberRepository memberRepository = new MemberRepositoryImpl();
}
```

---

### 4.2. 단일 책임 원칙 (SRP)

**원칙**: 하나의 클래스는 하나의 책임만 가져야 테스트 범위가 명확

#### AlertTriggerService의 책임 분리

```java
// ✅ AlertTriggerService: 전체 회원 순회 + 예외 격리만 담당
public void detectAnomaliesForAllMembers() {
    List<Long> memberIds = memberRepository.findDailyCheckEnabledMemberIds();

    for (Long memberId : memberIds) {
        try {
            detectAndNotifyForMember(memberId);  // 개별 처리는 private 메서드로 위임
        } catch (Exception e) {
            log.error("Member {}의 이상징후 감지 실패", memberId, e);
        }
    }
}

// ✅ AlertDetectionService: 감지 알고리즘만 담당 (이미 완성됨)
public List<AlertResult> detectAnomalies(Long memberId) { ... }

// ✅ AlertNotificationService: 알림 발송만 담당 (이미 완성됨)
public Long triggerAlert(Long memberId, AlertResult result) { ... }
```

**테스트 시**:
- `AlertTriggerService`: 회원 순회 로직만 테스트
- `AlertDetectionService`: 감지 알고리즘만 테스트
- `AlertNotificationService`: 알림 발송만 테스트

---

### 4.3. 예외 처리 격리

**원칙**: 예외가 발생해도 다른 로직에 영향 없도록 try-catch로 격리

#### DailyCheck 연동 (전체 회원 순회)
```java
// ✅ 개별 회원 실패가 전체 스케줄러를 중단시키지 않음
public void detectAnomaliesForAllMembers() {
    List<Long> memberIds = memberRepository.findDailyCheckEnabledMemberIds();
    int successCount = 0;
    int failureCount = 0;

    for (Long memberId : memberIds) {
        try {
            detectAndNotifyForMember(memberId);
            successCount++;
        } catch (Exception e) {
            failureCount++;
            log.error("❌ Member {}의 이상징후 감지 처리 실패", memberId, e);
        }
    }

    log.info("✅ 이상징후 감지 완료: 성공 {}, 실패 {}", successCount, failureCount);
}
```

**테스트 시나리오**:
- ✅ 정상 케이스: 전체 회원 성공
- ✅ 부분 실패 케이스: 일부 회원 실패해도 나머지 처리
- ✅ 전체 실패 케이스: 모든 회원 실패해도 예외 미전파

---

#### Conversation 연동 (실시간 키워드 감지)
```java
// ✅ 키워드 감지 실패가 대화 흐름을 중단시키지 않음
private void detectKeywordInRealtime(MessageEntity message, Long memberId) {
    try {
        AlertResult result = alertDetectionService.detectKeywordAlert(message, memberId);

        if (result.isAlert() && result.getAlertLevel() == AlertLevel.EMERGENCY) {
            alertNotificationService.triggerAlert(memberId, result);
            log.warn("⚠️ EMERGENCY keyword detected for member {}", memberId);
        }

    } catch (Exception e) {
        // 키워드 감지 실패는 대화 흐름에 영향 없음
        log.error("Keyword detection failed for member {}: {}",
                  memberId, e.getMessage(), e);
    }
}
```

**테스트 시나리오**:
- ✅ 정상 케이스: 키워드 감지 성공 → 알림 발송
- ✅ 예외 케이스: 감지 실패 → 대화 흐름 유지 + 로그 기록

---

### 4.4. 모킹 포인트 명시

**테스트 시 모킹할 의존성을 명확히 정의**

#### AlertTriggerService 모킹 포인트
```java
@ExtendWith(MockitoExtension.class)
class AlertTriggerServiceTest {

    @Mock private AlertDetectionService alertDetectionService;  // 감지 로직 모킹
    @Mock private AlertNotificationService alertNotificationService;  // 알림 로직 모킹
    @Mock private MemberRepository memberRepository;  // DB 조회 모킹

    @InjectMocks private AlertTriggerService alertTriggerService;

    @Test
    void detectAnomaliesForAllMembers_Success() {
        // Given
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L, 2L, 3L));

        AlertResult highAlert = AlertResult.createAlert(
            AlertLevel.HIGH, AlertType.NO_RESPONSE, "3일 무응답", null
        );
        when(alertDetectionService.detectAnomalies(anyLong()))
            .thenReturn(List.of(highAlert));

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then
        verify(alertDetectionService, times(3)).detectAnomalies(anyLong());
        verify(alertNotificationService, times(3)).triggerAlert(anyLong(), any());
    }
}
```

---

#### SimpleConversationService 모킹 포인트
```java
@ExtendWith(MockitoExtension.class)
class SimpleConversationServiceTest {

    @Mock private ConversationManager conversationManager;
    @Mock private MessageProcessor messageProcessor;
    @Mock private AlertDetectionService alertDetectionService;  // 신규 모킹
    @Mock private AlertNotificationService alertNotificationService;  // 신규 모킹

    @InjectMocks private SimpleConversationService conversationService;

    @Test
    void processUserMessage_EmergencyKeyword_TriggersAlert() {
        // Given
        String emergencyMessage = "죽고싶다";
        AlertResult emergencyAlert = AlertResult.createAlert(
            AlertLevel.EMERGENCY, AlertType.KEYWORD_DETECTION, "긴급 키워드 감지", null
        );

        when(alertDetectionService.detectKeywordAlert(any(), anyLong()))
            .thenReturn(emergencyAlert);

        // When
        conversationService.processUserMessage(1L, emergencyMessage);

        // Then
        verify(alertNotificationService, times(1)).triggerAlert(eq(1L), any());
    }
}
```

---

### 4.5. 시간 의존성 제거

**원칙**: 현재 시각에 의존하는 로직은 주입 가능하게 설계

#### ❌ Bad Example (테스트 어려움)
```java
public void scheduleNextRetry() {
    LocalDateTime now = LocalDateTime.now();  // 고정된 시간으로 테스트 불가
    this.scheduledTime = now.plusMinutes(5);
}
```

#### ✅ Good Example (테스트 가능)
```java
// 방법 1: Clock 주입 (Spring Boot 권장)
private final Clock clock;

public void scheduleNextRetry() {
    LocalDateTime now = LocalDateTime.now(clock);  // 테스트 시 고정 Clock 주입 가능
    this.scheduledTime = now.plusMinutes(5);
}

// 방법 2: 시간을 파라미터로 받기 (더 간단)
public void scheduleNextRetry(LocalDateTime currentTime) {
    this.scheduledTime = currentTime.plusMinutes(5);
}
```

**참고**: 현재 구현에서는 AlertHistory가 `alertDate`를 자동 생성하므로, 테스트 시 `createAlertWithDate()` 정적 팩토리 메서드 사용

---

## 📝 5. 구현 계획 (단계별)

### Phase 1: DailyCheck → AlertRule 연동 (무응답 분석)

#### Step 1: AlertTriggerService 구현
**목적**: AlertRule 호출을 전담하는 서비스 (SRP)

**파일**: `alertrule/application/scheduler/AlertTriggerService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertTriggerService {

    private final AlertDetectionService alertDetectionService;
    private final AlertNotificationService alertNotificationService;
    private final MemberRepository memberRepository;

    /**
     * 전체 활성 회원 이상징후 감지 (예외 격리)
     *
     * Note: @Transactional 없음 - 각 회원 처리마다 독립적인 트랜잭션 사용
     *       (AlertDetectionService, AlertNotificationService가 각자 트랜잭션 관리)
     */
    public void detectAnomaliesForAllMembers() {
        List<Long> activeMemberIds = memberRepository.findDailyCheckEnabledMemberIds();
        int successCount = 0;
        int failureCount = 0;

        log.info("🔍 이상징후 감지 시작: 대상 회원 {}명", activeMemberIds.size());

        for (Long memberId : activeMemberIds) {
            try {
                detectAndNotifyForMember(memberId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ Member {}의 이상징후 감지 처리 실패", memberId, e);
                // Phase 3: 모니터링 시스템에 알림 (선택)
            }
        }

        log.info("✅ 이상징후 감지 완료: 성공 {}, 실패 {}", successCount, failureCount);
    }

    /**
     * 개별 회원 감지 및 알림 (private)
     */
    private void detectAndNotifyForMember(Long memberId) {
        // 1. 이상징후 감지 (NoResponse + EmotionPattern)
        List<AlertResult> results = alertDetectionService.detectAnomalies(memberId);

        // 2. 감지된 위험 신호 처리
        for (AlertResult result : results) {
            if (result.isAlert()) {
                alertNotificationService.triggerAlert(memberId, result);
                log.info("⚠️ Member {}에게 {} 알림 발송", memberId, result.getAlertLevel());
            }
        }
    }
}
```

**설계 포인트**:
- ✅ 의존성 주입: 생성자 주입으로 모킹 가능
- ✅ 예외 격리: 개별 회원 실패가 전체에 영향 없음
- ✅ 단일 책임: 회원 순회만 담당, 감지/알림은 위임
- ✅ 로깅: 성공/실패 카운트 추적

---

#### Step 2: AlertScheduler 구현
**목적**: 매일 오후 10시 자동 감지 트리거

**파일**: `alertrule/application/scheduler/AlertScheduler.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final AlertTriggerService alertTriggerService;

    /**
     * 매일 오후 10시 이상징후 감지 (하루 데이터 집계 후)
     */
    @Scheduled(cron = "${maruni.scheduling.alert-detection.cron}")
    public void triggerDailyAnomalyDetection() {
        log.info("📅 [AlertScheduler] Daily anomaly detection triggered");
        alertTriggerService.detectAnomaliesForAllMembers();
    }
}
```

**설계 포인트**:
- ✅ 단일 책임: 스케줄링 트리거만 담당
- ✅ 설정 외부화: cron 표현식을 application.yml에서 관리
- ✅ 얇은 계층: 실제 로직은 AlertTriggerService에 위임

---

#### Step 3: application.yml 설정 추가
```yaml
maruni:
  scheduling:
    alert-detection:
      cron: "0 0 22 * * *"  # 매일 오후 10시
      enabled: true          # 테스트 시 비활성화 가능
```

---

#### Step 4-1: 단위 테스트 작성 (구현 완료 후)

##### AlertTriggerServiceTest.java
```java
@ExtendWith(MockitoExtension.class)
class AlertTriggerServiceTest {

    @Mock private AlertDetectionService alertDetectionService;
    @Mock private AlertNotificationService alertNotificationService;
    @Mock private MemberRepository memberRepository;
    @InjectMocks private AlertTriggerService alertTriggerService;

    @Test
    @DisplayName("전체 회원 감지 성공")
    void detectAnomaliesForAllMembers_Success() {
        // Given: 3명의 회원
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L, 2L, 3L));

        AlertResult highAlert = AlertResult.createAlert(
            AlertLevel.HIGH, AlertType.NO_RESPONSE, "3일 무응답", null
        );
        when(alertDetectionService.detectAnomalies(anyLong()))
            .thenReturn(List.of(highAlert));

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then
        verify(alertDetectionService, times(3)).detectAnomalies(anyLong());
        verify(alertNotificationService, times(3)).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("일부 회원 실패해도 나머지 처리 계속")
    void detectAnomaliesForAllMembers_PartialFailure() {
        // Given
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L, 2L, 3L));

        // Member 2 처리 시 예외 발생
        when(alertDetectionService.detectAnomalies(1L))
            .thenReturn(List.of(mock(AlertResult.class)));
        when(alertDetectionService.detectAnomalies(2L))
            .thenThrow(new RuntimeException("Database error"));
        when(alertDetectionService.detectAnomalies(3L))
            .thenReturn(List.of(mock(AlertResult.class)));

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then: 3명 모두 시도됨
        verify(alertDetectionService, times(3)).detectAnomalies(anyLong());
        // Member 1, 3만 알림 발송
        verify(alertNotificationService, times(2)).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("위험 신호 없을 때 알림 미발송")
    void detectAnomaliesForAllMembers_NoAlerts() {
        // Given
        when(memberRepository.findDailyCheckEnabledMemberIds())
            .thenReturn(List.of(1L));
        when(alertDetectionService.detectAnomalies(1L))
            .thenReturn(List.of());  // 빈 결과

        // When
        alertTriggerService.detectAnomaliesForAllMembers();

        // Then
        verify(alertNotificationService, never()).triggerAlert(anyLong(), any());
    }
}
```

##### AlertSchedulerTest.java
```java
@SpringBootTest
@TestPropertySource(properties = {
    "maruni.scheduling.alert-detection.enabled=false"  // 실제 스케줄러 비활성화
})
class AlertSchedulerTest {

    @MockBean private AlertTriggerService alertTriggerService;
    @Autowired private AlertScheduler alertScheduler;

    @Test
    @DisplayName("스케줄러가 AlertTriggerService 호출")
    void triggerDailyAnomalyDetection() {
        // When
        alertScheduler.triggerDailyAnomalyDetection();

        // Then
        verify(alertTriggerService, times(1)).detectAnomaliesForAllMembers();
    }
}
```

---

#### Step 4-2: 통합 테스트 (선택사항 - 데모용 간소화)

**데모 목적 판단: 단위 테스트로 충분, 통합 테스트는 선택사항**

**시간 있으면 작성 (선택사항):**
- 무응답 감지 → 알림 발송 → DB 저장 E2E
- 감정 패턴 감지 → 알림 발송 → DB 저장 E2E

**데모 목적 판단:** 단위 테스트로 충분

---

### Phase 2: Conversation → AlertRule 연동 (키워드 감지)

#### Step 1: SimpleConversationService 확장
**목적**: 메시지 처리 후 키워드 감지 추가 (예외 격리)

**파일**: `conversation/application/service/SimpleConversationService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleConversationService {

    // 기존 의존성
    private final ConversationManager conversationManager;
    private final MessageProcessor messageProcessor;
    private final ConversationMapper mapper;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;

    // 신규 의존성
    private final AlertDetectionService alertDetectionService;
    private final AlertNotificationService alertNotificationService;

    /**
     * 사용자 메시지 처리 (키워드 감지 추가)
     */
    @Transactional
    public ConversationResponseDto processUserMessage(Long memberId, String content) {
        log.info("Processing user message for member {}: {}", memberId, content);

        // 1. 기존 로직: 메시지 저장 + AI 응답
        ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);
        MessageExchangeResult result = messageProcessor.processMessage(conversation, content);

        // 2. 신규 로직: 키워드 실시간 감지 (예외 격리)
        detectKeywordInRealtime(result.getUserMessage(), memberId);

        return mapper.toResponseDto(result);
    }

    /**
     * 실시간 키워드 감지 (private, 예외 격리)
     */
    private void detectKeywordInRealtime(MessageEntity message, Long memberId) {
        try {
            AlertResult keywordResult = alertDetectionService.detectKeywordAlert(message, memberId);

            // EMERGENCY 키워드만 즉시 알림 발송
            if (keywordResult.isAlert() && keywordResult.getAlertLevel() == AlertLevel.EMERGENCY) {
                alertNotificationService.triggerAlert(memberId, keywordResult);
                log.warn("⚠️ EMERGENCY keyword detected for member {}: {}",
                         memberId, keywordResult.getMessage());
            } else if (keywordResult.isAlert()) {
                log.info("📌 HIGH keyword detected for member {} (로그만 기록)", memberId);
            }

        } catch (Exception e) {
            // 키워드 감지 실패는 대화 흐름에 영향 없음 (로그만 기록)
            log.error("Keyword detection failed for member {}: {}",
                      memberId, e.getMessage(), e);
        }
    }

    // ... 기존 메서드들 (processSystemMessage, getMyConversationHistory 등)
}
```

**설계 포인트**:
- ✅ 예외 격리: 키워드 감지 실패가 대화 흐름 중단 안 함
- ✅ 단일 책임: private 메서드로 키워드 감지 로직 분리
- ✅ 명확한 의도: EMERGENCY만 즉시 알림, HIGH는 로그만
- ✅ 기존 코드 영향 최소화: processUserMessage() 끝에 추가

---

#### Step 2: 테스트 작성 (구현 완료 후)

##### SimpleConversationServiceTest.java (확장)
```java
@ExtendWith(MockitoExtension.class)
class SimpleConversationServiceTest {

    @Mock private ConversationManager conversationManager;
    @Mock private MessageProcessor messageProcessor;
    @Mock private ConversationMapper mapper;
    @Mock private MessageRepository messageRepository;
    @Mock private MemberRepository memberRepository;

    // 신규 모킹
    @Mock private AlertDetectionService alertDetectionService;
    @Mock private AlertNotificationService alertNotificationService;

    @InjectMocks private SimpleConversationService conversationService;

    @Test
    @DisplayName("EMERGENCY 키워드 감지 시 즉시 알림")
    void processUserMessage_EmergencyKeyword_TriggersAlert() {
        // Given
        Long memberId = 1L;
        String emergencyMessage = "죽고싶다";

        ConversationEntity conversation = mock(ConversationEntity.class);
        MessageEntity userMessage = mock(MessageEntity.class);
        MessageExchangeResult result = new MessageExchangeResult(userMessage, mock(MessageEntity.class));

        when(conversationManager.findOrCreateActive(memberId)).thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, emergencyMessage)).thenReturn(result);

        AlertResult emergencyAlert = AlertResult.createAlert(
            AlertLevel.EMERGENCY, AlertType.KEYWORD_DETECTION, "긴급 키워드 감지", null
        );
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
            .thenReturn(emergencyAlert);

        // When
        conversationService.processUserMessage(memberId, emergencyMessage);

        // Then: 즉시 알림 발송
        verify(alertNotificationService, times(1)).triggerAlert(eq(memberId), any());
    }

    @Test
    @DisplayName("HIGH 키워드는 알림 미발송 (로그만 기록)")
    void processUserMessage_HighKeyword_NoImmediateAlert() {
        // Given
        Long memberId = 1L;
        String highMessage = "우울해";

        ConversationEntity conversation = mock(ConversationEntity.class);
        MessageEntity userMessage = mock(MessageEntity.class);
        MessageExchangeResult result = new MessageExchangeResult(userMessage, mock(MessageEntity.class));

        when(conversationManager.findOrCreateActive(memberId)).thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, highMessage)).thenReturn(result);

        AlertResult highAlert = AlertResult.createAlert(
            AlertLevel.HIGH, AlertType.KEYWORD_DETECTION, "경고 키워드 감지", null
        );
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
            .thenReturn(highAlert);

        // When
        conversationService.processUserMessage(memberId, highMessage);

        // Then: 알림 미발송
        verify(alertNotificationService, never()).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("일반 메시지는 키워드 감지 없음")
    void processUserMessage_NormalMessage_NoAlert() {
        // Given
        Long memberId = 1L;
        String normalMessage = "오늘 날씨가 좋네요";

        ConversationEntity conversation = mock(ConversationEntity.class);
        MessageEntity userMessage = mock(MessageEntity.class);
        MessageExchangeResult result = new MessageExchangeResult(userMessage, mock(MessageEntity.class));

        when(conversationManager.findOrCreateActive(memberId)).thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, normalMessage)).thenReturn(result);

        AlertResult noAlert = AlertResult.noAlert();
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
            .thenReturn(noAlert);

        // When
        conversationService.processUserMessage(memberId, normalMessage);

        // Then
        verify(alertNotificationService, never()).triggerAlert(anyLong(), any());
    }

    @Test
    @DisplayName("키워드 감지 실패 시 대화 흐름 유지")
    void processUserMessage_KeywordDetectionFails_ConversationContinues() {
        // Given
        Long memberId = 1L;
        String message = "테스트 메시지";

        ConversationEntity conversation = mock(ConversationEntity.class);
        MessageEntity userMessage = mock(MessageEntity.class);
        MessageExchangeResult result = new MessageExchangeResult(userMessage, mock(MessageEntity.class));

        when(conversationManager.findOrCreateActive(memberId)).thenReturn(conversation);
        when(messageProcessor.processMessage(conversation, message)).thenReturn(result);

        // 키워드 감지 실패
        when(alertDetectionService.detectKeywordAlert(userMessage, memberId))
            .thenThrow(new RuntimeException("Analyzer error"));

        // When & Then: 예외가 전파되지 않고 정상 응답
        assertDoesNotThrow(() -> conversationService.processUserMessage(memberId, message));
        verify(mapper, times(1)).toResponseDto(result);  // 응답 정상 생성됨
    }
}
```

---

## ⚠️ 6. 리스크 및 고려사항

### 6.1. 트랜잭션 분리 (데모용 단순 설계)

#### 설계 결정
```java
// AlertTriggerService.detectAnomaliesForAllMembers()
// ✅ @Transactional 없음 - 각 회원 처리마다 독립 트랜잭션

public void detectAnomaliesForAllMembers() {
    for (Long memberId : memberIds) {
        alertDetectionService.detectAnomalies(memberId);      // @Transactional
        alertNotificationService.triggerAlert(memberId, ...); // @Transactional
    }
}
```

**데모 목적 판단:**
- ✅ 회원 A 실패 → 회원 B 영향 없음 (트랜잭션 독립)
- ✅ DB 락 최소화
- ✅ 데모 규모 (회원 10~50명)에서 충분히 빠름
- ❌ 비동기 처리, 배치 최적화 등은 과한 엔지니어링

---

### 6.2. 중복 알림 방지 (⚠️ 수정 필요)

#### 문제
- 현재 UniqueConstraint: `{member_id, alert_rule_id, alert_date}`
- **치명적 문제**: MVP에서 `alert_rule_id`가 NULL이므로 중복 방지 실패
- PostgreSQL은 NULL을 다른 NULL과 다르게 취급 → 같은 날짜에 여러 알림 중복 저장됨

#### 해결 방안 (데모용 단순 수정)

**AlertHistory.java 수정:**
```java
@Table(name = "alert_history",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "alert_type", "alert_date"})
        // alert_rule_id 대신 alert_type 사용 (MVP용)
    },
    // ...
)
```

**추가 필드 필요:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private AlertType alertType; // 신규 필드 추가
```

**데이터 흐름 (중요!):**
```java
// AlertNotificationService.createAlertHistoryForMVP() 수정 필요
return AlertHistory.builder()
    .alertRule(null)
    .member(member)
    .alertLevel(alertResult.getAlertLevel())
    .alertType(alertResult.getAlertType())  // ⭐ 추가 필요!
    .alertMessage(alertResult.getMessage())
    .detectionDetails(detectionDetails)
    .alertDate(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0))
    .isNotificationSent(false)
    .build();
```

**변경 이유:**
- ✅ NULL 문제 해결: alertType은 항상 NOT NULL
- ✅ 중복 방지: 같은 날짜에 같은 타입의 알림은 1번만 발송
- ✅ 데이터 흐름 명확: AlertResult → AlertHistory로 alertType 전달
- ✅ 데모 단순화: 복잡한 AlertRule 생성 로직 불필요

---

### 6.3. Conversation 트랜잭션 경계 (데모용 단순화)

#### 해결 방안
```java
// try-catch로 예외 격리 (데모용 충분)
private void detectKeywordInRealtime(...) {
    try {
        // 키워드 감지 로직
    } catch (Exception e) {
        log.error("Keyword detection failed", e);
        // 예외는 여기서 끝! 대화 흐름은 계속됨
    }
}
```

**데모 목적 판단:**
- ✅ 키워드 감지 실패 시 대화 흐름 유지 (사용자 경험 최우선)
- ✅ 충분히 안전하고 단순한 구현
- ❌ 복잡한 REQUIRES_NEW 트랜잭션 불필요 (과한 엔지니어링)

---

### 6.4. 보호자 없는 회원

#### 문제
- 보호자가 없으면 알림 발송 불가

#### 해결 방안
```java
// AlertNotificationService에서 이미 처리됨 (Line 134)
if (!hasGuardian(member)) {
    log.warn("No guardian found for member {}, skip notification", memberId);
    return; // 알림 미발송
}
```

**현재 상태**: ✅ 이미 구현됨, 추가 작업 불필요

---

## ✅ 7. 완료 조건 (Definition of Done) - 데모용

### 필수 구현 (Phase 0: 사전 작업)
- [ ] AlertHistory에 alertType 필드 추가
- [ ] AlertHistory UniqueConstraint 수정 ({member_id, alert_type, alert_date})
- [ ] AlertNotificationService.createAlertHistoryForMVP() 수정 (alertType 설정 추가)
- [ ] alertRule 필드 nullable로 변경 (nullable = false → nullable = true)

### 필수 구현 (Phase 1)
- [ ] AlertTriggerService 구현 (alertrule/application/scheduler/)
- [ ] AlertScheduler 구현 (alertrule/application/scheduler/)
- [ ] application.yml 설정 추가

### 필수 구현 (Phase 2)
- [ ] SimpleConversationService 확장 (키워드 감지 try-catch 추가)

### 핵심 테스트만 (데모용 최소화)

#### 단위 테스트 (필수 3개만)
- [ ] AlertTriggerServiceTest
  - 전체 회원 감지 성공
  - 일부 회원 실패해도 나머지 처리
  - 위험 신호 없을 때 알림 미발송

- [ ] SimpleConversationServiceTest (1개 추가)
  - EMERGENCY 키워드 즉시 알림

#### 통합 테스트 (선택사항)
- [ ] (선택) 무응답 감지 → 알림 발송 E2E 테스트
- [ ] (선택) 감정 패턴 감지 → 알림 발송 E2E 테스트

**데모 목적 판단:**
- ✅ 핵심 기능만 테스트 (정상 w케이스 + 예외 격리)
- ❌ 성능 테스트, 복잡한 시나리오는 과한 엔지니어링
- ❌ 통합 테스트는 시간 있으면 추가 (선택사항)

### 문서화
- [ ] docs/domains/dailycheck.md 업데이트 (AlertRule 연동 명시)
- [ ] docs/domains/conversation.md 업데이트 (AlertRule 연동 명시)
- [ ] docs/domains/alertrule.md 업데이트 (자동 트리거 명시)

### 검증
- [ ] 로컬 환경에서 스케줄러 동작 확인
- [ ] 실제 키워드 입력 시 즉시 알림 확인
- [ ] 알림 이력 DB 저장 확인 (AlertHistory + NotificationHistory)
- [ ] 개별 회원 실패 시 나머지 회원 처리 확인

---

## 📅 8. 예상 일정 (데모용 단순화)

```
Phase 0: 사전 작업 (필수)
└─ 0.5일: AlertHistory 엔티티 수정 (alertType 필드 + UniqueConstraint)

Phase 1: DailyCheck 연동 (무응답 분석)
├─ Day 1: AlertTriggerService + AlertScheduler 구현
└─ Day 2: 핵심 테스트 3개 작성 + 로컬 검증

Phase 2: Conversation 연동 (키워드 감지)
├─ Day 3: SimpleConversationService 확장 (try-catch 추가)
└─ Day 4: 핵심 테스트 1개 작성 + 로컬 검증

총 소요 시간: 4.5일 (데모 목적 단순화)
```

**단축된 이유:**
- 테스트 개수 축소 (14개 → 4개)
- 통합 테스트 선택사항 처리
- 성능 테스트 제거

---

## 🔮 9. 향후 개선 사항 (데모 이후 고려)

**데모 목적 판단: 현재는 구현하지 않음**

### 9.1. 성능 최적화 (회원 100명 초과 시)
- Fetch Join으로 N+1 쿼리 제거
- 비동기 처리 (@Async)

### 9.2. 이벤트 기반 키워드 감지 (책임 분리)
- SimpleConversationService → ApplicationEventPublisher로 이벤트 발행
- AlertKeywordListener가 이벤트 구독하여 키워드 감지
- 장점: Conversation과 AlertRule 도메인 완전 분리

```java
// 이벤트 기반 리팩토링 예시
@Component
class AlertKeywordListener {
    @EventListener
    @Async
    void onMessageReceived(MessageReceivedEvent event) {
        // 키워드 감지 로직 (독립 트랜잭션)
    }
}
```

### 9.3. 알림 상태 관리
- AlertHistory에 RESOLVED 상태 추가
- 보호자가 "확인 완료" 표시

### 9.4. 모니터링 (운영 환경 필요 시)
- 실패율 모니터링
- 알림 발송 성공률 추적

---

## 🔗 10. 참고 문서

- [AlertRule 도메인 가이드](../domains/alertrule.md)
- [DailyCheck 도메인 가이드](../domains/dailycheck.md)
- [Conversation 도메인 가이드](../domains/conversation.md)
- [도메인 아키텍처 개요](../domains/README.md)
- [코딩 컨벤션](../specifications/coding-standards.md)
- [테스트 가이드](../specifications/testing-guide.md)

---

## 📝 11. 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2025-11-12 | 초안 작성 (TDD 방식) | - |
| 2025-11-12 | 검토 1차: 예외 격리 추가, 선 구현 후 테스트 방식 변경 | - |
| 2025-11-12 | 검토 2차: 트랜잭션 경계 수정, 통합 테스트 추가 | - |
| 2025-11-12 | **리뷰 반영: 데모 목적 단순화** | Claude |
|  | - UniqueConstraint 수정 (alert_rule_id → alert_type) | |
|  | - 패키지 위치 변경 (dailycheck → alertrule) | |
|  | - 테스트 개수 축소 (14개 → 4개) | |
|  | - 성능 최적화 내용 제거 | |
|  | - 복잡한 통합 테스트 선택사항 처리 | |
|  | - 예상 일정 단축 (6일 → 4.5일) | |
| 2025-11-14 | **실제 코드 검증 후 문서 정확성 개선** | Claude |
|  | - HIGH 키워드 처리 설명 수정 ("배치에서 처리" → "로그만 기록") | |
|  | - AlertDetectionService.java:139 확인 결과 KEYWORD_DETECTION은 배치 제외됨 | |
|  | - Phase 3에서 HIGH 키워드 누적 분석 추가 예정으로 명시 | |

---

**✅ 데모용 계획 확정 - 구현 시작 가능**
