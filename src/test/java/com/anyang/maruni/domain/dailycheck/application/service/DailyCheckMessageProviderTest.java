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

    @Test
    void 오늘_날짜_기준_메시지_생성() {
        // when
        String message = messageProvider.generateMessage();

        // then
        assertThat(message)
            .isNotNull()
            .isNotEmpty()
            .doesNotContain("{season}");
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
