package com.anyang.maruni.domain.member.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.anyang.maruni.domain.member.application.dto.request.MemberSaveRequest;
import com.anyang.maruni.domain.member.application.dto.request.MemberUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.mapper.MemberMapper;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private MemberMapper memberMapper;

	@InjectMocks
	private MemberService memberService;

	@Test
	@DisplayName("회원가입 성공 - 정상적인 요청으로 회원을 저장한다")
	void save_Success() {
		// given
		MemberSaveRequest request = new MemberSaveRequest();
		request.setMemberEmail("test@example.com");
		request.setMemberName("테스트");
		request.setMemberPassword("password123");

		String encodedPassword = "encodedPassword123";
		MemberEntity memberEntity = MemberEntity.createRegularMember(
			request.getMemberEmail(),
			request.getMemberName(),
			encodedPassword
		);

		given(memberRepository.findByMemberEmail(request.getMemberEmail()))
			.willReturn(Optional.empty());
		given(passwordEncoder.encode(request.getMemberPassword()))
			.willReturn(encodedPassword);
		given(memberMapper.toEntity(request, encodedPassword))
			.willReturn(memberEntity);

		// when
		assertThatCode(() -> memberService.save(request))
			.doesNotThrowAnyException();

		// then
		then(memberRepository).should().findByMemberEmail(request.getMemberEmail());
		then(passwordEncoder).should().encode(request.getMemberPassword());
		then(memberMapper).should().toEntity(request, encodedPassword);
		then(memberRepository).should().save(memberEntity);
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복 시 예외를 발생시킨다")
	void save_Fail_DuplicateEmail() {
		// given
		MemberSaveRequest request = new MemberSaveRequest();
		request.setMemberEmail("duplicate@example.com");
		request.setMemberName("테스트");
		request.setMemberPassword("password123");

		MemberEntity existingMember = MemberEntity.createRegularMember(
			request.getMemberEmail(),
			"기존회원",
			"encodedPassword"
		);

		given(memberRepository.findByMemberEmail(request.getMemberEmail()))
			.willReturn(Optional.of(existingMember));

		// when & then
		assertThatThrownBy(() -> memberService.save(request))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

		then(passwordEncoder).should(never()).encode(anyString());
		then(memberRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("전체 회원 조회 성공")
	void findAll_Success() {
		// given
		List<MemberEntity> entities = List.of(
			MemberEntity.createRegularMember("user1@example.com", "사용자1", "password1"),
			MemberEntity.createRegularMember("user2@example.com", "사용자2", "password2")
		);

		List<MemberResponse> responses = List.of(
			MemberResponse.builder().id(1L).memberEmail("user1@example.com").memberName("사용자1").build(),
			MemberResponse.builder().id(2L).memberEmail("user2@example.com").memberName("사용자2").build()
		);

		given(memberRepository.findAll()).willReturn(entities);
		given(memberMapper.toResponseList(entities)).willReturn(responses);

		// when
		List<MemberResponse> result = memberService.findAll();

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getMemberEmail()).isEqualTo("user1@example.com");
		assertThat(result.get(1).getMemberEmail()).isEqualTo("user2@example.com");

		then(memberRepository).should().findAll();
		then(memberMapper).should().toResponseList(entities);
	}

	@Test
	@DisplayName("ID로 회원 조회 성공")
	void findById_Success() {
		// given
		Long memberId = 1L;
		MemberEntity entity = MemberEntity.createRegularMember("test@example.com", "테스트", "password");
		MemberResponse response = MemberResponse.builder()
			.id(memberId)
			.memberEmail("test@example.com")
			.memberName("테스트")
			.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(entity));
		given(memberMapper.toResponse(entity)).willReturn(response);

		// when
		MemberResponse result = memberService.findById(memberId);

		// then
		assertThat(result.getId()).isEqualTo(memberId);
		assertThat(result.getMemberEmail()).isEqualTo("test@example.com");

		then(memberRepository).should().findById(memberId);
		then(memberMapper).should().toResponse(entity);
	}

	@Test
	@DisplayName("ID로 회원 조회 실패 - 존재하지 않는 회원")
	void findById_Fail_MemberNotFound() {
		// given
		Long memberId = 999L;
		given(memberRepository.findById(memberId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> memberService.findById(memberId))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

		then(memberRepository).should().findById(memberId);
		then(memberMapper).should(never()).toResponse(any());
	}

	@Test
	@DisplayName("회원 정보 수정 성공")
	void update_Success() {
		// given
		Long memberId = 1L;
		MemberUpdateRequest request = new MemberUpdateRequest();
		request.setId(memberId);
		request.setMemberName("수정된이름");
		request.setMemberPassword("newPassword123");

		String encodedPassword = "encodedNewPassword123";
		MemberEntity entity = MemberEntity.createRegularMember("test@example.com", "기존이름", "oldPassword");

		given(memberRepository.findById(memberId)).willReturn(Optional.of(entity));
		given(passwordEncoder.encode(request.getMemberPassword())).willReturn(encodedPassword);

		// when
		assertThatCode(() -> memberService.update(request))
			.doesNotThrowAnyException();

		// then
		then(memberRepository).should().findById(memberId);
		then(passwordEncoder).should().encode(request.getMemberPassword());
		then(memberRepository).should().save(entity);
	}

	@Test
	@DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
	void update_Fail_MemberNotFound() {
		// given
		Long memberId = 999L;
		MemberUpdateRequest request = new MemberUpdateRequest();
		request.setId(memberId);
		request.setMemberName("수정된이름");
		request.setMemberPassword("newPassword123");

		given(memberRepository.findById(memberId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> memberService.update(request))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

		then(memberRepository).should().findById(memberId);
		then(passwordEncoder).should(never()).encode(anyString());
		then(memberRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("회원 삭제 성공")
	void deleteById_Success() {
		// given
		Long memberId = 1L;
		given(memberRepository.existsById(memberId)).willReturn(true);

		// when
		assertThatCode(() -> memberService.deleteById(memberId))
			.doesNotThrowAnyException();

		// then
		then(memberRepository).should().existsById(memberId);
		then(memberRepository).should().deleteById(memberId);
	}

	@Test
	@DisplayName("회원 삭제 실패 - 존재하지 않는 회원")
	void deleteById_Fail_MemberNotFound() {
		// given
		Long memberId = 999L;
		given(memberRepository.existsById(memberId)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> memberService.deleteById(memberId))
			.isInstanceOf(BaseException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

		then(memberRepository).should().existsById(memberId);
		then(memberRepository).should(never()).deleteById(anyLong());
	}

	@Test
	@DisplayName("이메일 중복 확인 - 사용 가능한 이메일")
	void isEmailAvailable_True() {
		// given
		String email = "available@example.com";
		given(memberRepository.existsByMemberEmail(email)).willReturn(false);

		// when
		boolean result = memberService.isEmailAvailable(email);

		// then
		assertThat(result).isTrue();
		then(memberRepository).should().existsByMemberEmail(email);
	}

	@Test
	@DisplayName("이메일 중복 확인 - 이미 존재하는 이메일")
	void isEmailAvailable_False() {
		// given
		String email = "existing@example.com";
		given(memberRepository.existsByMemberEmail(email)).willReturn(true);

		// when
		boolean result = memberService.isEmailAvailable(email);

		// then
		assertThat(result).isFalse();
		then(memberRepository).should().existsByMemberEmail(email);
	}
}