# DailyCheck 메시지 다양화 계획서

**작성일**: 2025-11-09
**최종 수정**: 2025-11-09 (리뷰 반영)
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
3. **조합 방식**: 요일 + 계절을 자연스럽게 조합 (템플릿 기반)

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
- **순수 함수**: 동일한 입력(날짜)에 대해 동일한 출력
- **시간 주입**: LocalDate를 파라미터로 받아 테스트 용이
- **상태 없음**: 메시지 풀은 불변 상수로 관리
- **Seed 기반**: Random이지만 결정적 (테스트 가능 + 예측 불가)

### 3.2 메시지 구조 설계 (템플릿 기반)

#### 메시지 조합 방식
```
템플릿 기반: 요일 메시지에 {season} 플레이스홀더 포함

예시:
- "새로운 한 주가 시작됐어요! {season}"
  + "따뜻한 봄날, 산책 어떠세요?"
  = "새로운 한 주가 시작됐어요! 따뜻한 봄날, 산책 어떠세요?"

- "금요일이에요, 이번 주도 수고하셨어요. {season}"
  + "추운 날씨에 건강 조심하세요"
  = "금요일이에요, 이번 주도 수고하셨어요. 추운 날씨에 건강 조심하세요"
```

**장점:**
- 자연스러운 문장 구조 보장
- 메시지 개수 절감 (25 + 20 = 45개)
- 문맥 단절 방지 (단순 연결 시 "점심은 드셨나요. 눈이 오면 조심하세요" 같은 어색함 제거)

#### 메시지 풀 설계

**요일별 템플릿 메시지 (7종 × 3-4개 = 약 25개)**
```
월요일: "새로운 한 주가 시작됐어요! {season}" 등
화~목요일: "오늘 하루는 어떠신가요? {season}" 등
금요일: "이번 주도 수고하셨어요. {season}" 등
토요일: "편안한 토요일 보내세요. {season}" 등
일요일: "일요일이에요, 푹 쉬세요. {season}" 등
```

**계절별 스니펫 (4종 × 5개 = 약 20개)**
```
봄(3-5월): "따뜻한 봄날, 산책 어떠세요?", "봄꽃이 활짝 폈어요" 등
여름(6-8월): "더운 날씨에 건강 조심하세요", "시원하게 지내세요" 등
가을(9-11월): "선선한 날씨가 좋네요", "단풍 구경 다녀오셨나요" 등
겨울(12-2월): "추운 날씨에 따뜻하게 보내세요", "감기 조심하세요" 등
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
    ├── repository/                            # 기존 유지
    └── vo/
        └── SeasonType.java                    # 계절 Enum (신규) ⭐
```

---

## 📝 4. 구현 상세

### 4.1 SeasonType Enum (수정: switch 문으로 겨울 버그 해결)

**위치**: `domain/dailycheck/domain/vo/SeasonType.java`

```java
package com.anyang.maruni.domain.dailycheck.domain.vo;

/**
 * 계절 타입
 * MARUNI 네이밍 규칙: {의미}Type (EmotionType, MessageType 등과 일관성)
 */
public enum SeasonType {
    SPRING("봄"),
    SUMMER("여름"),
    AUTUMN("가을"),
    WINTER("겨울");

    private final String korean;

    SeasonType(String korean) {
        this.korean = korean;
    }

    /**
     * 월(month)로부터 계절 판별
     *
     * @param month 1-12월
     * @return 해당 계절
     * @throws IllegalArgumentException 잘못된 월
     */
    public static SeasonType fromMonth(int month) {
        return switch (month) {
            case 3, 4, 5 -> SPRING;      // 봄: 3-5월
            case 6, 7, 8 -> SUMMER;      // 여름: 6-8월
            case 9, 10, 11 -> AUTUMN;    // 가을: 9-11월
            case 12, 1, 2 -> WINTER;     // 겨울: 12월, 1-2월 (버그 수정!)
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }

    public String getKorean() {
        return korean;
    }
}
```

**주요 변경:**
- ❌ 원본: `WINTER("겨울", 12, 2)` → startMonth > endMonth 버그
- ✅ 수정: `case 12, 1, 2 -> WINTER` → 명확한 월 열거

### 4.2 DailyCheckMessageProvider 구현

**위치**: `application/service/DailyCheckMessageProvider.java`

```java
package com.anyang.maruni.domain.dailycheck.application.service;

import com.anyang.maruni.domain.dailycheck.domain.vo.SeasonType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DailyCheck 메시지 생성 서비스
 *
 * 단일 책임: 요일 + 계절 기반 다양한 안부 메시지 생성
 * 테스트 가능성: LocalDate 주입으로 결정적 테스트 가능
 */
@Service
@Slf4j
public class DailyCheckMessageProvider {

    // 메시지 길이 제한 (DB: VARCHAR(255), 푸시 알림 제약 고려)
    private static final int MAX_MESSAGE_LENGTH = 100;

    // 템플릿 메시지: {season} 플레이스홀더 포함
    private static final Map<DayOfWeek, List<String>> DAY_MESSAGES;

    // 계절별 스니펫 (플레이스홀더에 삽입될 내용)
    private static final Map<SeasonType, List<String>> SEASON_SNIPPETS;

    static {
        DAY_MESSAGES = Map.of(
            DayOfWeek.MONDAY, List.of(
                "새로운 한 주가 시작됐어요! {season}",
                "월요일 아침입니다. {season}",
                "활기찬 월요일이에요. {season}"
            ),
            DayOfWeek.TUESDAY, List.of(
                "화요일이에요. {season}",
                "오늘도 건강하게 지내세요. {season}",
                "좋은 하루 보내세요. {season}"
            ),
            DayOfWeek.WEDNESDAY, List.of(
                "벌써 수요일이네요. {season}",
                "한 주의 중간이에요. {season}",
                "오늘 하루는 어떠신가요? {season}"
            ),
            DayOfWeek.THURSDAY, List.of(
                "목요일이에요. {season}",
                "주말이 다가오네요. {season}",
                "오늘도 편안한 하루 되세요. {season}"
            ),
            DayOfWeek.FRIDAY, List.of(
                "금요일이에요, 이번 주도 수고하셨어요. {season}",
                "한 주의 마무리네요. {season}",
                "주말이 코앞이에요. {season}",
                "금요일 저녁이에요. {season}"
            ),
            DayOfWeek.SATURDAY, List.of(
                "편안한 토요일 보내세요. {season}",
                "여유로운 주말이에요. {season}",
                "토요일이에요, 즐거운 하루 되세요. {season}"
            ),
            DayOfWeek.SUNDAY, List.of(
                "일요일이에요, 푹 쉬세요. {season}",
                "평화로운 일요일 보내세요. {season}",
                "일요일이네요. {season}"
            )
        );

        SEASON_SNIPPETS = Map.of(
            SeasonType.SPRING, List.of(
                "따뜻한 봄날, 산책 어떠세요?",
                "봄꽃이 활짝 폈어요",
                "봄날씨가 참 좋네요",
                "새싹이 돋는 계절이에요",
                "봄바람이 기분 좋네요"
            ),
            SeasonType.SUMMER, List.of(
                "더운 날씨에 건강 조심하세요",
                "시원하게 지내세요",
                "무더운 여름이에요, 수분 섭취 충분히 하세요",
                "에어컨 바람에 감기 조심하세요",
                "여름 휴가는 잘 보내셨나요"
            ),
            SeasonType.AUTUMN, List.of(
                "선선한 날씨가 좋네요",
                "단풍 구경 다녀오셨나요",
                "가을이에요, 독서하기 좋은 계절이네요",
                "환절기 건강 관리 잘 하세요",
                "천고마비의 계절이에요"
            ),
            SeasonType.WINTER, List.of(
                "추운 날씨에 따뜻하게 보내세요",
                "겨울이에요, 감기 조심하세요",
                "따뜻한 차 한 잔 어떠세요",
                "눈이 오면 미끄러운 곳 조심하세요",
                "실내에서 따뜻하게 지내세요"
            )
        );
    }

    /**
     * 오늘 날짜 기준 메시지 생성 (프로덕션)
     */
    public String generateMessage() {
        return generateMessage(LocalDate.now());
    }

    /**
     * 특정 날짜 기준 메시지 생성 (테스트 가능)
     *
     * @param date 기준 날짜
     * @return 생성된 메시지
     */
    public String generateMessage(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        SeasonType season = SeasonType.fromMonth(date.getMonthValue());

        log.debug("Generating message for date={}, day={}, season={}",
                  date, dayOfWeek, season);

        String dayTemplate = selectDayMessage(dayOfWeek, date);
        String seasonSnippet = selectSeasonMessage(season, date);
        String message = combineMessages(dayTemplate, seasonSnippet);

        log.debug("Generated message (length={}): {}", message.length(), message);

        return message;
    }

    /**
     * 요일별 템플릿 메시지 선택 (Seed 기반 의사 랜덤)
     */
    private String selectDayMessage(DayOfWeek day, LocalDate date) {
        List<String> messages = DAY_MESSAGES.get(day);

        // Seed 기반 의사 랜덤: 테스트 가능 + 예측 불가
        long seed = date.toEpochDay();
        Random random = new Random(seed);
        int index = random.nextInt(messages.size());

        return messages.get(index);
    }

    /**
     * 계절별 스니펫 선택 (Seed 기반 의사 랜덤)
     */
    private String selectSeasonMessage(SeasonType season, LocalDate date) {
        List<String> messages = SEASON_SNIPPETS.get(season);

        // 요일 메시지와 다른 시드 사용 (독립적 선택)
        long seed = date.toEpochDay() + 1000;
        Random random = new Random(seed);
        int index = random.nextInt(messages.size());

        return messages.get(index);
    }

    /**
     * 템플릿과 스니펫 조합 (플레이스홀더 치환)
     */
    private String combineMessages(String dayTemplate, String seasonSnippet) {
        String combined = dayTemplate.replace("{season}", seasonSnippet);

        // 메시지 길이 검증 (방어적 프로그래밍)
        if (combined.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Message too long ({}), truncating: {}",
                     combined.length(), combined);
            return combined.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }

        return combined;
    }
}
```

### 4.3 DailyCheckOrchestrator 수정

**변경 사항:**

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyCheckOrchestrator {

    // ❌ 삭제
    // private static final String DAILY_CHECK_MESSAGE = "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?";

    // ✅ 추가
    private final DailyCheckMessageProvider messageProvider;

    // 기존 필드들 유지
    private final MemberRepository memberRepository;
    private final SimpleConversationService conversationService;
    private final NotificationHistoryService notificationHistoryService;
    private final DailyCheckRecordRepository dailyCheckRecordRepository;
    private final RetryService retryService;

    private void processMemberDailyCheck(Long memberId) {
        try {
            if (isAlreadySentToday(memberId)) {
                log.debug("Already sent to member {} today, skipping", memberId);
                return;
            }

            // ✅ 변경: 동적 메시지 생성
            String message = messageProvider.generateMessage();

            log.info("Daily check message generated for member {}: {}", memberId, message);

            String title = DAILY_CHECK_TITLE;

            var notificationHistory = notificationHistoryService.recordNotificationWithType(
                memberId,
                title,
                message,  // ⭐ 다양화된 메시지
                NotificationType.DAILY_CHECK,
                NotificationSourceType.DAILY_CHECK,
                null
            );

            if (notificationHistory != null) {
                handleSuccessfulSending(memberId, message);
            } else {
                handleFailedSending(memberId, message);
            }

        } catch (Exception e) {
            log.error("Error sending daily check message to member {}: {}", memberId, e.getMessage());
            // ⚠️ 재시도 시 원본 메시지 유지 (processRetryRecord에서 사용)
            retryService.scheduleRetry(memberId, messageProvider.generateMessage());
        }
    }

    // processRetryRecord()는 변경 불필요 - 원본 메시지 유지
    // 나머지 메서드들 기존 유지
}
```

---

## 🧪 5. 테스트 전략

### 5.1 테스트 대상

#### A. SeasonTypeTest
```java
package com.anyang.maruni.domain.dailycheck.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class SeasonTypeTest {

    @ParameterizedTest
    @CsvSource({
        "1, WINTER",
        "2, WINTER",
        "3, SPRING",
        "4, SPRING",
        "5, SPRING",
        "6, SUMMER",
        "7, SUMMER",
        "8, SUMMER",
        "9, AUTUMN",
        "10, AUTUMN",
        "11, AUTUMN",
        "12, WINTER"
    })
    void 월로_계절_판별(int month, SeasonType expected) {
        // when
        SeasonType actual = SeasonType.fromMonth(month);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "2025-02-28, WINTER",  // 겨울 마지막 날
        "2025-03-01, SPRING",  // 봄 첫 날 (경계!)
        "2025-05-31, SPRING",  // 봄 마지막 날
        "2025-06-01, SUMMER",  // 여름 첫 날 (경계!)
        "2025-08-31, SUMMER",  // 여름 마지막 날
        "2025-09-01, AUTUMN",  // 가을 첫 날 (경계!)
        "2025-11-30, AUTUMN",  // 가을 마지막 날
        "2025-12-01, WINTER",  // 겨울 첫 날 (경계!)
        "2025-12-31, WINTER",  // 연말
        "2026-01-01, WINTER"   // 연초 (겨울 지속!)
    })
    void 계절_경계_날짜_테스트(String dateStr, SeasonType expected) {
        // given
        LocalDate date = LocalDate.parse(dateStr);

        // when
        SeasonType actual = SeasonType.fromMonth(date.getMonthValue());

        // then
        assertThat(actual)
            .isEqualTo(expected)
            .withFailMessage("%s는 %s여야 하는데 %s로 판별됨",
                             dateStr, expected, actual);
    }

    @Test
    void 잘못된_월_예외_발생() {
        // when & then
        assertThatThrownBy(() -> SeasonType.fromMonth(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid month");

        assertThatThrownBy(() -> SeasonType.fromMonth(13))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid month");
    }
}
```

#### B. DailyCheckMessageProviderTest
```java
package com.anyang.maruni.domain.dailycheck.application.service;

import com.anyang.maruni.domain.dailycheck.domain.vo.SeasonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class DailyCheckMessageProviderTest {

    private DailyCheckMessageProvider messageProvider;

    @BeforeEach
    void setUp() {
        messageProvider = new DailyCheckMessageProvider();
    }

    @Test
    void 메시지_생성_성공() {
        // given
        LocalDate date = LocalDate.of(2025, 11, 10); // 월요일

        // when
        String message = messageProvider.generateMessage(date);

        // then
        assertThat(message).isNotNull().isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "2025-11-10, MONDAY",    // 월요일
        "2025-11-11, TUESDAY",   // 화요일
        "2025-11-12, WEDNESDAY", // 수요일
        "2025-11-13, THURSDAY",  // 목요일
        "2025-11-14, FRIDAY",    // 금요일
        "2025-11-15, SATURDAY",  // 토요일
        "2025-11-16, SUNDAY"     // 일요일
    })
    void 요일별_메시지_생성(String dateStr, DayOfWeek expectedDay) {
        // given
        LocalDate date = LocalDate.parse(dateStr);

        // when
        String message = messageProvider.generateMessage(date);

        // then
        assertThat(message).isNotNull();
        assertThat(date.getDayOfWeek()).isEqualTo(expectedDay);
    }

    @ParameterizedTest
    @CsvSource({
        "2025-04-15, SPRING",
        "2025-07-20, SUMMER",
        "2025-10-10, AUTUMN",
        "2025-01-15, WINTER"
    })
    void 계절별_메시지_생성(String dateStr, SeasonType expectedSeason) {
        // given
        LocalDate date = LocalDate.parse(dateStr);

        // when
        String message = messageProvider.generateMessage(date);

        // then
        assertThat(message).isNotNull();
        assertThat(SeasonType.fromMonth(date.getMonthValue()))
            .isEqualTo(expectedSeason);
    }

    @Test
    void 같은_날짜는_항상_같은_메시지_생성() {
        // given
        LocalDate date = LocalDate.of(2025, 11, 9);

        // when: 100번 반복 호출
        Set<String> messages = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            messages.add(messageProvider.generateMessage(date));
        }

        // then: 단 1개의 메시지만 생성되어야 함 (결정적)
        assertThat(messages)
            .hasSize(1)
            .withFailMessage("같은 날짜에 다른 메시지 생성: " + messages);
    }

    @Test
    void 연속된_같은_요일은_다른_메시지_가능() {
        // given
        LocalDate start = LocalDate.of(2025, 1, 6); // 월요일

        // when: 4주간 월요일 메시지 수집
        Set<String> mondayMessages = new HashSet<>();
        for (int week = 0; week < 4; week++) {
            LocalDate monday = start.plusWeeks(week);
            mondayMessages.add(messageProvider.generateMessage(monday));
        }

        // then: 최소 2개 이상의 다른 메시지 (다양성 확인)
        assertThat(mondayMessages.size())
            .isGreaterThanOrEqualTo(2)
            .withFailMessage("4주간 같은 메시지만 생성: " + mondayMessages);
    }

    @Test
    void 생성된_메시지_길이_제한_확인() {
        // given
        LocalDate anyDate = LocalDate.now();
        int maxLength = 100;

        // when
        String message = messageProvider.generateMessage(anyDate);

        // then
        assertThat(message.length())
            .isLessThanOrEqualTo(maxLength)
            .withFailMessage("메시지가 너무 깁니다 (%d자): %s",
                             message.length(), message);
    }

    @Test
    void 모든_요일_계절_조합_메시지_생성_가능() {
        // 7요일 × 4계절 = 28개 조합 모두 테스트
        for (DayOfWeek day : DayOfWeek.values()) {
            for (SeasonType season : SeasonType.values()) {
                // given: 해당 조합의 날짜 생성
                LocalDate date = findDateFor(day, season);

                // when
                String message = messageProvider.generateMessage(date);

                // then
                assertThat(message)
                    .isNotNull()
                    .isNotEmpty()
                    .withFailMessage("조합 실패: %s + %s", day, season);
            }
        }
    }

    @Test
    void 템플릿_플레이스홀더_치환_확인() {
        // given
        LocalDate springMonday = LocalDate.of(2025, 4, 14); // 봄 월요일

        // when
        String message = messageProvider.generateMessage(springMonday);

        // then: {season} 플레이스홀더가 치환되었는지 확인
        assertThat(message)
            .doesNotContain("{season}")
            .withFailMessage("플레이스홀더 미치환: " + message);
    }

    // === 헬퍼 메서드 ===

    private LocalDate findDateFor(DayOfWeek targetDay, SeasonType targetSeason) {
        // 2025년에서 해당 요일+계절 찾기
        LocalDate start = LocalDate.of(2025, 1, 1);

        for (int i = 0; i < 365; i++) {
            LocalDate date = start.plusDays(i);
            SeasonType season = SeasonType.fromMonth(date.getMonthValue());

            if (date.getDayOfWeek() == targetDay && season == targetSeason) {
                return date;
            }
        }

        throw new IllegalStateException("날짜 찾기 실패: " + targetDay + " + " + targetSeason);
    }
}
```

#### C. DailyCheckOrchestratorTest (수정)
```java
@ExtendWith(MockitoExtension.class)
class DailyCheckOrchestratorTest {

    @Mock
    private DailyCheckMessageProvider messageProvider;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationHistoryService notificationHistoryService;

    // ... 기타 Mock

    @InjectMocks
    private DailyCheckOrchestrator orchestrator;

    @Test
    void 메시지_생성_위임_확인() {
        // given
        Long memberId = 1L;
        String expectedMessage = "새로운 한 주가 시작됐어요! 따뜻한 봄날, 산책 어떠세요?";

        when(messageProvider.generateMessage())
            .thenReturn(expectedMessage);

        when(dailyCheckRecordRepository.existsSuccessfulRecordByMemberIdAndDate(any(), any()))
            .thenReturn(false);

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

- **SeasonType**: 95% 이상
  - 월별 계절 판별 (12개)
  - 경계값 테스트 (10개)
  - 예외 케이스 (2개)

- **DailyCheckMessageProvider**: 90% 이상
  - 요일별 메시지 생성 (7개)
  - 계절별 메시지 생성 (4개)
  - 메시지 조합 (1개)
  - 결정적 선택 검증 (2개)
  - 길이 검증 (1개)
  - 모든 조합 커버리지 (28개)
  - 플레이스홀더 치환 (1개)

- **DailyCheckOrchestrator**: 기존 유지 + 메시지 위임 검증

---

## 📅 6. 구현 순서

### Step 1: SeasonType Enum 생성
- [ ] `domain/dailycheck/domain/vo/SeasonType.java` 생성
- [ ] switch 문으로 `fromMonth()` 구현
- [ ] 겨울 경계 처리 (12월, 1-2월)
- [ ] `SeasonTypeTest.java` 작성 및 실행

### Step 2: DailyCheckMessageProvider 구현
- [ ] `application/service/DailyCheckMessageProvider.java` 생성
- [ ] 템플릿 기반 메시지 풀 정의 (요일 25개, 계절 20개)
- [ ] Seed 기반 의사 랜덤 선택 구현
- [ ] 플레이스홀더 치환 로직 구현
- [ ] 메시지 길이 검증 로직 추가
- [ ] `DailyCheckMessageProviderTest.java` 작성 및 실행

### Step 3: DailyCheckOrchestrator 수정
- [ ] MessageProvider 의존성 주입
- [ ] 하드코딩 메시지 상수 제거
- [ ] 동적 메시지 생성 호출
- [ ] `DailyCheckOrchestratorTest.java` 수정 및 실행

### Step 4: 통합 테스트
- [ ] 전체 테스트 실행 확인
- [ ] 테스트 커버리지 90% 이상 확인

### Step 5: 수동 검증
- [ ] 간단한 Main 메서드로 메시지 출력 확인
- [ ] 7일간 메시지 다양성 확인
- [ ] 계절 전환 시뮬레이션

### Step 6: 문서 업데이트
- [ ] `docs/domains/dailycheck.md` 업데이트
- [ ] `CLAUDE.md` Package Structure 섹션 업데이트

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
- [ ] SeasonType Enum 구현 (switch 문)
- [ ] DailyCheckMessageProvider 구현 (템플릿 기반)
- [ ] 요일별 템플릿 메시지 25개 작성
- [ ] 계절별 스니펫 20개 작성
- [ ] Seed 기반 선택 알고리즘 구현
- [ ] 메시지 길이 검증 로직 추가
- [ ] DailyCheckOrchestrator 수정
- [ ] 기존 기능 정상 동작 (중복 발송 방지, 재시도 등)

### 테스트 완료
- [ ] SeasonType 테스트 (95% 이상)
  - [ ] 월별 계절 판별 (12개)
  - [ ] 경계값 테스트 (10개)
  - [ ] 예외 케이스 (2개)
- [ ] MessageProvider 테스트 (90% 이상)
  - [ ] 요일별 메시지 생성 (7개)
  - [ ] 계절별 메시지 생성 (4개)
  - [ ] 결정적 선택 검증 (2개)
  - [ ] 메시지 길이 검증 (1개)
  - [ ] 모든 조합 커버리지 (28개)
  - [ ] 플레이스홀더 치환 (1개)
- [ ] Orchestrator 통합 테스트 수정
- [ ] 전체 테스트 통과

### 문서 완료
- [ ] docs/domains/dailycheck.md 업데이트
- [ ] CLAUDE.md 패키지 구조 업데이트

---

## 📊 9. 예상 효과

### 정량적 효과
- **메시지 다양성**: 1개 → 약 500개 조합 (25 × 20)
- **중복 확률**: 100% → 0.2% (Seed 기반으로 예측 불가)
- **메시지 개수**: 45개 (템플릿 25 + 스니펫 20)

### 정성적 효과
- **사용자 경험 개선**: 반복감 감소, 맥락 있는 소통
- **자연스러운 문장**: 템플릿 기반으로 문맥 단절 방지
- **시스템 품질**: 테스트 가능한 구조, 확장 가능한 설계
- **유지보수성**: 메시지 추가/수정 용이 (45개만 관리)

---

## 📋 10. 리뷰 반영 사항 요약

### 반영된 주요 변경
1. ✅ **Season → SeasonType** (네이밍 일관성)
2. ✅ **switch 문 사용** (겨울 버그 수정)
3. ✅ **템플릿 기반 조합** (자연스러운 문장)
4. ✅ **Seed 기반 랜덤** (테스트 가능 + 예측 불가)
5. ✅ **메시지 길이 검증** (방어적 프로그래밍)
6. ✅ **테스트 케이스 강화** (경계값, 모든 조합, 길이)

### 원본 대비 개선점
- **견고성**: 겨울 처리 버그 해결, 경계값 테스트 추가
- **사용자 경험**: 자연스러운 문장, 다양성 증가
- **유지보수성**: 45개 메시지로 500+ 조합 생성
- **테스트 가능성**: 결정적이지만 예측 불가한 선택

---

**작성자**: Claude Code
**검토**: 리뷰 반영 완료
**다음 단계**: Step 1부터 구현 시작
