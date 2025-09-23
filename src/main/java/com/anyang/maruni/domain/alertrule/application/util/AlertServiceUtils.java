package com.anyang.maruni.domain.alertrule.application.util;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.alertrule.application.analyzer.AlertResult;
import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * AlertRule 도메인 서비스들의 공통 유틸리티
 *
 * 여러 서비스에서 공통으로 사용하는 유틸리티 메서드들을 제공
 * DRY(Don't Repeat Yourself) 원칙 준수
 */
@Component
@RequiredArgsConstructor
public class AlertServiceUtils {

    private final MemberRepository memberRepository;
    private final AlertConfigurationProperties alertConfig;

    /**
     * 회원 검증 및 조회 공통 메서드
     *
     * 4개 서비스(Detection, Management, Notification, History)에서 공통 사용
     *
     * @param memberId 회원 ID
     * @return 검증된 회원 엔티티
     */
    public MemberEntity validateAndGetMember(Long memberId) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }

    /**
     * 감지 상세 정보 JSON 생성 공통 메서드
     *
     * AlertHistory, AlertNotification에서 공통 사용
     *
     * @param alertResult 알림 결과
     * @return JSON 형태의 상세 정보
     */
    public String createDetectionDetailsJson(AlertResult alertResult) {
        // TODO: Phase 2에서 구현 예정
        throw new UnsupportedOperationException("Phase 2에서 구현 예정");
    }
}