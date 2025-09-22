# 🛠️ AlertRule 도메인 리팩토링 플랜

## 📋 **현재 상태 분석**

### ✅ **우수한 부분**
- **TDD + DDD 완전 적용**: 테스트 커버리지와 도메인 모델링 우수
- **3종 감지 알고리즘**: EmotionPattern/NoResponse/Keyword 분석기 구현
- **정적 팩토리 메서드**: AlertRule, AlertCondition, AlertHistory 생성 패턴 일관성
- **비즈니스 로직 캡슐화**: AlertCondition.evaluate() 메서드의 switch expression 활용

### ⚠️ **개선 필요 영역**
1. **성능 문제**: N+1 쿼리, 대용량 데이터 처리 비효율
2. **하드코딩**: 임계값, 키워드, 메시지 템플릿 등 코드 내 고정
3. **확장성 부족**: 새로운 분석기 추가 시 코드 변경 필요
4. **더미 구현**: Controller 메서드들의 실제 구현 미완성
5. **에러 처리**: 도메인별 예외 처리 체계 부족

### 🔍 **발견된 구체적 문제점**

#### **Controller 계층 문제점**
- `AlertRuleController.getAlertRule()`: 더미 응답 반환
- `AlertRuleController.updateAlertRule()`: 실제 업데이트 없이 요청 데이터만 반환
- `AlertRuleController.deleteAlertRule()`: 빈 구현
- Service 계층에 대응하는 메서드들 누락

#### **성능 문제점**
- `AlertRuleRepository.findActiveRulesByMemberId()`: member.guardian 조회 시 N+1 발생
- `EmotionPatternAnalyzer.analyzeEmotionPattern()`: 대용량 메시지 처리 시 성능 저하
- 하드코딩된 임계값들: 코드 변경 없이 조정 불가

#### **확장성 문제점**
- 새로운 분석기 추가 시 `AlertRuleService.analyzeByRuleType()` 수정 필요
- 키워드 목록이 하드코딩되어 동적 관리 불가
- AlertType enum에 미사용 값들 존재 (HEALTH_CONCERN, EMERGENCY)

---

## 🎯 **Phase 1: 즉시 개선 (1-2주)**

### 1.1 **Controller 완성 (우선순위: 높음)**

#### **목표**
- AlertRuleController의 모든 더미 구현을 실제 구현으로 교체
- Service 계층에 누락된 메서드들 추가
- 일관된 에러 처리 체계 구축

#### **작업 내용**
1. **AlertRuleService 메서드 추가**
   ```java
   public AlertRule getAlertRuleById(Long alertRuleId)
   public AlertRule updateAlertRule(Long alertRuleId, String ruleName, String description, AlertLevel alertLevel)
   public void deleteAlertRule(Long alertRuleId)
   ```

2. **Controller 실제 구현**
   ```java
   // 더미 구현 제거
   @GetMapping("/{id}")
   public AlertRuleResponseDto getAlertRule(@PathVariable Long id) {
       AlertRule alertRule = alertRuleService.getAlertRuleById(id);
       return AlertRuleResponseDto.from(alertRule);
   }
   ```

3. **도메인 전용 예외 클래스**
   ```java
   public class AlertRuleNotFoundException extends BaseException
   public class AlertRuleAccessDeniedException extends BaseException
   public class InvalidAlertConditionException extends BaseException
   ```

### 1.2 **N+1 쿼리 해결 (우선순위: 높음)**

#### **문제점**
현재 `AlertRuleRepository.findActiveRulesByMemberId()`는 Guardian 정보가 필요할 때 추가 쿼리 발생

#### **해결 방안**
```java
@Query("SELECT ar FROM AlertRule ar " +
       "JOIN FETCH ar.member m " +
       "LEFT JOIN FETCH m.guardian " +
       "WHERE ar.member.id = :memberId AND ar.isActive = true")
List<AlertRule> findActiveRulesWithMemberAndGuardian(@Param("memberId") Long memberId);
```

### 1.3 **Configuration Properties 도입**

#### **현재 하드코딩된 값들**
```java
// EmotionPatternAnalyzer
private static final int HIGH_RISK_CONSECUTIVE_DAYS = 3;
private static final double HIGH_RISK_NEGATIVE_RATIO = 0.7;

// KeywordAnalyzer
private static final String[] EMERGENCY_KEYWORDS = {"도와주세요", "아파요", ...};

// AlertRuleService
private static final int DEFAULT_ANALYSIS_DAYS = 7;
```

#### **Configuration 클래스 도입**
```yaml
# application.yml
maruni:
  alert:
    analysis:
      default-days: 7
    emotion:
      high-risk-consecutive-days: 3
      high-risk-negative-ratio: 0.7
      medium-risk-consecutive-days: 2
      medium-risk-negative-ratio: 0.5
    keyword:
      emergency: ["도와주세요", "아파요", "응급실", "119"]
      warning: ["우울해", "외로워", "죽고싶어"]
    notification:
      title-template: "[MARUNI 알림] %s 단계 이상징후 감지"
```

```java
@ConfigurationProperties(prefix = "maruni.alert")
@Data
public class AlertConfigurationProperties {
    private Analysis analysis = new Analysis();
    private Emotion emotion = new Emotion();
    private Keyword keyword = new Keyword();
    private Notification notification = new Notification();

    @Data
    public static class Analysis {
        private int defaultDays = 7;
    }

    @Data
    public static class Emotion {
        private int highRiskConsecutiveDays = 3;
        private double highRiskNegativeRatio = 0.7;
        private int mediumRiskConsecutiveDays = 2;
        private double mediumRiskNegativeRatio = 0.5;
    }

    @Data
    public static class Keyword {
        private List<String> emergency = List.of("도와주세요", "아파요", "응급실", "119");
        private List<String> warning = List.of("우울해", "외로워", "죽고싶어");
    }

    @Data
    public static class Notification {
        private String titleTemplate = "[MARUNI 알림] %s 단계 이상징후 감지";
    }
}
```

---

## 🔧 **Phase 2: 구조 개선 (3-4주)**

### 2.1 **Strategy Pattern 적용**

#### **현재 문제점**
`AlertRuleService.analyzeByRuleType()`에서 switch문으로 분석기 선택, 확장성 부족

#### **개선 방안**
```java
public interface AnomalyAnalyzer {
    AlertResult analyze(MemberEntity member, AnalysisContext context);
    AlertType getSupportedType();
    boolean supports(AlertType alertType);
}

@Component
public class EmotionPatternAnalyzer implements AnomalyAnalyzer {
    @Override
    public AlertType getSupportedType() {
        return AlertType.EMOTION_PATTERN;
    }

    @Override
    public boolean supports(AlertType alertType) {
        return AlertType.EMOTION_PATTERN.equals(alertType);
    }
}

@Service
public class AlertAnalysisOrchestrator {
    private final Map<AlertType, AnomalyAnalyzer> analyzers;

    @Autowired
    public AlertAnalysisOrchestrator(List<AnomalyAnalyzer> analyzerList) {
        this.analyzers = analyzerList.stream()
            .collect(Collectors.toMap(
                AnomalyAnalyzer::getSupportedType,
                analyzer -> analyzer
            ));
    }

    public AlertResult analyze(AlertType alertType, MemberEntity member, AnalysisContext context) {
        AnomalyAnalyzer analyzer = analyzers.get(alertType);
        if (analyzer == null) {
            throw new UnsupportedAlertTypeException("지원하지 않는 알림 유형: " + alertType);
        }
        return analyzer.analyze(member, context);
    }
}
```

### 2.2 **Database Optimization**

#### **복합 인덱스 추가**
```sql
-- 회원별 활성 규칙 조회 최적화
CREATE INDEX idx_alert_rule_member_type_active
ON alert_rule(member_id, alert_type, is_active);

-- 알림 이력 조회 최적화
CREATE INDEX idx_alert_history_member_date
ON alert_history(member_id, alert_date);

-- 알림 레벨별 조회 최적화
CREATE INDEX idx_alert_rule_level_active
ON alert_rule(alert_level, is_active);
```

#### **JPQL 최적화**
```java
// 하드코딩된 ORDER BY 제거
@Query("SELECT ar FROM AlertRule ar " +
       "WHERE ar.member.id = :memberId AND ar.isActive = true " +
       "ORDER BY ar.alertLevel.priority DESC, ar.createdAt DESC")
List<AlertRule> findActiveRulesByMemberIdOrderedByPriority(@Param("memberId") Long memberId);
```

### 2.3 **Enum 정리**

#### **AlertType 미사용 값 제거**
```java
public enum AlertType {
    EMOTION_PATTERN("감정패턴", "연속적인 부정적 감정 감지"),
    NO_RESPONSE("무응답", "일정 기간 응답 없음"),
    KEYWORD_DETECTION("키워드감지", "위험 키워드 포함된 응답");
    // HEALTH_CONCERN, EMERGENCY 제거
}
```

#### **AlertLevel에 우선순위 추가**
```java
public enum AlertLevel {
    LOW("낮음", 1, "정보성 알림"),
    MEDIUM("보통", 2, "주의 관찰 필요"),
    HIGH("높음", 3, "빠른 확인 필요"),
    EMERGENCY("긴급", 4, "즉시 대응 필요");

    private final int priority; // 정렬용 우선순위 추가
}
```

---

## ⚡ **Phase 3: 성능 및 확장성 (5-6주)**

### 3.1 **비동기 처리 도입**

#### **대량 분석 작업 비동기화**
```java
@Service
public class AsyncAlertAnalysisService {

    @Async("alertAnalysisExecutor")
    public CompletableFuture<List<AlertResult>> analyzeBatch(List<Long> memberIds) {
        return CompletableFuture.supplyAsync(() ->
            memberIds.parallelStream()
                .map(this::analyzeMember)
                .collect(Collectors.toList())
        );
    }

    @EventListener
    @Async
    public void handleBulkAnalysisRequest(BulkAnalysisRequestEvent event) {
        // 대량 분석 요청 처리
    }
}

@Configuration
public class AsyncConfig {
    @Bean(name = "alertAnalysisExecutor")
    public Executor alertAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("alert-analysis-");
        executor.initialize();
        return executor;
    }
}
```

### 3.2 **캐싱 전략**

#### **자주 조회되는 데이터 캐싱**
```java
@Service
public class AlertRuleService {

    @Cacheable(value = "activeAlertRules", key = "#memberId")
    public List<AlertRule> getActiveRulesByMemberId(Long memberId) {
        return alertRuleRepository.findActiveRulesByMemberId(memberId);
    }

    @CacheEvict(value = "activeAlertRules", key = "#alertRule.member.id")
    public AlertRule save(AlertRule alertRule) {
        return alertRuleRepository.save(alertRule);
    }

    @Caching(evict = {
        @CacheEvict(value = "activeAlertRules", key = "#memberId"),
        @CacheEvict(value = "alertHistory", key = "#memberId")
    })
    public void evictMemberAlertCache(Long memberId) {
        // 회원별 알림 관련 캐시 무효화
    }
}
```

### 3.3 **이벤트 기반 아키텍처**

#### **도메인 이벤트 도입**
```java
// 도메인 이벤트
public class AlertTriggeredEvent {
    private final Long memberId;
    private final AlertResult alertResult;
    private final LocalDateTime triggeredAt;
    private final String source; // 어떤 분석기에서 발생했는지
}

public class AlertRuleCreatedEvent {
    private final Long alertRuleId;
    private final Long memberId;
    private final AlertType alertType;
}

// 이벤트 발행
@Service
public class AlertRuleService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long triggerAlert(Long memberId, AlertResult alertResult) {
        // 기존 로직...

        // 이벤트 발행
        eventPublisher.publishEvent(new AlertTriggeredEvent(memberId, alertResult, LocalDateTime.now(), "system"));

        return savedHistory.getId();
    }
}

// 이벤트 리스너
@Component
public class AlertEventListener {

    @EventListener
    @Async
    public void handleAlertTriggered(AlertTriggeredEvent event) {
        // 알림 발송 처리를 이벤트로 분리
    }

    @EventListener
    public void handleAlertRuleCreated(AlertRuleCreatedEvent event) {
        // 새 규칙 생성 시 관련 처리
    }
}
```

---

## 📊 **Phase 4: 모니터링 및 메트릭 (7-8주)**

### 4.1 **운영 메트릭 추가**

#### **Micrometer를 활용한 메트릭 수집**
```java
@Component
public class AlertMetricsCollector {
    private final MeterRegistry meterRegistry;
    private final Counter alertDetectionCounter;
    private final Timer analysisTimer;
    private final Gauge activeRulesGauge;

    public AlertMetricsCollector(MeterRegistry meterRegistry, AlertRuleRepository alertRuleRepository) {
        this.meterRegistry = meterRegistry;
        this.alertDetectionCounter = Counter.builder("alert.detection")
            .description("Number of alerts detected")
            .tag("type", "total")
            .register(meterRegistry);

        this.analysisTimer = Timer.builder("alert.analysis.duration")
            .description("Time taken for alert analysis")
            .register(meterRegistry);

        this.activeRulesGauge = Gauge.builder("alert.rules.active")
            .description("Number of active alert rules")
            .register(meterRegistry, alertRuleRepository, repo -> repo.count());
    }

    public void recordAlertDetection(AlertType type, AlertLevel level) {
        Counter.builder("alert.detection")
            .tag("type", type.name())
            .tag("level", level.name())
            .register(meterRegistry)
            .increment();
    }

    public void recordAnalysisTime(AlertType type, Duration duration) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("alert.analysis.duration")
            .tag("type", type.name())
            .register(meterRegistry));
    }

    public void recordNotificationSuccess(boolean success) {
        Counter.builder("alert.notification")
            .tag("result", success ? "success" : "failure")
            .register(meterRegistry)
            .increment();
    }
}
```

### 4.2 **Health Check 강화**

#### **AlertRule 시스템 상태 모니터링**
```java
@Component
public class AlertSystemHealthIndicator implements HealthIndicator {

    private final AlertRuleRepository alertRuleRepository;
    private final List<AnomalyAnalyzer> analyzers;
    private final NotificationService notificationService;

    @Override
    public Health health() {
        Health.Builder status = Health.up();

        try {
            // 1. 데이터베이스 연결 상태
            long totalRules = alertRuleRepository.count();
            status.withDetail("total_rules", totalRules);

            // 2. 분석기별 상태 체크
            Map<String, String> analyzerStatus = new HashMap<>();
            for (AnomalyAnalyzer analyzer : analyzers) {
                try {
                    // 간단한 더미 분석으로 상태 확인
                    analyzerStatus.put(analyzer.getSupportedType().name(), "UP");
                } catch (Exception e) {
                    analyzerStatus.put(analyzer.getSupportedType().name(), "DOWN: " + e.getMessage());
                    status.down();
                }
            }
            status.withDetail("analyzers", analyzerStatus);

            // 3. 알림 시스템 상태
            try {
                // NotificationService 상태 확인
                status.withDetail("notification_service", "UP");
            } catch (Exception e) {
                status.withDetail("notification_service", "DOWN: " + e.getMessage());
                status.down();
            }

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }

        return status.build();
    }
}
```

---

## 🔍 **상세 실행 계획**

### **Week 1-2: 기반 정리**
1. **Day 1-3: Controller 더미 구현 제거**
   - AlertRuleService에 누락된 메서드 추가
   - Controller의 실제 구현 완성
   - 통합 테스트 작성

2. **Day 4-5: Configuration Properties 도입**
   - AlertConfigurationProperties 클래스 생성
   - 기존 하드코딩된 상수들을 Properties로 이관
   - 테스트 케이스 업데이트

3. **Day 6-7: N+1 쿼리 해결**
   - Repository에 JOIN FETCH 쿼리 추가
   - 성능 테스트로 개선 효과 검증

4. **Day 8-10: 도메인 예외 체계 구축**
   - AlertRule 전용 예외 클래스들 생성
   - GlobalExceptionHandler에 예외 처리 추가
   - 에러 응답 표준화

### **Week 3-4: 구조 개선**
1. **Day 1-5: Strategy Pattern 적용**
   - AnomalyAnalyzer 인터페이스 정의
   - 기존 분석기들을 인터페이스 구현으로 변경
   - AlertAnalysisOrchestrator 생성

2. **Day 6-7: Database 인덱스 최적화**
   - 복합 인덱스 추가
   - 쿼리 성능 측정 및 튜닝

3. **Day 8-10: Enum 정리 및 리팩토링**
   - AlertType 미사용 값 제거
   - AlertLevel에 우선순위 필드 추가
   - 관련 테스트 케이스 수정

### **Week 5-6: 성능 최적화**
1. **Day 1-4: 비동기 처리 도입**
   - AsyncAlertAnalysisService 구현
   - ThreadPool 설정 최적화
   - 부하 테스트 실행

2. **Day 5-7: 캐싱 전략 적용**
   - Redis 캐시 설정
   - 자주 조회되는 데이터 캐싱
   - 캐시 무효화 전략 구현

3. **Day 8-10: 이벤트 기반 아키텍처**
   - 도메인 이벤트 정의
   - 이벤트 발행/구독 구조 구축
   - 비동기 이벤트 처리

### **Week 7-8: 운영 준비**
1. **Day 1-3: 메트릭 수집 시스템**
   - AlertMetricsCollector 구현
   - Micrometer 메트릭 설정
   - Grafana 대시보드 준비

2. **Day 4-5: Health Check 강화**
   - AlertSystemHealthIndicator 구현
   - 장애 감지 및 알림 체계

3. **Day 6-8: 성능 테스트 및 튜닝**
   - JMeter를 활용한 부하 테스트
   - 병목 지점 식별 및 개선
   - 최종 성능 검증

4. **Day 9-10: 문서화 업데이트**
   - API 문서 업데이트
   - 운영 가이드 작성
   - 마이그레이션 가이드 작성

---

## 📋 **리스크 관리**

### **기존 기능 영향도 최소화**
- **Phase별 점진적 적용**: 한 번에 모든 변경사항 적용 금지
- **Backward Compatibility**: 기존 API 호환성 유지
- **Feature Toggle**: 새로운 기능들을 점진적으로 활성화

### **테스트 전략**
- **기존 테스트 케이스 유지**: 리팩토링 시 기능 회귀 방지
- **성능 테스트 추가**: 대용량 데이터 처리 검증
- **통합 테스트 강화**: 도메인 간 연동 검증

### **모니터링 계획**
- **메트릭 기반 성능 추적**: 리팩토링 전후 성능 비교
- **에러율 모니터링**: 예외 발생 패턴 추적
- **사용자 영향도 측정**: API 응답 시간, 처리량 모니터링

### **롤백 계획**
- **데이터베이스 마이그레이션**: 롤백 스크립트 준비
- **코드 변경사항**: Git 브랜치 전략으로 빠른 롤백 지원
- **설정 변경사항**: 기존 설정값 백업 및 복원 절차

---

## 🎯 **성공 지표 (KPI)**

### **성능 개선 목표**
- **API 응답 시간**: 95th percentile 500ms 이하
- **대량 분석 처리량**: 시간당 10,000건 이상 처리
- **데이터베이스 쿼리**: N+1 문제 완전 해결

### **코드 품질 목표**
- **테스트 커버리지**: 90% 이상 유지
- **사이클로매틱 복잡도**: 평균 5 이하
- **중복 코드**: 5% 이하

### **운영 안정성 목표**
- **가용성**: 99.9% 이상
- **에러율**: 0.1% 이하
- **평균 복구 시간**: 15분 이하

---

**🎯 최종 목표: AlertRule 도메인을 현재의 100% 완성 상태에서 → 확장 가능하고 성능 최적화된 엔터프라이즈급 도메인으로 발전시키기**

## 📚 **참고 문서**
- [AlertRule 도메인 구현 가이드](../domains/alertrule.md)
- [MARUNI 프로젝트 아키텍처](../README.md)
- [코딩 표준](../specifications/coding-standards.md)
- [성능 최적화 가이드](../specifications/performance-guide.md)