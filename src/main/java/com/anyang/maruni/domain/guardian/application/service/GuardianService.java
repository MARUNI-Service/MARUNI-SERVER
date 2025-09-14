package com.anyang.maruni.domain.guardian.application.service;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Guardian Service (TDD Red - 더미 구현)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final MemberRepository memberRepository;

    // TDD Red: 더미 구현들 (컴파일은 성공, 테스트는 실패)

    @Transactional
    public GuardianResponseDto createGuardian(GuardianRequestDto request) {
        // 더미 구현 - null 반환으로 테스트 실패 유도
        return null;
    }

    @Transactional
    public void assignGuardianToMember(Long memberId, Long guardianId) {
        // 더미 구현 - 아무것도 하지 않음
    }

    public List<MemberResponse> getMembersByGuardian(Long guardianId) {
        // 더미 구현 - 빈 리스트 반환으로 테스트 실패 유도
        return List.of();
    }

    @Transactional
    public void removeGuardianFromMember(Long memberId) {
        // 더미 구현 - 아무것도 하지 않음
    }
}