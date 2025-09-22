package com.anyang.maruni.domain.guardian.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.application.exception.GuardianNotFoundException;
import com.anyang.maruni.domain.guardian.application.exception.GuardianNotAssignedException;
import com.anyang.maruni.domain.guardian.application.mapper.GuardianMapper;
import com.anyang.maruni.domain.guardian.application.validator.GuardianValidator;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import com.anyang.maruni.domain.guardian.domain.service.GuardianDomainService;
import com.anyang.maruni.domain.member.application.exception.MemberNotFoundException;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Guardian Service - 보호자 관리 애플리케이션 서비스
 *
 * 리팩토링된 구조: 단일 책임 원칙 적용
 * - GuardianValidator: 유효성 검증 로직
 * - GuardianMapper: DTO 변환 로직
 * - GuardianDomainService: 복잡한 도메인 로직
 * - GuardianService: 애플리케이션 로직 조율
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final MemberRepository memberRepository;
    private final GuardianValidator guardianValidator;
    private final GuardianMapper guardianMapper;
    private final GuardianDomainService guardianDomainService;

    // ========== Public Methods ==========

    @Transactional
    public GuardianResponseDto createGuardian(GuardianRequestDto request) {
        guardianValidator.validateEmailNotExists(request.getGuardianEmail());

        GuardianEntity guardian = GuardianEntity.createGuardian(
            request.getGuardianName(),
            request.getGuardianEmail(),
            request.getGuardianPhone(),
            request.getRelation(),
            request.getNotificationPreference()
        );

        GuardianEntity savedGuardian = guardianRepository.save(guardian);
        return guardianMapper.toResponseDto(savedGuardian);
    }

    public GuardianResponseDto getGuardianById(Long guardianId) {
        GuardianEntity guardian = findGuardianById(guardianId);
        return guardianMapper.toResponseDto(guardian);
    }

    @Transactional
    public GuardianResponseDto updateGuardianInfo(Long guardianId, String name, String phone) {
        GuardianEntity guardian = findGuardianById(guardianId);
        guardian.updateGuardianInfo(name, phone);
        return guardianMapper.toResponseDto(guardian);
    }

    @Transactional
    public void deactivateGuardian(Long guardianId) {
        GuardianEntity guardian = findGuardianById(guardianId);
        List<MemberEntity> connectedMembers = memberRepository.findByGuardian(guardian);

        guardianDomainService.deactivateGuardianSafely(guardian, connectedMembers);
    }

    @Transactional
    public void assignGuardianToMember(Long memberId, Long guardianId) {
        MemberEntity member = findMemberById(memberId);
        GuardianEntity guardian = findGuardianById(guardianId);

        guardianDomainService.establishGuardianRelation(member, guardian);
        memberRepository.save(member);
    }

    public List<MemberResponse> getMembersByGuardian(Long guardianId) {
        GuardianEntity guardian = findGuardianById(guardianId);
        List<MemberEntity> members = memberRepository.findByGuardian(guardian);

        return guardianMapper.toMemberResponseList(members);
    }

    public GuardianResponseDto getGuardianByMemberId(Long memberId) {
        MemberEntity member = findMemberById(memberId);
        GuardianEntity guardian = member.getGuardian();

        if (guardian == null) {
            throw new GuardianNotAssignedException();
        }

        return guardianMapper.toResponseDto(guardian);
    }

    @Transactional
    public void removeGuardianFromMember(Long memberId) {
        MemberEntity member = findMemberById(memberId);
        guardianDomainService.removeGuardianRelation(member);
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
}