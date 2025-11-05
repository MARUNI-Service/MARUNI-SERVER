# Notification 도메인

**최종 업데이트**: 2025-11-05
**상태**: ✅ MVP 알림 타입 시스템 완성

## 📋 개요

알림 타입별 분류 및 이력 관리 시스템입니다. 5종의 알림 타입으로 구조화된 알림을 제공하고, 이력 조회 API를 지원합니다.

### 핵심 기능
- **알림 타입 시스템**: 5종 타입별 분류 (DAILY_CHECK, GUARDIAN_REQUEST, ALERT, SYSTEM, CHAT)
- **알림 이력 관리**: 메타데이터 포함 영속화
- **조회 API**: 읽지 않은 개수, 이력 조회, 읽음 처리
- 다중 채널 확장 가능 구조 (FCM 등 추후 추가)

## 🏗️ 주요 구조

### NotificationService 인터페이스 (Domain Layer)
```java
// 알림 타입 포함 발송
boolean sendNotificationWithType(
    Long memberId,
    String title,
    String message,
    NotificationType type,
    Map<String, String> metadata,
    Long referenceId
);

// 기본 푸시 알림 (레거시)
boolean sendPushNotification(Long memberId, String title, String message);
```

### NotificationHistory Entity (Domain Layer)
```java
- id: Long
- memberId: Long
- title: String
- message: String
- notificationType: NotificationType      // 알림 타입
- metadata: String (JSON)                 // 메타데이터
- referenceId: Long                       // 참조 ID (선택)
- isRead: Boolean                         // 읽음 여부
- readAt: LocalDateTime                   // 읽은 시간
- sentAt: LocalDateTime                   // 발송 시간
```

### NotificationType Enum (5종)
```java
DAILY_CHECK         // 안부 확인 알림
GUARDIAN_REQUEST    // 보호자 요청 관련
ALERT              // 긴급 알림 (이상징후)
SYSTEM             // 시스템 알림
CHAT               // 대화 알림
```

## 🔧 핵심 서비스

### NotificationQueryService (Application Layer)
**알림 조회 전담 서비스**

```java
// 읽지 않은 알림 개수
int getUnreadNotificationCount(Long memberId);

// 알림 이력 조회
List<NotificationHistoryResponseDto> getNotificationHistory(Long memberId);

// 알림 읽음 처리
void markAsRead(Long notificationId, Long memberId);

// 모든 알림 읽음 처리
void markAllAsRead(Long memberId);
```

### NotificationHistoryService (Domain Layer)
**알림 이력 관리 전담 서비스**

```java
// 이력 기록
NotificationHistory recordNotification(
    Long memberId,
    String title,
    String message,
    NotificationType type,
    Map<String, String> metadata,
    Long referenceId,
    boolean success
);

// 메타데이터 JSON 변환
String convertMetadataToJson(Map<String, String> metadata);
Map<String, String> convertJsonToMetadata(String json);
```

### MockPushNotificationService (Infrastructure Layer)
**MVP용 Mock 구현체**

```java
// 알림 타입 포함 발송
boolean sendNotificationWithType(...) {
    log.info("🔔 [{}] 알림 발송: {}", type, title);
    // 이력 자동 저장
    return true;
}
```

## 📊 메타데이터 활용

### 알림 타입별 메타데이터 예시
```java
// DAILY_CHECK
{
  "conversationId": "123",
  "scheduledTime": "09:00"
}

// GUARDIAN_REQUEST
{
  "requestId": "456",
  "requesterName": "김순자",
  "relation": "FAMILY"
}

// ALERT
{
  "alertHistoryId": "789",
  "alertLevel": "HIGH",
  "alertType": "EMOTION_PATTERN"
}
```

## 🔗 도메인 연동

### DailyCheck → Notification
```java
// 안부 메시지 발송
notificationService.sendNotificationWithType(
    memberId,
    "안부 메시지",
    message,
    NotificationType.DAILY_CHECK,
    Map.of("conversationId", conversationId.toString()),
    conversationId
);
```

### Guardian → Notification
```java
// 보호자 요청 알림
notificationService.sendNotificationWithType(
    guardianId,
    "보호자 요청",
    message,
    NotificationType.GUARDIAN_REQUEST,
    Map.of("requestId", requestId.toString(), "requesterName", name),
    requestId
);
```

### AlertRule → Notification
```java
// 긴급 알림 발송
notificationService.sendNotificationWithType(
    guardianId,
    "[HIGH] 알림",
    alertMessage,
    NotificationType.ALERT,
    Map.of("alertHistoryId", historyId.toString(), "alertLevel", "HIGH"),
    historyId
);
```

## 📁 패키지 구조

```
notification/
├── domain/
│   ├── service/              # NotificationService (인터페이스)
│   ├── entity/               # NotificationHistory
│   ├── repository/           # NotificationHistoryRepository
│   └── vo/                   # NotificationType
├── application/
│   ├── service/              # NotificationQueryService
│   └── dto/                  # NotificationHistoryResponseDto
├── presentation/
│   └── controller/           # NotificationController
└── infrastructure/
    └── service/              # MockPushNotificationService
```

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
    "message": "안녕하세요",
    "notificationType": "DAILY_CHECK",
    "metadata": {"conversationId": "123"},
    "referenceId": 123,
    "isRead": false,
    "sentAt": "2025-11-05T09:00:00"
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

## ✅ 완성도

- [x] 알림 타입 시스템 (5종)
- [x] 알림 이력 영속화
- [x] 메타데이터 JSON 저장
- [x] 읽음 여부 추적
- [x] 조회 API (4개)
- [x] Mock 구현체 (MVP)
- [ ] Firebase FCM 연동 (Phase 3)
- [ ] 재시도 메커니즘 (Phase 3)
- [ ] 통계 및 모니터링 (Phase 3)

**MVP 알림 시스템 완성**

## 🚀 향후 계획 (Phase 3)

### FCM 연동
- Firebase Admin SDK 통합
- 실제 푸시 알림 발송
- 토큰 관리 시스템

### 안정성 강화
- 재시도 메커니즘
- Fallback 전략
- 통계 및 모니터링

### 고도화
- 알림 설정 관리
- 알림 스케줄링
- 다중 채널 지원
