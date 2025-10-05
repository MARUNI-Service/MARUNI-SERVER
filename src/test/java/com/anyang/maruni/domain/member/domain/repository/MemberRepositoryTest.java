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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.config.JpaConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("MemberRepository 데이터 접근 테스트")
class MemberRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private MemberRepository memberRepository;

	private MemberEntity regularMember;

	@BeforeEach
	void setUp() {
		// 일반 회원 생성
		regularMember = MemberEntity.createMember(
			"regular@example.com",
			"일반 사용자",
			"encodedPassword123"
		);

		entityManager.persistAndFlush(regularMember);
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
		Boolean exists = memberRepository.existsByMemberEmail("regular@example.com");

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
	@DisplayName("회원 저장 및 조회")
	void save_And_FindById() {
		// given
		MemberEntity newMember = MemberEntity.createMember(
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
		assertThat(members).hasSize(1);
		assertThat(members)
			.extracting("memberEmail")
			.containsExactly("regular@example.com");
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
	@DisplayName("BaseTimeEntity 상속 - 생성시간과 수정시간 자동 설정")
	void baseTimeEntity_AutoTimestamp() {
		// given
		MemberEntity newMember = MemberEntity.createMember(
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