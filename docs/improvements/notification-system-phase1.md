# 알림 시스템 개선 계획 (Phase 1 - MVP Demo)

> **⚠️ 아카이브 문서**
>
> **작성일**: 2025-11-05
> **구현 완료**: 2025-11-06
> **상태**: ✅ **구현 완료 (실제 구현은 더 단순화됨)**
>
> 이 문서는 초기 계획 문서입니다. **실제 구현 결과는 [docs/domains/notification.md](../domains/notification.md)를 참조하세요.**
>
> ### 계획 vs 실제 구현
>
> | 항목 | 계획 (이 문서) | 실제 구현 |
> |------|------------|---------|
> | **구조** | NotificationService → Decorator → Mock | NotificationHistoryService 직접 호출 |
> | **제거 파일** | Retry, Fallback만 제거 | + NotificationService, Decorator, Mock 모두 제거 (5개 파일) |
> | **알림 발송** | Mock이 가짜 발송 | NotificationHistoryService가 이력만 저장 |
> | **단순성** | 3계층 (Service → Decorator → Mock) | 1계층 (NotificationHistoryService) |
> | **알림 타입** | 8종 | 9종 (GUARDIAN_ACCEPT, GUARDIAN_REJECT 추가) |
> | **결과** | MVP 단순화 | **더욱 단순화** (불필요한 추상화 완전 제거) |

---

## 🎯 목표

### 1. problems.md 4가지 문제 해결
1. ❌ **알림 타입 부족**: ALERT만 지원 → 8개 타입 지원
2. ❌ **읽음 상태 추적 불가** → isRead/readAt 필드 추가
3. ❌ **통합 API 없음** → GET /api/notifications 제공
4. ❌ **알림 출처 구분 불가** → notificationType/sourceType 추가

### 2. FCM 구조 단순화 (MVP)
- ❌ **제거**: Retry, Fallback, StabilityEnhancedConfig (복잡도 감소)
- ✅ **유지**: NotificationHistoryDecorator (알림 이력 저장 필수)
- ✅ **목표**: 빠른 데모 구현, 핵심 기능만

---

## 🔧 Part 1: FCM 구조 단순화

### 제거 대상 (MVP에 불필요)

**삭제할 파일:**
```
❌ infrastructure/decorator/RetryableNotificationService.java
❌ infrastructure/decorator/FallbackNotificationService.java
❌ infrastructure/config/StabilityEnhancedNotificationConfig.java
❌ infrastructure/config/NotificationRetryConfig.java
```

**이유:**
- Retry (3회 재시도): 데모에서 불필요, 복잡도만 증가
- Fallback (Firebase→Mock): 프로덕션 필요, MVP 불필요
- 복잡한 Bean 구성: 단순한 Config로 충분

### 유지 대상

**유지할 파일:**
```
✅ infrastructure/decorator/NotificationHistoryDecorator.java
✅ infrastructure/service/FirebasePushNotificationService.java
✅ infrastructure/service/MockPushNotificationService.java
```

**새로운 단순 Config:**
```java
@Configuration
public class SimpleNotificationConfig {

    @Bean
    @Primary
    public NotificationService notificationService(
        NotificationService baseService,  // Firebase or Mock
        NotificationHistoryRepository repository
    ) {
        return new NotificationHistoryDecorator(baseService, repository);
    }
}
```

---

## 📦 Part 2: 알림 타입/읽음 상태 기능 추가

### Step 1: Enum 추가

**파일**: `domain/notification/domain/vo/NotificationType.java`
```java
public enum NotificationType {
    DAILY_CHECK("안부 메시지"),
    EMOTION_ALERT("감정 패턴 이상"),
    NO_RESPONSE_ALERT("무응답 이상"),
    KEYWORD_ALERT("키워드 감지"),
    GUARDIAN_REQUEST("보호자 등록 요청"),
    GUARDIAN_ACCEPT("보호자 요청 수락"),
    GUARDIAN_REJECT("보호자 요청 거절"),
    SYSTEM("시스템 알림");

    private final String description;
    // getter
}
```

**파일**: `domain/notification/domain/vo/NotificationSourceType.java`
```java
public enum NotificationSourceType {
    DAILY_CHECK("안부 확인 시스템"),
    ALERT_RULE("이상징후 감지 시스템"),
    GUARDIAN_REQUEST("보호자 관리 시스템"),
    SYSTEM("시스템");

    private final String description;
    // getter
}
```

### Step 2: NotificationHistory 엔티티 확장

**파일**: `domain/notification/domain/entity/NotificationHistory.java`

**클래스에 추가:**
```java
@Setter(AccessLevel.PRIVATE)  // 클래스 레벨
```

**필드 5개 추가:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private NotificationType notificationType;

@Enumerated(EnumType.STRING)
@Column(nullable = false)
private NotificationSourceType sourceType;

@Column(name = "source_entity_id")
private Long sourceEntityId;

@Column(nullable = false)
private Boolean isRead = false;

@Column(name = "read_at")
private LocalDateTime readAt;
```

**정적 팩토리 메서드 2개 추가:**
```java
public static NotificationHistory createSuccessWithType(
    Long memberId, String title, String message,
    NotificationChannelType channelType,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId,
    String externalMessageId
) {
    return NotificationHistory.builder()
        .memberId(memberId)
        .title(title)
        .message(message)
        .channelType(channelType)
        .notificationType(notificationType)
        .sourceType(sourceType)
        .sourceEntityId(sourceEntityId)
        .success(true)
        .externalMessageId(externalMessageId)
        .isRead(false)
        .build();
}

public static NotificationHistory createFailureWithType(
    Long memberId, String title, String message,
    NotificationChannelType channelType,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId,
    String errorMessage
) {
    return NotificationHistory.builder()
        .memberId(memberId)
        .title(title)
        .message(message)
        .channelType(channelType)
        .notificationType(notificationType)
        .sourceType(sourceType)
        .sourceEntityId(sourceEntityId)
        .success(false)
        .errorMessage(errorMessage)
        .isRead(false)
        .build();
}
```

**비즈니스 메서드 추가:**
```java
public void markAsRead() {
    if (!this.isRead) {
        this.setIsRead(true);
        this.setReadAt(LocalDateTime.now());
    }
}
```

### Step 3: Repository 확장

**파일**: `domain/notification/domain/repository/NotificationHistoryRepository.java`

**메서드 1개 추가:**
```java
Long countByMemberIdAndIsReadFalse(Long memberId);
```

### Step 4: NotificationService 인터페이스 확장

**파일**: `domain/notification/domain/service/NotificationService.java`

**메서드 추가:**
```java
boolean sendNotificationWithType(
    Long memberId,
    String title,
    String message,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId
);
```

### Step 5: Application Layer 추가

**파일**: `domain/notification/application/dto/response/NotificationResponseDto.java`
```java
public record NotificationResponseDto(
    Long id,
    String title,
    String message,
    NotificationType type,
    NotificationSourceType sourceType,
    Long sourceEntityId,
    Boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
    public static NotificationResponseDto from(NotificationHistory history) {
        return new NotificationResponseDto(
            history.getId(),
            history.getTitle(),
            history.getMessage(),
            history.getNotificationType(),
            history.getSourceType(),
            history.getSourceEntityId(),
            history.getIsRead(),
            history.getReadAt(),
            history.getCreatedAt()
        );
    }
}
```

**파일**: `domain/notification/application/service/NotificationQueryService.java`
```java
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationHistoryRepository repository;

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getAllNotifications(Long memberId) {
        return repository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .stream()
            .map(NotificationResponseDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long memberId) {
        return repository.countByMemberIdAndIsReadFalse(memberId);
    }

    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId) {
        NotificationHistory notification = repository.findById(notificationId)
            .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();

        return NotificationResponseDto.from(notification);
    }
}
```

### Step 6: Presentation Layer 추가

**파일**: `domain/notification/presentation/NotificationController.java`
```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Notification", description = "통합 알림 API")
public class NotificationController {

    private final NotificationQueryService queryService;

    @GetMapping
    @Operation(summary = "전체 알림 조회")
    public List<NotificationResponseDto> getAllNotifications(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return queryService.getAllNotifications(memberId);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "안읽은 알림 개수")
    public Long getUnreadCount(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return queryService.getUnreadCount(memberId);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리")
    public NotificationResponseDto markAsRead(@PathVariable Long id) {
        return queryService.markAsRead(id);
    }
}
```

### Step 7: NotificationHistoryDecorator 수정

**파일**: `infrastructure/decorator/NotificationHistoryDecorator.java`

**변경사항:**
1. 의존성 변경: `NotificationHistoryService` → `NotificationHistoryRepository`
2. `sendNotificationWithType()` 메서드 구현 추가

**수정 코드:**
```java
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryDecorator implements NotificationService {

    private final NotificationService delegate;
    private final NotificationHistoryRepository repository;  // 변경

    @Override
    public boolean sendNotificationWithType(
        Long memberId, String title, String message,
        NotificationType notificationType,
        NotificationSourceType sourceType,
        Long sourceEntityId
    ) {
        try {
            boolean success = delegate.sendPushNotification(memberId, title, message);

            if (success) {
                NotificationHistory history = NotificationHistory.createSuccessWithType(
                    memberId, title, message, getChannelType(),
                    notificationType, sourceType, sourceEntityId, null
                );
                repository.save(history);
                log.info("✅ Notification sent and recorded");
            } else {
                NotificationHistory history = NotificationHistory.createFailureWithType(
                    memberId, title, message, getChannelType(),
                    notificationType, sourceType, sourceEntityId,
                    "Service returned false"
                );
                repository.save(history);
                log.warn("❌ Notification failed and recorded");
            }
            return success;
        } catch (Exception e) {
            NotificationHistory history = NotificationHistory.createFailureWithType(
                memberId, title, message, getChannelType(),
                notificationType, sourceType, sourceEntityId,
                "Exception: " + e.getMessage()
            );
            repository.save(history);
            log.error("💥 Notification exception", e);
            return false;
        }
    }

    // 기존 sendPushNotification() 유지
}
```

### Step 8: 도메인 연동

#### Guardian 도메인 (3곳)

**파일**: `domain/guardian/application/service/GuardianRelationService.java`

**변경 위치 1 - sendRequest():**
```java
// 기존
notificationService.sendPushNotification(guardianId, "보호자 등록 요청", message);

// 변경 후
notificationService.sendNotificationWithType(
    guardianId,
    "보호자 등록 요청",
    message,
    NotificationType.GUARDIAN_REQUEST,
    NotificationSourceType.GUARDIAN_REQUEST,
    savedRequest.getId()
);
```

**변경 위치 2 - acceptRequest():**
```java
notificationService.sendNotificationWithType(
    request.getRequester().getMemberId(),
    "보호자 요청 수락",
    message,
    NotificationType.GUARDIAN_ACCEPT,
    NotificationSourceType.GUARDIAN_REQUEST,
    requestId
);
```

**변경 위치 3 - rejectRequest():**
```java
notificationService.sendNotificationWithType(
    request.getRequester().getMemberId(),
    "보호자 요청 거절",
    message,
    NotificationType.GUARDIAN_REJECT,
    NotificationSourceType.GUARDIAN_REQUEST,
    requestId
);
```

#### DailyCheck 도메인 (1곳)

**파일**: `domain/dailycheck/application/scheduler/DailyCheckOrchestrator.java`

```java
// 기존
notificationService.sendPushNotification(memberId, "안부 메시지", message);

// 변경 후
notificationService.sendNotificationWithType(
    memberId,
    "안부 메시지",
    message,
    NotificationType.DAILY_CHECK,
    NotificationSourceType.DAILY_CHECK,
    dailyCheckRecord.getId()
);
```

#### AlertRule 도메인 (1곳)

**파일**: `domain/alertrule/application/service/core/AlertNotificationService.java`

```java
// AlertType에 따라 NotificationType 매핑
NotificationType notificationType = switch (alertResult.getAlertType()) {
    case EMOTION_PATTERN -> NotificationType.EMOTION_ALERT;
    case NO_RESPONSE -> NotificationType.NO_RESPONSE_ALERT;
    case KEYWORD -> NotificationType.KEYWORD_ALERT;
};

notificationService.sendNotificationWithType(
    guardianId,
    title,
    message,
    notificationType,
    NotificationSourceType.ALERT_RULE,
    alertHistory.getId()
);
```

---

## 📊 DB 마이그레이션

```sql
-- 1. 필드 추가
ALTER TABLE notification_history
ADD COLUMN notification_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
ADD COLUMN source_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
ADD COLUMN source_entity_id BIGINT,
ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN read_at TIMESTAMP;

-- 2. 인덱스 추가 (성능 최적화)
CREATE INDEX idx_notification_history_member_created
ON notification_history(member_id, created_at DESC);

CREATE INDEX idx_notification_history_member_isread
ON notification_history(member_id, is_read);
```

---

## 🗂️ 작업 순서

```
1️⃣ FCM 정리 (30분)
   ├── Retry/Fallback 데코레이터 삭제
   ├── StabilityEnhancedConfig 삭제
   └── 단순 Config 생성

2️⃣ Domain Layer (30분)
   ├── Enum 2개 생성
   ├── NotificationHistory 확장
   ├── Repository 메서드 추가
   └── NotificationService 인터페이스 확장

3️⃣ Application Layer (20분)
   ├── NotificationQueryService 생성
   └── NotificationResponseDto 생성

4️⃣ Infrastructure Layer (20분)
   └── NotificationHistoryDecorator 수정

5️⃣ Presentation Layer (15분)
   └── NotificationController 생성

6️⃣ 도메인 연동 (30분)
   ├── Guardian (3곳)
   ├── DailyCheck (1곳)
   └── AlertRule (1곳)

7️⃣ DB 마이그레이션 (10분)
   └── ALTER TABLE 실행

⏱️ 총 예상 시간: 2.5시간
```

---

## ✅ 최종 API

```
GET  /api/notifications              # 전체 알림 조회 (최신순)
GET  /api/notifications/unread-count # 안읽은 알림 개수
PATCH /api/notifications/{id}/read   # 알림 읽음 처리
```

**응답 예시:**
```json
{
  "data": [
    {
      "id": 1,
      "title": "보호자 등록 요청",
      "message": "김순자님이 보호자로 등록을 요청했습니다",
      "type": "GUARDIAN_REQUEST",
      "sourceType": "GUARDIAN_REQUEST",
      "sourceEntityId": 10,
      "isRead": false,
      "readAt": null,
      "createdAt": "2025-11-05T09:00:00"
    }
  ]
}
```

---

## 💡 MVP vs 기존 Phase 1 비교

| 항목 | 기존 Phase 1 | MVP 단순화 |
|------|------------|-----------|
| 안전망 | Retry + Fallback + History (3중) | History만 (1중) |
| Config | StabilityEnhanced (복잡) | Simple (단순) |
| 재시도 | 최대 3회 (지수 백오프) | 없음 |
| Fallback | Firebase→Mock 자동 전환 | 없음 |
| 테스트 | 전체 TDD | 핵심만 |
| 예상 시간 | 6-8시간 | 2.5시간 |
| 목표 | 상용 서비스 | 빠른 데모 |

---

## 🚀 다음 단계 (Phase 2)

MVP 데모 후 필요 시 추가:
- [ ] Retry 시스템 재도입 (3회 재시도)
- [ ] Fallback 시스템 재도입 (Firebase→Mock)
- [ ] 알림 필터링 (타입별, 읽음/안읽음)
- [ ] 알림 페이징 처리
- [ ] 전체 테스트 코드 작성

---

**작성자**: Claude Code
**버전**: 2.0 (MVP 단순화)
**최종 수정**: 2025-11-05
