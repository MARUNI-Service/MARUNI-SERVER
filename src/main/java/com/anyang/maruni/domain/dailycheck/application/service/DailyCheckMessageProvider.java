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
