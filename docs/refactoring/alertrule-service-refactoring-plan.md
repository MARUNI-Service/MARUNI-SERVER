# AlertRuleService 리팩토링 계획서

## 📋 개요

**AlertRuleService 클래스가 SRP(Single Responsibility Principle)를 위반하여 유지보수성과 확장성에 문제가 발생하고 있습니다. 이 문서는 체계적인 리팩토링 계획을 제시합니다.**

### 문제 현황
- **총 468줄의 대형 클래스**: 과도한 책임 집중
- **5개 서로 다른 책임 혼재**: SRP 위반
- **테스트 복잡성 증가**: 단일 클래스에 모든 로직 집중
- **유지보수 어려움**: 책임 경계 불분명

### 목표
- ✅ **SRP 준수**: 단일 책임별 클래스 분리
- ✅ **기존 API 호환성**: Controller 인터페이스 변경 없음
- ✅ **테스트 호환성**: 기존 테스트 코드 동작 보장
- ✅ **성능 유지**: 리팩토링으로 인한 성능 저하 방지
- ✅ **DDD 구조 준수**: Application Layer 내 서비스 분리

---

## 🔍 현재 상태 분석

### AlertRuleService의 다중 책임 (SRP 위반)

#### 1. 이상징후 감지 로직 (8개 메서드)
```java
// 감지 관련 메서드들
detectAnomalies(Long memberId)
detectKeywordAlert(MessageEntity message, Long memberId)
processAlertRules(MemberEntity member, List<AlertRule> activeRules)
isAlertTriggered(AlertResult analysisResult)
analyzeByRuleType(MemberEntity member, AlertRule rule)
createAnalysisContext(AlertType alertType, int defaultDays)
getActiveRulesByMemberId(Long memberId)
getActiveRulesByMemberIdOrderedByPriority(Long memberId)
```

#### 2. 알림 규칙 CRUD (8개 메서드)
```java
// CRUD 관련 메서드들
createAlertRule(MemberEntity member, AlertType alertType, AlertLevel alertLevel, AlertCondition condition)
createAlertRuleByType(MemberEntity member, AlertType alertType, AlertLevel alertLevel, AlertCondition condition)
createEmotionPatternAlertRule(MemberEntity member, AlertLevel alertLevel, AlertCondition condition)
createNoResponseAlertRule(MemberEntity member, AlertLevel alertLevel, AlertCondition condition)
createKeywordAlertRule(MemberEntity member, AlertLevel alertLevel, AlertCondition condition)
getAlertRuleById(Long alertRuleId)
updateAlertRule(Long alertRuleId, String ruleName, String ruleDescription, AlertLevel alertLevel)
deleteAlertRule(Long alertRuleId)
toggleAlertRule(Long alertRuleId, boolean active)
```

#### 3. 알림 발송 처리 (4개 메서드)
```java
// 알림 발송 관련 메서드들
triggerAlert(Long memberId, AlertResult alertResult)
sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage)
hasGuardian(MemberEntity member)
performNotificationSending(MemberEntity member, AlertLevel alertLevel, String alertMessage, Long memberId)
```

#### 4. 알림 이력 관리 (2개 메서드)
```java
// 이력 관리 관련 메서드들
recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult)
getRecentAlertHistory(Long memberId, int days)
createAlertHistoryForMVP(MemberEntity member, AlertResult alertResult)
```

#### 5. 유틸리티 기능 (3개 메서드)
```java
// 공통 유틸리티 메서드들
validateAndGetMember(Long memberId)
createDetectionDetailsJson(AlertResult alertResult)
handleNotificationResult(Long memberId, boolean success, String errorMessage)
```

---

## 🎯 리팩토링 전략

### Target Architecture (DDD Application Layer)

```
com.anyang.maruni.domain.alertrule.application/
├── service/
│   ├── AlertRuleService.java              # Facade 패턴 (기존 API 호환)
│   ├── AlertDetectionService.java         # 이상징후 감지 전담
│   ├── AlertRuleManagementService.java    # 알림 규칙 CRUD 전담
│   ├── AlertNotificationService.java      # 알림 발송 전담
│   └── AlertHistoryService.java           # 알림 이력 관리 전담
└── util/
    └── AlertServiceUtils.java              # 공통 유틸리티
```

### 책임 분리 설계

#### 1. AlertDetectionService (이상징후 감지)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertDetectionService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertAnalysisOrchestrator analysisOrchestrator;
    private final AlertConfigurationProperties alertConfig;

    /**
     * 회원의 이상징후 종합 감지
     */
    @Transactional
    public List<AlertResult> detectAnomalies(Long memberId) {
        // 이상징후 감지 로직만 담당
    }

    /**
     * 실시간 키워드 감지
     */
    @Transactional
    public AlertResult detectKeywordAlert(MessageEntity message, Long memberId) {
        // 키워드 감지 로직만 담당
    }

    // 관련 private 메서드들
    private List<AlertResult> processAlertRules(MemberEntity member, List<AlertRule> activeRules)
    private boolean isAlertTriggered(AlertResult analysisResult)
    private AlertResult analyzeByRuleType(MemberEntity member, AlertRule rule)
    private AnalysisContext createAnalysisContext(AlertType alertType, int defaultDays)
}
```

#### 2. AlertRuleManagementService (CRUD 관리)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleManagementService {

    private final AlertRuleRepository alertRuleRepository;
    private final MemberRepository memberRepository;

    /**
     * 알림 규칙 생성
     */
    @Transactional
    public AlertRule createAlertRule(MemberEntity member, AlertType alertType,
                                   AlertLevel alertLevel, AlertCondition condition) {
        // CRUD 로직만 담당
    }

    /**
     * 알림 규칙 수정
     */
    @Transactional
    public AlertRule updateAlertRule(Long alertRuleId, String ruleName,
                                   String ruleDescription, AlertLevel alertLevel) {
        // 수정 로직만 담당
    }

    /**
     * 알림 규칙 삭제
     */
    @Transactional
    public void deleteAlertRule(Long alertRuleId) {
        // 삭제 로직만 담당
    }

    /**
     * 알림 규칙 활성화/비활성화
     */
    @Transactional
    public AlertRule toggleAlertRule(Long alertRuleId, boolean active) {
        // 상태 변경 로직만 담당
    }

    /**
     * 알림 규칙 조회
     */
    public AlertRule getAlertRuleById(Long alertRuleId) {
        // 조회 로직만 담당
    }

    /**
     * 회원별 활성 규칙 조회
     */
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        // 목록 조회 로직만 담당
    }

    // 관련 private 메서드들
    private AlertRule createAlertRuleByType(...)
    private AlertRule createEmotionPatternAlertRule(...)
    private AlertRule createNoResponseAlertRule(...)
    private AlertRule createKeywordAlertRule(...)
}
```

#### 3. AlertNotificationService (알림 발송)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertNotificationService {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final AlertConfigurationProperties alertConfig;
    private final AlertServiceUtils alertServiceUtils;

    /**
     * 알림 발생 처리
     */
    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        // 알림 트리거 로직만 담당
    }

    /**
     * 보호자 알림 발송
     */
    @Transactional
    public void sendGuardianNotification(Long memberId, AlertLevel alertLevel, String alertMessage) {
        // 발송 로직만 담당
    }

    // 관련 private 메서드들
    private boolean hasGuardian(MemberEntity member)
    private void performNotificationSending(...)
    private void handleNotificationResult(Long memberId, boolean success, String errorMessage) {
        // 알림 발송 결과 처리 (AlertServiceUtils에서 이동)
        if (!success) {
            System.err.printf(alertConfig.getNotification().getNotificationFailureLog() + "%n", memberId);
            if (errorMessage != null) {
                System.err.printf(alertConfig.getNotification().getNotificationErrorLog() + "%n", errorMessage);
            }
        }
    }
}
```

#### 4. AlertHistoryService (이력 관리)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final MemberRepository memberRepository;

    /**
     * 알림 이력 기록
     */
    @Transactional
    public AlertHistory recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult) {
        // 이력 기록 로직만 담당
    }

    /**
     * 최근 알림 이력 조회
     */
    public List<AlertHistory> getRecentAlertHistory(Long memberId, int days) {
        // 이력 조회 로직만 담당
    }

    // 관련 private 메서드들
    private AlertHistory createAlertHistoryForMVP(MemberEntity member, AlertResult alertResult)
}
```

#### 5. AlertServiceUtils (공통 유틸리티)
```java
@Component
@RequiredArgsConstructor
public class AlertServiceUtils {

    private final MemberRepository memberRepository;
    private final AlertConfigurationProperties alertConfig;

    /**
     * 회원 검증 및 조회 (4개 서비스 공통 사용)
     */
    public MemberEntity validateAndGetMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원: " + memberId));
    }

    /**
     * 감지 상세 정보 JSON 생성 (AlertHistory, AlertNotification 공통 사용)
     */
    public String createDetectionDetailsJson(AlertResult alertResult) {
        return String.format(alertConfig.getNotification().getDetectionDetailsJsonTemplate(),
                alertResult.getAlertLevel(), alertResult.getAnalysisDetails());
    }
}
```

#### 6. AlertRuleService (Facade 패턴)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleService {

    // 새로 분리된 서비스들을 주입
    private final AlertDetectionService detectionService;
    private final AlertRuleManagementService managementService;
    private final AlertNotificationService notificationService;
    private final AlertHistoryService historyService;

    // 기존 API 메서드들을 위임 패턴으로 유지
    public List<AlertResult> detectAnomalies(Long memberId) {
        return detectionService.detectAnomalies(memberId);
    }

    public AlertRule createAlertRule(MemberEntity member, AlertType alertType,
                                   AlertLevel alertLevel, AlertCondition condition) {
        return managementService.createAlertRule(member, alertType, alertLevel, condition);
    }

    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        return notificationService.triggerAlert(memberId, alertResult);
    }

    public AlertHistory recordAlertHistory(AlertRule alertRule, MemberEntity member, AlertResult alertResult) {
        return historyService.recordAlertHistory(alertRule, member, alertResult);
    }

    // ... 나머지 모든 기존 메서드들도 위임 방식으로 유지
}
```

---

## 🔄 마이그레이션 계획

### Phase 1: 새 서비스 클래스 생성 (TDD Red)

#### Step 1-1: 빈 서비스 클래스들 생성
```bash
# 새 파일 생성
AlertDetectionService.java (빈 클래스)
AlertRuleManagementService.java (빈 클래스)
AlertNotificationService.java (빈 클래스)
AlertHistoryService.java (빈 클래스)
AlertServiceUtils.java (빈 클래스)
```

#### Step 1-2: 기본 구조 및 의존성 설정
```java
// 각 서비스의 기본 구조와 필요한 Repository 의존성만 설정
// 아직 메서드 구현은 하지 않음 (Red 단계)
```

### Phase 2: 메서드 이동 (TDD Green)

#### Step 2-1: AlertDetectionService 구현
```java
// AlertRuleService에서 감지 관련 8개 메서드를 이동
detectAnomalies() → AlertDetectionService
detectKeywordAlert() → AlertDetectionService
processAlertRules() → AlertDetectionService
isAlertTriggered() → AlertDetectionService
analyzeByRuleType() → AlertDetectionService
createAnalysisContext() → AlertDetectionService
getActiveRulesByMemberId() → AlertDetectionService
getActiveRulesByMemberIdOrderedByPriority() → AlertDetectionService
```

#### Step 2-2: AlertRuleManagementService 구현
```java
// AlertRuleService에서 CRUD 관련 8개 메서드를 이동
createAlertRule() → AlertRuleManagementService
createAlertRuleByType() → AlertRuleManagementService
createEmotionPatternAlertRule() → AlertRuleManagementService
createNoResponseAlertRule() → AlertRuleManagementService
createKeywordAlertRule() → AlertRuleManagementService
getAlertRuleById() → AlertRuleManagementService
updateAlertRule() → AlertRuleManagementService
deleteAlertRule() → AlertRuleManagementService
toggleAlertRule() → AlertRuleManagementService
```

#### Step 2-3: AlertNotificationService 구현
```java
// AlertRuleService에서 알림 발송 관련 4개 메서드를 이동
triggerAlert() → AlertNotificationService
sendGuardianNotification() → AlertNotificationService
hasGuardian() → AlertNotificationService
performNotificationSending() → AlertNotificationService
```

#### Step 2-4: AlertHistoryService 구현
```java
// AlertRuleService에서 이력 관리 관련 2개 메서드를 이동
recordAlertHistory() → AlertHistoryService
getRecentAlertHistory() → AlertHistoryService
createAlertHistoryForMVP() → AlertHistoryService
```

#### Step 2-5: AlertServiceUtils 구현
```java
// AlertRuleService에서 공통 유틸리티 2개 메서드를 이동
validateAndGetMember() → AlertServiceUtils (4개 서비스 공통 사용)
createDetectionDetailsJson() → AlertServiceUtils (AlertHistory, AlertNotification 공통 사용)

// handleNotificationResult는 AlertNotificationService의 private 메서드로 이동
handleNotificationResult() → AlertNotificationService (알림 발송 전용)
```

### Phase 3: Facade 패턴 적용 (TDD Blue)

#### Step 3-1: AlertRuleService를 Facade로 변경
```java
// 기존 AlertRuleService의 모든 메서드를 위임 방식으로 변경
// 기존 메서드 시그니처는 완전히 동일하게 유지
// 내부적으로만 새로 분리된 서비스들에게 위임

@Service
@RequiredArgsConstructor
public class AlertRuleService {
    private final AlertDetectionService detectionService;
    private final AlertRuleManagementService managementService;
    private final AlertNotificationService notificationService;
    private final AlertHistoryService historyService;

    // 기존 API를 그대로 유지하면서 내부적으로 위임
    public List<AlertResult> detectAnomalies(Long memberId) {
        return detectionService.detectAnomalies(memberId);
    }
    // ... 나머지 모든 메서드들도 동일한 방식
}
```

#### Step 3-2: 의존성 최적화
```java
// 각 서비스가 필요한 Repository만 주입받도록 최적화
// 불필요한 의존성 제거로 성능 향상
```

---

## 🧪 테스트 호환성 보장

### 기존 AlertRuleServiceTest 유지

#### 현재 테스트 구조
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService 테스트")
class AlertRuleServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private AlertHistoryRepository alertHistoryRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;
    @Mock private AlertAnalysisOrchestrator analysisOrchestrator;
    @Mock private AlertConfigurationProperties alertConfig;

    @InjectMocks
    private AlertRuleService alertRuleService;

    // 기존 테스트 메서드들...
}
```

#### 리팩토링 후 테스트 구조
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService 테스트")
class AlertRuleServiceTest {

    // 새로 분리된 서비스들을 Mock으로 주입
    @Mock private AlertDetectionService alertDetectionService;
    @Mock private AlertRuleManagementService alertRuleManagementService;
    @Mock private AlertNotificationService alertNotificationService;
    @Mock private AlertHistoryService alertHistoryService;

    @InjectMocks
    private AlertRuleService alertRuleService; // Facade 패턴

    // 기존 테스트 메서드들은 그대로 유지
    // 내부적으로 Mock 서비스들의 응답을 검증하는 방식으로 변경

    @Test
    @DisplayName("이상징후 감지 - 성공")
    void detectAnomalies_Success() {
        // Given
        Long memberId = 1L;
        List<AlertResult> expectedResults = Arrays.asList(
            AlertResult.createAlert(AlertLevel.HIGH, "테스트 메시지", null)
        );

        // AlertDetectionService의 응답을 Mock
        when(alertDetectionService.detectAnomalies(memberId))
            .thenReturn(expectedResults);

        // When
        List<AlertResult> results = alertRuleService.detectAnomalies(memberId);

        // Then
        assertThat(results).isEqualTo(expectedResults);
        verify(alertDetectionService).detectAnomalies(memberId);
    }

    // 기존 테스트 로직은 그대로 유지하되,
    // Mock 대상만 새로 분리된 서비스들로 변경
}
```

### 새 개별 서비스 테스트 추가

#### AlertDetectionServiceTest
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertDetectionService 테스트")
class AlertDetectionServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private AlertAnalysisOrchestrator analysisOrchestrator;
    @Mock private AlertConfigurationProperties alertConfig;

    @InjectMocks
    private AlertDetectionService alertDetectionService;

    // 감지 로직에 특화된 독립적인 테스트들
}
```

#### AlertRuleManagementServiceTest
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleManagementService 테스트")
class AlertRuleManagementServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private AlertRuleManagementService alertRuleManagementService;

    // CRUD 로직에 특화된 독립적인 테스트들
}
```

#### AlertNotificationServiceTest
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertNotificationService 테스트")
class AlertNotificationServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;
    @Mock private AlertConfigurationProperties alertConfig;

    @InjectMocks
    private AlertNotificationService alertNotificationService;

    // 알림 발송 로직에 특화된 독립적인 테스트들
}
```

#### AlertHistoryServiceTest
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertHistoryService 테스트")
class AlertHistoryServiceTest {

    @Mock private AlertHistoryRepository alertHistoryRepository;
    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private AlertHistoryService alertHistoryService;

    // 이력 관리 로직에 특화된 독립적인 테스트들
}
```

---

## 🔌 API 호환성 보장

### Controller 인터페이스 변경 없음

#### 현재 AlertRuleController
```java
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@AutoApiResponse
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @PostMapping
    public AlertRuleResponseDto createAlertRule(
            @AuthenticationPrincipal MemberEntity member,
            @Valid @RequestBody AlertRuleCreateRequestDto request) {

        AlertRule alertRule = alertRuleService.createAlertRule(
                member,
                request.getAlertType(),
                request.getAlertLevel(),
                convertToAlertCondition(request.getCondition())
        );

        return AlertRuleResponseDto.from(alertRule);
    }

    // ... 나머지 8개 API 메서드들
}
```

#### 리팩토링 후 AlertRuleController
```java
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@AutoApiResponse
public class AlertRuleController {

    // 동일한 AlertRuleService 주입 (Facade 패턴)
    private final AlertRuleService alertRuleService;

    @PostMapping
    public AlertRuleResponseDto createAlertRule(
            @AuthenticationPrincipal MemberEntity member,
            @Valid @RequestBody AlertRuleCreateRequestDto request) {

        // 동일한 메서드 호출 (내부적으로만 위임)
        AlertRule alertRule = alertRuleService.createAlertRule(
                member,
                request.getAlertType(),
                request.getAlertLevel(),
                convertToAlertCondition(request.getCondition())
        );

        return AlertRuleResponseDto.from(alertRule);
    }

    // Controller 코드는 완전히 동일하게 유지
    // API 응답, 요청 형태 모두 변경 없음
}
```

### Swagger API 문서 변경 없음
- 모든 API 엔드포인트 동일
- 요청/응답 DTO 동일
- API 문서 자동 생성 결과 동일

---

## ⚡ 성능 및 의존성 최적화

### 의존성 주입 최적화

#### Before (현재)
```java
@Service
public class AlertRuleService {
    // 모든 의존성을 하나의 클래스에서 관리
    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final AlertAnalysisOrchestrator analysisOrchestrator;
    private final AlertConfigurationProperties alertConfig;

    // 468줄의 대형 클래스
}
```

#### After (리팩토링 후)
```java
// 각 서비스는 필요한 의존성만 주입받음

@Service
public class AlertDetectionService {
    // 감지에 필요한 의존성만
    private final AlertRuleRepository alertRuleRepository;
    private final AlertAnalysisOrchestrator analysisOrchestrator;
    private final AlertConfigurationProperties alertConfig;
}

@Service
public class AlertRuleManagementService {
    // CRUD에 필요한 의존성만
    private final AlertRuleRepository alertRuleRepository;
    private final MemberRepository memberRepository;
}

@Service
public class AlertNotificationService {
    // 알림 발송에 필요한 의존성만
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final AlertConfigurationProperties alertConfig;
}

@Service
public class AlertHistoryService {
    // 이력 관리에 필요한 의존성만
    private final AlertHistoryRepository alertHistoryRepository;
    private final MemberRepository memberRepository;
}

@Service
public class AlertRuleService { // Facade
    // 분리된 서비스들만
    private final AlertDetectionService detectionService;
    private final AlertRuleManagementService managementService;
    private final AlertNotificationService notificationService;
    private final AlertHistoryService historyService;
}
```

### 메모리 사용량 최적화
- **Before**: 468줄 단일 클래스 + 6개 Repository 의존성
- **After**: 4개 독립 서비스 (각각 ~120줄) + 필요한 의존성만

### 테스트 성능 향상
- **Before**: 단일 대형 테스트 클래스
- **After**: 4개 독립적 테스트 클래스 (병렬 실행 가능)

---

## 📅 구현 로드맵

### Week 1: 기반 구조 (TDD Red)
- [ ] **Day 1-2**: 4개 새 서비스 클래스 생성
  - AlertDetectionService.java (빈 클래스)
  - AlertRuleManagementService.java (빈 클래스)
  - AlertNotificationService.java (빈 클래스)
  - AlertHistoryService.java (빈 클래스)

- [ ] **Day 3-4**: AlertServiceUtils 공통 클래스 생성
  - 공통 유틸리티 메서드 2개 이동 (validateAndGetMember, createDetectionDetailsJson)
  - handleNotificationResult는 AlertNotificationService로 이동
  - 의존성 주입 구조 설계

- [ ] **Day 5**: 기본 구조 및 의존성 설정 완료
  - 각 서비스의 Repository 의존성 설정
  - Spring Boot 애플리케이션 정상 기동 확인

### Week 2: 메서드 이동 (TDD Green)
- [ ] **Day 1-2**: AlertDetectionService 구현
  - 감지 관련 8개 메서드 이동 및 구현
  - 단위 테스트 작성 및 통과 확인

- [ ] **Day 3**: AlertRuleManagementService 구현
  - CRUD 관련 8개 메서드 이동 및 구현
  - 단위 테스트 작성 및 통과 확인

- [ ] **Day 4**: AlertNotificationService 구현
  - 알림 발송 관련 4개 메서드 이동 및 구현
  - 단위 테스트 작성 및 통과 확인

- [ ] **Day 5**: AlertHistoryService 구현
  - 이력 관리 관련 2개 메서드 이동 및 구현
  - 단위 테스트 작성 및 통과 확인

### Week 3: Facade 적용 (TDD Blue)
- [ ] **Day 1-2**: AlertRuleService Facade 패턴 적용
  - 기존 메서드들을 위임 방식으로 변경
  - 메서드 시그니처 완전히 동일하게 유지

- [ ] **Day 3**: 기존 API 호환성 검증
  - AlertRuleController 동작 확인
  - 모든 API 엔드포인트 테스트

- [ ] **Day 4**: 성능 테스트 및 최적화
  - 의존성 주입 최적화
  - 메모리 사용량 측정 및 개선

- [ ] **Day 5**: 통합 테스트 실행
  - 전체 시스템 통합 테스트
  - 기존 기능 완전 동작 확인

### Week 4: 테스트 및 검증
- [ ] **Day 1-2**: 개별 서비스 테스트 작성
  - AlertDetectionServiceTest
  - AlertRuleManagementServiceTest
  - AlertNotificationServiceTest
  - AlertHistoryServiceTest

- [ ] **Day 3**: 기존 AlertRuleServiceTest 수정
  - Mock 대상을 새 서비스들로 변경
  - 기존 테스트 로직은 그대로 유지

- [ ] **Day 4**: 통합 테스트 검증
  - 전체 테스트 스위트 실행
  - 코드 커버리지 확인 (기존 수준 유지)

- [ ] **Day 5**: 문서 업데이트
  - docs/domains/alertrule.md 업데이트
  - 리팩토링 결과 문서화
  - CLAUDE.md 패키지 구조 업데이트

---

## 📊 예상 효과

### Before vs After 비교

| **항목** | **Before (현재)** | **After (리팩토링 후)** | **개선 효과** |
|---------|------------------|----------------------|-------------|
| **클래스 크기** | 468줄 단일 클래스 | 4개 서비스 (각각 ~120줄) | 75% 크기 감소 |
| **책임 분리** | 5개 책임 혼재 | 책임별 단일 클래스 | SRP 완전 준수 |
| **테스트 복잡성** | 단일 대형 테스트 클래스 | 4개 독립적 단위 테스트 | 테스트 격리 |
| **유지보수성** | 어려움 (책임 경계 불분명) | 쉬움 (명확한 책임 분리) | 대폭 향상 |
| **확장성** | 제한적 (단일 클래스 수정) | 우수 (독립적 확장) | 확장 용이 |
| **의존성 관리** | 6개 Repository 한 곳 집중 | 필요한 의존성만 분산 | 메모리 최적화 |
| **병렬 개발** | 어려움 (충돌 위험) | 가능 (독립적 개발) | 개발 생산성 향상 |

### 코드 품질 지표

#### 복잡도 감소
- **Cyclomatic Complexity**: 25+ → 8 이하 (각 서비스)
- **Lines of Code per Class**: 468 → 120 이하
- **Number of Dependencies**: 6 → 2-3 (각 서비스)

#### 응집도 향상
- **기능적 응집도**: 낮음 → 높음 (단일 책임)
- **모듈 응집도**: 낮음 → 높음 (관련 기능만 그룹핑)

#### 결합도 감소
- **데이터 결합도**: 높음 → 낮음 (필요한 데이터만 전달)
- **제어 결합도**: 높음 → 낮음 (독립적 제어 흐름)

---

## ⚠️ 주의사항 및 리스크 관리

### 반드시 지켜야 할 원칙

#### 1. 기존 API 인터페이스 완전 보존
```java
// ❌ 금지: 메서드 시그니처 변경
// public AlertRule createAlertRule(CreateAlertRuleDto dto) // 새로운 DTO

// ✅ 필수: 기존 시그니처 완전 유지
public AlertRule createAlertRule(MemberEntity member, AlertType alertType,
                               AlertLevel alertLevel, AlertCondition condition)
```

#### 2. 테스트 호환성 보장
```java
// ❌ 금지: 기존 테스트 로직 변경
// @Test void detectAnomalies_NewLogic() // 새로운 테스트 방식

// ✅ 필수: 기존 테스트 로직 유지 + Mock 대상만 변경
@Test void detectAnomalies_Success() {
    // 기존 Given-When-Then 구조 그대로 유지
    // Mock 대상만 새 서비스들로 변경
}
```

#### 3. 성능 저하 방지
```java
// ❌ 금지: 추가적인 네트워크 호출이나 DB 쿼리
// 리팩토링으로 인한 성능 저하 발생 시키지 않음

// ✅ 필수: 기존 성능 수준 유지 또는 향상
// 의존성 최적화로 메모리 사용량 감소
```

#### 4. DDD 구조 준수
```java
// ❌ 금지: Infrastructure Layer에 비즈니스 로직 추가
// ❌ 금지: Domain Layer 엔티티 변경

// ✅ 필수: Application Layer 내에서만 서비스 분리
// 기존 DDD 계층 구조 완전 유지
```

### 리스크 요소 및 대응 방안

#### Risk 1: 순환 의존성 발생
**발생 가능 시나리오**: 새로 분리된 서비스들 간 상호 참조
```java
// ❌ 위험: AlertDetectionService → AlertNotificationService → AlertDetectionService
```

**대응 방안**:
- Facade 패턴으로 의존성 방향 통제
- 각 서비스는 Repository와 외부 서비스만 의존
- 서비스 간 직접 의존성 금지

#### Risk 2: 트랜잭션 경계 문제
**발생 가능 시나리오**: 여러 서비스에 걸친 트랜잭션 처리
```java
// ❌ 위험: 서비스 A에서 시작한 트랜잭션이 서비스 B에서 롤백되지 않음
```

**대응 방안**:
- Facade 서비스에서 트랜잭션 경계 관리
- 각 개별 서비스는 단순 비즈니스 로직만 담당
- @Transactional 어노테이션 적절히 배치

#### Risk 3: 기존 테스트 실패
**발생 가능 시나리오**: Mock 구조 변경으로 기존 테스트 실패
```java
// ❌ 위험: 기존 @Mock AlertRuleRepository가 동작하지 않음
```

**대응 방안**:
- 점진적 마이그레이션으로 테스트 단계별 확인
- 기존 테스트 실패 시 즉시 롤백 후 재검토
- 테스트 우선순위: 기존 동작 보장 > 새 기능 테스트

### 롤백 계획
각 Week 단위로 체크포인트 설정하여 문제 발생 시 이전 상태로 롤백 가능

---

## 🎯 성공 지표

### 정량적 지표
- [ ] **코드 라인 수**: 468줄 → 4개 서비스 합계 480줄 이하
- [ ] **클래스당 메서드 수**: 25개 → 8개 이하 (각 서비스)
- [ ] **의존성 개수**: 6개 → 3개 이하 (각 서비스)
- [ ] **테스트 커버리지**: 기존 수준 유지 (90% 이상)
- [ ] **빌드 시간**: 기존 대비 10% 이내 차이

### 정성적 지표
- [ ] **SRP 준수**: 각 클래스가 단일 책임만 담당
- [ ] **가독성 향상**: 메서드 이름으로 책임 명확히 구분
- [ ] **유지보수성**: 특정 기능 수정 시 해당 서비스만 변경
- [ ] **확장성**: 새 기능 추가 시 새 서비스 추가로 해결
- [ ] **테스트 격리**: 각 서비스별 독립적 테스트 가능

### 호환성 지표
- [ ] **API 호환성**: 모든 기존 API 엔드포인트 정상 동작
- [ ] **테스트 호환성**: 기존 테스트 코드 100% 통과
- [ ] **성능 호환성**: 기존 대비 성능 저하 없음
- [ ] **의존성 호환성**: 외부 모듈과의 연동 정상 동작

---

## 📝 결론

이 리팩토링 계획을 통해 AlertRuleService의 SRP 위반 문제를 해결하고, 코드 품질과 유지보수성을 대폭 향상시킬 수 있습니다.

### 핵심 성과
1. **468줄 → 4개 독립 서비스**: 75% 크기 감소
2. **5개 혼재 책임 → 단일 책임**: SRP 완전 준수
3. **기존 API 100% 호환**: 무중단 리팩토링
4. **테스트 격리**: 독립적 단위 테스트 가능

### 장기적 효과
- **개발 생산성 향상**: 병렬 개발 가능
- **버그 감소**: 명확한 책임 경계
- **확장성 증대**: 새 기능 추가 용이
- **코드 품질**: 높은 응집도, 낮은 결합도

---

## 📋 리뷰 반영 사항 (2025-09-23)

### 피드백 1: AlertServiceUtils 메서드 배치 재검토 ✅ 반영됨

**변경 사항**:
- `handleNotificationResult()` → AlertNotificationService의 private 메서드로 이동
- `validateAndGetMember()`와 `createDetectionDetailsJson()`만 AlertServiceUtils에 유지

**근거**:
- `handleNotificationResult`는 오직 알림 발송에서만 사용되는 특화 기능
- `validateAndGetMember`는 4개 서비스 모두에서 사용하는 진짜 공통 기능 (DRY 원칙)
- `createDetectionDetailsJson`는 AlertHistory, AlertNotification에서 공통 사용
- MARUNI 프로젝트는 이미 `global/` 패키지에 공통 유틸리티 패턴 사용중

### 피드백 2: 서비스 간 의존성 설계 검증 ✅ 현재 계획 유지

**제안된 대안**: AlertDetectionService → AlertRuleManagementService → AlertRuleRepository

**현재 계획 유지 이유**:

#### 1. DDD Layer 원칙 준수
- AlertDetectionService와 AlertRuleManagementService는 모두 **Application Service Layer**
- DDD에서 같은 레이어의 서비스끼리 의존하는 것은 안티패턴
- 각 Application Service는 Domain Repository에 직접 접근하는 것이 정석

#### 2. MARUNI 기존 패턴과 일치
- 다른 도메인들(Conversation, Guardian 등)에서 Application Service들이 서로 의존하지 않음
- 예: ConversationService가 MemberService에 의존하지 않고 MemberRepository 직접 사용

#### 3. 책임 분리 관점
- AlertDetectionService: "이상징후 감지를 위해 필요한 알림 규칙 조회"
- AlertRuleManagementService: "사용자의 알림 규칙 CRUD 관리"
- **둘 다 같은 AlertRule 엔티티를 다루지만 목적과 컨텍스트가 다름**

#### 4. 성능 및 복잡성
- 중간 서비스 계층 제거로 호출 스택 단순화
- 트랜잭션 전파 복잡성 제거
- 낮은 결합도 유지로 독립적 변경 가능

### 최종 설계 검증

**현재 리팩토링 계획은 다음 원칙들을 모두 만족**:
- ✅ **SRP 준수**: 각 서비스가 단일 책임만 담당
- ✅ **DDD 원칙**: Application Service → Domain Repository 직접 접근
- ✅ **기존 패턴 일치**: MARUNI 프로젝트의 기존 구조와 일관성
- ✅ **낮은 결합도**: 서비스 간 독립성 보장
- ✅ **높은 응집도**: 관련 기능들의 적절한 그룹핑

**이 계획은 MARUNI 프로젝트의 TDD + DDD 방법론을 완벽히 준수하면서도 기존 시스템의 안정성을 보장하는 실용적인 리팩토링 방안입니다.**