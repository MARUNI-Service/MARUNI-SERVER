package com.anyang.maruni.domain.guardian.application.validator;

import com.anyang.maruni.domain.guardian.application.exception.GuardianEmailAlreadyExistsException;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Guardian 도메인 유효성 검증 컴포넌트
 *
 * 단일 책임: Guardian 관련 비즈니스 규칙 검증
 * - 이메일 중복 검증
 * - 보호자 상태 검증
 * - 비즈니스 규칙 검증
 */
@Component
@RequiredArgsConstructor
public class GuardianValidator {

    private final GuardianRepository guardianRepository;

    /**
     * 보호자 이메일 중복 검증
     *
     * @param email 검증할 이메일
     * @throws GuardianEmailAlreadyExistsException 이미 존재하는 이메일인 경우
     */
    public void validateEmailNotExists(String email) {
        guardianRepository.findByGuardianEmailAndIsActiveTrue(email)
            .ifPresent(guardian -> {
                throw new GuardianEmailAlreadyExistsException(email);
            });
    }

    /**
     * 보호자가 활성 상태인지 검증
     *
     * @param guardian 검증할 보호자
     * @return 활성 상태 여부
     */
    public boolean isActiveGuardian(GuardianEntity guardian) {
        return guardian.getIsActive();
    }

    /**
     * 보호자가 회원을 담당할 수 있는지 검증
     * 향후 확장: 보호자당 최대 담당 회원 수 제한 등
     *
     * @param guardian 검증할 보호자
     * @return 담당 가능 여부
     */
    public boolean canAssignToMember(GuardianEntity guardian) {
        return isActiveGuardian(guardian);
        // 향후 확장: 담당 회원 수 제한 로직 추가 가능
        // return isActiveGuardian(guardian) && guardian.getMembers().size() < MAX_MEMBERS_PER_GUARDIAN;
    }
}