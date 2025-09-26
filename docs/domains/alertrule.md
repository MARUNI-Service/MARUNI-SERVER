# AlertRule 도메인 구현 가이드 (2025-09-25 현재 구조)

## 🎯 도메인 개요

**AlertRule 도메인**은 MARUNI 프로젝트의 핵심 도메인으로, **노인들의 이상징후를 자동으로 감지하고 보호자에게 실시간 알림을 제공**하는 시스템입니다.

### 핵심 기능
- **3종 이상징후 감지 알고리즘**: 감정패턴/무응답/키워드 분석
- **실시간 알림 발송**: 보호자에게 즉시 알림 전송
- **알림 규칙 관리**: 개별 맞춤형 감지 규칙 설정
- **이력 추적**: 모든 감지 및 알림 발송 기록 관리

## 📐 DDD 아키텍처 구조

### 패키지 구조
```
com.anyang.maruni.domain.alertrule/
├── application/                           # Application Layer
│   ├── dto/                              # Request/Response DTO (6개)
│   │   ├── request/                     # 요청 DTO
│   │   │   ├── AlertRuleCreateRequestDto.java     ✅ Swagger 문서화 완료
│   │   │   ├── AlertRuleUpdateRequestDto.java     ✅ Swagger 문서화 완료
│   │   │   └── AlertConditionDto.java              ✅ Swagger 문서화 완료
│   │   └── response/                    # 응답 DTO
│   │       ├── AlertRuleResponseDto.java           ✅ Swagger 문서화 완료
│   │       ├── AlertHistoryResponseDto.java        ✅ Swagger 문서화 완료
│   │       └── AlertDetectionResultDto.java        ✅ Swagger 문서화 완료
│   ├── service/                          # Application Service (Facade + 4개 분리된 서비스)
│   │   ├── core/                        # 핵심 비즈니스 서비스
│   │   │   ├── AlertRuleService.java            ✅ Facade 패턴 (모든 API 유지)
│   │   │   ├── AlertDetectionService.java       ✅ 이상징후 감지 전담
│   │   │   ├── AlertRuleManagementService.java  ✅ 알림 규칙 CRUD 전담
│   │   │   ├── AlertNotificationService.java    ✅ 알림 발송 전담
│   │   │   └── AlertHistoryService.java         ✅ 이력 관리 전담
│   │   ├── orchestrator/                # 오케스트레이션
│   │   │   └── AlertAnalysisOrchestrator.java   ✅ 분석 흐름 조율
│   │   └── util/                        # 유틸리티
│   │       └── AlertServiceUtils.java           ✅ 공통 서비스 유틸
│   ├── analyzer/                         # 이상징후 분석기 (Strategy Pattern)
│   │   ├── strategy/                    # 분석 전략
│   │   │   ├── AnomalyAnalyzer.java           ✅ Strategy 인터페이스
│   │   │   ├── EmotionPatternAnalyzer.java    ✅ 감정패턴 분석기
│   │   │   ├── NoResponseAnalyzer.java        ✅ 무응답 분석기
│   │   │   └── KeywordAnalyzer.java           ✅ 키워드 분석기
│   │   ├── vo/                          # Value Object
│   │   │   ├── AlertResult.java               ✅ 분석 결과
│   │   │   └── AnalysisContext.java           ✅ 분석 컨텍스트
│   │   └── util/                        # 분석 유틸리티
│   │       └── AnalyzerUtils.java             ✅ 공통 분석 유틸
│   └── config/                           # 설정
│       └── AlertConfigurationProperties.java   ✅ Alert 설정 프로퍼티
├── domain/                               # Domain Layer
│   ├── entity/                           # Domain Entity
│   │   ├── AlertRule.java                     ✅ 알림 규칙 엔티티
│   │   ├── AlertHistory.java                  ✅ 알림 이력 엔티티
│   │   ├── AlertCondition.java                ✅ 알림 조건 (Embeddable)
│   │   ├── AlertType.java                     ✅ 알림 유형 Enum
│   │   └── AlertLevel.java                    ✅ 알림 레벨 Enum
│   ├── repository/                       # Repository Interface
│   │   ├── AlertRuleRepository.java           ✅ 알림 규칙 저장소
│   │   └── AlertHistoryRepository.java        ✅ 알림 이력 저장소
│   └── exception/                        # Domain Exception
│       ├── AlertRuleNotFoundException.java     ✅ 알림 규칙 없음 예외
│       ├── AlertRuleAccessDeniedException.java ✅ 접근 거부 예외
│       ├── AlertRuleCreationFailedException.java ✅ 생성 실패 예외
│       ├── InvalidAlertConditionException.java ✅ 잘못된 조건 예외
│       └── UnsupportedAlertTypeException.java  ✅ 지원 안함 유형 예외
└── presentation/                         # Presentation Layer
    └── controller/                       # REST API Controller
        └── AlertRuleController.java           ✅ 8개 엔드포인트, Member 규격 적용
```

### 계층간 의존성
```
Presentation Layer
      ↓
Application Layer (Facade → 분리된 4개 서비스)
      ↓
Domain Layer (Entity + Repository Interface)
      ↓
Infrastructure Layer (JPA Repository 자동 구현)
```

## 🧠 핵심 비즈니스 로직

### 1. Strategy Pattern 적용 이상징후 분석 시스템

#### AnomalyAnalyzer 인터페이스
```java
public interface AnomalyAnalyzer {
    AlertResult analyze(MemberEntity member, AnalysisContext context);
    AlertType getSupportedType();
    boolean supports(AlertType alertType);
}
```

#### 감정 패턴 분석기 (EmotionPatternAnalyzer)
```java
@Component
public class EmotionPatternAnalyzer implements AnomalyAnalyzer {
    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        // 최근 N일간 사용자 메시지 조회
        // 감정 패턴 분석 (부정 감정 비율, 연속 일수)
        // 위험도 판정 (HIGH/MEDIUM/LOW)
    }
}
```

#### 무응답 패턴 분석기 (NoResponseAnalyzer)
```java
@Component
public class NoResponseAnalyzer implements AnomalyAnalyzer {
    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        // DailyCheck 기록 기반 무응답 패턴 분석
        // 연속 무응답 일수 및 응답률 기준 위험도 평가
    }
}
```

#### 키워드 감지 분석기 (KeywordAnalyzer)
```java
@Component
public class KeywordAnalyzer implements AnomalyAnalyzer {
    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        // 긴급/경고 키워드 감지
        // 즉시 알림 발송 대상 판정
    }
}
```

### 2. Facade Pattern 적용 서비스 구조

#### AlertRuleService (Facade)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleService {
    // 새로 분리된 서비스들에게 위임
    private final AlertDetectionService detectionService;
    private final AlertRuleManagementService managementService;
    private final AlertNotificationService notificationService;
    private final AlertHistoryService historyService;

    // ========== 이상징후 감지 관련 API ==========
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        return detectionService.detectAnomalies(memberId);
    }

    // ========== 알림 규칙 CRUD 관련 API ==========
    @Transactional
    public AlertRule createAlertRule(MemberEntity member, AlertType alertType,
                                   AlertLevel alertLevel, AlertCondition condition) {
        return managementService.createAlertRule(member, alertType, alertLevel, condition);
    }
    // ... 기타 위임 메서드들
}
```

### 3. 알림 규칙 관리 시스템

#### AlertRule 엔티티 (정적 팩토리 메서드)
```java
@Entity
@Table(name = "alert_rule", indexes = {
    @Index(name = "idx_alert_rule_member_type_active", columnList = "member_id, alert_type, is_active"),
    @Index(name = "idx_alert_rule_level_active", columnList = "alert_level, is_active"),
    @Index(name = "idx_alert_rule_member_active", columnList = "member_id, is_active")
})
public class AlertRule extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Column(nullable = false)
    private String ruleName;

    @Column(columnDefinition = "TEXT")
    private String ruleDescription;

    @Embedded
    private AlertCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 정적 팩토리 메서드
    public static AlertRule createEmotionPatternRule(MemberEntity member, int consecutiveDays, AlertLevel alertLevel);
    public static AlertRule createNoResponseRule(MemberEntity member, int noResponseDays, AlertLevel alertLevel);
    public static AlertRule createKeywordRule(MemberEntity member, String keywords, AlertLevel alertLevel);

    // 비즈니스 로직
    public boolean shouldTriggerAlert(List<MessageEntity> recentMessages);
    public void activate();
    public void deactivate();
    public void updateRule(String ruleName, String ruleDescription, AlertLevel alertLevel);
}
```

#### AlertCondition (Embeddable)
```java
@Embeddable
public class AlertCondition {
    private Integer consecutiveDays;                // 연속 일수 조건
    private Integer thresholdCount;                 // 임계값
    private String keywords;                        // 키워드 (쉼표로 구분)

    // 정적 팩토리 메서드
    public static AlertCondition createEmotionCondition(int consecutiveDays);
    public static AlertCondition createNoResponseCondition(int noResponseDays);
    public static AlertCondition createKeywordCondition(String keywords);

    // 조건 평가 메서드
    public boolean evaluate(List<MessageEntity> recentMessages, AlertType alertType);
}
```

#### AlertHistory 엔티티
```java
@Entity
@Table(name = "alert_history", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"member_id", "alert_rule_id", "alert_date"})
})
public class AlertHistory extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id")
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    @Column(nullable = false)
    private String alertMessage;

    @Column(columnDefinition = "TEXT")
    private String detectionDetails;                // JSON 형태 상세 정보

    @Column(nullable = false)
    @Builder.Default
    private Boolean isNotificationSent = false;

    private LocalDateTime notificationSentAt;

    @Column(columnDefinition = "TEXT")
    private String notificationResult;

    @Column(nullable = false)
    private LocalDateTime alertDate;                // 중복 방지용

    // 정적 팩토리 메서드
    public static AlertHistory createFromDetection(AlertRule alertRule, MemberEntity member, AlertResult alertResult);
    public static AlertHistory createFromKeywordDetection(MemberEntity member, AlertResult alertResult);
    public static AlertHistory createFromManualTrigger(AlertRule alertRule, MemberEntity member, AlertResult alertResult);

    // 비즈니스 로직
    public void markNotificationSent(boolean success, String result);
    public boolean isDuplicate(LocalDateTime targetDate);
}
```

### 4. Enum 정의

#### AlertType
```java
public enum AlertType {
    EMOTION_PATTERN("감정패턴", "연속적인 부정적 감정 감지"),
    NO_RESPONSE("무응답", "일정 기간 응답 없음"),
    KEYWORD_DETECTION("키워드감지", "위험 키워드 포함된 응답");

    private final String displayName;
    private final String description;
}
```

#### AlertLevel
```java
public enum AlertLevel {
    LOW("낮음", 1, "정보성 알림"),
    MEDIUM("보통", 2, "주의 관찰 필요"),
    HIGH("높음", 3, "빠른 확인 필요"),
    EMERGENCY("긴급", 4, "즉시 대응 필요");

    private final String displayName;
    private final int priority;
    private final String description;
}
```

## 🌐 REST API 구조

### AlertRuleController (Member 규격 적용 완료)

```java
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "알림 규칙 관리 API", description = "이상징후 감지 알림 규칙 관리 API")
@CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    // 8개 엔드포인트 - 모두 Member Controller 규격 적용 완료
}
```

#### API 엔드포인트 목록 (8개)

1. **알림 규칙 생성**
   - `POST /api/alert-rules`
   - Request: `AlertRuleCreateRequestDto`
   - Response: `AlertRuleResponseDto`

2. **알림 규칙 목록 조회**
   - `GET /api/alert-rules`
   - Response: `List<AlertRuleResponseDto>`

3. **알림 규칙 상세 조회**
   - `GET /api/alert-rules/{id}`
   - Response: `AlertRuleResponseDto`

4. **알림 규칙 수정**
   - `PUT /api/alert-rules/{id}`
   - Request: `AlertRuleUpdateRequestDto`
   - Response: `AlertRuleResponseDto`

5. **알림 규칙 삭제**
   - `DELETE /api/alert-rules/{id}`

6. **알림 규칙 활성화/비활성화**
   - `POST /api/alert-rules/{id}/toggle`
   - Query Parameter: `active=true/false`
   - Response: `AlertRuleResponseDto`

7. **알림 이력 조회**
   - `GET /api/alert-rules/history`
   - Query Parameter: `days=30` (기본값)
   - Response: `List<AlertHistoryResponseDto>`

8. **수동 이상징후 감지**
   - `POST /api/alert-rules/detect`
   - Response: `AlertDetectionResultDto`

### Swagger 문서화 완료

#### 모든 DTO에 @Schema 어노테이션 적용
- **Request DTO**: `AlertRuleCreateRequestDto`, `AlertRuleUpdateRequestDto`, `AlertConditionDto`
- **Response DTO**: `AlertRuleResponseDto`, `AlertHistoryResponseDto`, `AlertDetectionResultDto`

#### Controller에 Member 규격 패턴 적용
- `@ApiResponses` 상세 응답 코드 정의
- `@Parameter(hidden = true)` 인증 파라미터 숨김
- `@CustomExceptionDescription` 공통 예외 설명
- `@SuccessCodeAnnotation` 성공 코드 지정
- 한글 태그명 및 설명

## 🔗 도메인 간 연동

### Conversation 도메인 연동
```java
// 실시간 키워드 감지 (이벤트 기반)
@EventListener
public void handleNewMessage(MessageCreatedEvent event) {
    MessageEntity message = event.getMessage();
    AlertResult keywordResult = keywordAnalyzer.analyzeKeywordRisk(message);
    if (keywordResult.isAlert()) {
        alertRuleService.triggerAlert(message.getMemberId(), keywordResult);
    }
}
```

### Guardian 도메인 연동
```java
// 보호자 알림 발송 시스템
public void sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage) {
    List<GuardianEntity> guardians = guardianRepository.findActiveGuardiansByMemberId(memberId);

    for (GuardianEntity guardian : guardians) {
        if (guardian.isNotificationEnabled(alertLevel)) {
            boolean success = notificationService.sendPushNotification(
                guardian.getId(), createTitle(alertLevel), alertMessage);
            // 발송 결과 기록
        }
    }
}
```

### DailyCheck 도메인 연동
```java
// 무응답 패턴 감지를 위한 DailyCheck 기록 활용
public AlertResult analyzeNoResponsePattern(MemberEntity member, AnalysisContext context) {
    List<DailyCheckRecord> recentChecks = dailyCheckRecordRepository
        .findByMemberIdAndDateRangeOrderByCheckDateDesc(
            member.getId(), context.getStartDate(), context.getEndDate());

    // 무응답 패턴 분석 로직
    return evaluateNoResponseRisk(recentChecks, context.getAnalysisDays());
}
```

### Member/Auth 도메인 연동
```java
// JWT 인증 기반 API 보안
@PostMapping
public AlertRuleResponseDto createAlertRule(
        @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member,
        @Valid @RequestBody AlertRuleCreateRequestDto request) {
    // member 객체를 통한 권한 검증 및 비즈니스 로직 처리
}
```

## 🧪 테스트 전략

### TDD 기반 테스트 구조
```
src/test/java/com/anyang/maruni/domain/alertrule/
├── domain/entity/                    # 엔티티 단위 테스트
│   ├── AlertRuleTest.java
│   ├── AlertHistoryTest.java
│   └── AlertConditionTest.java
├── domain/repository/                # Repository 통합 테스트
│   ├── AlertRuleRepositoryTest.java
│   └── AlertHistoryRepositoryTest.java
├── application/service/              # 서비스 단위 테스트
│   ├── AlertRuleServiceTest.java
│   ├── AlertDetectionServiceTest.java
│   ├── AlertRuleManagementServiceTest.java
│   ├── AlertNotificationServiceTest.java
│   └── AlertHistoryServiceTest.java
├── application/analyzer/             # 분석기 단위 테스트
│   ├── EmotionPatternAnalyzerTest.java
│   ├── NoResponseAnalyzerTest.java
│   └── KeywordAnalyzerTest.java
└── presentation/controller/          # Controller 통합 테스트
    └── AlertRuleControllerTest.java
```

### 테스트 작성 패턴
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService 테스트")
class AlertRuleServiceTest {
    @Mock private AlertDetectionService detectionService;
    @Mock private AlertRuleManagementService managementService;
    @Mock private AlertNotificationService notificationService;
    @Mock private AlertHistoryService historyService;

    @InjectMocks
    private AlertRuleService alertRuleService;

    @Test
    @DisplayName("이상징후 감지 - 성공")
    void detectAnomalies_Success() {
        // Given
        Long memberId = 1L;
        List<AlertResult> expectedResults = Arrays.asList(
            AlertResult.createEmergency("긴급 키워드 감지"),
            AlertResult.createHigh("3일 연속 부정감정")
        );
        when(detectionService.detectAnomalies(memberId)).thenReturn(expectedResults);

        // When
        List<AlertResult> results = alertRuleService.detectAnomalies(memberId);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getAlertLevel()).isEqualTo(AlertLevel.EMERGENCY);
        verify(detectionService).detectAnomalies(memberId);
    }
}
```

## 📈 성능 최적화

### 인덱스 최적화
```sql
-- AlertRule 테이블
CREATE INDEX idx_alert_rule_member_type_active ON alert_rule (member_id, alert_type, is_active);
CREATE INDEX idx_alert_rule_level_active ON alert_rule (alert_level, is_active);
CREATE INDEX idx_alert_rule_member_active ON alert_rule (member_id, is_active);

-- AlertHistory 테이블
CREATE UNIQUE INDEX idx_alert_history_unique ON alert_history (member_id, alert_rule_id, alert_date);
```

### JPA 성능 최적화
```java
// N+1 문제 방지를 위한 fetch join
@Query("SELECT ar FROM AlertRule ar " +
       "JOIN FETCH ar.member m " +
       "LEFT JOIN FETCH m.guardian " +
       "WHERE ar.member.id = :memberId AND ar.isActive = true")
List<AlertRule> findActiveRulesWithMemberAndGuardian(@Param("memberId") Long memberId);
```

### 배치 처리 최적화
```java
// 대량 알림 발송 시 배치 처리
@Transactional
public void batchProcessAlerts(List<AlertResult> alertResults) {
    List<AlertHistory> histories = alertResults.stream()
        .map(this::convertToHistory)
        .collect(Collectors.toList());

    alertHistoryRepository.saveAll(histories); // 배치 insert
}
```

## 🚀 확장 가능성

### 새로운 분석기 추가
```java
@Component
public class SleepPatternAnalyzer implements AnomalyAnalyzer {
    @Override
    public AlertType getSupportedType() {
        return AlertType.SLEEP_PATTERN;
    }

    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        // 수면 패턴 분석 로직
    }
}
```

### 알림 채널 확장
```java
// SMS, 이메일, 카카오톡 등 다양한 알림 채널 지원
public interface NotificationChannel {
    boolean send(String recipient, String title, String message);
    NotificationChannelType getChannelType();
}
```

### 머신러닝 모델 연동
```java
// 향후 AI 모델 기반 이상징후 감지 확장 가능
@Component
public class MLBasedAnalyzer implements AnomalyAnalyzer {
    private final MLModelService mlModelService;

    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        // ML 모델을 활용한 고도화된 분석
    }
}
```

## 🎯 Claude Code 작업 가이드

### 현재 완성 상태
- ✅ **8개 REST API 엔드포인트 완성**: Member Controller 규격 적용
- ✅ **6개 DTO Swagger 문서화 완성**: 모든 @Schema 어노테이션 적용
- ✅ **Facade Pattern 적용**: SRP 준수하며 기존 API 호환성 유지
- ✅ **Strategy Pattern 적용**: 3개 분석기 확장 가능한 구조
- ✅ **완전한 도메인 간 연동**: Guardian, Conversation, DailyCheck 연동

### 향후 확장 시 주의사항

1. **새로운 AnomalyAnalyzer 추가 시**
   ```java
   // 1. AnomalyAnalyzer 인터페이스 구현
   // 2. @Component 어노테이션 추가
   // 3. AlertType enum에 새 타입 추가
   // 4. AlertAnalysisOrchestrator에서 자동 감지
   ```

2. **AlertLevel 우선순위 변경 시**
   ```java
   // AlertLevel enum의 priority 값 수정
   // 기존: LOW(1), MEDIUM(2), HIGH(3), EMERGENCY(4)
   ```

3. **성능 최적화 시**
   - N+1 문제 방지: `@EntityGraph` 또는 `JOIN FETCH` 활용
   - 인덱스 최적화: 쿼리 패턴에 맞는 복합 인덱스 추가
   - 배치 처리: 대량 데이터 처리 시 `saveAll()` 활용

4. **보안 고려사항**
   - 권한 검증: 회원별 알림 규칙 접근 제어
   - 입력 검증: `@Valid` 어노테이션과 Bean Validation 활용
   - SQL Injection 방지: JPA Query Methods 또는 `@Query` 사용

### API 사용 예시
```bash
# 감정 패턴 알림 규칙 생성
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "condition": {
    "consecutiveDays": 3,
    "thresholdCount": null,
    "keywords": null,
    "description": "3일 연속 부정감정 감지"
  },
  "ruleName": "연속 부정감정 감지",
  "description": "3일 연속 부정적 감정 감지 시 HIGH 레벨 알림"
}

# 수동 이상징후 감지 실행
POST /api/alert-rules/detect
Authorization: Bearer {JWT_TOKEN}

# 최근 30일 알림 이력 조회
GET /api/alert-rules/history?days=30
Authorization: Bearer {JWT_TOKEN}
```

---

**AlertRule 도메인은 MARUNI 프로젝트의 핵심 가치인 '실시간 이상징후 감지 및 보호자 알림'을 구현하는 완성된 도메인입니다. Facade Pattern과 Strategy Pattern을 적용하여 확장 가능하면서도 유지보수가 용이한 구조로 설계되었습니다.** 🚀