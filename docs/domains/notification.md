# Notification 도메인

**최종 업데이트**: 2025-11-06
**상태**: ✅ MVP 알림 타입 시스템 완성 + Mock 제거 리팩토링

## 📋 개요

알림 타입별 분류 및 이력 관리 시스템입니다. 5종의 알림 타입으로 구조화된 알림을 제공하고, 이력 조회 API를 지원합니다.

### 핵심 기능
- **알림 타입 시스템**: 5종 타입별 분류 (DAILY_CHECK, GUARDIAN_REQUEST, ALERT, SYSTEM, CHAT)
- **알림 이력 관리**: 메타데이터 포함 영속화
- **조회 API**: 읽지 않은 개수, 이력 조회, 읽음 처리
- **단순화된 구조**: NotificationHistoryService 직접 호출

## 🏗️ 아키텍처

### 단순화된 알림 구조 (MVP)
```
비즈니스 로직 (DailyCheck, AlertRule, Guardian)
        ↓
NotificationHistoryService.recordNotificationWithType()
        ↓
NotificationHistory 저장 (PostgreSQL)
```

**설계 철학**: MVP에서는 실제 푸시 발송 없이 알림 이력만 관리합니다. 불필요한 인터페이스와 추상화 계층을 제거하여 단순하고 명확한 구조를 유지합니다.

## 🔧 핵심 서비스

### NotificationHistoryService (Domain Layer)
**알림 이력 저장 및 관리 전담 서비스**

```java
/**
 * MVP: 타입 정보를 포함한 알림 이력 저장
 */
NotificationHistory recordNotificationWithType(
    Long memberId,
    String title,
    String message,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId
);

/**
 * MVP: 기본 알림 이력 저장 (타입 정보 없음)
 */
NotificationHistory recordNotification(
    Long memberId,
    String title,
    String message
);

/**
 * 특정 회원의 알림 이력 조회
 */
List<NotificationHistory> getHistoryByMember(Long memberId);

/**
 * 알림 성공률 계산
 */
double calculateSuccessRate(LocalDateTime from);

/**
 * 알림 발송 통계 조회
 */
NotificationStatistics getStatistics(LocalDateTime from);

/**
 * 오래된 이력 데이터 정리
 */
long cleanupOldHistory(LocalDateTime before);
```

### NotificationQueryService (Application Layer)
**알림 조회 및 읽음 처리 전담 서비스**

```java
/**
 * 읽지 않은 알림 개수
 */
int getUnreadNotificationCount(Long memberId);

/**
 * 알림 이력 조회
 */
List<NotificationHistoryResponseDto> getNotificationHistory(Long memberId);

/**
 * 알림 읽음 처리
 */
void markAsRead(Long notificationId, Long memberId);

/**
 * 모든 알림 읽음 처리
 */
void markAllAsRead(Long memberId);
```

## 📦 도메인 모델

### NotificationHistory Entity (Domain Layer)
```java
@Entity
@Table(name = "notification_history")
public class NotificationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 받는 회원 ID
    @Column(nullable = false)
    private Long memberId;

    // 알림 제목
    @Column(nullable = false)
    private String title;

    // 알림 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // 알림 채널 타입 (PUSH, EMAIL, SMS 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channelType;

    // 발송 성공 여부
    @Column(nullable = false)
    private Boolean success;

    // 실패 시 오류 메시지
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // 외부 시스템에서 반환한 메시지 ID
    @Column
    private String externalMessageId;

    // --- MVP 추가 필드 ---

    // 알림 타입 (DAILY_CHECK, GUARDIAN_REQUEST 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    // 알림 출처 타입 (DAILY_CHECK, ALERT_RULE, GUARDIAN_REQUEST 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private NotificationSourceType sourceType;

    // 출처 엔티티 ID (DailyCheckRecord, AlertHistory, GuardianRequest 등)
    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // 읽은 시각
    @Column(name = "read_at")
    private LocalDateTime readAt;
}
```

### NotificationType Enum (5종)
```java
public enum NotificationType {
    DAILY_CHECK,        // 안부 확인 알림
    GUARDIAN_REQUEST,   // 보호자 요청 알림
    GUARDIAN_ACCEPT,    // 보호자 수락 알림
    GUARDIAN_REJECT,    // 보호자 거절 알림
    EMOTION_ALERT,      // 감정 패턴 감지 알림
    NO_RESPONSE_ALERT,  // 무응답 감지 알림
    KEYWORD_ALERT,      // 키워드 감지 알림
    SYSTEM,             // 시스템 알림
    CHAT                // 대화 알림
}
```

### NotificationSourceType Enum
```java
public enum NotificationSourceType {
    DAILY_CHECK,        // 안부 확인 시스템
    ALERT_RULE,         // 이상징후 감지 시스템
    GUARDIAN_REQUEST,   // 보호자 요청 시스템
    SYSTEM,             // 시스템
    CHAT                // 대화 시스템
}
```

### NotificationChannelType Enum
```java
public enum NotificationChannelType {
    PUSH,   // 푸시 알림 (MVP: 이력만 저장)
    EMAIL,  // 이메일 (미구현)
    SMS     // SMS (미구현)
}
```

## 🔗 도메인 연동

### DailyCheck → Notification
```java
// DailyCheckOrchestrator.java (Line 89-96)

var notificationHistory = notificationHistoryService.recordNotificationWithType(
    memberId,
    "안부 메시지",
    "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?",
    NotificationType.DAILY_CHECK,
    NotificationSourceType.DAILY_CHECK,
    null  // DailyCheckRecord ID는 발송 후 생성되므로 null
);

if (notificationHistory != null) {
    handleSuccessfulSending(memberId, message);
} else {
    handleFailedSending(memberId, message);
}
```

### Guardian → Notification
```java
// GuardianRelationService.java (Line 70-77)

// 보호자 요청 알림
notificationHistoryService.recordNotificationWithType(
    guardianId,
    "보호자 등록 요청",
    String.format("%s님이 보호자로 등록을 요청했습니다", requesterName),
    NotificationType.GUARDIAN_REQUEST,
    NotificationSourceType.GUARDIAN_REQUEST,
    savedRequest.getId()
);
```

### AlertRule → Notification
```java
// AlertNotificationService.java (Line 175-182)

// 이상징후 감지 시 보호자 알림
var notificationHistory = notificationHistoryService.recordNotificationWithType(
    guardianId,
    "[HIGH] 알림",
    alertMessage,
    NotificationType.EMOTION_ALERT,  // AlertType에 따라 매핑
    NotificationSourceType.ALERT_RULE,
    alertHistoryId
);

handleNotificationResult(memberId, notificationHistory != null, null);
```

## 📁 패키지 구조

```
notification/
├── domain/
│   ├── service/              # NotificationHistoryService (인터페이스)
│   ├── entity/               # NotificationHistory
│   ├── repository/           # NotificationHistoryRepository
│   └── vo/                   # NotificationType, NotificationSourceType, NotificationChannelType
├── application/
│   ├── service/              # NotificationQueryService
│   └── dto/                  # NotificationHistoryResponseDto
├── infrastructure/
│   └── service/              # NotificationHistoryServiceImpl
└── presentation/
    └── controller/           # NotificationController
```

**Note**: `infrastructure` 레이어는 `NotificationHistoryServiceImpl`만 포함합니다. Mock 관련 파일은 모두 제거되었습니다.

## 🎯 REST API

### 1. 읽지 않은 알림 개수
```http
GET /api/notifications/unread/count
Authorization: Bearer {token}

Response: 5
```

### 2. 알림 이력 조회
```http
GET /api/notifications/history
Authorization: Bearer {token}

Response: [
  {
    "id": 1,
    "title": "안부 메시지",
    "message": "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?",
    "notificationType": "DAILY_CHECK",
    "sourceType": "DAILY_CHECK",
    "sourceEntityId": 123,
    "isRead": false,
    "sentAt": "2025-11-06T09:00:00",
    "readAt": null
  }
]
```

### 3. 알림 읽음 처리
```http
POST /api/notifications/{id}/read
Authorization: Bearer {token}

Response: { "message": "알림을 읽음 처리했습니다." }
```

### 4. 모든 알림 읽음 처리
```http
POST /api/notifications/read-all
Authorization: Bearer {token}

Response: { "message": "모든 알림을 읽음 처리했습니다." }
```

## 🧪 테스트 전략

### NotificationHistoryServiceImplTest
- ✅ 성공한 알림 이력 저장
- ✅ 실패한 알림 이력 저장
- ✅ 회원별 알림 이력 조회
- ✅ 성공률 계산
- ✅ 통계 정보 조회
- ✅ 입력 검증 예외 처리

### NotificationQueryServiceTest
- ✅ 읽지 않은 알림 개수 조회
- ✅ 알림 이력 조회
- ✅ 알림 읽음 처리
- ✅ 모든 알림 읽음 처리
- ✅ 권한 검증

## ✅ 완성도

- [x] 알림 타입 시스템 (9종)
- [x] 알림 출처 타입 (5종)
- [x] 알림 이력 영속화
- [x] 읽음 여부 추적
- [x] 조회 API (4개)
- [x] NotificationHistoryService 직접 호출 구조
- [x] Mock 제거 및 단순화 완료
- [ ] Firebase FCM 연동 (Phase 3)
- [ ] 실제 푸시 발송 (Phase 3)
- [ ] 재시도 메커니즘 (Phase 3)

**MVP 알림 시스템 완성 + 리팩토링 완료**

## 🔄 리팩토링 히스토리 (2025-11-06)

### Before: 3계층 구조
```
NotificationService (인터페이스)
    ↓
NotificationHistoryDecorator (래퍼)
    ↓
MockPushNotificationService (Mock)
```

### After: 단순화된 구조
```
NotificationHistoryService (직접 호출)
    ↓
NotificationHistory 저장
```

### 제거된 파일 (5개)
- `NotificationService.java` (interface)
- `MockPushNotificationService.java`
- `MockNotificationRecord.java`
- `NotificationHistoryDecorator.java`
- `NotificationDecoratorConfig.java`

### 변경 이유
- **MVP 단순성**: 실제 푸시 발송 없이 이력만 관리하는 MVP에서 불필요한 추상화 제거
- **명확성**: 직접 호출로 코드 흐름이 명확해짐
- **유지보수성**: 계층이 줄어들어 코드 추적이 쉬워짐

## 🚀 향후 계획 (Phase 3)

### FCM 연동
- Firebase Admin SDK 통합
- 실제 푸시 알림 발송
- 토큰 관리 시스템
- 발송 성공/실패 처리

### 안정성 강화
- 재시도 메커니즘
- Fallback 전략
- 통계 및 모니터링

### 고도화
- 알림 설정 관리 (알림 on/off, 타입별 설정)
- 알림 스케줄링 (야간 알림 제한 등)
- 다중 채널 지원 (EMAIL, SMS)
