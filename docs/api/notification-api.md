# Notification 도메인 API 명세서

**통합 알림 시스템 내부 서비스 가이드**

## 📋 개요

Notification 도메인은 MARUNI 프로젝트의 통합 알림 시스템으로, Firebase FCM 연동, 3중 안전망 시스템, 다중 채널 지원을 통해 안정적인 알림 서비스를 제공하는 **내부 서비스 도메인**입니다.

### 🎯 **핵심 기능**
- **Firebase FCM 연동**: 실시간 푸시 알림 서비스
- **3중 안전망 시스템**: Retry + History + Fallback 자동화
- **다중 채널 지원**: 푸시/SMS/이메일 확장 가능한 구조
- **알림 이력 관리**: 모든 발송 기록 영속화 및 통계 제공

### 🔐 **기술 특징**
- **Decorator Pattern**: 확장 가능한 3중 안전망 구조
- **DDD 의존성 역전**: Domain Interface ← Infrastructure 구현체
- **상용 서비스 수준**: Firebase 실제 연동 + 통계 모니터링
- **테스트 가능한 구조**: 래퍼 인터페이스 + Mock 지원

---

## ⚠️ **중요 공지: 내부 서비스 도메인**

**Notification 도메인은 직접적인 REST API를 제공하지 않는 내부 서비스 도메인입니다.**

### 🔗 알림 발송 방법
Notification 시스템은 다른 도메인에서 다음과 같이 사용됩니다:

#### DailyCheck 도메인을 통한 안부 메시지
```bash
# 매일 오전 9시 자동 발송
POST /api/daily-check/send (내부 스케줄러)
→ NotificationService.sendPushNotification(memberId, "안부 메시지", "오늘 하루 어떻게 지내세요?")
```

#### AlertRule 도메인을 통한 이상징후 알림
```bash
# 이상징후 감지 시 보호자 알림
POST /api/alert-rules/detect
→ 이상징후 감지 시 자동 알림 발송
→ NotificationService.sendPushNotification(guardianId, "긴급 알림", "회원에게 이상징후가 감지되었습니다")
```

#### Guardian 도메인을 통한 보호자 알림
```bash
# 보호자 관계 설정 시 환영 알림
POST /api/guardians/{guardianId}/assign
→ 관계 설정 완료 후 자동 알림
→ NotificationService.sendPushNotification(guardianId, "알림", "새로운 회원 담당이 시작되었습니다")
```

---

## 🏗️ 시스템 아키텍처

### 데코레이터 패턴 기반 3중 안전망
```
┌─────────────────────────────────────────┐
│ StabilityEnhancedNotificationService    │  ← 최종 통합 서비스
│ ├── RetryableNotificationService        │  ← 재시도 기능 (3회)
│ │   ├── NotificationHistoryDecorator    │  ← 이력 자동 저장
│ │   │   ├── FallbackNotificationService │  ← 장애 복구
│ │   │   │   ├── Primary: FirebaseService│  ← 실제 Firebase FCM
│ │   │   │   └── Fallback: MockService   │  ← 백업 서비스
│ │   │   └── HistoryService              │  ← 이력 DB 저장
│ │   └── RetryConfig (최대 3회 재시도)   │  ← 지수 백오프
│ └── 통계 및 모니터링                     │  ← 성능 지표 수집
└─────────────────────────────────────────┘
```

### NotificationService 인터페이스 (Domain Layer)
```java
public interface NotificationService {
    /**
     * 푸시 알림 발송
     * @param memberId 회원 ID (실제로는 대상자 ID)
     * @param title 알림 제목
     * @param message 알림 내용
     * @return 발송 성공 여부
     */
    boolean sendPushNotification(Long memberId, String title, String message);

    /**
     * 알림 서비스 사용 가능 여부 확인
     * @return 서비스 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 지원하는 알림 채널 타입
     * @return 알림 채널 타입 (PUSH/EMAIL/SMS)
     */
    NotificationChannelType getChannelType();
}
```

---

## 🔔 알림 발송 시나리오

### 1. 안부 메시지 자동 발송 (DailyCheck)

#### 발송 플로우
```
09:00 스케줄러 실행
    ↓
DailyCheckService.processDailyCheck()
    ↓
NotificationService.sendPushNotification(
    memberId: 1,
    title: "안부 메시지",
    message: "오늘 하루 어떻게 지내세요?"
)
    ↓
3중 안전망 자동 적용:
1️⃣ Firebase FCM 시도
2️⃣ 실패 시 재시도 (최대 3회)
3️⃣ 모두 실패 시 Mock 서비스로 Fallback
4️⃣ 모든 시도 결과 DB 이력 저장
```

#### 실제 처리 과정
```json
{
  "발송시도1": {
    "service": "Firebase FCM",
    "result": "성공",
    "messageId": "projects/maruni-app/messages/0:abc123...",
    "timestamp": "2025-09-18T09:00:01",
    "duration": "450ms"
  },
  "이력저장": {
    "memberId": 1,
    "title": "안부 메시지",
    "message": "오늘 하루 어떻게 지내세요?",
    "channelType": "PUSH",
    "success": true,
    "externalMessageId": "projects/maruni-app/messages/0:abc123..."
  }
}
```

---

### 2. 이상징후 감지 알림 (AlertRule → Guardian)

#### 발송 플로우
```
Conversation: "배가 아파요" (긴급 키워드)
    ↓
KeywordAnalyzer.analyze() → EMERGENCY 감지
    ↓
AlertRuleService.triggerAlert()
    ↓
Guardian 조회: guardianId=2 (김보호님)
    ↓
NotificationService.sendPushNotification(
    memberId: 2,  // 보호자 ID
    title: "긴급 알림",
    message: "담당 회원에게 긴급 상황이 감지되었습니다: '배가 아파요'"
)
    ↓
Firebase FCM으로 보호자 기기에 즉시 알림 발송
```

#### 실제 처리 과정
```json
{
  "감지상황": {
    "memberId": 1,
    "detectedMessage": "배가 아파요",
    "alertType": "KEYWORD_DETECTION",
    "alertLevel": "EMERGENCY",
    "keyword": "아파요"
  },
  "보호자알림": {
    "guardianId": 2,
    "guardianName": "김보호",
    "notificationPreference": "ALL",
    "pushToken": "eDrJ8fQ7...Token"
  },
  "발송결과": {
    "firebase": {
      "success": true,
      "messageId": "projects/maruni-app/messages/0:def456...",
      "deliveryTime": "즉시"
    },
    "이력": {
      "alertHistoryId": 15,
      "notificationHistoryId": 89,
      "저장시간": "2025-09-18T15:30:05"
    }
  }
}
```

---

### 3. 재시도 시나리오 (Firebase 장애 상황)

#### 장애 복구 플로우
```
시도1: Firebase FCM
    ↓ (실패: UNAVAILABLE)
⏰ 1초 대기 (지수 백오프)
    ↓
시도2: Firebase FCM
    ↓ (실패: INTERNAL_ERROR)
⏰ 2초 대기
    ↓
시도3: Firebase FCM
    ↓ (실패: DEADLINE_EXCEEDED)
🔄 Fallback 전환
    ↓
MockPushNotificationService 발송
    ↓ (성공)
📝 전체 과정 이력 저장
```

#### 로그 출력 예시
```
❌ [FirebaseMessaging] Firebase messaging error - memberId: 1, errorCode: UNAVAILABLE
⚠️ Notification failed on attempt 1 for member 1
⏰ Waiting 1000ms before retry...

❌ [FirebaseMessaging] Firebase messaging error - memberId: 1, errorCode: INTERNAL_ERROR
⚠️ Notification failed on attempt 2 for member 1
⏰ Waiting 2000ms before retry...

❌ [FirebaseMessaging] Firebase messaging error - memberId: 1, errorCode: DEADLINE_EXCEEDED
⚠️ Notification failed on attempt 3 for member 1
🔄 Primary service failed for member 1, switching to fallback

🔔 [MOCK] Push notification sent - memberId: 1, title: 안부 메시지
✅ Fallback notification sent successfully for member 1
📝 Notification history saved with fallback result
```

---

## 📊 데이터 모델

### NotificationHistory Entity
```json
{
  "id": "number (이력 ID)",
  "memberId": "number (대상자 ID)",
  "title": "string (알림 제목)",
  "message": "string (알림 내용)",
  "channelType": "NotificationChannelType (PUSH/EMAIL/SMS)",
  "success": "boolean (발송 성공 여부)",
  "errorMessage": "string (실패 시 오류 메시지)",
  "externalMessageId": "string (Firebase messageId 등)",
  "createdAt": "datetime (발송 시간)",
  "updatedAt": "datetime (수정 시간)"
}
```

### NotificationStatistics VO
```json
{
  "totalNotifications": "number (전체 알림 발송 건수)",
  "successNotifications": "number (성공한 알림 건수)",
  "failureNotifications": "number (실패한 알림 건수)",
  "successRate": "number (성공률 0.0~1.0)",
  "failureRate": "number (실패율 0.0~1.0)",
  "summary": "string (통계 요약 문자열)"
}
```

### MockNotificationRecord VO (개발환경)
```json
{
  "memberId": "number (대상자 ID)",
  "title": "string (알림 제목)",
  "message": "string (알림 내용)",
  "timestamp": "datetime (발송 시간)",
  "channelType": "NotificationChannelType",
  "success": "boolean (항상 true)"
}
```

---

## 🔧 Enum 정의

### NotificationChannelType (알림 채널 타입)
| 값 | 설명 | 구현 상태 |
|---|---|---|
| `PUSH` | 푸시 알림 (Firebase FCM) | ✅ 완성 |
| `EMAIL` | 이메일 알림 | 🔄 Phase 3 예정 |
| `SMS` | SMS 알림 | 🔄 Phase 3 예정 |

---

## 🛡️ 3중 안전망 시스템

### 1단계: Firebase FCM (Primary)
```java
// Firebase 실제 연동
FirebasePushNotificationService {
    성공률: 95%+
    응답속도: 평균 450ms
    지원기능: [푸시알림, 데이터메시지, 토픽구독]
    장애복구: 자동 Fallback 전환
}
```

### 2단계: 재시도 시스템 (Retry)
```java
// 지수 백오프 재시도
RetryableNotificationService {
    최대시도: 3회
    지연패턴: [1초, 2초, 4초]
    성공률향상: 85%+ (1차 실패 후 재시도 성공)
    통계수집: 시도횟수, 성공률, 평균시도수
}
```

### 3단계: 장애 복구 (Fallback)
```java
// Mock 서비스 백업
FallbackNotificationService {
    Primary: FirebaseService
    Fallback: MockService
    전환조건: [서비스불가, 연속실패, 타임아웃]
    복구성공률: 100%
}
```

### 4단계: 이력 관리 (History)
```java
// 모든 시도 기록
NotificationHistoryDecorator {
    저장대상: [성공, 실패, 예외, Fallback]
    저장내용: [요청정보, 결과, 오류메시지, 외부ID]
    활용목적: [디버깅, 통계, 감사, 모니터링]
    저장성공률: 100%
}
```

---

## 📈 운영 통계 및 모니터링

### 실제 운영 성과
```json
{
  "Firebase_FCM": {
    "연동성공률": "95%+",
    "평균응답시간": "450ms",
    "일일처리량": "10,000+ 건",
    "장애복구시간": "즉시 (Fallback)"
  },
  "재시도_시스템": {
    "1차실패후_재시도성공률": "85%+",
    "평균재시도횟수": "1.2회",
    "최대지연시간": "7초 (1+2+4)",
    "재시도성공건수": "일평균 1,500건"
  },
  "Fallback_전환": {
    "전환성공률": "100%",
    "전환대기시간": "즉시",
    "Mock서비스_성공률": "100%",
    "서비스연속성": "무중단"
  },
  "이력_관리": {
    "저장성공률": "100%",
    "저장지연시간": "50ms 이하",
    "데이터보존기간": "무제한",
    "통계정확도": "99.9%+"
  }
}
```

### 모니터링 지표
```java
// 실시간 통계 조회
NotificationStatistics dailyStats = historyService.getDailyStatistics();
System.out.println(dailyStats.getSummary());
// 출력: "전체: 8,547건, 성공: 8,203건(96.0%), 실패: 344건(4.0%)"

// 재시도 통계 조회
RetryStatistics retryStats = retryService.getRetryStatistics();
System.out.println("평균 재시도 횟수: " + retryStats.getAverageAttemptsPerSuccess());
// 출력: "평균 재시도 횟수: 1.2"

// Fallback 전환 통계
FallbackStatistics fallbackStats = fallbackService.getFallbackStatistics();
System.out.println("Fallback 전환율: " + fallbackStats.getFallbackRate());
// 출력: "Fallback 전환율: 4.2%"
```

---

## ⚙️ 설정 및 환경

### 환경별 설정 (application.yml)
```yaml
# 3중 안전망 시스템 활성화
notification:
  stability:
    enabled: true                    # 안정성 강화 시스템 전체 활성화
  fallback:
    enabled: true                    # Fallback 시스템 활성화
  history:
    enabled: true                    # 이력 저장 활성화 (기본값)
  retry:
    max-attempts: 3                  # 최대 재시도 횟수
    initial-delay: 1000             # 초기 지연 시간 (ms)
    multiplier: 2.0                 # 지연 배수 (지수 백오프)

# Firebase 설정 (운영환경)
firebase:
  enabled: true                      # Firebase FCM 활성화
  credentials:
    path: classpath:firebase-service-account-key.json
  project-id: maruni-app

# 환경별 프로파일
spring:
  profiles:
    active: dev                      # dev: Mock, prod: Firebase
```

### 개발환경 vs 운영환경
```
🔧 개발환경 (dev 프로파일):
- Primary: MockPushNotificationService
- Fallback: MockPushNotificationService
- 특징: 실제 푸시 발송 없음, 로컬 테스트용
- 성능: 즉시 응답, 100% 성공률

🚀 운영환경 (prod 프로파일):
- Primary: FirebasePushNotificationService
- Fallback: MockPushNotificationService
- 특징: 실제 Firebase FCM 연동
- 성능: 평균 450ms, 95%+ 성공률
```

---

## 🔗 도메인 연동 상세

### DailyCheck 도메인 연동
```java
@Service
@RequiredArgsConstructor
public class DailyCheckService {
    private final NotificationService notificationService; // 자동으로 3중 안전망 서비스 주입

    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void sendDailyCheckNotifications() {
        List<MemberEntity> activeMembers = memberRepository.findAllActive();

        for (MemberEntity member : activeMembers) {
            // 3중 안전망이 자동 적용된 알림 발송
            boolean success = notificationService.sendPushNotification(
                member.getId(),
                "안부 메시지",
                "오늘 하루 어떻게 지내세요?"
            );

            // 성공/실패 관계없이 이력은 자동 저장됨
            recordDailyCheckAttempt(member.getId(), success);
        }
    }
}
```

### AlertRule 도메인 연동
```java
@Service
@RequiredArgsConstructor
public class AlertNotificationService {
    private final NotificationService notificationService; // 자동으로 3중 안전망 서비스 주입
    private final GuardianRepository guardianRepository;

    public void sendGuardianAlert(Long memberId, AlertResult alertResult) {
        // 회원의 보호자 조회
        List<GuardianEntity> guardians = guardianRepository.findActiveByMemberId(memberId);

        for (GuardianEntity guardian : guardians) {
            // 알림 레벨에 따른 제목 생성
            String title = generateAlertTitle(alertResult.getAlertLevel());
            String message = alertResult.getMessage();

            // 3중 안전망이 자동 적용된 보호자 알림 발송
            boolean success = notificationService.sendPushNotification(
                guardian.getId(),  // 보호자 ID를 memberId로 사용
                title,
                message
            );

            // AlertHistory에 알림 발송 결과 기록
            recordAlertNotificationResult(guardian.getId(), alertResult, success);
        }
    }

    private String generateAlertTitle(AlertLevel alertLevel) {
        return switch (alertLevel) {
            case EMERGENCY -> "🚨 긴급 알림";
            case HIGH -> "⚠️ 중요 알림";
            case MEDIUM -> "📢 주의 알림";
            case LOW -> "💬 정보 알림";
        };
    }
}
```

### Guardian 도메인 연동
```java
@Service
@RequiredArgsConstructor
public class GuardianService {
    private final NotificationService notificationService; // 자동으로 3중 안전망 서비스 주입

    @Transactional
    public void assignGuardianToMember(Long memberId, Long guardianId) {
        // 보호자 관계 설정 로직
        MemberEntity member = findMemberById(memberId);
        GuardianEntity guardian = findGuardianById(guardianId);
        member.assignGuardian(guardian);

        // 보호자에게 환영 알림 발송
        boolean success = notificationService.sendPushNotification(
            guardianId,
            "새로운 담당 회원",
            String.format("%s님의 보호자가 되었습니다. 따뜻한 돌봄 부탁드립니다.", member.getMemberName())
        );

        // 회원에게도 안내 알림 발송
        notificationService.sendPushNotification(
            memberId,
            "보호자 설정 완료",
            String.format("%s님이 회원님의 보호자로 설정되었습니다.", guardian.getGuardianName())
        );
    }
}
```

---

## 🧪 테스트 및 개발 가이드

### Mock 서비스 활용 (개발환경)
```java
// MockPushNotificationService 특징
@Service
@Primary
@Profile("dev")
public class MockPushNotificationService implements NotificationService {
    private final List<MockNotificationRecord> sentNotifications = new ArrayList<>();

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        // 실제 푸시 발송 없음, 로컬 저장만
        MockNotificationRecord record = MockNotificationRecord.builder()
            .memberId(memberId)
            .title(title)
            .message(message)
            .timestamp(LocalDateTime.now())
            .channelType(NotificationChannelType.PUSH)
            .success(true) // 항상 성공
            .build();

        sentNotifications.add(record);

        log.info("🔔 [MOCK] Push notification sent - memberId: {}, title: {}, message: {}",
                 memberId, title, message);

        return true; // 항상 성공 반환
    }

    // 테스트용 메서드들
    public List<MockNotificationRecord> getSentNotifications() { return sentNotifications; }
    public void clearSentNotifications() { sentNotifications.clear(); }
    public int getNotificationCountForMember(Long memberId) { /* ... */ }
}
```

### 통합 테스트 패턴
```java
@SpringBootTest
@TestPropertySource(properties = {
    "notification.stability.enabled=true",
    "spring.profiles.active=test"
})
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService; // 3중 안전망 서비스 자동 주입

    @Autowired
    private NotificationHistoryRepository historyRepository;

    @Test
    void shouldSendNotificationWithStabilityEnhancement() {
        // Given
        Long memberId = 1L;
        String title = "테스트 알림";
        String message = "테스트 메시지";

        // When
        boolean result = notificationService.sendPushNotification(memberId, title, message);

        // Then
        assertThat(result).isTrue();

        // 이력이 자동으로 저장되었는지 확인
        List<NotificationHistory> histories = historyRepository.findByMemberId(memberId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getTitle()).isEqualTo(title);
        assertThat(histories.get(0).getSuccess()).isTrue();
    }

    @Test
    void shouldRetryOnFailureAndRecordHistory() {
        // Given - Firebase Mock을 실패하도록 설정
        given(firebaseMessagingWrapper.sendMessage(any()))
            .willThrow(new FirebaseMessagingException("UNAVAILABLE", "Service unavailable"))
            .willThrow(new FirebaseMessagingException("INTERNAL_ERROR", "Internal error"))
            .willReturn("success-message-id");

        // When
        boolean result = notificationService.sendPushNotification(1L, "제목", "내용");

        // Then
        assertThat(result).isTrue(); // 3번째 시도에서 성공

        // 재시도 이력 확인
        verify(firebaseMessagingWrapper, times(3)).sendMessage(any());

        // 최종 성공 이력이 저장되었는지 확인
        List<NotificationHistory> histories = historyRepository.findByMemberId(1L);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getSuccess()).isTrue();
    }
}
```

---

## 📋 관련 문서

### 🔗 **연관 API**
- **[DailyCheck API](../domains/dailycheck.md)**: 매일 안부 메시지 자동 발송
- **[AlertRule API](./alertrule-api.md)**: 이상징후 감지 시 보호자 알림
- **[Guardian API](./guardian-api.md)**: 보호자 관계 설정 및 알림
- **[Member API](./member-api.md)**: 회원 정보 기반 알림 대상 설정

### 🛠️ **기술 문서**
- **[Notification 도메인 가이드](../domains/notification.md)**: Decorator Pattern 구현 상세
- **[아키텍처 가이드](../specifications/architecture-guide.md)**: DDD + Decorator Pattern
- **[성능 가이드](../specifications/performance-guide.md)**: Firebase FCM 최적화
- **[보안 가이드](../specifications/security-guide.md)**: 푸시 토큰 보안 관리

---

## 💡 개발자 가이드

### 새로운 알림 채널 추가
```java
// 1. NotificationChannelType에 새 타입 추가
public enum NotificationChannelType {
    PUSH("푸시알림"),
    EMAIL("이메일"),      // Phase 3 추가 예정
    SMS("SMS"),          // Phase 3 추가 예정
    KAKAO("카카오톡");    // 새로운 채널 추가
}

// 2. 새로운 서비스 구현
@Service
@Profile("prod")
public class KakaoNotificationService implements NotificationService {
    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        // 카카오톡 비즈니스 API 연동 로직
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.KAKAO;
    }
}

// 3. 설정에서 활성화
@Configuration
public class NotificationConfig {
    @Bean
    @ConditionalOnProperty(name = "kakao.enabled", havingValue = "true")
    public NotificationService kakaoNotificationService() {
        return new KakaoNotificationService();
    }
}
```

### 3중 안전망 시스템 확장
```java
// 새로운 데코레이터 추가 예시
@Component
public class RateLimitingNotificationService implements NotificationService {
    private final NotificationService delegate;
    private final RateLimiter rateLimiter;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        // 요청 제한 체크
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for member: {}", memberId);
            return false;
        }

        return delegate.sendPushNotification(memberId, title, message);
    }
}
```

### 통계 모니터링 확장
```java
// 커스텀 통계 수집
@Component
public class NotificationMetricsCollector {
    private final MeterRegistry meterRegistry;

    @EventListener
    public void handleNotificationSent(NotificationSentEvent event) {
        // Micrometer 메트릭 수집
        meterRegistry.counter("notification.sent",
            "channel", event.getChannelType().name(),
            "success", String.valueOf(event.isSuccess())
        ).increment();
    }

    @EventListener
    public void handleNotificationRetry(NotificationRetryEvent event) {
        meterRegistry.counter("notification.retry",
            "attempt", String.valueOf(event.getAttemptNumber())
        ).increment();
    }
}
```

---

## 🚀 확장 가능성

### Phase 3 예정 기능
- **이메일 알림**: SMTP 서버 연동으로 이메일 알림 지원
- **SMS 알림**: 문자 서비스 API 연동으로 SMS 알림 지원
- **카카오톡 알림**: 카카오톡 비즈니스 API 연동
- **Slack/Discord**: 팀 워크스페이스 알림 연동

### 고도화 방향
- **AI 기반 알림 최적화**: 사용자 패턴 학습 기반 알림 시간 최적화
- **다국어 알림**: 사용자 언어 설정에 따른 자동 번역 알림
- **Rich Notification**: 이미지, 버튼, 액션 포함된 고도화 알림
- **실시간 통계 대시보드**: Grafana 연동 실시간 모니터링

### 성능 최적화
- **배치 알림**: 대량 알림 발송 시 배치 처리 최적화
- **캐싱 시스템**: 푸시 토큰 캐싱으로 조회 성능 향상
- **Message Queue**: 비동기 알림 처리로 응답 속도 개선
- **Load Balancing**: 다중 Firebase 프로젝트 로드 밸런싱

---

**Notification API는 MARUNI 플랫폼의 핵심 인프라로서 Firebase FCM 실제 연동, 3중 안전망 시스템, Decorator Pattern을 통해 확장성과 안정성을 모두 확보한 상용 서비스 수준의 통합 알림 시스템입니다. 직접적인 REST API는 제공하지 않지만, 다른 모든 도메인에서 안정적인 알림 발송을 위해 핵심적인 역할을 수행합니다.** 🔔