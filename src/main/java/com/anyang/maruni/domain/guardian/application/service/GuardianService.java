package com.anyang.maruni.domain.guardian.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.application.exception.GuardianNotFoundException;
import com.anyang.maruni.domain.guardian.application.exception.MemberNotFoundException;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Guardian Service - 보호자 관리 비즈니스 로직
 *
 * 기능:
 * - 보호자 생성 및 관리
 * - 회원-보호자 관계 설정/해제
 * - 보호자별 담당 회원 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final MemberRepository memberRepository;

    // ========== Public Methods ==========

    @Transactional
    public GuardianResponseDto createGuardian(GuardianRequestDto request) {
        validateGuardianEmailNotExists(request.getGuardianEmail());

        GuardianEntity guardian = GuardianEntity.createGuardian(
            request.getGuardianName(),
            request.getGuardianEmail(),
            request.getGuardianPhone(),
            request.getRelation(),
            request.getNotificationPreference()
        );

        GuardianEntity savedGuardian = guardianRepository.save(guardian);
        return GuardianResponseDto.from(savedGuardian);
    }

    public GuardianResponseDto getGuardianById(Long guardianId) {
        GuardianEntity guardian = findGuardianById(guardianId);
        return GuardianResponseDto.from(guardian);
    }

    @Transactional
    public GuardianResponseDto updateGuardianInfo(Long guardianId, String name, String phone) {
        GuardianEntity guardian = findGuardianById(guardianId);
        guardian.updateGuardianInfo(name, phone);
        return GuardianResponseDto.from(guardian);
    }

    @Transactional
    public void deactivateGuardian(Long guardianId) {
        GuardianEntity guardian = findGuardianById(guardianId);

        // 연결된 모든 회원의 보호자 관계 해제
        List<MemberEntity> members = memberRepository.findByGuardian(guardian);
        members.forEach(MemberEntity::removeGuardian);

        // 보호자 비활성화
        guardian.deactivate();
    }

    @Transactional
    public void assignGuardianToMember(Long memberId, Long guardianId) {
        MemberEntity member = findMemberById(memberId);
        GuardianEntity guardian = findGuardianById(guardianId);

        member.assignGuardian(guardian);
        memberRepository.save(member);
    }

    public List<MemberResponse> getMembersByGuardian(Long guardianId) {
        GuardianEntity guardian = findGuardianById(guardianId);
        List<MemberEntity> members = memberRepository.findByGuardian(guardian);

        return convertToMemberResponses(members);
    }

    @Transactional
    public void removeGuardianFromMember(Long memberId) {
        MemberEntity member = findMemberById(memberId);
        member.removeGuardian();
        memberRepository.save(member);
    }

    // ========== Private Helper Methods ==========

    private MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    private GuardianEntity findGuardianById(Long guardianId) {
        return guardianRepository.findById(guardianId)
            .orElseThrow(() -> new GuardianNotFoundException(guardianId));
    }

    private List<MemberResponse> convertToMemberResponses(List<MemberEntity> members) {
        return members.stream()
            .map(MemberResponse::from)
            .toList();
    }

    private void validateGuardianEmailNotExists(String email) {
        guardianRepository.findByGuardianEmailAndIsActiveTrue(email)
            .ifPresent(guardian -> {
                throw new IllegalArgumentException("Guardian with email already exists: " + email);
            });
    }
}