package com.anyang.maruni.domain.dailycheck.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

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

    @Test
    void 한국어_이름_반환() {
        // when & then
        assertThat(SeasonType.SPRING.getKorean()).isEqualTo("봄");
        assertThat(SeasonType.SUMMER.getKorean()).isEqualTo("여름");
        assertThat(SeasonType.AUTUMN.getKorean()).isEqualTo("가을");
        assertThat(SeasonType.WINTER.getKorean()).isEqualTo("겨울");
    }
}
