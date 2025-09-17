package com.anyang.maruni.domain.conversation.domain.vo;

import java.util.Collections;
import java.util.List;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 프로필 정보 (Value Object)
 *
 * AI 대화에서 활용할 사용자 특성 정보를 담습니다.
 * MVP에서는 기본 정보만 제공하며, 향후 확장 예정입니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class MemberProfile {

    /**
     * 회원 ID
     */
    private final Long memberId;

    /**
     * 연령대 정보 ("60대", "70대", "80대 이상" 등)
     */
    private final String ageGroup;

    /**
     * 성격 유형 ("활발함", "내성적", "신중함" 등)
     */
    private final String personalityType;

    /**
     * 건강 관심사 목록
     */
    @Builder.Default
    private final List<String> healthConcerns = Collections.emptyList();

    /**
     * 최근 감정 패턴
     */
    private final EmotionType recentEmotionPattern;

    /**
     * 기본 프로필 생성
     *
     * @param memberId 회원 ID
     * @return 기본값으로 초기화된 MemberProfile
     */
    public static MemberProfile createDefault(Long memberId) {
        return MemberProfile.builder()
                .memberId(memberId)
                .ageGroup("70대")
                .personalityType("일반")
                .healthConcerns(Collections.emptyList())
                .recentEmotionPattern(EmotionType.NEUTRAL)
                .build();
    }
}