# Phase 2 알림 시스템 설계

## 🔔 알림 시스템 아키텍처

Phase 2의 알림 시스템은 **모바일 앱 기반 푸시 알림을 우선**으로 하되, **확장 가능한 다중 채널 구조**로 설계되었습니다.

## 🎯 MVP 알림 채널 우선순위

### 1. 푸시 알림 (Firebase FCM) - 주요 채널 🚀
- **대상**: 모바일 앱 사용자
- **용도**: 실시간 안부 메시지, 긴급 알림
- **장점**: 즉시 전달, 높은 도달률, 비용 효율적
- **Phase 2 구현**: Firebase Cloud Messaging

### 2. 이메일 - 백업 채널 📧
- **대상**: 보호자, 상세 정보 제공용
- **용도**: 주간/월간 리포트, 상세 알림 내역
- **장점**: 풍부한 정보 전달, 기록 보존
- **Phase 3 구현 예정**

### 3. SMS - 긴급 상황 전용 📱
- **대상**: 긴급 상황 시 즉시 연락 필요한 경우
- **용도**: 응답 없음 알림, 응급 상황 감지
- **장점**: 100% 도달 보장, 앱 설치 불필요
- **Phase 3 구현 예정**

## 🏗️ 인터페이스 기반 아키텍처

### 핵심 인터페이스
```java
public interface NotificationService {
    // 푸시 알림 발송
    boolean sendPushNotification(Long memberId, String title, String message);

    // 채널 가용성 체크
    boolean isAvailable();

    // 채널 타입 반환
    NotificationChannelType getChannelType();
}
```

### 확장 인터페이스 (Phase 3)
```java
public interface ExtendedNotificationService extends NotificationService {
    // 이메일 발송
    boolean sendEmail(String email, String subject, String content);

    // SMS 발송
    boolean sendSMS(String phone, String message);

    // 다중 채널 발송
    NotificationResult sendMultiChannel(NotificationRequest request);

    // 발송 이력 조회
    List<NotificationHistory> getNotificationHistory(Long memberId, LocalDate from, LocalDate to);
}
```

### 채널 타입 정의
```java
public enum NotificationChannelType {
    PUSH("푸시알림"),
    EMAIL("이메일"),
    SMS("SMS"),
    MOCK("Mock");

    private final String displayName;
}
```

## 🔧 현재 구현 상태

### ✅ Mock 구현체 (개발 단계)
**파일**: `MockPushNotificationService.java`
```java
@Service
@Primary  // 개발 단계에서 우선 사용
@Slf4j
public class MockPushNotificationService implements NotificationService {

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        // 개발용 로그 출력
        log.info("📱 Mock Push Notification");
        log.info("   👤 Member: {}", memberId);
        log.info("   📋 Title: {}", title);
        log.info("   💬 Message: {}", message);
        log.info("   ✅ Status: Success (Mock)");

        // 항상 성공으로 처리 (TDD 개발용)
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true; // Mock은 항상 사용 가능
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.MOCK;
    }
}
```

### ⏳ Firebase FCM 구현체 (준비 중)
**파일**: `FirebasePushNotificationService.java` (향후 구현)
```java
@Service
@ConditionalOnProperty(name = "maruni.notification.push.provider", havingValue = "firebase")
@Slf4j
public class FirebasePushNotificationService implements NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;

    @Override
    public boolean sendPushNotification(Long memberId, String title, String message) {
        try {
            // 1. 회원 FCM 토큰 조회
            String fcmToken = getFcmTokenByMemberId(memberId);

            // 2. Firebase 메시지 생성
            Message firebaseMessage = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(message)
                    .build())
                .putData("type", "daily_check")
                .putData("memberId", memberId.toString())
                .build();

            // 3. Firebase로 발송
            String response = firebaseMessaging.send(firebaseMessage);
            log.info("Firebase push notification sent successfully: {}", response);
            return true;

        } catch (Exception e) {
            log.error("Failed to send Firebase push notification to member {}: {}",
                     memberId, e.getMessage());
            return false;
        }
    }
}
```

## 📊 알림 발송 플로우

### DailyCheck → Notification 연동
```java
// DailyCheckService에서 알림 발송 요청
private void processMemberDailyCheck(Long memberId) {
    String title = DAILY_CHECK_TITLE;
    String message = DAILY_CHECK_MESSAGE;

    // NotificationService를 통한 발송
    boolean success = notificationService.sendPushNotification(memberId, title, message);

    if (success) {
        handleSuccessfulSending(memberId, message);
    } else {
        handleFailedSending(memberId, message); // 자동 재시도 스케줄링
    }
}
```

### Guardian → Notification 연동 (Week 6 계획)
```java
// GuardianService에서 보호자 알림 발송
public void notifyGuardians(Long memberId, AlertLevel alertLevel, String alertMessage) {
    List<GuardianEntity> guardians = guardianRepository.findActiveGuardiansByMemberId(memberId);

    for (GuardianEntity guardian : guardians) {
        // 보호자별 알림 설정에 따른 채널 선택
        sendGuardianNotification(guardian, alertLevel, alertMessage);
    }
}

private void sendGuardianNotification(GuardianEntity guardian, AlertLevel alertLevel, String message) {
    switch (guardian.getNotificationPreference()) {
        case PUSH -> notificationService.sendPushNotification(guardian.getId(), "MARUNI 알림", message);
        case EMAIL -> notificationService.sendEmail(guardian.getGuardianEmail(), "MARUNI 알림", message);
        case ALL -> sendMultiChannelNotification(guardian, message);
    }
}
```

## 🔐 보안 및 개인정보 보호

### 1. FCM 토큰 관리
```java
@Entity
@Table(name = "member_fcm_token")
public class MemberFcmToken extends BaseTimeEntity {

    @Id
    private Long memberId;

    @Column(nullable = false)
    @ColumnTransformer(
        read = "AES_DECRYPT(fcm_token, UNHEX(SHA2(@encryption_key, 256)))",
        write = "AES_ENCRYPT(?, UNHEX(SHA2(@encryption_key, 256)))"
    )
    private String fcmToken; // 암호화 저장

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType; // ANDROID, IOS
}
```

### 2. 알림 내용 최소화
```java
// 민감한 정보는 포함하지 않음
private static final String DAILY_CHECK_TITLE = "안부 메시지";
private static final String DAILY_CHECK_MESSAGE = "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?";

// 긴급 상황에서도 구체적 내용은 앱 내에서만 확인 가능
private static final String EMERGENCY_ALERT_MESSAGE = "중요한 알림이 있습니다. 앱을 확인해주세요.";
```

## 📈 알림 성능 및 모니터링

### 발송 성공률 추적
```java
@Entity
@Table(name = "notification_log")
public class NotificationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private String channelType;
    private String title;
    private String message;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime sentAt;
    private Integer retryCount;
}
```

### 성능 메트릭스
```yaml
목표 지표:
  - 푸시 알림 성공률: 95% 이상
  - 평균 발송 시간: 3초 이내
  - 일일 발송량: 10,000건 처리 가능
  - 재시도 성공률: 85% 이상

모니터링 도구:
  - Spring Boot Actuator: 헬스 체크
  - 커스텀 메트릭스: 발송 성공률, 응답 시간
  - 로그 기반 알림: 실패율 임계치 초과 시 관리자 알림
```

## 🔮 Phase 3 확장 계획

### 1. 다중 채널 통합 관리
```java
@Service
public class IntegratedNotificationService {

    private final List<NotificationService> notificationServices;

    public NotificationResult sendWithFallback(NotificationRequest request) {
        // 우선순위별 채널 시도
        for (NotificationService service : getOrderedServices(request.getPriority())) {
            if (service.isAvailable()) {
                boolean success = sendNotification(service, request);
                if (success) return NotificationResult.success(service.getChannelType());
            }
        }
        return NotificationResult.failure("All channels failed");
    }
}
```

### 2. 개인화된 알림 설정
```java
@Entity
public class NotificationPreference extends BaseTimeEntity {
    private Long memberId;
    private NotificationChannelType preferredChannel;
    private LocalTime quietHoursStart;    // 방해 금지 시간
    private LocalTime quietHoursEnd;
    private Boolean weekendEnabled;       // 주말 알림 여부
    private AlertLevel minimumAlertLevel; // 최소 알림 레벨
}
```

### 3. 실시간 알림 대시보드
- 발송 현황 실시간 모니터링
- 채널별 성공률 분석
- 사용자별 알림 기록 관리
- 긴급 상황 대응 체계

## 🎯 Phase 2 알림 시스템 완성 목표

```yaml
✅ 인터페이스 기반 아키텍처: 확장성 확보
✅ Mock 구현체: TDD 개발 환경 완성
✅ DailyCheck 연동: 정기 안부 메시지 발송
⏳ Firebase FCM 연동: 실제 푸시 알림 구현
⏳ Guardian 연동: 보호자 알림 시스템 구축
⏳ 알림 이력 관리: 발송 기록 및 분석

비즈니스 가치:
- 실시간 소통 채널 확보
- 자동화된 안부 확인 시스템
- 긴급 상황 즉시 대응 체계
- 확장 가능한 다중 채널 구조
```

**Phase 2 알림 시스템은 MARUNI의 핵심 가치인 '능동적 소통'을 실현하는 핵심 인프라입니다.** 🚀