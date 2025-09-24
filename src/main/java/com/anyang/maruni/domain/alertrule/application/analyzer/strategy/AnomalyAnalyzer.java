package com.anyang.maruni.domain.alertrule.application.analyzer.strategy;

import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AnalysisContext;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

/**
 * 이상징후 분석을 위한 Strategy Pattern 인터페이스
 *
 * 각 분석기(EmotionPattern, NoResponse, Keyword)는 이 인터페이스를 구현하여
 * 다형성을 통한 확장 가능한 분석 시스템을 제공합니다.
 *
 * Phase 2 리팩토링: Strategy Pattern 적용
 */
public interface AnomalyAnalyzer {

    /**
     * 이상징후 분석을 수행합니다.
     *
     * @param member 분석 대상 회원
     * @param context 분석에 필요한 컨텍스트 정보
     * @return 분석 결과
     */
    AlertResult analyze(MemberEntity member, AnalysisContext context);

    /**
     * 이 분석기가 지원하는 알림 타입을 반환합니다.
     *
     * @return 지원하는 AlertType
     */
    AlertType getSupportedType();

    /**
     * 주어진 알림 타입을 이 분석기가 처리할 수 있는지 확인합니다.
     *
     * @param alertType 확인할 알림 타입
     * @return 처리 가능 여부
     */
    boolean supports(AlertType alertType);
}