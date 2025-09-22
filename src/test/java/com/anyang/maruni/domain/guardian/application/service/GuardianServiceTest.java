package com.anyang.maruni.domain.guardian.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.application.exception.GuardianEmailAlreadyExistsException;
import com.anyang.maruni.domain.guardian.application.mapper.GuardianMapper;
import com.anyang.maruni.domain.guardian.application.validator.GuardianValidator;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRepository;
import com.anyang.maruni.domain.guardian.domain.service.GuardianDomainService;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;

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

    @Mock
    private GuardianValidator guardianValidator;

    @Mock
    private GuardianMapper guardianMapper;

    @Mock
    private GuardianDomainService guardianDomainService;

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

        GuardianEntity savedGuardian = mock(GuardianEntity.class);
        GuardianResponseDto expectedResponse = GuardianResponseDto.builder()
            .id(1L)
            .guardianName("김보호자")
            .guardianEmail("guardian@example.com")
            .guardianPhone("010-1234-5678")
            .relation(GuardianRelation.FAMILY)
            .notificationPreference(NotificationPreference.ALL)
            .isActive(true)
            .build();

        // Mock 설정
        given(guardianRepository.save(any(GuardianEntity.class))).willReturn(savedGuardian);
        given(guardianMapper.toResponseDto(savedGuardian)).willReturn(expectedResponse);

        // When
        GuardianResponseDto response = guardianService.createGuardian(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getGuardianName()).isEqualTo("김보호자");
        assertThat(response.getGuardianEmail()).isEqualTo("guardian@example.com");
        verify(guardianValidator).validateEmailNotExists("guardian@example.com");
        verify(guardianRepository).save(any(GuardianEntity.class));
        verify(guardianMapper).toResponseDto(savedGuardian);
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
        GuardianEntity guardian = mock(GuardianEntity.class);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(guardianRepository.findById(guardianId)).willReturn(Optional.of(guardian));

        // When
        guardianService.assignGuardianToMember(memberId, guardianId);

        // Then
        verify(memberRepository).findById(memberId);
        verify(guardianRepository).findById(guardianId);
        verify(guardianDomainService).establishGuardianRelation(member, guardian);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("보호자가 담당하는 회원 목록을 조회할 수 있다")
    void getMembersByGuardian_shouldReturnMemberList() {
        // Given
        Long guardianId = 1L;
        GuardianEntity guardian = mock(GuardianEntity.class);

        List<MemberEntity> members = Arrays.asList(
            MemberEntity.createRegularMember("member1@example.com", "김회원1", "password123"),
            MemberEntity.createRegularMember("member2@example.com", "김회원2", "password123")
        );

        List<MemberResponse> expectedResponses = Arrays.asList(
            MemberResponse.builder().id(1L).memberName("김회원1").build(),
            MemberResponse.builder().id(2L).memberName("김회원2").build()
        );

        given(guardianRepository.findById(guardianId)).willReturn(Optional.of(guardian));
        given(memberRepository.findByGuardian(guardian)).willReturn(members);
        given(guardianMapper.toMemberResponseList(members)).willReturn(expectedResponses);

        // When
        List<MemberResponse> responses = guardianService.getMembersByGuardian(guardianId);

        // Then
        assertThat(responses).hasSize(2);
        verify(guardianRepository).findById(guardianId);
        verify(memberRepository).findByGuardian(guardian);
        verify(guardianMapper).toMemberResponseList(members);
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
        verify(guardianDomainService).removeGuardianRelation(member);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("중복된 보호자 이메일로 생성 시 GuardianEmailAlreadyExistsException 발생")
    void createGuardian_withDuplicateEmail_shouldThrowGuardianEmailAlreadyExistsException() {
        // Given
        String duplicateEmail = "guardian@example.com";
        GuardianRequestDto request = GuardianRequestDto.builder()
            .guardianName("김보호자")
            .guardianEmail(duplicateEmail)
            .guardianPhone("010-1234-5678")
            .relation(GuardianRelation.FAMILY)
            .notificationPreference(NotificationPreference.ALL)
            .build();

        // GuardianValidator가 예외를 던지도록 설정
        doThrow(new GuardianEmailAlreadyExistsException())
            .when(guardianValidator).validateEmailNotExists(duplicateEmail);

        // When & Then
        assertThatThrownBy(() -> guardianService.createGuardian(request))
            .isInstanceOf(GuardianEmailAlreadyExistsException.class)
            .hasMessageContaining("이미 등록된 보호자 이메일입니다");

        verify(guardianValidator).validateEmailNotExists(duplicateEmail);
        verify(guardianRepository, never()).save(any(GuardianEntity.class));
    }
}