package com.anyang.maruni.domain.guardian.domain.service;

import com.anyang.maruni.domain.guardian.application.exception.GuardianBusinessException;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.response.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Guardian 도메인 서비스
 *
 * 단일 책임: 복잡한 Guardian 도메인 비즈니스 로직 처리
 * - 보호자 비활성화 시 안전한 관계 해제
 * - 보호자-회원 관계 관리의 복잡한 로직
 * - 도메인 규칙 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardianDomainService {

    /**
     * 보호자를 안전하게 비활성화
     *
     * 연결된 모든 회원과의 관계를 먼저 해제한 후 보호자를 비활성화
     * 트랜잭션 내에서 원자적으로 처리하여 데이터 일관성 보장
     *
     * @param guardian 비활성화할 보호자
     * @param connectedMembers 연결된 회원 리스트
     */
    @Transactional
    public void deactivateGuardianSafely(GuardianEntity guardian, List<MemberEntity> connectedMembers) {
        log.info("보호자 안전 비활성화 시작. Guardian ID: {}, 연결된 회원 수: {}",
                guardian.getId(), connectedMembers.size());

        // 1. 연결된 모든 회원의 보호자 관계 해제
        connectedMembers.forEach(member -> {
            member.removeGuardian();
            log.debug("회원 {} 의 보호자 관계 해제 완료", member.getId());
        });

        // 2. 보호자 비활성화
        guardian.deactivate();

        log.info("보호자 안전 비활성화 완료. Guardian ID: {}", guardian.getId());
    }

    /**
     * 보호자-회원 관계 설정 시 비즈니스 규칙 적용
     *
     * @param member 회원
     * @param guardian 보호자
     * @throws GuardianBusinessException 비즈니스 규칙 위반 시
     */
    @Transactional
    public void establishGuardianRelation(MemberEntity member, GuardianEntity guardian) {
        log.info("보호자-회원 관계 설정 시작. Member ID: {}, Guardian ID: {}",
                member.getId(), guardian.getId());

        // 1. 자기 자신을 보호자로 설정하는지 검증
        if (member.getId().equals(guardian.getId())) {
            throw new GuardianBusinessException(ErrorCode.GUARDIAN_SELF_ASSIGNMENT_NOT_ALLOWED);
        }

        // 2. 보호자가 활성 상태인지 검증
        if (!guardian.getIsActive()) {
            throw new GuardianBusinessException(ErrorCode.GUARDIAN_DEACTIVATION_FAILED);
        }

        // 3. 기존 보호자가 있다면 로그 남기기 (허용하되 경고)
        if (member.getGuardian() != null) {
            log.warn("회원 {}에게 이미 보호자 {}가 설정되어 있습니다. 새로운 보호자 {}로 변경합니다.",
                    member.getId(), member.getGuardian().getId(), guardian.getId());
        }

        // 4. 관계 설정
        member.assignGuardian(guardian);

        log.info("보호자-회원 관계 설정 완료. Member ID: {}, Guardian ID: {}",
                member.getId(), guardian.getId());
    }

    /**
     * 보호자-회원 관계 해제
     *
     * @param member 관계를 해제할 회원
     */
    @Transactional
    public void removeGuardianRelation(MemberEntity member) {
        if (member.getGuardian() != null) {
            Long guardianId = member.getGuardian().getId();
            member.removeGuardian();
            log.info("회원 {} 의 보호자 {} 관계 해제 완료", member.getId(), guardianId);
        } else {
            log.warn("회원 {} 에게 설정된 보호자가 없습니다", member.getId());
        }
    }
}