# DailyCheck 메시지 다양화 계획서

**작성일**: 2025-11-09
**목적**: 안부 메시지를 요일별 + 계절별로 다양화하여 사용자 경험 개선

---

## 📋 1. 현재 상황 분석

### 현재 구조
```java
// DailyCheckOrchestrator.java (Line 33-34)
private static final String DAILY_CHECK_TITLE = "안부 메시지";
private static final String DAILY_CHECK_MESSAGE = "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?";
```

### 문제점
- **단일 메시지**: 모든 날, 모든 계절에 동일한 메시지
- **사용자 피로도**: 반복되는 메시지로 인한 흥미 저하
- **맥락 부재**: 요일, 계절, 날씨 등 맥락이 반영되지 않음

---

## 🎯 2. 개선 목표

### 핵심 요구사항
1. **요일별 메시지**: 월요일~일요일 각각 특화된 메시지
2. **계절별 메시지**: 봄/여름/가을/겨울 계절감 있는 메시지
3. **조합 방식**: 요일 + 계절을 자연스럽게 조합

### 비기능 요구사항
- **테스트 가능성**: 메시지 생성 로직을 독립적으로 테스트 가능
- **확장성**: 추후 시간대, 날씨, 특별일 등 추가 가능한 구조
- **유지보수성**: 메시지 추가/수정이 용이한 구조
- **기존 호환성**: 기존 DailyCheck 시스템과 완전 호환

---

## 🏗️ 3. 설계 방향

### 3.1 핵심 설계 원칙

#### A. 단일 책임 원칙 (SRP)
```
DailyCheckMessageProvider: 메시지 생성만 담당
  ├── 요일 판별
  ├── 계절 판별
  └── 메시지 조합

DailyCheckOrchestrator: 비즈니스 로직 조정만 담당
  └── MessageProvider에 메시지 생성 위임
```

#### B. 의존성 주입
```java
@Service
public class DailyCheckOrchestrator {
    private final DailyCheckMessageProvider messageProvider; // 주입

    public void processMemberDailyCheck(Long memberId) {
        String message = messageProvider.generateMessage(); // 위임
        // ...
    }
}
```

#### C. 테스트 가능성
- **순수 함수**: 동일한 입력(날짜, 시간)에 대해 동일한 출력
- **시간 주입**: LocalDate/LocalDateTime을 파라미터로 받아 테스트 용이
- **상태 없음**: 메시지 풀은 불변 상수로 관리

### 3.2 메시지 구조 설계

#### 메시지 조합 방식
```
최종 메시지 = 요일 인사 + 계절 메시지 조합

예시:
- 월요일 + 봄: "새로운 한 주가 시작됐어요! 따뜻한 봄날, 기분 좋게 시작해보세요."
- 금요일 + 겨울: "이번 주도 수고하셨어요. 추운 날씨에 건강 조심하세요!"
- 일요일 + 여름: "편안한 일요일 보내세요. 더위에 시원하게 지내시길 바랍니다."
```

#### 메시지 풀 설계

**요일별 메시지 (7종 × 3-4개 = 약 25개)**
```
월요일: 새로운 한 주 시작, 활기찬 시작 등
화~목요일: 일상적인 안부, 중간 점검 등
금요일: 한 주 마무리, 주말 기대 등
토요일: 여유로운 휴식, 자유로운 시간 등
일요일: 편안한 휴식, 다음 주 준비 등
```

**계절별 메시지 (4종 × 4-5개 = 약 20개)**
```
봄(3-5월): 따뜻함, 꽃, 새 시작, 산책 등
여름(6-8월): 더위, 시원함, 휴가, 건강 주의 등
가을(9-11월): 선선함, 단풍, 독서, 건강 관리 등
겨울(12-2월): 추위, 따뜻함, 건강, 실내 활동 등
```

### 3.3 패키지 구조

```
domain/dailycheck/
├── application/
│   ├── scheduler/
│   │   ├── DailyCheckScheduler.java          # 스케줄링 트리거 (기존)
│   │   ├── DailyCheckOrchestrator.java       # 비즈니스 로직 (수정)
│   │   └── RetryService.java                 # 재시도 관리 (기존)
│   └── service/
│       └── DailyCheckMessageProvider.java    # 메시지 생성 (신규) ⭐
└── domain/
    ├── entity/                                # 기존 유지
    └── repository/                            # 기존 유지
```

---

## 📝 4. 구현 상세

### 4.1 DailyCheckMessageProvider 설계

#### 클래스 구조
```java
@Service
public class DailyCheckMessageProvider {

    // 메시지 풀 (불변 상수)
    private static final Map<DayOfWeek, List<String>> DAY_MESSAGES;
    private static final Map<Season, List<String>> SEASON_MESSAGES;

    // 초기화 블록
    static {
        // 요일별 메시지 초기화
        // 계절별 메시지 초기화
    }

    // 공개 API
    public String generateMessage() {
        return generateMessage(LocalDate.now());
    }

    // 테스트 가능한 메서드 (시간 주입)
    public String generateMessage(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Season season = determineSeason(date);

        String dayMessage = selectDayMessage(dayOfWeek);
        String seasonMessage = selectSeasonMessage(season);

        return combineMessages(dayMessage, seasonMessage);
    }

    // 내부 메서드
    private Season determineSeason(LocalDate date) { /* ... */ }
    private String selectDayMessage(DayOfWeek day) { /* ... */ }
    private String selectSeasonMessage(Season season) { /* ... */ }
    private String combineMessages(String day, String season) { /* ... */ }
}
```

#### Season Enum
```java
public enum Season {
    SPRING("봄", 3, 5),
    SUMMER("여름", 6, 8),
    AUTUMN("가을", 9, 11),
    WINTER("겨울", 12, 2);

    private final String korean;
    private final int startMonth;
    private final int endMonth;

    public static Season fromMonth(int month) { /* ... */ }
}
```

### 4.2 메시지 풀 상세

#### 요일별 메시지
```java
static {
    DAY_MESSAGES = Map.of(
        DayOfWeek.MONDAY, List.of(
            "새로운 한 주가 시작됐어요",
            "월요일이에요, 오늘도 활기차게 시작해보세요",
            "한 주의 시작, 좋은 일만 가득하길 바랍니다"
        ),
        DayOfWeek.TUESDAY, List.of(
            "화요일이에요, 오늘 하루는 어떠신가요",
            "오늘도 건강하게 지내시길 바랍니다",
            "점심은 맛있게 드셨나요"
        ),
        DayOfWeek.WEDNESDAY, List.of(
            "한 주의 중간, 수요일이에요",
            "벌써 수요일이네요, 오늘 하루도 잘 보내세요",
            "오늘 기분은 어떠신가요"
        ),
        DayOfWeek.THURSDAY, List.of(
            "목요일이에요, 주말이 다가오네요",
            "오늘도 편안한 하루 보내세요",
            "오늘 하루는 어떻게 보내셨나요"
        ),
        DayOfWeek.FRIDAY, List.of(
            "금요일이에요, 이번 주도 수고하셨어요",
            "한 주의 마무리, 잘 마무리하시길 바랍니다",
            "주말이 코앞이네요, 조금만 더 힘내세요"
        ),
        DayOfWeek.SATURDAY, List.of(
            "편안한 토요일 보내세요",
            "여유로운 주말이에요, 즐거운 하루 되세요",
            "토요일이에요, 하고 싶은 일 하며 보내세요"
        ),
        DayOfWeek.SUNDAY, List.of(
            "일요일이에요, 푹 쉬시길 바랍니다",
            "평화로운 일요일 보내세요",
            "일요일이네요, 내일을 위해 충분히 쉬세요"
        )
    );
}
```

#### 계절별 메시지
```java
static {
    SEASON_MESSAGES = Map.of(
        Season.SPRING, List.of(
            "따뜻한 봄날이에요, 산책 어떠세요",
            "봄꽃이 활짝 폈어요, 나들이 다녀오셨나요",
            "봄날씨가 참 좋네요, 기분 좋은 하루 되세요",
            "새싹이 돋는 계절이에요, 활력 넘치는 하루 되세요"
        ),
        Season.SUMMER, List.of(
            "더운 날씨에 건강 조심하세요",
            "시원한 음료 드시며 더위 이겨내세요",
            "무더운 여름이에요, 에어컨 바람에 감기 조심하세요",
            "여름 휴가는 잘 보내셨나요",
            "더위에 수분 섭취 충분히 하세요"
        ),
        Season.AUTUMN, List.of(
            "선선한 가을 날씨가 좋네요",
            "단풍 구경은 다녀오셨나요",
            "가을이에요, 독서하기 좋은 계절이네요",
            "환절기 건강 관리 잘 하세요",
            "천고마비의 계절이에요, 맛있는 것 드세요"
        ),
        Season.WINTER, List.of(
            "추운 날씨에 따뜻하게 보내세요",
            "겨울이에요, 감기 조심하세요",
            "따뜻한 차 한 잔 어떠세요",
            "눈이 오면 미끄러운 곳 조심하세요",
            "실내에서 따뜻하게 지내시길 바랍니다"
        )
    );
}
```

### 4.3 메시지 선택 알고리즘

#### 선택 방식
```java
private String selectDayMessage(DayOfWeek day) {
    List<String> messages = DAY_MESSAGES.get(day);

    // 옵션 1: 날짜 기반 결정적 선택 (같은 날은 같은 메시지)
    int index = LocalDate.now().getDayOfYear() % messages.size();
    return messages.get(index);

    // 옵션 2: 랜덤 선택 (매번 다른 메시지, 테스트 어려움)
    // Random random = new Random();
    // return messages.get(random.nextInt(messages.size()));
}
```

**채택 방식**: 옵션 1 (결정적 선택)
- **장점**: 테스트 가능, 예측 가능
- **단점**: 같은 날짜에는 같은 메시지
- **해결**: 연중 날짜(getDayOfYear)를 사용하여 충분히 다양화

#### 메시지 조합 방식
```java
private String combineMessages(String dayMessage, String seasonMessage) {
    // 자연스러운 조합 패턴
    return String.format("%s. %s", dayMessage, seasonMessage);

    // 예시:
    // "새로운 한 주가 시작됐어요. 따뜻한 봄날이에요, 산책 어떠세요?"
    // "금요일이에요, 이번 주도 수고하셨어요. 더운 날씨에 건강 조심하세요."
}
```

### 4.4 DailyCheckOrchestrator 수정

#### 변경 사항
```java
@Service
@RequiredArgsConstructor
public class DailyCheckOrchestrator {

    // 기존 상수 제거
    // private static final String DAILY_CHECK_MESSAGE = "..."; ❌

    // 새로운 의존성 추가
    private final DailyCheckMessageProvider messageProvider; // ⭐

    private void processMemberDailyCheck(Long memberId) {
        // 기존: 하드코딩된 메시지
        // String message = DAILY_CHECK_MESSAGE; ❌

        // 변경: 동적 메시지 생성
        String message = messageProvider.generateMessage(); // ⭐

        // 나머지 로직은 동일
        var notificationHistory = notificationHistoryService.recordNotificationWithType(
            memberId, DAILY_CHECK_TITLE, message, ...
        );
    }
}
```

---

## 🧪 5. 테스트 전략

### 5.1 테스트 대상

#### A. DailyCheckMessageProviderTest
```java
@ExtendWith(MockitoExtension.class)
class DailyCheckMessageProviderTest {

    private DailyCheckMessageProvider messageProvider;

    @BeforeEach
    void setUp() {
        messageProvider = new DailyCheckMessageProvider();
    }

    // 1. 요일별 메시지 테스트
    @Test
    void 월요일_메시지_생성() {
        // given
        LocalDate monday = LocalDate.of(2025, 11, 10); // 월요일

        // when
        String message = messageProvider.generateMessage(monday);

        // then
        assertThat(message).contains("월요일", "한 주");
    }

    // 2. 계절별 메시지 테스트
    @Test
    void 봄_메시지_생성() {
        // given
        LocalDate spring = LocalDate.of(2025, 4, 15); // 봄

        // when
        String message = messageProvider.generateMessage(spring);

        // then
        assertThat(message).containsAnyOf("봄", "따뜻", "꽃");
    }

    // 3. 메시지 조합 테스트
    @Test
    void 월요일_봄_메시지_조합() {
        // given
        LocalDate mondaySpring = LocalDate.of(2025, 4, 14);

        // when
        String message = messageProvider.generateMessage(mondaySpring);

        // then
        assertThat(message)
            .contains("월요일")
            .containsAnyOf("봄", "따뜻");
    }

    // 4. 경계값 테스트
    @ParameterizedTest
    @CsvSource({
        "3, SPRING",
        "5, SPRING",
        "6, SUMMER",
        "8, SUMMER",
        "9, AUTUMN",
        "11, AUTUMN",
        "12, WINTER",
        "2, WINTER"
    })
    void 계절_판별_테스트(int month, Season expected) {
        // when
        Season season = Season.fromMonth(month);

        // then
        assertThat(season).isEqualTo(expected);
    }

    // 5. 결정적 선택 테스트
    @Test
    void 같은_날짜는_같은_메시지_생성() {
        // given
        LocalDate date = LocalDate.of(2025, 11, 9);

        // when
        String message1 = messageProvider.generateMessage(date);
        String message2 = messageProvider.generateMessage(date);

        // then
        assertThat(message1).isEqualTo(message2);
    }

    // 6. 다른 날짜는 다른 메시지
    @Test
    void 다른_날짜는_다른_메시지_생성_가능성() {
        // given
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);

        // when
        String message1 = messageProvider.generateMessage(date1);
        String message2 = messageProvider.generateMessage(date2);

        // then (요일이 같아도 날짜가 다르면 다를 수 있음)
        // 단, 메시지가 같을 수도 있으므로 단순 확인
        assertThat(message1).isNotNull();
        assertThat(message2).isNotNull();
    }
}
```

#### B. DailyCheckOrchestratorTest (수정)
```java
@ExtendWith(MockitoExtension.class)
class DailyCheckOrchestratorTest {

    @Mock
    private DailyCheckMessageProvider messageProvider; // 추가

    @InjectMocks
    private DailyCheckOrchestrator orchestrator;

    @Test
    void 메시지_생성_위임_확인() {
        // given
        Long memberId = 1L;
        String expectedMessage = "테스트 메시지";

        when(messageProvider.generateMessage())
            .thenReturn(expectedMessage);

        // when
        orchestrator.processMemberDailyCheck(memberId);

        // then
        verify(messageProvider, times(1)).generateMessage();
        verify(notificationHistoryService).recordNotificationWithType(
            eq(memberId),
            any(),
            eq(expectedMessage), // 생성된 메시지 사용 확인
            any(),
            any(),
            any()
        );
    }
}
```

### 5.2 테스트 커버리지 목표

- **DailyCheckMessageProvider**: 90% 이상
  - 요일별 메시지 생성 (7개)
  - 계절별 메시지 생성 (4개)
  - 메시지 조합 (1개)
  - 계절 판별 (경계값 테스트)
  - 결정적 선택 검증

- **DailyCheckOrchestrator**: 기존 유지 + 메시지 위임 검증

---

## 📅 6. 구현 순서

### Step 1: Season Enum 생성
- 계절 판별 로직 구현
- 월 → 계절 매핑

### Step 2: DailyCheckMessageProvider 구현
- 메시지 풀 정의 (요일 25개, 계절 20개)
- 메시지 선택 알고리즘 (결정적 선택)
- 메시지 조합 로직

### Step 3: DailyCheckOrchestrator 수정
- MessageProvider 의존성 주입
- 하드코딩 메시지 제거
- 동적 메시지 생성 호출

### Step 4: 테스트 코드 작성
- MessageProviderTest (단위 테스트)
- OrchestratorTest 수정 (통합 확인)

### Step 5: 수동 검증
- 로컬 환경에서 실제 발송 테스트
- 요일별/계절별 메시지 확인

---

## 🔄 7. 확장 가능성

### 향후 추가 가능 기능
1. **시간대별 메시지**: 아침/점심/저녁 다른 메시지
2. **날씨 연동**: OpenWeather API로 날씨 반영
3. **특별일 메시지**: 공휴일, 기념일 특화 메시지
4. **개인화 메시지**: 회원 이름, 나이, 관심사 반영
5. **A/B 테스트**: 메시지 효과 분석

### 확장을 위한 구조
```java
public interface MessageStrategy {
    String generateMessage(MessageContext context);
}

// 현재: DaySeasonStrategy
// 향후: WeatherStrategy, HolidayStrategy, PersonalizedStrategy
```

---

## ✅ 8. 완료 기준

### 기능 완료
- [ ] Season Enum 구현
- [ ] DailyCheckMessageProvider 구현
- [ ] 요일별 메시지 25개 작성
- [ ] 계절별 메시지 20개 작성
- [ ] DailyCheckOrchestrator 수정
- [ ] 기존 기능 정상 동작 (중복 발송 방지, 재시도 등)

### 테스트 완료
- [ ] MessageProvider 단위 테스트 (90% 이상)
- [ ] Orchestrator 통합 테스트 수정
- [ ] 수동 검증 (실제 발송 확인)

### 문서 완료
- [ ] docs/domains/dailycheck.md 업데이트
- [ ] 메시지 풀 목록 문서화
- [ ] 확장 가이드 작성

---

## 📊 9. 예상 효과

### 정량적 효과
- 메시지 다양성: 1개 → 약 500개 조합 (25 × 20)
- 중복 확률: 100% → 0.2% (같은 요일/계절 조합에서도 날짜별 다름)

### 정성적 효과
- 사용자 경험 개선: 반복감 감소, 맥락 있는 소통
- 시스템 품질: 테스트 가능한 구조, 확장 가능한 설계
- 유지보수성: 메시지 추가/수정 용이

---

**작성자**: Claude Code
**검토 필요**: 메시지 풀 내용 (자연스러운 한국어, 노인 친화적 표현)
