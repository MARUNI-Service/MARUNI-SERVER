package com.anyang.maruni.domain.guardian.application.service;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Guardian Service TDD Red 테스트
 *
 * 일대다 관계 기반 Guardian Service 테스트
 * - 보호자 생성 및 관리
 * - 회원-보호자 연결 관리
 * - 보호자별 회원 조회
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Guardian Service 테스트")
class GuardianServiceTest {

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private GuardianService guardianService;

    @Test
    @DisplayName("새 보호자를 생성할 수 있다")
    void createGuardian_shouldCreateNewGuardian() {
        // Given
        GuardianRequestDto request = GuardianRequestDto.builder()
            .guardianName("김보호자")
            .guardianEmail("guardian@example.com")
            .guardianPhone("010-1234-5678")
            .relation(GuardianRelation.FAMILY)
            .notificationPreference(NotificationPreference.ALL)
            .build();

        // Mock GuardianEntity 사용 (더미 구현으로 인한 null 문제 해결)
        GuardianEntity savedGuardian = mock(GuardianEntity.class);

        // Mock 객체에 대한 getter 반환값 설정
        given(savedGuardian.getId()).willReturn(1L);
        given(savedGuardian.getGuardianName()).willReturn("김보호자");
        given(savedGuardian.getGuardianEmail()).willReturn("guardian@example.com");
        given(savedGuardian.getGuardianPhone()).willReturn("010-1234-5678");
        given(savedGuardian.getRelation()).willReturn(GuardianRelation.FAMILY);
        given(savedGuardian.getNotificationPreference()).willReturn(NotificationPreference.ALL);
        given(savedGuardian.getIsActive()).willReturn(true);

        given(guardianRepository.save(any(GuardianEntity.class)))
            .willReturn(savedGuardian);

        // When
        GuardianResponseDto response = guardianService.createGuardian(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getGuardianName()).isEqualTo("김보호자");
        assertThat(response.getGuardianEmail()).isEqualTo("guardian@example.com");
        verify(guardianRepository).save(any(GuardianEntity.class));
    }

    @Test
    @DisplayName("회원에게 보호자를 할당할 수 있다")
    void assignGuardianToMember_shouldAssignGuardian() {
        // Given
        Long memberId = 1L;
        Long guardianId = 2L;

        MemberEntity member = MemberEntity.createRegularMember(
            "member@example.com", "김회원", "password123"
        );

        // Mock GuardianEntity 사용 (더미 구현으로 인한 null 문제 해결)
        GuardianEntity guardian = mock(GuardianEntity.class);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(guardianRepository.findById(guardianId)).willReturn(Optional.of(guardian));

        // When
        guardianService.assignGuardianToMember(memberId, guardianId);

        // Then
        verify(memberRepository).findById(memberId);
        verify(guardianRepository).findById(guardianId);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("보호자가 담당하는 회원 목록을 조회할 수 있다")
    void getMembersByGuardian_shouldReturnMemberList() {
        // Given
        Long guardianId = 1L;

        // Mock GuardianEntity 사용 (더미 구현으로 인한 null 문제 해결)
        GuardianEntity guardian = mock(GuardianEntity.class);

        List<MemberEntity> members = Arrays.asList(
            MemberEntity.createRegularMember("member1@example.com", "김회원1", "password123"),
            MemberEntity.createRegularMember("member2@example.com", "김회원2", "password123")
        );

        given(guardianRepository.findById(guardianId)).willReturn(Optional.of(guardian));
        given(memberRepository.findByGuardian(guardian)).willReturn(members);

        // When
        List<MemberResponse> responses = guardianService.getMembersByGuardian(guardianId);

        // Then - 더미 구현으로 인해 빈 리스트 반환, 테스트 실패 예상
        assertThat(responses).hasSize(2);
        verify(guardianRepository).findById(guardianId);
        verify(memberRepository).findByGuardian(guardian);
    }

    @Test
    @DisplayName("회원의 보호자를 제거할 수 있다")
    void removeGuardianFromMember_shouldRemoveGuardian() {
        // Given
        Long memberId = 1L;

        MemberEntity member = MemberEntity.createRegularMember(
            "member@example.com", "김회원", "password123"
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // When
        guardianService.removeGuardianFromMember(memberId);

        // Then
        verify(memberRepository).findById(memberId);
        verify(memberRepository).save(member);
    }
}