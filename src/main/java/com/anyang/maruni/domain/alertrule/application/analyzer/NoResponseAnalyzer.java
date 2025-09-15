package com.anyang.maruni.domain.alertrule.application.analyzer;

import com.anyang.maruni.domain.dailycheck.domain.entity.DailyCheckRecord;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 무응답 패턴 분석기
 *
 * 일정 기간 무응답 상태를 감지하여 위험도를 평가합니다.
 * TDD Red 단계: 더미 구현
 */
@Component
public class NoResponseAnalyzer {

    /**
     * 회원의 무응답 패턴 분석
     * @param member 분석 대상 회원
     * @param analysisDays 분석 기간 (일)
     * @return 무응답 패턴 분석 결과
     */
    public AlertResult analyzeNoResponsePattern(MemberEntity member, int analysisDays) {
        // TODO: TDD Red 단계 - 더미 구현
        // 실제 구현에서는:
        // 1. DailyCheck 기록 조회
        // 2. 응답 패턴 분석
        // 3. 위험도 판정

        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 응답 패턴 계산 (내부 메서드)
     * @param recentChecks DailyCheck 기록 목록
     * @return 응답 패턴 정보
     */
    private ResponsePattern calculateResponsePattern(List<DailyCheckRecord> recentChecks) {
        // TODO: TDD Red 단계 - 더미 구현
        throw new UnsupportedOperationException("TDD Red 단계: 구현 예정");
    }

    /**
     * 응답 패턴 정보 VO
     */
    public static class ResponsePattern {
        private final int totalCheckDays;
        private final int responseDays;
        private final int noResponseDays;
        private final int consecutiveNoResponseDays;
        private final double responseRate;

        public ResponsePattern(int totalCheckDays, int responseDays, int noResponseDays,
                              int consecutiveNoResponseDays, double responseRate) {
            this.totalCheckDays = totalCheckDays;
            this.responseDays = responseDays;
            this.noResponseDays = noResponseDays;
            this.consecutiveNoResponseDays = consecutiveNoResponseDays;
            this.responseRate = responseRate;
        }

        // Getter 메서드들
        public int getTotalCheckDays() { return totalCheckDays; }
        public int getResponseDays() { return responseDays; }
        public int getNoResponseDays() { return noResponseDays; }
        public int getConsecutiveNoResponseDays() { return consecutiveNoResponseDays; }
        public double getResponseRate() { return responseRate; }
    }
}