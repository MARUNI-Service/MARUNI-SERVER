package com.anyang.maruni.domain.alertrule.application.analyzer.strategy;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AnalysisContext;
import com.anyang.maruni.domain.alertrule.application.analyzer.util.AnalyzerUtils;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.dailycheck.domain.entity.DailyCheckRecord;
import com.anyang.maruni.domain.dailycheck.domain.repository.DailyCheckRecordRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.RequiredArgsConstructor;

/**
 * 무응답 패턴 분석기
 *
 * 일정 기간 무응답 상태를 감지하여 위험도를 평가합니다.
 * Phase 2 리팩토링: AnomalyAnalyzer 구현체
 */
@Component
@RequiredArgsConstructor
public class NoResponseAnalyzer implements AnomalyAnalyzer {

    private final DailyCheckRecordRepository dailyCheckRecordRepository;
    private final AlertConfigurationProperties alertConfig;

    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        return analyzeNoResponsePattern(member, context.getAnalysisDays());
    }

    @Override
    public AlertType getSupportedType() {
        return AlertType.NO_RESPONSE;
    }

    @Override
    public boolean supports(AlertType alertType) {
        return AlertType.NO_RESPONSE.equals(alertType);
    }

    /**
     * 회원의 무응답 패턴 분석
     * @param member 분석 대상 회원
     * @param analysisDays 분석 기간 (일)
     * @return 무응답 패턴 분석 결과
     */
    public AlertResult analyzeNoResponsePattern(MemberEntity member, int analysisDays) {
        // 1. 최근 N일간 DailyCheck 기록 조회
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(analysisDays);

        List<DailyCheckRecord> recentChecks = dailyCheckRecordRepository.findByMemberIdAndDateRangeOrderByCheckDateDesc(
                member.getId(), startDate, endDate);

        if (recentChecks.isEmpty()) {
            return AlertResult.noAlert();
        }

        // 2. 응답 패턴 분석
        ResponsePattern responsePattern = calculateResponsePattern(recentChecks);

        // 3. 위험도 판정
        return evaluateNoResponseRisk(responsePattern);
    }

    /**
     * 응답 패턴 계산 (내부 메서드)
     * @param recentChecks DailyCheck 기록 목록
     * @return 응답 패턴 정보
     */
    private ResponsePattern calculateResponsePattern(List<DailyCheckRecord> recentChecks) {
        int totalCheckDays = recentChecks.size();
        int responseDays = 0;
        int noResponseDays = 0;

        // 성공/실패 일수 계산
        for (DailyCheckRecord check : recentChecks) {
            if (check.getSuccess()) {
                responseDays++;
            } else {
                noResponseDays++;
            }
        }

        // 응답률 계산
        double responseRate = totalCheckDays > 0 ? (double) responseDays / totalCheckDays : 0.0;

        // 연속적인 무응답 일수 계산 (최신 기록부터)
        int consecutiveNoResponseDays = calculateConsecutiveNoResponseDays(recentChecks);

        return new ResponsePattern(totalCheckDays, responseDays, noResponseDays,
                                  consecutiveNoResponseDays, responseRate);
    }

    /**
     * 연속적인 무응답 일수 계산
     * @param recentChecks DailyCheck 기록 목록 (최신순 정렬)
     * @return 연속적인 무응답 일수
     */
    private int calculateConsecutiveNoResponseDays(List<DailyCheckRecord> recentChecks) {
        int consecutiveDays = 0;

        for (DailyCheckRecord check : recentChecks) {
            if (!check.getSuccess()) {
                consecutiveDays++;
            } else {
                // 성공한 기록이 나오면 연속성이 끊어짐
                break;
            }
        }

        return consecutiveDays;
    }

    /**
     * 무응답 위험도 평가
     * @param responsePattern 응답 패턴
     * @return 알림 결과
     */
    private AlertResult evaluateNoResponseRisk(ResponsePattern responsePattern) {
        int consecutiveNoResponseDays = responsePattern.consecutiveNoResponseDays();
        double responseRate = responsePattern.responseRate();

        // 고위험: 연속 무응답 또는 낮은 응답률
        if (consecutiveNoResponseDays >= alertConfig.getNoResponse().getHighRiskConsecutiveNoResponseDays()
            || responseRate < alertConfig.getNoResponse().getHighRiskMinResponseRate()) {
            String message = AnalyzerUtils.createConsecutiveDaysMessage(
                    consecutiveNoResponseDays, responseRate, "무응답");
            return AlertResult.createAlert(AlertLevel.HIGH, AlertType.NO_RESPONSE, message, responsePattern);
        }

        // 중위험: 연속 무응답 또는 낮은 응답률
        if (consecutiveNoResponseDays >= alertConfig.getNoResponse().getMediumRiskConsecutiveNoResponseDays()
            || responseRate < alertConfig.getNoResponse().getMediumRiskMinResponseRate()) {
            String message = AnalyzerUtils.createConsecutiveDaysMessage(
                    consecutiveNoResponseDays, responseRate, "무응답");
            return AlertResult.createAlert(AlertLevel.MEDIUM, AlertType.NO_RESPONSE, message, responsePattern);
        }

        // 저위험 또는 알림 없음
        return AlertResult.noAlert();
    }

    /**
         * 응답 패턴 정보 VO
         */
        public record ResponsePattern(int totalCheckDays, int responseDays, int noResponseDays,
                                      int consecutiveNoResponseDays, double responseRate) {
    }
}