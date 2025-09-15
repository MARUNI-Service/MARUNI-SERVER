package com.anyang.maruni.domain.guardian.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Guardian Service (TDD Red - 더미 구현)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final MemberRepository memberRepository;

    // TDD Green: 실제 구현

    @Transactional
    public GuardianResponseDto createGuardian(GuardianRequestDto request) {
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

    @Transactional
    public void assignGuardianToMember(Long memberId, Long guardianId) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        GuardianEntity guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("Guardian not found: " + guardianId));

        member.assignGuardian(guardian);
        memberRepository.save(member);
    }

    public List<MemberResponse> getMembersByGuardian(Long guardianId) {
        GuardianEntity guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("Guardian not found: " + guardianId));

        List<MemberEntity> members = memberRepository.findByGuardian(guardian);
        return members.stream()
            .map(MemberResponse::from)
            .toList();
    }

    @Transactional
    public void removeGuardianFromMember(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        member.removeGuardian();
        memberRepository.save(member);
    }
}