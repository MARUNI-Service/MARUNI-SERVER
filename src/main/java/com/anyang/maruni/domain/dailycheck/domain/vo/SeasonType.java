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
            case 12, 1, 2 -> WINTER;     // 겨울: 12월, 1-2월
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }

    public String getKorean() {
        return korean;
    }
}
