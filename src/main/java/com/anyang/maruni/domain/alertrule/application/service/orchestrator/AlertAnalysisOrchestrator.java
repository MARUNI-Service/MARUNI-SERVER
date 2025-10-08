package com.anyang.maruni.domain.alertrule.application.service.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.alertrule.application.analyzer.strategy.AnomalyAnalyzer;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AnalysisContext;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.alertrule.domain.exception.UnsupportedAlertTypeException;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy Pattern을 적용한 이상징후 분석 오케스트레이터
 *
 * 다양한 분석기들을 관리하고 적절한 분석기를 선택하여 분석을 수행합니다.
 * Phase 2 리팩토링: Strategy Pattern 핵심 구현체
 */
@Service
@Slf4j
public class AlertAnalysisOrchestrator {

    private final Map<AlertType, AnomalyAnalyzer> analyzers;

    /**
     * 생성자를 통한 분석기 자동 등록
     * Spring의 의존성 주입으로 모든 AnomalyAnalyzer 구현체를 자동 수집
     *
     * @param analyzerList Spring이 주입하는 모든 AnomalyAnalyzer 구현체 목록
     */
    @Autowired
    public AlertAnalysisOrchestrator(List<AnomalyAnalyzer> analyzerList) {
        this.analyzers = analyzerList.stream()
                .collect(Collectors.toMap(
                        AnomalyAnalyzer::getSupportedType,
                        analyzer -> analyzer,
                        (existing, duplicate) -> {
                            log.warn("Duplicate analyzer found for type: {}. Using existing: {}",
                                    existing.getSupportedType(), existing.getClass().getSimpleName());
                            return existing;
                        }
                ));

        log.info("Initialized AlertAnalysisOrchestrator with {} analyzers: {}",
                analyzers.size(),
                analyzers.keySet().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")));
    }

    /**
     * 지정된 알림 타입에 대한 이상징후 분석을 수행합니다.
     *
     * @param alertType 분석할 알림 타입
     * @param member 분석 대상 회원
     * @param context 분석 컨텍스트
     * @return 분석 결과
     * @throws UnsupportedAlertTypeException 지원하지 않는 알림 타입인 경우
     */
    public AlertResult analyzeByType(AlertType alertType, MemberEntity member, AnalysisContext context) {
        AnomalyAnalyzer analyzer = analyzers.get(alertType);

        if (analyzer == null) {
            throw new UnsupportedAlertTypeException(alertType);
        }

        log.debug("Analyzing anomaly for type: {} using analyzer: {}",
                alertType, analyzer.getClass().getSimpleName());

        return analyzer.analyze(member, context);
    }

    /**
     * 특정 알림 타입이 지원되는지 확인합니다.
     *
     * @param alertType 확인할 알림 타입
     * @return 지원 여부
     */
    public boolean isSupported(AlertType alertType) {
        return analyzers.containsKey(alertType);
    }
}