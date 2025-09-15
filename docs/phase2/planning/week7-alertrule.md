# Week 7: AlertRule 도메인 TDD 개발 계획 (2025-09-15)

## 🎯 Week 7 목표: Phase 2 MVP 완성

**AlertRule(이상징후 감지) 도메인 완전 구현**을 통해 MARUNI의 **Phase 2 MVP를 100% 완성**합니다.
AI 분석 기반 실시간 이상징후 감지 및 보호자 자동 알림 시스템으로 **실제 운영 가능한 노인 돌봄 서비스**를 완성합니다.

### 📋 주요 구현 사항
- **실시간 이상징후 감지**: AI 감정 분석 + 패턴 분석 기반 위험 상황 판단
- **자동 보호자 알림**: Guardian 시스템 연동을 통한 즉시 알림 발송
- **이력 추적 시스템**: 모든 감지 및 알림 이력 완전 `기록`
- **다단계 알림 레벨**: 경고/주의/위험/긴급 4단계 알림 시스템
- **완전한 TDD 적용**: Red-Green-Refactor 완전 사이클 구현

## 📅 Week 7 상세 일정 계획

### 🔴 **Day 1-2: Red 단계** - 실패하는 테스트 작성
#### 📅 Day 1 (2025-09-16): AlertRule 도메인 설계 및 테스트 작성
- [ ] **AlertRule 엔티티 설계**: AlertRule, AlertHistory, AlertCondition
- [ ] **이상징후 감지 로직 설계**: AI 분석 연동 알고리즘
- [ ] **DDD 패키지 구조**: Domain/Application/Infrastructure 계층 완성
- [ ] **10개 테스트 시나리오 작성**: Entity(3개), Repository(3개), Service(4개)
- [ ] **더미 구현**: 컴파일 성공하되 모든 테스트 실패하는 Perfect Red State

#### 📅 Day 2 (2025-09-17): 통합 테스트 시나리오 및 연동 설계
- [ ] **도메인 간 연동 테스트**: Conversation, Guardian, DailyCheck 연동
- [ ] **실시간 감지 플로우 테스트**: 전체 비즈니스 플로우 검증
- [ ] **알림 발송 시스템 테스트**: Guardian 시스템과의 완전 통합
- [ ] **기존 테스트 보호**: 60+ 기존 테스트 모두 정상 동작 확인

### 🟢 **Day 3-4: Green 단계** - 최소 구현으로 테스트 통과
#### 📅 Day 3 (2025-09-18): 핵심 비즈니스 로직 구현
- [ ] **AlertRuleService 구현**: 이상징후 감지 핵심 로직
- [ ] **AlertCondition 판정 로직**: 감정 분석 + 패턴 분석 알고리즘
- [ ] **AlertHistory 기록 시스템**: 모든 감지 이력 저장
- [ ] **5개 핵심 테스트 통과**: 주요 비즈니스 로직 검증

#### 📅 Day 4 (2025-09-19): 도메인 간 연동 구현
- [ ] **Conversation 연동**: AI 감정 분석 결과 실시간 수신
- [ ] **Guardian 연동**: 보호자 알림 발송 시스템 구현
- [ ] **DailyCheck 연동**: 무응답 패턴 감지 연동
- [ ] **10개 테스트 모두 통과**: Green 단계 완전 달성

### 🔵 **Day 5-6: Refactor 단계** - 코드 품질 향상
#### 📅 Day 5 (2025-09-20): 체계적 리팩토링
- [ ] **하드코딩 제거**: 임계값, 메시지 등 상수화
- [ ] **중복 로직 추출**: 공통 감지 로직 메서드 분리
- [ ] **예외 처리 강화**: 커스텀 예외 및 ErrorCode 확장
- [ ] **성능 최적화**: 감지 알고리즘 효율성 개선

#### 📅 Day 6 (2025-09-21): Controller 및 API 구현
- [ ] **AlertRuleController REST API**: 7개 엔드포인트 구현
- [ ] **DTO 계층 완성**: Request/Response DTO + Bean Validation
- [ ] **Swagger API 문서화**: 완전한 API 문서화
- [ ] **통합 테스트**: 전체 API 동작 검증

### 🎉 **Day 7: Phase 2 MVP 완성**
#### 📅 Day 7 (2025-09-22): 최종 통합 및 MVP 검증
- [ ] **전체 시스템 통합 테스트**: End-to-End 플로우 검증
- [ ] **Phase 2 완성도 검증**: 모든 요구사항 충족 확인
- [ ] **문서 업데이트**: 완료 보고서 및 CLAUDE.md 업데이트
- [ ] **Phase 3 준비**: 다음 단계 계획 수립

## 🏗️ AlertRule 도메인 아키텍처 설계

### DDD 구조
```
com.anyang.maruni.domain.alertrule/
├── application/                 # Application Layer
│   ├── dto/                    # Request/Response DTO
│   │   ├── AlertRuleRequestDto.java
│   │   ├── AlertRuleResponseDto.java
│   │   ├── AlertHistoryResponseDto.java
│   │   └── AlertConditionDto.java
│   └── service/                # Application Service
│       ├── AlertRuleService.java
│       └── AlertDetectionService.java
├── domain/                     # Domain Layer
│   ├── entity/                 # Domain Entity
│   │   ├── AlertRule.java
│   │   ├── AlertHistory.java
│   │   ├── AlertCondition.java
│   │   └── AlertType.java (Enum)
│   └── repository/             # Repository Interface
│       ├── AlertRuleRepository.java
│       └── AlertHistoryRepository.java
└── presentation/               # Presentation Layer
    └── controller/             # REST API Controller
        └── AlertRuleController.java
```

### 핵심 엔티티 설계

#### AlertRule Entity
```java
@Entity
@Table(name = "alert_rule")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType; // EMOTION_PATTERN, NO_RESPONSE, KEYWORD_DETECTION

    @Column(nullable = false)
    private String ruleName;

    @Column(columnDefinition = "TEXT")
    private String ruleDescription;

    @Embedded
    private AlertCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel; // LOW, MEDIUM, HIGH, EMERGENCY

    @Column(nullable = false)
    private Boolean isActive = true;

    // 정적 팩토리 메서드
    public static AlertRule createEmotionPatternRule(
        MemberEntity member, int consecutiveDays, AlertLevel alertLevel) {
        return AlertRule.builder()
            .member(member)
            .alertType(AlertType.EMOTION_PATTERN)
            .ruleName("연속 부정감정 감지")
            .ruleDescription(consecutiveDays + "일 연속 부정적 감정 감지 시 알림")
            .condition(AlertCondition.createEmotionCondition(consecutiveDays))
            .alertLevel(alertLevel)
            .isActive(true)
            .build();
    }

    public static AlertRule createNoResponseRule(
        MemberEntity member, int noResponseDays, AlertLevel alertLevel) {
        return AlertRule.builder()
            .member(member)
            .alertType(AlertType.NO_RESPONSE)
            .ruleName("무응답 감지")
            .ruleDescription(noResponseDays + "일 연속 무응답 시 알림")
            .condition(AlertCondition.createNoResponseCondition(noResponseDays))
            .alertLevel(alertLevel)
            .isActive(true)
            .build();
    }

    // 비즈니스 로직
    public boolean shouldTriggerAlert(List<MessageEntity> recentMessages) {
        return condition.evaluate(recentMessages, alertType);
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }
}
```

#### AlertCondition Embedded Entity
```java
@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertCondition {

    @Column(name = "consecutive_days")
    private Integer consecutiveDays;

    @Column(name = "threshold_count")
    private Integer thresholdCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_emotion")
    private EmotionType targetEmotion;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords; // JSON 형태로 저장

    // 정적 팩토리 메서드
    public static AlertCondition createEmotionCondition(int consecutiveDays) {
        return AlertCondition.builder()
            .consecutiveDays(consecutiveDays)
            .targetEmotion(EmotionType.NEGATIVE)
            .thresholdCount(1)
            .build();
    }

    public static AlertCondition createNoResponseCondition(int noResponseDays) {
        return AlertCondition.builder()
            .consecutiveDays(noResponseDays)
            .thresholdCount(0)
            .build();
    }

    // 비즈니스 로직: 조건 평가
    public boolean evaluate(List<MessageEntity> recentMessages, AlertType alertType) {
        switch (alertType) {
            case EMOTION_PATTERN:
                return evaluateEmotionPattern(recentMessages);
            case NO_RESPONSE:
                return evaluateNoResponsePattern(recentMessages);
            case KEYWORD_DETECTION:
                return evaluateKeywordPattern(recentMessages);
            default:
                return false;
        }
    }

    private boolean evaluateEmotionPattern(List<MessageEntity> messages) {
        // 연속적인 부정적 감정 패턴 감지 로직
        int consecutiveNegativeDays = 0;
        for (MessageEntity message : messages) {
            if (message.getEmotion() == EmotionType.NEGATIVE) {
                consecutiveNegativeDays++;
                if (consecutiveNegativeDays >= this.consecutiveDays) {
                    return true;
                }
            } else {
                consecutiveNegativeDays = 0;
            }
        }
        return false;
    }

    private boolean evaluateNoResponsePattern(List<MessageEntity> messages) {
        // 무응답 패턴 감지 로직 (DailyCheck 기록과 연동)
        return messages.size() < this.consecutiveDays;
    }

    private boolean evaluateKeywordPattern(List<MessageEntity> messages) {
        // 위험 키워드 감지 로직
        if (keywords == null) return false;
        String[] keywordArray = keywords.split(",");

        return messages.stream()
            .anyMatch(message ->
                Arrays.stream(keywordArray)
                    .anyMatch(keyword ->
                        message.getContent().toLowerCase()
                            .contains(keyword.trim().toLowerCase())
                    )
            );
    }
}
```

#### AlertHistory Entity
```java
@Entity
@Table(name = "alert_history")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String alertMessage;

    @Column(columnDefinition = "TEXT")
    private String detectionDetails; // JSON 형태로 감지 상세 정보 저장

    @Column(nullable = false)
    private Boolean isNotificationSent = false;

    @Column
    private LocalDateTime notificationSentAt;

    @Column(columnDefinition = "TEXT")
    private String notificationResult; // 알림 발송 결과

    // 정적 팩토리 메서드
    public static AlertHistory createAlert(
        AlertRule alertRule, MemberEntity member, String alertMessage, String detectionDetails) {
        return AlertHistory.builder()
            .alertRule(alertRule)
            .member(member)
            .alertLevel(alertRule.getAlertLevel())
            .alertMessage(alertMessage)
            .detectionDetails(detectionDetails)
            .isNotificationSent(false)
            .build();
    }

    // 비즈니스 로직
    public void markNotificationSent(String result) {
        this.isNotificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
        this.notificationResult = result;
    }
}
```

### Enum 정의

#### AlertType
```java
public enum AlertType {
    EMOTION_PATTERN("감정패턴", "연속적인 부정적 감정 감지"),
    NO_RESPONSE("무응답", "일정 기간 응답 없음"),
    KEYWORD_DETECTION("키워드감지", "위험 키워드 포함된 응답"),
    HEALTH_CONCERN("건강우려", "건강 관련 우려사항 감지"),
    EMERGENCY("긴급상황", "즉시 대응이 필요한 상황");

    private final String displayName;
    private final String description;

    AlertType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
```

#### AlertLevel (Guardian에서 가져와서 공통 사용)
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

## 🧠 이상징후 감지 알고리즘 설계

### 핵심 감지 로직

#### 1. 감정 패턴 분석
```java
public class EmotionPatternAnalyzer {

    public AlertResult analyzeEmotionPattern(MemberEntity member, int analysisDays) {
        // 최근 N일간 메시지 조회
        List<MessageEntity> recentMessages = messageRepository
            .findRecentUserMessagesByMemberId(member.getId(), analysisDays);

        // 감정 패턴 분석
        EmotionTrend trend = calculateEmotionTrend(recentMessages);

        // 위험도 판정
        if (trend.getConsecutiveNegativeDays() >= 3) {
            return AlertResult.createAlert(AlertLevel.HIGH,
                "3일 연속 부정적 감정 감지", trend);
        }

        if (trend.getNegativeRatio() > 0.7) {
            return AlertResult.createAlert(AlertLevel.MEDIUM,
                "최근 부정적 감정 비율 70% 초과", trend);
        }

        return AlertResult.noAlert();
    }

    private EmotionTrend calculateEmotionTrend(List<MessageEntity> messages) {
        // 감정 추세 계산 로직
        int positiveCount = 0, negativeCount = 0, neutralCount = 0;
        int consecutiveNegative = 0, maxConsecutiveNegative = 0;

        for (MessageEntity message : messages) {
            switch (message.getEmotion()) {
                case POSITIVE -> {
                    positiveCount++;
                    consecutiveNegative = 0;
                }
                case NEGATIVE -> {
                    negativeCount++;
                    consecutiveNegative++;
                    maxConsecutiveNegative = Math.max(maxConsecutiveNegative, consecutiveNegative);
                }
                case NEUTRAL -> {
                    neutralCount++;
                    consecutiveNegative = 0;
                }
            }
        }

        return EmotionTrend.builder()
            .totalMessages(messages.size())
            .positiveCount(positiveCount)
            .negativeCount(negativeCount)
            .neutralCount(neutralCount)
            .consecutiveNegativeDays(maxConsecutiveNegative)
            .negativeRatio((double) negativeCount / messages.size())
            .build();
    }
}
```

#### 2. 무응답 패턴 감지
```java
public class NoResponseAnalyzer {

    public AlertResult analyzeNoResponsePattern(MemberEntity member, int analysisDays) {
        // DailyCheck 기록 조회
        List<DailyCheckRecord> recentChecks = dailyCheckRepository
            .findRecentRecordsByMemberId(member.getId(), analysisDays);

        // 응답 패턴 분석
        ResponsePattern pattern = calculateResponsePattern(recentChecks);

        // 위험도 판정
        if (pattern.getConsecutiveNoResponseDays() >= 2) {
            return AlertResult.createAlert(AlertLevel.EMERGENCY,
                "2일 연속 무응답", pattern);
        }

        if (pattern.getResponseRate() < 0.3) {
            return AlertResult.createAlert(AlertLevel.HIGH,
                "응답률 30% 미만", pattern);
        }

        return AlertResult.noAlert();
    }
}
```

#### 3. 키워드 기반 감지
```java
public class KeywordAnalyzer {

    private static final String[] EMERGENCY_KEYWORDS = {
        "도와주세요", "아파요", "숨이", "가슴이", "쓰러짐", "응급실", "119"
    };

    private static final String[] WARNING_KEYWORDS = {
        "우울해", "외로워", "죽고싶어", "포기", "희망없어", "의미없어"
    };

    public AlertResult analyzeKeywordRisk(MessageEntity message) {
        String content = message.getContent().toLowerCase();

        // 긴급 키워드 감지
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (content.contains(keyword)) {
                return AlertResult.createAlert(AlertLevel.EMERGENCY,
                    "긴급 키워드 감지: " + keyword,
                    KeywordMatch.emergency(keyword, content));
            }
        }

        // 경고 키워드 감지
        for (String keyword : WARNING_KEYWORDS) {
            if (content.contains(keyword)) {
                return AlertResult.createAlert(AlertLevel.HIGH,
                    "위험 키워드 감지: " + keyword,
                    KeywordMatch.warning(keyword, content));
            }
        }

        return AlertResult.noAlert();
    }
}
```

## 🧪 TDD 테스트 시나리오 (총 12개)

### AlertRule Entity & Repository 테스트 (4개)
1. **AlertRule 생성 테스트**: `createEmotionPatternRule_shouldCreateValidRule`
2. **AlertCondition 평가 테스트**: `evaluateEmotionPattern_shouldDetectConsecutiveNegative`
3. **활성 AlertRule 조회 테스트**: `findActiveRulesByMemberId_shouldReturnActiveRules`
4. **AlertRule 활성화/비활성화 테스트**: `activateDeactivateRule_shouldToggleStatus`

### AlertHistory Entity & Repository 테스트 (3개)
5. **AlertHistory 생성 테스트**: `createAlert_shouldCreateValidHistory`
6. **알림 발송 마킹 테스트**: `markNotificationSent_shouldUpdateStatus`
7. **회원별 알림 이력 조회**: `findAlertHistoryByMemberId_shouldReturnHistory`

### AlertRuleService 테스트 (5개)
8. **실시간 감지 테스트**: `detectAnomalies_shouldDetectEmotionPattern`
9. **무응답 감지 테스트**: `detectAnomalies_shouldDetectNoResponse`
10. **키워드 감지 테스트**: `detectAnomalies_shouldDetectEmergencyKeywords`
11. **보호자 알림 발송 테스트**: `sendGuardianNotification_shouldNotifyAllGuardians`
12. **알림 이력 기록 테스트**: `recordAlertHistory_shouldSaveCompleteHistory`

## 🔗 도메인 간 연동 설계

### 1. Conversation 도메인 연동
```java
@Service
@RequiredArgsConstructor
public class AlertDetectionService {

    private final ConversationRepository conversationRepository;
    private final AlertRuleService alertRuleService;

    @EventListener
    public void handleNewMessage(MessageCreatedEvent event) {
        // 새 메시지 생성 시 실시간 감지
        MessageEntity message = event.getMessage();

        // 즉시 키워드 감지 (긴급상황)
        AlertResult keywordResult = keywordAnalyzer.analyzeKeywordRisk(message);
        if (keywordResult.isAlert()) {
            alertRuleService.triggerAlert(message.getMemberId(), keywordResult);
        }

        // 감정 패턴 분석 (일일 분석)
        if (isEndOfDay()) {
            AlertResult emotionResult = emotionAnalyzer.analyzeEmotionPattern(
                message.getMember(), 7);
            if (emotionResult.isAlert()) {
                alertRuleService.triggerAlert(message.getMemberId(), emotionResult);
            }
        }
    }
}
```

### 2. Guardian 도메인 연동
```java
@Service
@RequiredArgsConstructor
public class GuardianNotificationService {

    private final GuardianService guardianService;
    private final NotificationService notificationService;

    public void notifyGuardians(Long memberId, AlertLevel alertLevel, String alertMessage) {
        // 회원의 모든 보호자 조회
        List<GuardianEntity> guardians = guardianService.getGuardiansByMemberId(memberId);

        // 알림 레벨에 따른 보호자 필터링
        List<GuardianEntity> targetGuardians = guardians.stream()
            .filter(guardian -> shouldNotifyGuardian(guardian, alertLevel))
            .toList();

        // 보호자별 맞춤 알림 발송
        for (GuardianEntity guardian : targetGuardians) {
            sendPersonalizedAlert(guardian, alertLevel, alertMessage);
        }
    }

    private void sendPersonalizedAlert(GuardianEntity guardian, AlertLevel alertLevel, String message) {
        String personalizedMessage = String.format(
            "[MARUNI %s] %s님의 %s 상황이 감지되었습니다.\n%s",
            alertLevel.getDisplayName(),
            guardian.getMember().getMemberName(),
            alertLevel.getDescription(),
            message
        );

        // 보호자 알림 설정에 따른 다중 채널 발송
        switch (guardian.getNotificationPreference()) {
            case PUSH -> notificationService.sendPushNotification(guardian.getId(), personalizedMessage);
            case EMAIL -> notificationService.sendEmail(guardian.getGuardianEmail(), personalizedMessage);
            case ALL -> {
                notificationService.sendPushNotification(guardian.getId(), personalizedMessage);
                notificationService.sendEmail(guardian.getGuardianEmail(), personalizedMessage);
            }
        }
    }
}
```

### 3. DailyCheck 도메인 연동
```java
@Component
@RequiredArgsConstructor
public class ScheduledAlertDetection {

    private final DailyCheckService dailyCheckService;
    private final AlertDetectionService alertDetectionService;

    @Scheduled(cron = "0 0 22 * * *") // 매일 밤 10시 종합 분석
    public void performDailyAnalysis() {
        List<MemberEntity> allActiveMembers = memberRepository.findAllActiveMembers();

        for (MemberEntity member : allActiveMembers) {
            // 무응답 패턴 분석
            analyzeNoResponsePattern(member);

            // 감정 패턴 분석
            analyzeEmotionPattern(member);

            // 종합 위험도 평가
            evaluateOverallRisk(member);
        }
    }

    private void analyzeNoResponsePattern(MemberEntity member) {
        // 최근 DailyCheck 기록 분석
        List<DailyCheckRecord> recentChecks = dailyCheckRepository
            .findRecentRecordsByMemberId(member.getId(), 7);

        NoResponseAnalyzer analyzer = new NoResponseAnalyzer();
        AlertResult result = analyzer.analyzeNoResponsePattern(member, 7);

        if (result.isAlert()) {
            alertRuleService.triggerAlert(member.getId(), result);
        }
    }
}
```

## 📊 Phase 2 MVP 완성 목표

### 🎯 완성 시 달성 상태
```yaml
✅ Phase 2 MVP: 100% 완료
✅ AlertRule 도메인: 100% TDD 구현
✅ 실시간 감지 시스템: 완전 자동화
✅ 보호자 알림 시스템: 다중 채널 지원
✅ 전체 테스트 커버리지: 95% 이상
✅ 실제 운영 준비: 완료

비즈니스 완성도:
- 🚀 완전 자동화된 24/7 돌봄 서비스
- ⚡ 실시간 이상징후 감지 및 대응
- 📱 즉시 보호자 알림 발송
- 📊 완전한 이력 추적 및 분석
- 🎯 Phase 3 확장 준비 완료
```

### 🏆 **MVP 핵심 가치 제공**
1. **예방적 돌봄**: 문제 발생 전 조기 감지
2. **즉시 대응**: 이상 상황 실시간 알림
3. **가족 안심**: 보호자의 걱정 해소
4. **지속적 모니터링**: 24/7 건강 상태 추적

### 🚀 **Week 7 완료 후 MARUNI 서비스 수준**
- **실제 운영 가능**: 상용 서비스 수준 달성
- **확장성 확보**: Phase 3 고도화 기반 마련
- **시장 출시 준비**: MVP로 시장 검증 가능
- **투자 유치 준비**: 완성된 프로토타입으로 투자 어필

**MARUNI Phase 2 MVP가 완성되면, 실제 노인 돌봄 시장에 출시 가능한 완전한 서비스가 됩니다!** 🎉

## 📝 완료 후 문서 업데이트 계획

### Week 7 완료 시 업데이트 문서
- **[Week 7 완료 보고서](../completed/week7-alertrule-report.md)**: TDD 완전 사이클 기록
- **[Phase 2 MVP 완성 보고서](../completed/phase2-mvp-completion.md)**: 전체 성과 요약
- **[CLAUDE.md](../../../CLAUDE.md)**: 프로젝트 현황 업데이트
- **[Phase 3 계획서](../../phase3/README.md)**: 다음 단계 준비

**AlertRule 도메인으로 MARUNI의 핵심 가치인 '실시간 이상징후 감지 및 보호자 알림'을 완성합니다!** 🚀