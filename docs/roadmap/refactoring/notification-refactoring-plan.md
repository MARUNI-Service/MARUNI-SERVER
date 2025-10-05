# Notification 도메인 리팩토링 계획 (2025-09-24)

## 🎯 **리팩토링 목표**

**현재 상태**: Mock 알림 서비스 (개발용)
**목표 상태**: 실제 운영 가능한 푸시 알림 시스템

### 핵심 전환 목표
- Mock 알림 → 실제 Firebase FCM 푸시 알림
- 인메모리 이력 → 데이터베이스 영속화
- 단순 발송 → 재시도 + Fallback 안정성 확보
- 고정 메시지 → 템플릿 기반 개인화

## 📊 **현재 구조 분석**

### 현재 패키지 구조
```
notification/
├── domain/service/
│   ├── NotificationService.java           # 인터페이스 (3개 메서드)
│   └── NotificationChannelType.java       # Enum (PUSH/EMAIL/SMS/IN_APP)
└── infrastructure/
    ├── MockPushNotificationService.java   # Mock 구현체 (@Profile("dev"))
    └── MockNotificationRecord.java        # 테스트용 VO
```

### 장점
- ✅ **완성된 DDD 구조**: Domain/Infrastructure 계층 분리
- ✅ **인터페이스 기반 설계**: 확장 가능한 아키텍처
- ✅ **Profile 분리**: 개발/운영 환경 구분
- ✅ **테스트 지원**: Mock 구현체로 완벽한 테스트 환경
- ✅ **도메인 연동**: DailyCheckOrchestrator, AlertRuleService에서 사용 중

### 현재 제약사항
- ❌ **Mock만 존재**: 실제 푸시 서비스 없음
- ❌ **인메모리 저장**: 재시작 시 이력 소실
- ❌ **에러 처리 부족**: 실패 시 재시도 로직 없음
- ❌ **모니터링 부재**: 발송률/실패율 추적 불가

## 🏗️ **리팩토링 아키텍처 설계**

### 최종 목표 아키텍처
```
📱 MARUNI Notification Architecture (리팩토링 완료 후)

┌─────────────────── Application Layer ───────────────────┐
│  DailyCheckOrchestrator   AlertNotificationService      │
│  (기존 코드 무수정)        (기존 코드 무수정)              │
└─────────────────────┬─────────────────────────────────────┘
                      │ NotificationService (기존 인터페이스)
┌─────────────────── Domain Layer ────────────────────────┐
│  📋 NotificationService              🏷️  Templates       │
│   ├─ sendPushNotification()          ├─ DAILY_CHECK    │
│   ├─ isAvailable()                   ├─ ALERT_HIGH     │
│   └─ getChannelType()                └─ ALERT_MEDIUM   │
│                                                         │
│  📊 ExtendedNotificationService (확장)                  │
│   ├─ sendNotificationWithDetails()                     │
│   ├─ getHistory()                                       │
│   └─ retryFailedNotification()                         │
└─────────────────────┬─────────────────────────────────────┘
                      │
┌─────────────────── Infrastructure Layer ─────────────────┐
│                                                         │
│  🎭 NotificationHistoryDecorator (@Primary)            │
│   └─ 모든 알림에 이력 저장 기능 추가                     │
│                     ⬇️                                  │
│  🔄 FallbackNotificationService                        │
│   └─ Firebase ➜ Mock ➜ 실패 처리                       │
│                     ⬇️                                  │
│  ┌─────────────────────────────────────────────────┐   │
│  │  🚀 Firebase          📱 Mock         📧 Email  │   │
│  │  @Profile("prod")     @Profile("dev")  (Phase3) │   │
│  │  ├─ FCM API 연동      ├─ 로그만 출력   ├─ SMTP   │   │
│  │  ├─ Circuit Breaker  ├─ 인메모리 이력  └─ 템플릿  │   │
│  │  └─ Retry 로직       └─ 테스트 지원             │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  📊 Data Layer                                         │
│  ├─ NotificationHistory (발송 이력 영속화)              │
│  ├─ NotificationTemplate (메시지 템플릿)                │
│  └─ NotificationPreference (개인별 설정)                │
└─────────────────────────────────────────────────────────┘
```

### 단계별 구조 전환
```
🔄 Phase 1: Firebase 기본 연동 (3주)
notification/
├── domain/service/
│   ├── NotificationService.java           # 기존 인터페이스 (수정없음)
│   ├── ExtendedNotificationService.java   # 확장 인터페이스 (신규)
│   └── NotificationChannelType.java       # 기존 Enum (수정없음)
├── domain/entity/                         # 신규 Domain Entity
│   ├── NotificationHistory.java          # 발송 이력
│   └── NotificationTemplate.java         # 메시지 템플릿
├── domain/repository/                     # 신규 Repository 인터페이스
│   ├── NotificationHistoryRepository.java
│   └── NotificationTemplateRepository.java
└── infrastructure/
    ├── MockPushNotificationService.java  # 기존 Mock (유지)
    ├── FirebasePushNotificationService.java # 신규 Firebase 구현체
    ├── NotificationHistoryDecorator.java # 이력 저장 데코레이터
    └── FallbackNotificationService.java  # Fallback 처리

🚀 Phase 2: 안정성 강화 (2주)
└── infrastructure/advanced/
    ├── CircuitBreakerNotificationService.java # 장애 대응
    ├── RetryableNotificationService.java      # 재시도 로직
    └── NotificationMetricsCollector.java      # 모니터링
```

## 🔄 **단계별 구현 계획**

### **Phase 1: Firebase 기본 연동 (3주)**

#### **Week 1: Firebase 설정 및 기본 구현**

**1-1. Firebase 환경 설정**
```bash
# 1. Firebase 프로젝트 생성
# 2. FCM 설정 활성화
# 3. 서비스 계정 키 발급
# 4. config/firebase-service-account-key.json 저장
```

**1-2. 의존성 추가**
```gradle
// build.gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

**1-3. FirebasePushNotificationService 구현**
```java
@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class FirebasePushNotificationService implements NotificationService {

    private final FirebaseApp firebaseApp;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        try {
            // FCM 메시지 빌드
            Message firebaseMessage = Message.builder()
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(message)
                    .build())
                .setToken(getPushTokenByMemberId(memberId))
                .build();

            // 발송
            String response = FirebaseMessaging.getInstance(firebaseApp)
                .send(firebaseMessage);

            log.info("🚀 [FIREBASE] Push notification sent - memberId: {}, messageId: {}",
                memberId, response);

            return true;
        } catch (Exception e) {
            log.error("❌ [FIREBASE] Push notification failed - memberId: {}, error: {}",
                memberId, e.getMessage());
            throw new NotificationException(NotificationErrorCode.FIREBASE_SEND_FAILED, e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Firebase 연결 상태 확인
            FirebaseMessaging.getInstance(firebaseApp);
            return true;
        } catch (Exception e) {
            log.warn("Firebase service unavailable: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.PUSH;
    }

    private String getPushTokenByMemberId(Long memberId) {
        // Member 엔티티에서 푸시 토큰 조회 로직 구현 필요
        return "mock_token_" + memberId; // 임시 구현
    }
}
```

#### **Week 2: 이력 저장 시스템 구축**

**2-1. NotificationHistory Entity**
```java
@Entity
@Table(name = "notification_history")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channelType;

    @Column(nullable = false)
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private String externalMessageId; // Firebase messageId 등

    // 정적 팩토리 메서드 (기존 MARUNI 패턴)
    public static NotificationHistory create(Long memberId, String title,
            String message, NotificationChannelType channelType, boolean success) {
        return NotificationHistory.builder()
            .memberId(memberId)
            .title(title)
            .message(message)
            .channelType(channelType)
            .success(success)
            .build();
    }

    public static NotificationHistory createFailure(Long memberId, String title,
            String message, NotificationChannelType channelType, String errorMessage) {
        return NotificationHistory.builder()
            .memberId(memberId)
            .title(title)
            .message(message)
            .channelType(channelType)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
```

**2-2. Repository 인터페이스**
```java
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    List<NotificationHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<NotificationHistory> findByMemberIdAndSuccessOrderByCreatedAtDesc(Long memberId, Boolean success);

    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.success = true AND h.createdAt >= :from")
    long countSuccessNotifications(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.createdAt >= :from")
    long countTotalNotifications(@Param("from") LocalDateTime from);

    void deleteByCreatedAtBefore(LocalDateTime before);
}
```

#### **Week 3: Decorator 패턴 적용**

**3-1. NotificationHistoryDecorator**
```java
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryDecorator implements NotificationService {

    private final NotificationService delegate;
    private final NotificationHistoryRepository historyRepository;

    @Override
    @Transactional
    public boolean sendPushNotification(Long memberId, String title, String message) {
        boolean success = false;
        String errorMessage = null;
        String externalMessageId = null;

        try {
            success = delegate.sendPushNotification(memberId, title, message);

            // Firebase인 경우 messageId 추출 (향후 확장)
            if (delegate instanceof FirebasePushNotificationService) {
                // externalMessageId = extractMessageId();
            }

        } catch (NotificationException e) {
            errorMessage = e.getMessage();
            log.error("Notification failed for member {}: {}", memberId, e.getMessage());
        } catch (Exception e) {
            errorMessage = "Unexpected error: " + e.getMessage();
            log.error("Unexpected notification error for member {}: {}", memberId, e.getMessage());
        }

        // 이력 저장 (성공/실패 모두)
        NotificationHistory history = success
            ? NotificationHistory.create(memberId, title, message, delegate.getChannelType(), true)
            : NotificationHistory.createFailure(memberId, title, message, delegate.getChannelType(), errorMessage);

        if (externalMessageId != null) {
            history.setExternalMessageId(externalMessageId);
        }

        historyRepository.save(history);

        return success;
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public NotificationChannelType getChannelType() {
        return delegate.getChannelType();
    }
}
```

### **Phase 2: 안정성 강화 (2주)**

#### **Week 4: Fallback 시스템**

**4-1. FallbackNotificationService**
```java
@Service
@ConditionalOnProperty(name = "features.notification.fallback.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FallbackNotificationService implements NotificationService {

    @Qualifier("firebasePushNotificationService")
    private final NotificationService primaryService;

    @Qualifier("mockPushNotificationService")
    private final NotificationService fallbackService;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        // 1차: Primary 서비스 시도
        if (primaryService.isAvailable()) {
            try {
                boolean result = primaryService.sendPushNotification(memberId, title, message);
                if (result) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Primary notification service failed, falling back: {}", e.getMessage());
            }
        }

        // 2차: Fallback 서비스 시도
        log.info("Using fallback notification service for member {}", memberId);
        return fallbackService.sendPushNotification(memberId, title, message);
    }

    @Override
    public boolean isAvailable() {
        return primaryService.isAvailable() || fallbackService.isAvailable();
    }

    @Override
    public NotificationChannelType getChannelType() {
        return primaryService.getChannelType();
    }
}
```

#### **Week 5: 재시도 로직**

**5-1. RetryableNotificationService**
```java
@Service
@RequiredArgsConstructor
public class RetryableNotificationService {

    private final NotificationService notificationService;

    @Retryable(
        value = {NotificationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean sendPushNotificationWithRetry(Long memberId, String title, String message) {
        log.info("Attempting to send notification to member {} (attempt: {})",
            memberId, getCurrentAttempt());

        boolean result = notificationService.sendPushNotification(memberId, title, message);

        if (!result) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SEND_FAILED);
        }

        return result;
    }

    @Recover
    public boolean recoverFromNotificationFailure(NotificationException ex, Long memberId,
            String title, String message) {
        log.error("All retry attempts failed for member {}: {}", memberId, ex.getMessage());

        // 최종 실패 처리 (관리자 알림, 메트릭 기록 등)
        recordFinalFailure(memberId, title, message, ex);

        return false;
    }

    private int getCurrentAttempt() {
        // 현재 재시도 횟수 추적 로직
        return 1;
    }

    private void recordFinalFailure(Long memberId, String title, String message, Exception ex) {
        // 최종 실패 메트릭 기록
        log.error("CRITICAL: Notification completely failed for member {}", memberId);
    }
}
```

### **Phase 3: 템플릿 시스템 (2주)**

#### **Week 6-7: 템플릿 기반 메시지 관리**

**6-1. NotificationTemplate Entity**
```java
@Entity
@Table(name = "notification_template")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate extends BaseTimeEntity {

    @Id
    private String templateId; // "DAILY_CHECK", "ALERT_HIGH", "ALERT_MEDIUM"

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageTemplate; // "안녕하세요 {memberName}님! 오늘 하루는 어떻게 지내고 계신가요?"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channelType;

    @Column(nullable = false)
    private Boolean active; // 활성화 여부

    // 정적 팩토리 메서드
    public static NotificationTemplate create(String templateId, String title,
            String messageTemplate, NotificationChannelType channelType) {
        return NotificationTemplate.builder()
            .templateId(templateId)
            .title(title)
            .messageTemplate(messageTemplate)
            .channelType(channelType)
            .active(true)
            .build();
    }

    // 템플릿 변수 치환
    public String formatMessage(Map<String, String> variables) {
        String formatted = messageTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return formatted;
    }
}
```

**6-2. Template Service**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationService notificationService;

    @Transactional
    public boolean sendTemplatedNotification(Long memberId, String templateId,
            Map<String, String> variables) {

        NotificationTemplate template = templateRepository.findByTemplateIdAndActive(templateId, true)
            .orElseThrow(() -> new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND));

        String formattedMessage = template.formatMessage(variables);

        return notificationService.sendPushNotification(memberId, template.getTitle(), formattedMessage);
    }

    public List<NotificationTemplate> getActiveTemplates() {
        return templateRepository.findByActiveOrderByTemplateId(true);
    }
}
```

## 🔧 **MARUNI 기존 패턴 준수**

### 예외 처리 구조
```java
// 기존 BaseException 계층 활용
public class NotificationException extends BaseException {
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

public enum NotificationErrorCode implements ErrorCode {
    FIREBASE_CONNECTION_FAILED(500, "N001", "Firebase 연결 실패"),
    PUSH_TOKEN_INVALID(400, "N002", "유효하지 않은 푸시 토큰"),
    NOTIFICATION_SEND_FAILED(500, "N003", "알림 발송 실패"),
    TEMPLATE_NOT_FOUND(404, "N004", "알림 템플릿을 찾을 수 없음"),
    FIREBASE_SEND_FAILED(500, "N005", "Firebase 메시지 발송 실패");

    private final int status;
    private final String code;
    private final String message;

    NotificationErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public int getStatus() { return status; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
```

### Service 계층 패턴
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationHistoryService {

    private final NotificationHistoryRepository historyRepository;

    public List<NotificationHistory> getHistoryByMemberId(Long memberId) {
        return historyRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    public List<NotificationHistory> getFailedNotifications(Long memberId) {
        return historyRepository.findByMemberIdAndSuccessOrderByCreatedAtDesc(memberId, false);
    }

    public double getSuccessRate(LocalDateTime from) {
        long total = historyRepository.countTotalNotifications(from);
        if (total == 0) return 1.0;

        long success = historyRepository.countSuccessNotifications(from);
        return (double) success / total;
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void cleanOldHistory() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        historyRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
        log.info("Cleaned notification history older than {}", thirtyDaysAgo);
    }
}
```

## ⚙️ **설정 및 Feature Flag**

### application.yml 설정
```yaml
# 개발 환경 (application-dev.yml)
features:
  notification:
    firebase:
      enabled: false  # Firebase 비활성화
      service-account-key-path: config/firebase-dev-service-account.json
    history:
      enabled: true   # 이력 저장 활성화
    fallback:
      enabled: true   # Fallback 시스템 활성화
    template:
      enabled: true   # 템플릿 시스템 활성화

spring:
  profiles:
    active: dev

# 운영 환경 (application-prod.yml)
features:
  notification:
    firebase:
      enabled: true   # Firebase 활성화
      service-account-key-path: config/firebase-prod-service-account.json
      project-id: maruni-prod
    history:
      enabled: true
    fallback:
      enabled: true
    template:
      enabled: true

spring:
  profiles:
    active: prod

# Firebase 설정
firebase:
  push:
    timeout: 5000
    retry:
      max-attempts: 3
      backoff-multiplier: 2
```

### 환경변수 (.env 파일 추가)
```bash
# Firebase 설정
FIREBASE_PROJECT_ID=maruni-prod
FIREBASE_PRIVATE_KEY_ID=your_private_key_id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxxx@maruni-prod.iam.gserviceaccount.com
FIREBASE_CLIENT_ID=your_client_id

# Feature Flags
NOTIFICATION_FIREBASE_ENABLED=true
NOTIFICATION_HISTORY_ENABLED=true
NOTIFICATION_FALLBACK_ENABLED=true
```

## 🧪 **테스트 전략**

### Unit Test
```java
@ExtendWith(MockitoExtension.class)
class FirebasePushNotificationServiceTest {

    @Mock private FirebaseApp firebaseApp;
    @Mock private FirebaseMessaging firebaseMessaging;

    @InjectMocks private FirebasePushNotificationService service;

    @Test
    void shouldSendPushNotificationSuccessfully() {
        // Given
        Long memberId = 1L;
        String title = "테스트 제목";
        String message = "테스트 메시지";

        given(firebaseMessaging.send(any(Message.class)))
            .willReturn("mock-message-id");

        // When
        boolean result = service.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void shouldThrowExceptionWhenFirebaseFails() {
        // Given
        Long memberId = 1L;
        given(firebaseMessaging.send(any(Message.class)))
            .willThrow(new FirebaseMessagingException(ErrorCode.INVALID_ARGUMENT, "Invalid token"));

        // When & Then
        assertThatThrownBy(() -> service.sendPushNotification(memberId, "title", "message"))
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining("Firebase 메시지 발송 실패");
    }
}
```

### Integration Test
```java
@SpringBootTest
@TestPropertySource(properties = {
    "features.notification.firebase.enabled=false",
    "features.notification.history.enabled=true",
    "features.notification.fallback.enabled=true"
})
class NotificationServiceIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationHistoryRepository historyRepository;

    @Test
    @Transactional
    void shouldSaveHistoryWhenNotificationSent() {
        // Given
        Long memberId = 1L;
        String title = "통합테스트";
        String message = "통합테스트 메시지";

        // When
        boolean result = notificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();

        List<NotificationHistory> histories =
            historyRepository.findByMemberId(memberId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getTitle()).isEqualTo(title);
        assertThat(histories.get(0).getMessage()).isEqualTo(message);
        assertThat(histories.get(0).getSuccess()).isTrue();
    }

    @Test
    void shouldFallbackToMockWhenFirebaseUnavailable() {
        // Firebase가 비활성화된 상태에서 Mock이 작동하는지 확인
        // Given & When & Then
        boolean result = notificationService.sendPushNotification(1L, "fallback", "test");
        assertThat(result).isTrue();
    }
}
```

## 📊 **모니터링 및 운영**

### 메트릭 수집
```java
@Component
@RequiredArgsConstructor
public class NotificationMetricsCollector {

    private final MeterRegistry meterRegistry;

    @EventListener
    public void handleNotificationSent(NotificationSentEvent event) {
        Counter.builder("notification.sent")
            .tag("channel", event.getChannelType().name())
            .tag("success", String.valueOf(event.isSuccess()))
            .tag("service", event.getServiceType()) // "firebase", "mock"
            .register(meterRegistry)
            .increment();
    }

    @EventListener
    public void handleNotificationFailed(NotificationFailedEvent event) {
        Counter.builder("notification.failed")
            .tag("channel", event.getChannelType().name())
            .tag("error_code", event.getErrorCode())
            .tag("retry_count", String.valueOf(event.getRetryCount()))
            .register(meterRegistry)
            .increment();
    }

    @Scheduled(fixedRate = 60000) // 매 1분마다
    public void collectSuccessRateMetrics() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        double successRate = getSuccessRate(oneHourAgo);

        Gauge.builder("notification.success_rate")
            .tag("period", "1hour")
            .register(meterRegistry, successRate);
    }

    private double getSuccessRate(LocalDateTime from) {
        // NotificationHistoryService에서 성공률 계산
        return 0.0;
    }
}
```

### Health Check
```java
@Component
public class NotificationHealthIndicator implements HealthIndicator {

    private final NotificationService notificationService;

    @Override
    public Health health() {
        if (notificationService.isAvailable()) {
            return Health.up()
                .withDetail("service", notificationService.getClass().getSimpleName())
                .withDetail("channel", notificationService.getChannelType())
                .build();
        } else {
            return Health.down()
                .withDetail("service", notificationService.getClass().getSimpleName())
                .withDetail("reason", "Service unavailable")
                .build();
        }
    }
}
```

## 🚀 **예상 성과**

### 기술적 개선
- ✅ **실제 푸시 알림**: Firebase FCM을 통한 진짜 알림 서비스
- ✅ **완전한 이력 추적**: 모든 발송 성공/실패 데이터베이스 저장
- ✅ **높은 가용성**: Fallback + 재시도로 99% 이상 발송 성공률
- ✅ **운영 안정성**: Circuit Breaker로 장애 전파 방지
- ✅ **확장 가능성**: 템플릿 시스템으로 메시지 개인화 기반 마련

### 비즈니스 가치
- 📱 **실제 사용자 도달**: Mock에서 실제 푸시 알림으로 전환
- 📊 **데이터 기반 개선**: 발송 이력 분석으로 서비스 최적화
- 🎯 **개인화된 알림**: 템플릿 기반 상황별 메시지 제공
- 🚨 **신뢰성 확보**: 긴급상황 알림 누락 방지
- 💰 **운영 비용 절약**: Mock → Firebase 단계적 전환으로 초기 비용 최소화

### 성과 지표 (리팩토링 완료 후)
- **발송 성공률**: 95% 이상 (Fallback 시스템)
- **응답 시간**: Firebase 3초 이내, Mock 즉시
- **재시도 성공률**: 실패 알림의 80% 이상 재발송 성공
- **이력 보관**: 30일간 완전한 발송 이력 추적
- **장애 대응**: Circuit Breaker로 외부 장애 격리

## 💡 **핵심 설계 원칙**

1. **Zero Impact**: 기존 15개 파일의 코드 수정 없음
2. **점진적 개선**: Feature Flag 기반 안전한 단계적 배포
3. **DDD 구조 보존**: Domain ← Infrastructure 의존성 유지
4. **MARUNI 패턴 준수**: BaseTimeEntity, 정적 팩토리, BaseException 활용
5. **운영 안정성**: Fallback + 재시도 + Circuit Breaker
6. **확장 가능성**: 인터페이스 기반으로 향후 채널 확장 용이

## 📅 **실행 타임라인**

```
🗓️ 7주간 단계별 실행 계획

Week 1: Firebase 환경 설정 및 기본 구현
├── Firebase 프로젝트 생성
├── FCM 설정 및 서비스 키 발급
├── FirebasePushNotificationService 기본 구현
└── 개발환경 테스트

Week 2: 데이터베이스 이력 시스템
├── NotificationHistory Entity 생성
├── Repository 및 Service 구현
├── 마이그레이션 스크립트 작성
└── 기본 CRUD 테스트

Week 3: Decorator 패턴 적용
├── NotificationHistoryDecorator 구현
├── @Primary 설정으로 기존 코드와 연동
├── 통합 테스트 작성
└── 이력 저장 검증

Week 4: Fallback 시스템
├── FallbackNotificationService 구현
├── Firebase ↔ Mock 전환 로직
├── Feature Flag 기반 제어
└── 장애 상황 테스트

Week 5: 재시도 및 안정성
├── @Retryable 기반 재시도 구현
├── Circuit Breaker 패턴 적용
├── 에러 처리 개선
└── 복구 로직 테스트

Week 6-7: 템플릿 시스템 및 마무리
├── NotificationTemplate Entity 구현
├── 템플릿 기반 발송 서비스
├── 기본 템플릿 데이터 생성
├── 전체 시스템 통합 테스트
└── 운영 환경 배포 준비

📊 마일스톤:
✅ Week 3 완료: 실제 푸시 알림 + 이력 저장 (MVP 달성)
✅ Week 5 완료: 안정성 확보 (운영 준비)
✅ Week 7 완료: 전체 시스템 완성 (확장 기반 마련)
```

---

**완성 후 MARUNI의 노인 돌봄 서비스가 Mock 알림에서 실제 사용자에게 도달하는 진짜 알림 서비스로 업그레이드됩니다.** 🎯

이 리팩토링을 통해 MARUNI는 **실제 운영 가능한 완전한 알림 시스템**을 갖추게 되어, 진정한 노인 돌봄 서비스로서 사용자와 보호자에게 신뢰할 수 있는 알림을 제공할 수 있습니다.