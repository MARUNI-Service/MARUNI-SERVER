# 알림 시스템 개선 계획 (Phase 1)

**작성일**: 2025-11-05
**상태**: 📋 계획 수립 완료
**우선순위**: 🔴 높음

---

## 🎯 개선 목표

### 해결할 문제 (docs/ploblems.md 기반)
1. ❌ **알림 타입 부족**: ALERT만 지원, 나머지 3개 타입 불가
2. ❌ **읽음 상태 추적 불가**: 안읽은 알림 표시 불가능
3. ❌ **통합 API 없음**: 여러 API 병합 필요 (복잡도↑)
4. ❌ **보호자 요청 알림 없음**: 푸시 놓치면 요청 확인 불가

### 개선 후 결과
1. ✅ **4가지 알림 타입 지원**: GUARDIAN_REQUEST, ALERT, DAILY_CHECK, SYSTEM
2. ✅ **읽음/안읽음 상태 서버 관리**: isRead, readAt 필드로 추적
3. ✅ **통합 알림 API 제공**: GET /api/notifications
4. ✅ **보호자 요청 알림 저장**: NotificationHistory에 통합 저장

---

## 📐 아키텍처 설계

### 현재 구조 (문제점)
```
각 도메인이 알림을 독립적으로 관리
├── NotificationHistory (Notification 도메인)
│   └── 모든 푸시 알림 발송 기록만 저장 (출처 추적 불가)
├── AlertHistory (AlertRule 도메인)
│   └── 이상징후 감지 알림 (NotificationHistory와 연계 없음)
├── DailyCheckRecord (DailyCheck 도메인)
│   └── 안부 메시지 발송 기록 (별도 저장)
└── GuardianRequest (Guardian 도메인)
    └── 보호자 요청 알림 (이력 저장 안 함!)
```

### 개선 구조 (Phase 1)
```
NotificationHistory를 중앙 집중식 알림 저장소로 확장
├── NotificationType (enum)
│   ├── DAILY_CHECK          (안부 메시지)
│   ├── EMOTION_ALERT        (감정 패턴 이상)
│   ├── NO_RESPONSE_ALERT    (무응답 이상)
│   ├── KEYWORD_ALERT        (키워드 감지)
│   ├── GUARDIAN_REQUEST     (보호자 요청)
│   ├── GUARDIAN_ACCEPT      (보호자 수락)
│   ├── GUARDIAN_REJECT      (보호자 거절)
│   └── SYSTEM               (시스템 알림)
│
├── NotificationSourceType (enum)
│   ├── DAILY_CHECK
│   ├── ALERT_RULE
│   ├── GUARDIAN_REQUEST
│   └── SYSTEM
│
└── NotificationHistory (확장)
    ├── 기존 필드 (memberId, title, message, channelType, success...)
    └── 신규 필드
        ├── notificationType: NotificationType
        ├── sourceType: NotificationSourceType
        ├── sourceEntityId: Long (출처 엔티티 ID)
        ├── isRead: Boolean (읽음 여부)
        └── readAt: LocalDateTime (읽은 시각)
```

---

## 📦 구현 상세

### 1. Domain Layer (Notification 도메인)

#### 1-1. Enum 추가

**파일**: `domain/notification/domain/vo/NotificationType.java`
```java
public enum NotificationType {
    // 안부 확인
    DAILY_CHECK("안부 메시지"),

    // 이상징후 감지
    EMOTION_ALERT("감정 패턴 이상"),
    NO_RESPONSE_ALERT("무응답 이상"),
    KEYWORD_ALERT("키워드 감지"),

    // 보호자 관리
    GUARDIAN_REQUEST("보호자 등록 요청"),
    GUARDIAN_ACCEPT("보호자 요청 수락"),
    GUARDIAN_REJECT("보호자 요청 거절"),

    // 시스템
    SYSTEM("시스템 알림");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
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

    NotificationSourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

#### 1-2. NotificationHistory 엔티티 확장

**파일**: `domain/notification/domain/entity/NotificationHistory.java`

**신규 필드 추가:**
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

**신규 정적 팩토리 메서드:**
```java
public static NotificationHistory createWithType(
    Long memberId,
    String title,
    String message,
    NotificationChannelType channelType,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId
) {
    NotificationHistory history = new NotificationHistory();
    history.memberId = memberId;
    history.title = title;
    history.message = message;
    history.channelType = channelType;
    history.notificationType = notificationType;
    history.sourceType = sourceType;
    history.sourceEntityId = sourceEntityId;
    history.isRead = false;
    return history;
}
```

**신규 비즈니스 메서드:**
```java
public void markAsRead() {
    if (!this.isRead) {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
```

#### 1-3. NotificationService 인터페이스 확장

**파일**: `domain/notification/domain/service/NotificationService.java`

**신규 메서드 추가:**
```java
/**
 * 타입 정보를 포함한 알림 발송
 */
void sendNotificationWithType(
    Long memberId,
    String title,
    String message,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId
);
```

---

### 2. Application Layer (Notification 도메인)

#### 2-1. NotificationResponseDto (신규)

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

#### 2-2. NotificationQueryService (신규)

**파일**: `domain/notification/application/service/NotificationQueryService.java`

```java
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    /**
     * 회원의 모든 알림 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getAllNotifications(Long memberId) {
        return notificationHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .stream()
            .map(NotificationResponseDto::from)
            .toList();
    }

    /**
     * 안읽은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long memberId) {
        return notificationHistoryRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId) {
        NotificationHistory notification = notificationHistoryRepository.findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notification.markAsRead();

        return NotificationResponseDto.from(notification);
    }
}
```

#### 2-3. NotificationHistoryRepository 확장

**파일**: `domain/notification/domain/repository/NotificationHistoryRepository.java`

**신규 메서드 추가:**
```java
List<NotificationHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

Long countByMemberIdAndIsReadFalse(Long memberId);
```

---

### 3. Presentation Layer (Notification 도메인)

#### 3-1. NotificationController (신규)

**파일**: `domain/notification/presentation/NotificationController.java`

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Notification", description = "통합 알림 API")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    @Operation(summary = "전체 알림 조회", description = "로그인한 회원의 모든 알림을 최신순으로 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "알림 조회 성공")
    })
    public List<NotificationResponseDto> getAllNotifications(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return notificationQueryService.getAllNotifications(memberId);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "안읽은 알림 개수 조회", description = "로그인한 회원의 안읽은 알림 개수")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public Long getUnreadCount(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return notificationQueryService.getUnreadCount(memberId);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    public NotificationResponseDto markAsRead(@PathVariable Long id) {
        return notificationQueryService.markAsRead(id);
    }
}
```

---

### 4. Infrastructure Layer 수정

#### 4-1. NotificationHistoryDecorator 수정

**파일**: `domain/notification/infrastructure/decorator/NotificationHistoryDecorator.java`

**신규 메서드 구현:**
```java
@Override
public void sendNotificationWithType(
    Long memberId,
    String title,
    String message,
    NotificationType notificationType,
    NotificationSourceType sourceType,
    Long sourceEntityId
) {
    try {
        // 1. 실제 알림 발송 (위임)
        delegate.sendPushNotification(memberId, title, message);

        // 2. 성공 시 NotificationHistory 저장
        NotificationHistory history = NotificationHistory.createWithType(
            memberId,
            title,
            message,
            NotificationChannelType.PUSH,
            notificationType,
            sourceType,
            sourceEntityId
        );
        history.markSuccess(null);  // 성공 표시

        notificationHistoryRepository.save(history);

    } catch (Exception e) {
        // 3. 실패 시에도 NotificationHistory 저장
        NotificationHistory history = NotificationHistory.createWithType(
            memberId,
            title,
            message,
            NotificationChannelType.PUSH,
            notificationType,
            sourceType,
            sourceEntityId
        );
        history.markFailure(e.getMessage());

        notificationHistoryRepository.save(history);

        throw e;
    }
}
```

---

### 5. 각 도메인 연동

#### 5-1. Guardian 도메인 수정

**파일**: `domain/guardian/application/service/GuardianRelationService.java`

**변경 전:**
```java
notificationService.sendPushNotification(
    guardianId,
    "보호자 등록 요청",
    message
);
```

**변경 후:**
```java
notificationService.sendNotificationWithType(
    guardianId,
    "보호자 등록 요청",
    message,
    NotificationType.GUARDIAN_REQUEST,
    NotificationSourceType.GUARDIAN_REQUEST,
    guardianRequest.getId()
);
```

**수정 대상:**
- `requestGuardianRelation()` - GUARDIAN_REQUEST
- `acceptGuardianRequest()` - GUARDIAN_ACCEPT (requester에게)
- `rejectGuardianRequest()` - GUARDIAN_REJECT (requester에게)

#### 5-2. DailyCheck 도메인 수정

**파일**: `domain/dailycheck/application/scheduler/DailyCheckOrchestrator.java`

**변경 후:**
```java
notificationService.sendNotificationWithType(
    memberId,
    "안부 메시지",
    "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?",
    NotificationType.DAILY_CHECK,
    NotificationSourceType.DAILY_CHECK,
    dailyCheckRecord.getId()
);
```

#### 5-3. AlertRule 도메인 수정

**파일**: `domain/alertrule/application/service/core/AlertNotificationService.java`

**변경 후:**
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

### 6. 기존 API Deprecated 처리

#### 6-1. AlertHistoryController

**파일**: `domain/alertrule/presentation/AlertHistoryController.java`

**수정:**
```java
@Deprecated(since = "2.1.0", forRemoval = true)
@GetMapping("/history")
@Operation(
    summary = "[Deprecated] 이상징후 알림 이력 조회",
    description = """
        ⚠️ 이 API는 더 이상 권장되지 않습니다.
        대신 GET /api/notifications를 사용하세요.

        이 API는 향후 버전에서 제거될 예정입니다.
        """
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공 (Deprecated)")
})
public List<AlertHistoryResponseDto> getAlertHistory(
    @RequestParam(defaultValue = "30") int days
) {
    // 기존 로직 유지
}
```

---

## 🧪 테스트 전략 (구현 후 작성)

### 1. 엔티티 테스트

**파일**: `NotificationHistoryTest.java`

```java
@Test
void markAsRead_shouldSetIsReadTrueAndReadAt() {
    // given
    NotificationHistory notification = NotificationHistory.createWithType(
        1L, "제목", "내용",
        NotificationChannelType.PUSH,
        NotificationType.DAILY_CHECK,
        NotificationSourceType.DAILY_CHECK,
        100L
    );

    // when
    notification.markAsRead();

    // then
    assertThat(notification.getIsRead()).isTrue();
    assertThat(notification.getReadAt()).isNotNull();
}

@Test
void markAsRead_shouldNotChangeReadAtIfAlreadyRead() {
    // given
    NotificationHistory notification = NotificationHistory.createWithType(...);
    notification.markAsRead();
    LocalDateTime firstReadAt = notification.getReadAt();

    // when
    notification.markAsRead();  // 두 번째 호출

    // then
    assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
}
```

### 2. 서비스 테스트

**파일**: `NotificationQueryServiceTest.java`

```java
@Test
void getAllNotifications_shouldReturnNotificationsOrderedByCreatedAtDesc() {
    // given
    Long memberId = 1L;
    // ... 테스트 데이터 준비

    // when
    List<NotificationResponseDto> result = notificationQueryService
        .getAllNotifications(memberId);

    // then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).createdAt())
        .isAfter(result.get(1).createdAt());
}

@Test
void getUnreadCount_shouldReturnOnlyUnreadNotifications() {
    // given
    Long memberId = 1L;
    // 읽은 알림 2개, 안읽은 알림 3개 생성

    // when
    Long unreadCount = notificationQueryService.getUnreadCount(memberId);

    // then
    assertThat(unreadCount).isEqualTo(3L);
}

@Test
void markAsRead_shouldChangeIsReadToTrue() {
    // given
    NotificationHistory notification = // ... 저장

    // when
    NotificationResponseDto result = notificationQueryService
        .markAsRead(notification.getId());

    // then
    assertThat(result.isRead()).isTrue();
    assertThat(result.readAt()).isNotNull();
}
```

### 3. 컨트롤러 테스트

**파일**: `NotificationControllerTest.java`

```java
@Test
void getAllNotifications_shouldReturn200WithNotificationList() throws Exception {
    // given
    List<NotificationResponseDto> notifications = List.of(/* ... */);
    when(notificationQueryService.getAllNotifications(anyLong()))
        .thenReturn(notifications);

    // when & then
    mockMvc.perform(get("/api/notifications")
            .with(user("1").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(3));
}

@Test
void markAsRead_shouldReturn200WithUpdatedNotification() throws Exception {
    // given
    Long notificationId = 1L;
    NotificationResponseDto response = /* ... */;
    when(notificationQueryService.markAsRead(notificationId))
        .thenReturn(response);

    // when & then
    mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
            .with(user("1").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isRead").value(true));
}
```

### 4. 통합 테스트

**파일**: `NotificationIntegrationTest.java`

```java
@Test
void guardianRequest_shouldSaveNotificationHistory() {
    // given
    GuardianRequestDto request = /* ... */;

    // when
    guardianRelationService.requestGuardianRelation(requesterId, request);

    // then
    List<NotificationHistory> notifications =
        notificationHistoryRepository.findByMemberId(guardianId);

    assertThat(notifications).hasSize(1);
    assertThat(notifications.get(0).getNotificationType())
        .isEqualTo(NotificationType.GUARDIAN_REQUEST);
    assertThat(notifications.get(0).getSourceType())
        .isEqualTo(NotificationSourceType.GUARDIAN_REQUEST);
}

@Test
void dailyCheckScheduler_shouldSaveNotificationHistory() {
    // given
    Long memberId = 1L;

    // when
    dailyCheckScheduler.sendDailyCheckMessages();

    // then
    List<NotificationHistory> notifications =
        notificationHistoryRepository.findByMemberId(memberId);

    assertThat(notifications)
        .filteredOn(n -> n.getNotificationType() == NotificationType.DAILY_CHECK)
        .hasSize(1);
}
```

---

## 📝 작업 순서 (구현 우선 → 테스트 나중)

### Step 1: Domain Layer - Enum 및 엔티티 확장
1. `NotificationType` enum 추가
2. `NotificationSourceType` enum 추가
3. `NotificationHistory` 엔티티 확장 (5개 필드 + `markAsRead()` 메서드)
4. `NotificationHistoryRepository` 메서드 추가
   - `findByMemberIdOrderByCreatedAtDesc(Long memberId)`
   - `countByMemberIdAndIsReadFalse(Long memberId)`

### Step 2: Application Layer - Service 및 DTO
5. `NotificationResponseDto` 추가
6. `NotificationQueryService` 구현
   - `getAllNotifications(Long memberId)`
   - `getUnreadCount(Long memberId)`
   - `markAsRead(Long notificationId)`
7. `NotificationService` 인터페이스 확장
   - `sendNotificationWithType()` 메서드 추가

### Step 3: Infrastructure Layer - Decorator 수정
8. `NotificationHistoryDecorator` 수정
   - `sendNotificationWithType()` 구현

### Step 4: Presentation Layer - Controller
9. `NotificationController` 구현
   - `GET /api/notifications`
   - `GET /api/notifications/unread-count`
   - `PATCH /api/notifications/{id}/read`

### Step 5: 도메인 연동
10. **Guardian 도메인** 수정
    - `GuardianRelationService` 4곳 수정
      - requestGuardianRelation → GUARDIAN_REQUEST
      - acceptGuardianRequest → GUARDIAN_ACCEPT
      - rejectGuardianRequest → GUARDIAN_REJECT
      - removeGuardianRelation → SYSTEM

11. **DailyCheck 도메인** 수정
    - `DailyCheckOrchestrator` 1곳 수정
      - sendDailyCheckMessages → DAILY_CHECK

12. **AlertRule 도메인** 수정
    - `AlertNotificationService` 1곳 수정
      - sendAlertToGuardians → EMOTION_ALERT/NO_RESPONSE_ALERT/KEYWORD_ALERT

### Step 6: 기존 API Deprecated 처리
13. `AlertHistoryController` @Deprecated 어노테이션 추가

### Step 7: DB 마이그레이션
14. DB 스키마 변경 (ALTER TABLE)
15. 기존 데이터 마이그레이션 (선택 사항)

### Step 8: 문서 업데이트
16. `docs/domains/notification.md` 업데이트
17. `docs/problems.md` 해결 완료 표시
18. `CLAUDE.md` Package Structure 업데이트

### Step 9: 테스트 코드 작성 (구현 완료 후)
19. **엔티티 테스트** (`NotificationHistoryTest`)
    - `markAsRead()` 테스트
    - `createWithType()` 정적 팩토리 메서드 테스트

20. **Repository 테스트** (`NotificationHistoryRepositoryTest`)
    - `findByMemberIdOrderByCreatedAtDesc()` 테스트
    - `countByMemberIdAndIsReadFalse()` 테스트

21. **서비스 테스트** (`NotificationQueryServiceTest`)
    - `getAllNotifications()` 테스트
    - `getUnreadCount()` 테스트
    - `markAsRead()` 테스트

22. **컨트롤러 테스트** (`NotificationControllerTest`)
    - GET /api/notifications 테스트
    - GET /api/notifications/unread-count 테스트
    - PATCH /api/notifications/{id}/read 테스트

23. **통합 테스트** (`NotificationIntegrationTest`)
    - Guardian 요청 → NotificationHistory 저장 확인
    - DailyCheck 발송 → NotificationHistory 저장 확인
    - AlertRule 감지 → NotificationHistory 저장 확인

### Step 10: 최종 검증
24. 전체 테스트 실행 및 통과 확인
25. Swagger UI 문서 확인
26. 로컬 환경 통합 테스트

---

## 📊 DB 마이그레이션

### ALTER TABLE 스크립트
```sql
-- NotificationHistory 테이블에 새 필드 추가
ALTER TABLE notification_history
ADD COLUMN notification_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
ADD COLUMN source_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
ADD COLUMN source_entity_id BIGINT,
ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN read_at TIMESTAMP;

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_notification_history_member_created
ON notification_history(member_id, created_at DESC);

CREATE INDEX idx_notification_history_member_isread
ON notification_history(member_id, is_read);
```

### 기존 데이터 마이그레이션
```sql
-- 기존 NotificationHistory 데이터는 SYSTEM 타입으로 기본 설정됨
-- 필요시 AlertHistory와 매칭하여 타입 업데이트
UPDATE notification_history nh
SET notification_type = 'EMOTION_ALERT',
    source_type = 'ALERT_RULE',
    source_entity_id = ah.id
FROM alert_history ah
WHERE nh.member_id = ah.member_id
  AND nh.created_at BETWEEN ah.notification_sent_at - INTERVAL '1 second'
                        AND ah.notification_sent_at + INTERVAL '1 second'
  AND ah.is_notification_sent = TRUE;
```

---

## ✅ 완료 기준 (Definition of Done)

### 1. 기능 구현 완료
- [ ] NotificationType, NotificationSourceType enum 추가
- [ ] NotificationHistory 엔티티 확장 (5개 필드)
- [ ] NotificationQueryService 구현 (3개 메서드)
- [ ] NotificationController 구현 (3개 API)
- [ ] Guardian/DailyCheck/AlertRule 도메인 연동
- [ ] AlertHistoryController @Deprecated 처리
- [ ] DB 마이그레이션 완료

### 2. 테스트 코드 작성 완료
- [ ] 엔티티 테스트 (NotificationHistoryTest) 작성 및 통과
- [ ] Repository 테스트 (NotificationHistoryRepositoryTest) 작성 및 통과
- [ ] 서비스 테스트 (NotificationQueryServiceTest) 작성 및 통과
- [ ] 컨트롤러 테스트 (NotificationControllerTest) 작성 및 통과
- [ ] 통합 테스트 (NotificationIntegrationTest) 작성 및 통과
- [ ] 전체 테스트 실행 및 통과 확인

### 3. 문서 업데이트
- [ ] docs/domains/notification.md 업데이트
- [ ] docs/problems.md 해결 완료 표시
- [ ] CLAUDE.md Package Structure 업데이트
- [ ] API 명세서 (Swagger) 자동 생성 확인

### 4. 최종 검증
- [ ] 로컬 환경 전체 기능 테스트
- [ ] Swagger UI에서 API 수동 테스트
- [ ] Postman으로 통합 시나리오 테스트
- [ ] 개발 서버 배포 준비 완료

---

## 🚀 Phase 2 계획 (추후)

### 고급 기능
- [ ] Guardian의 NotificationPreference 적용 (PUSH/EMAIL/SMS)
- [ ] 알림 필터링 (타입별, 읽음/안읽음)
- [ ] 알림 검색 (키워드, 날짜 범위)
- [ ] 알림 일괄 읽음 처리 (PATCH /api/notifications/read-all)
- [ ] 알림 삭제 기능
- [ ] 알림 페이징 처리

### 성능 최적화
- [ ] NotificationHistory 조회 쿼리 최적화
- [ ] Redis 캐시 적용 (안읽은 알림 개수)
- [ ] 배치 알림 발송

---

**작성자**: Claude Code
**검토자**: [담당자명]
**최종 승인**: [PM/리드명]

