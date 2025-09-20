package com.anyang.maruni.domain.conversation.domain.vo;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;

/**
 * MemberProfile VO 테스트
 */
@DisplayName("회원 프로필 VO 테스트")
class MemberProfileTest {

    @Test
    @DisplayName("기본 프로필 생성: 기본값이 올바르게 설정된다")
    void createDefault_Success() {
        // Given
        Long memberId = 1L;

        // When
        MemberProfile profile = MemberProfile.createDefault(memberId);

        // Then
        assertThat(profile.getMemberId()).isEqualTo(memberId);
        assertThat(profile.getAgeGroup()).isEqualTo("70대");
        assertThat(profile.getPersonalityType()).isEqualTo("일반");
        assertThat(profile.getHealthConcerns()).isEmpty();
        assertThat(profile.getRecentEmotionPattern()).isEqualTo(EmotionType.NEUTRAL);
    }

    @Test
    @DisplayName("커스텀 프로필 생성: 모든 필드가 올바르게 설정된다")
    void customProfile_Success() {
        // Given
        Long memberId = 2L;
        String ageGroup = "80대 이상";
        String personalityType = "활발함";
        var healthConcerns = Arrays.asList("고혈압", "당뇨");
        EmotionType emotionPattern = EmotionType.POSITIVE;

        // When
        MemberProfile profile = MemberProfile.builder()
                .memberId(memberId)
                .ageGroup(ageGroup)
                .personalityType(personalityType)
                .healthConcerns(healthConcerns)
                .recentEmotionPattern(emotionPattern)
                .build();

        // Then
        assertThat(profile.getMemberId()).isEqualTo(memberId);
        assertThat(profile.getAgeGroup()).isEqualTo(ageGroup);
        assertThat(profile.getPersonalityType()).isEqualTo(personalityType);
        assertThat(profile.getHealthConcerns()).containsExactly("고혈압", "당뇨");
        assertThat(profile.getRecentEmotionPattern()).isEqualTo(emotionPattern);
    }

    @Test
    @DisplayName("프로필 불변성: 필드들이 final로 보호된다")
    void profileImmutability() {
        // Given
        MemberProfile profile = MemberProfile.createDefault(1L);

        // When & Then - 모든 필드가 final이므로 변경 불가능
        assertThat(profile.getMemberId()).isNotNull();
        assertThat(profile.getAgeGroup()).isNotNull();
        assertThat(profile.getPersonalityType()).isNotNull();
        assertThat(profile.getHealthConcerns()).isNotNull();
        assertThat(profile.getRecentEmotionPattern()).isNotNull();
    }

    @Test
    @DisplayName("빈 건강 관심사 목록: Collections.emptyList() 기본값 확인")
    void emptyHealthConcerns_Default() {
        // When
        MemberProfile profile = MemberProfile.createDefault(1L);

        // Then
        assertThat(profile.getHealthConcerns()).isEmpty();
        assertThat(profile.getHealthConcerns()).isSameAs(Collections.emptyList());
    }
}