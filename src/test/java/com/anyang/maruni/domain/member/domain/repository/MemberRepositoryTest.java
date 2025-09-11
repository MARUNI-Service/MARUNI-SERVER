package com.anyang.maruni.domain.member.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.oauth2.domain.entity.SocialType;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MemberRepository 데이터 접근 테스트")
class MemberRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private MemberRepository memberRepository;

	private MemberEntity regularMember;
	private MemberEntity socialMember;

	@BeforeEach
	void setUp() {
		// 일반 회원 생성
		regularMember = MemberEntity.createRegularMember(
			"regular@example.com",
			"일반 사용자",
			"encodedPassword123"
		);

		// 소셜 회원 생성
		socialMember = MemberEntity.createSocialMember(
			"social@example.com",
			"소셜 사용자",
			"encodedSocialPassword",
			SocialType.GOOGLE,
			"google12345"
		);

		entityManager.persistAndFlush(regularMember);
		entityManager.persistAndFlush(socialMember);
	}

	@Test
	@DisplayName("이메일로 회원 조회 성공")
	void findByMemberEmail_Success() {
		// when
		Optional<MemberEntity> result = memberRepository.findByMemberEmail("regular@example.com");

		// then
		assertThat(result).isPresent();
		assertThat(result.get().getMemberName()).isEqualTo("일반 사용자");
		assertThat(result.get().getMemberPassword()).isEqualTo("encodedPassword123");
		assertThat(result.get().getSocialType()).isNull();
	}

	@Test
	@DisplayName("이메일로 회원 조회 실패 - 존재하지 않는 이메일")
	void findByMemberEmail_NotFound() {
		// when
		Optional<MemberEntity> result = memberRepository.findByMemberEmail("notexist@example.com");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("이메일 존재 여부 확인 - 존재하는 경우")
	void existsByMemberEmail_True() {
		// when
		Boolean exists = memberRepository.existsByMemberEmail("social@example.com");

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("이메일 존재 여부 확인 - 존재하지 않는 경우")
	void existsByMemberEmail_False() {
		// when
		Boolean exists = memberRepository.existsByMemberEmail("nonexistent@example.com");

		// then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("소셜 타입과 소셜 ID로 회원 조회 성공")
	void findBySocialTypeAndSocialId_Success() {
		// when
		Optional<MemberEntity> result = memberRepository.findBySocialTypeAndSocialId(
			SocialType.GOOGLE,
			"google12345"
		);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().getMemberEmail()).isEqualTo("social@example.com");
		assertThat(result.get().getMemberName()).isEqualTo("소셜 사용자");
		assertThat(result.get().getSocialType()).isEqualTo(SocialType.GOOGLE);
		assertThat(result.get().getSocialId()).isEqualTo("google12345");
	}

	@Test
	@DisplayName("소셜 타입과 소셜 ID로 회원 조회 실패 - 존재하지 않는 조합")
	void findBySocialTypeAndSocialId_NotFound() {
		// when
		Optional<MemberEntity> result = memberRepository.findBySocialTypeAndSocialId(
			SocialType.KAKAO,
			"kakao12345"
		);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("소셜 타입과 소셜 ID로 회원 조회 실패 - 잘못된 소셜 ID")
	void findBySocialTypeAndSocialId_WrongSocialId() {
		// when
		Optional<MemberEntity> result = memberRepository.findBySocialTypeAndSocialId(
			SocialType.GOOGLE,
			"wrongGoogleId"
		);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("회원 저장 및 조회")
	void save_And_FindById() {
		// given
		MemberEntity newMember = MemberEntity.createRegularMember(
			"new@example.com",
			"새로운 사용자",
			"newPassword"
		);

		// when
		MemberEntity saved = memberRepository.save(newMember);
		Optional<MemberEntity> found = memberRepository.findById(saved.getId());

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getMemberEmail()).isEqualTo("new@example.com");
		assertThat(found.get().getMemberName()).isEqualTo("새로운 사용자");
		assertThat(found.get().getId()).isNotNull();
	}

	@Test
	@DisplayName("전체 회원 조회")
	void findAll() {
		// when
		List<MemberEntity> members = memberRepository.findAll();

		// then
		assertThat(members).hasSize(2);
		assertThat(members)
			.extracting("memberEmail")
			.containsExactlyInAnyOrder("regular@example.com", "social@example.com");
	}

	@Test
	@DisplayName("회원 삭제")
	void deleteById() {
		// given
		Long memberId = regularMember.getId();

		// when
		memberRepository.deleteById(memberId);
		entityManager.flush();

		// then
		Optional<MemberEntity> result = memberRepository.findById(memberId);
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("회원 존재 여부 확인 - 존재하는 경우")
	void existsById_True() {
		// when
		boolean exists = memberRepository.existsById(regularMember.getId());

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("회원 존재 여부 확인 - 존재하지 않는 경우")
	void existsById_False() {
		// when
		boolean exists = memberRepository.existsById(999L);

		// then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("회원 정보 수정")
	void update_Member() {
		// given
		MemberEntity member = memberRepository.findById(regularMember.getId()).orElseThrow();

		// when
		member.updateMemberInfo("수정된 이름", "수정된 비밀번호");
		entityManager.flush();

		// then
		MemberEntity updated = memberRepository.findById(regularMember.getId()).orElseThrow();
		assertThat(updated.getMemberName()).isEqualTo("수정된 이름");
		assertThat(updated.getMemberPassword()).isEqualTo("수정된 비밀번호");
		assertThat(updated.getMemberEmail()).isEqualTo("regular@example.com"); // 변경되지 않음
	}

	@Test
	@DisplayName("소셜 정보 수정")
	void update_SocialInfo() {
		// given
		MemberEntity member = memberRepository.findById(socialMember.getId()).orElseThrow();

		// when
		member.updateSocialInfo(SocialType.KAKAO, "kakao67890");
		entityManager.flush();

		// then
		MemberEntity updated = memberRepository.findById(socialMember.getId()).orElseThrow();
		assertThat(updated.getSocialType()).isEqualTo(SocialType.KAKAO);
		assertThat(updated.getSocialId()).isEqualTo("kakao67890");
	}

	@Test
	@DisplayName("BaseTimeEntity 상속 - 생성시간과 수정시간 자동 설정")
	void baseTimeEntity_AutoTimestamp() {
		// given
		MemberEntity newMember = MemberEntity.createRegularMember(
			"timestamp@example.com",
			"시간 테스트",
			"password"
		);

		// when
		MemberEntity saved = memberRepository.save(newMember);
		entityManager.flush();

		// then
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
	}
}