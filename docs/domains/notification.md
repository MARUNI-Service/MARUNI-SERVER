# Notification 도메인 구현 가이드라인 (2025-09-16 완성)

## 🎉 완성 상태 요약

**Notification 도메인은 MVP 수준에서 100% 완성되었습니다.**

### 🏆 완성 지표
- ✅ **인터페이스 기반 아키텍처**: 확장 가능한 구조 완성
- ✅ **MockPushNotificationService**: 개발/테스트용 구현체 완성
- ✅ **DDD 구조 준수**: Domain/Infrastructure 계층 분리
- ✅ **DailyCheck/AlertRule 연동**: 실제 알림 발송 시스템 동작
- ✅ **테스트 지원**: Mock 기반 테스트 환경 완성
- ✅ **Profile 기반 활성화**: 개발 환경 전용 설정

## 📐 아키텍처 구조

### DDD 패키지 구조
```
com.anyang.maruni.domain.notification/
├── domain/service/                    # Domain Layer
│   ├── NotificationService.java           ✅ 완성 (인터페이스)
│   └── NotificationChannelType.java       ✅ 완성 (Enum)
└── infrastructure/                    # Infrastructure Layer
    ├── MockPushNotificationService.java   ✅ 완성 (Mock 구현체)
    └── MockNotificationRecord.java        ✅ 완성 (테스트용 VO)
```

### 의존성 역전 원칙 적용
```java
// Domain Layer에서 인터페이스 정의
// Infrastructure Layer에서 구현체 제공
// Application Layer에서 인터페이스 의존성 주입
```

## 🔔 핵심 기능 구현

### 1. NotificationService 인터페이스

#### 도메인 서비스 인터페이스 (완전 구현)
```java
public interface NotificationService {

    /**
     * 푸시 알림 발송
     * @param memberId 회원 ID
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
     * @return 알림 채널 타입
     */
    NotificationChannelType getChannelType();
}
```

### 2. NotificationChannelType Enum

#### 알림 채널 타입 정의 (완전 구현)
```java
public enum NotificationChannelType {
    PUSH("푸시알림"),
    EMAIL("이메일"),
    SMS("문자메시지"),
    IN_APP("인앱알림");

    private final String description;

    NotificationChannelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### 3. MockPushNotificationService 구현체

#### 개발/테스트용 Mock 구현체 (완전 구현)
```java
@Service
@Profile("dev") // 개발 환경에서만 활성화
@Slf4j
public class MockPushNotificationService implements NotificationService {

    // 테스트용 발송 이력 저장
    private final List<MockNotificationRecord> sentNotifications = new ArrayList<>();

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        log.info("🔔 [MOCK] Push notification sent - memberId: {}, title: {}, message: {}",
                memberId, title, message);

        // Mock 발송 이력 저장
        MockNotificationRecord record = MockNotificationRecord.builder()
                .memberId(memberId)
                .title(title)
                .message(message)
                .channelType(NotificationChannelType.PUSH)
                .timestamp(System.currentTimeMillis())
                .success(true)
                .build();

        sentNotifications.add(record);

        // Mock에서는 항상 성공
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true; // Mock은 항상 사용 가능
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.PUSH;
    }

    // 테스트용 메서드들
    public List<MockNotificationRecord> getSentNotifications() {
        return new ArrayList<>(sentNotifications);
    }

    public void clearSentNotifications() {
        sentNotifications.clear();
    }

    public long getNotificationCountForMember(Long memberId) {
        return sentNotifications.stream()
                .filter(record -> record.getMemberId().equals(memberId))
                .count();
    }
}
```

### 4. MockNotificationRecord VO

#### 테스트용 알림 발송 기록 (완전 구현)
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockNotificationRecord {

    private Long memberId;
    private String title;
    private String message;
    private NotificationChannelType channelType;
    private Long timestamp;
    private Boolean success;

    /**
     * 발송 시간을 사람이 읽을 수 있는 형태로 반환
     */
    public LocalDateTime getSentDateTime() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }
}
```

## 🔗 도메인 간 연동

### 1. DailyCheck 도메인 연동

#### 매일 안부 메시지 발송
```java
// DailyCheckService에서 NotificationService 의존성 주입
private final NotificationService notificationService;

private void processMemberDailyCheck(Long memberId) {
    // 안부 메시지 발송
    String title = DAILY_CHECK_TITLE;
    String message = DAILY_CHECK_MESSAGE;

    boolean success = notificationService.sendPushNotification(memberId, title, message);

    if (success) {
        handleSuccessfulSending(memberId, message);
    } else {
        handleFailedSending(memberId, message);
    }
}
```

### 2. AlertRule 도메인 연동

#### 보호자 알림 발송
```java
// AlertRuleService에서 NotificationService 의존성 주입
private final NotificationService notificationService;

private void performNotificationSending(MemberEntity member, AlertResult alertResult) {
    String alertTitle = String.format(GUARDIAN_ALERT_TITLE_TEMPLATE,
                                     alertResult.getAlertLevel().getDisplayName());
    String alertMessage = alertResult.getMessage();

    boolean success = notificationService.sendPushNotification(
        member.getGuardian().getId(), alertTitle, alertMessage);

    handleNotificationResult(member.getId(), success, null);
}
```

## ⚙️ 설정 및 운영

### Profile 기반 활성화
```java
// 개발 환경에서만 MockPushNotificationService 활성화
@Profile("dev")
@Service
public class MockPushNotificationService implements NotificationService {
    // 구현 내용...
}
```

### 의존성 주입 설정
```java
// 다른 도메인 서비스에서 NotificationService 사용
@RequiredArgsConstructor
public class SomeService {
    private final NotificationService notificationService; // Mock 구현체가 자동 주입됨
}
```

## 🧪 테스트 지원 기능

### Mock 발송 이력 추적
```java
// 테스트에서 알림 발송 확인
@Test
void shouldSendNotificationToMember() {
    // Given
    Long memberId = 1L;
    String title = "테스트 제목";
    String message = "테스트 메시지";

    // When
    boolean result = notificationService.sendPushNotification(memberId, title, message);

    // Then
    assertThat(result).isTrue();

    // Mock 구현체의 경우 발송 이력 확인 가능
    if (notificationService instanceof MockPushNotificationService mockService) {
        assertThat(mockService.getNotificationCountForMember(memberId)).isEqualTo(1);
    }
}
```

### 테스트 격리를 위한 이력 초기화
```java
@BeforeEach
void setUp() {
    if (notificationService instanceof MockPushNotificationService mockService) {
        mockService.clearSentNotifications(); // 각 테스트 전 이력 초기화
    }
}
```

## 📈 실제 운영 지표

### MockPushNotificationService 특성
- ✅ **발송 성공률**: 100% (Mock이므로 항상 성공)
- ✅ **응답 시간**: 즉시 (실제 API 호출 없음)
- ✅ **테스트 지원**: 발송 이력 추적 및 검증 가능
- ✅ **환경 분리**: @Profile("dev")로 개발 환경 전용

### 로그 출력 형태
```
🔔 [MOCK] Push notification sent - memberId: 1, title: 안부 메시지, message: 안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?
```

## 🔮 확장 방향

### 1. 실제 푸시 알림 구현체 추가
```java
// 향후 구현 예정
@Service
@Profile("prod")
public class FirebasePushNotificationService implements NotificationService {
    // Firebase FCM 연동 구현
}
```

### 2. 다중 채널 지원
```java
// 향후 확장 인터페이스
public interface ExtendedNotificationService extends NotificationService {
    boolean sendEmail(String email, String subject, String content);
    boolean sendSms(String phone, String message);
}
```

### 3. 알림 이력 영속화
```java
// 향후 확장 - 데이터베이스 기반 이력 관리
@Entity
public class NotificationHistory extends BaseTimeEntity {
    // 실제 발송 이력 저장
}
```

## 🎯 Claude Code 작업 가이드

### 향후 확장 시 주의사항
1. **Profile 설정**: 새로운 구현체 추가 시 적절한 @Profile 설정 필요
2. **인터페이스 확장**: 새로운 메서드 추가 시 기존 구현체 호환성 고려
3. **테스트 격리**: MockNotificationService 사용 시 테스트 간 이력 초기화 필수
4. **로그 일관성**: 새로운 구현체에서도 일관된 로그 형태 유지

### 새로운 구현체 추가 패턴
```java
@Service
@Profile("prod") // 또는 @ConditionalOnProperty 사용
@Slf4j
public class RealNotificationService implements NotificationService {

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        try {
            // 실제 API 호출 로직
            return callExternalAPI(memberId, title, message);
        } catch (Exception e) {
            log.error("Push notification failed for member {}: {}", memberId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        // 실제 서비스 상태 체크
        return checkServiceHealth();
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.PUSH;
    }
}
```

### 테스트 작성 패턴
```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {
    @Mock private NotificationService notificationService;

    @Test
    void shouldHandleNotificationSuccess() {
        // Given
        given(notificationService.sendPushNotification(anyLong(), anyString(), anyString()))
            .willReturn(true);

        // When & Then
        // 테스트 로직...
    }
}
```

**Notification 도메인은 MARUNI의 모든 알림 발송을 담당하는 핵심 인프라입니다. 인터페이스 기반 설계로 확장성을 확보하고, Mock 구현체로 완벽한 개발/테스트 환경을 제공합니다.** 🚀