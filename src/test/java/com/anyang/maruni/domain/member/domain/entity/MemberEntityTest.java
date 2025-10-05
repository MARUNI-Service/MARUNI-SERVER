package com.anyang.maruni.domain.member.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DisplayName("MemberEntity 도메인 로직 테스트")
class MemberEntityTest {

	@Test
	@DisplayName("회원 생성 - createMember 정적 팩토리 메서드")
	void createMember_Success() {
		// given
		String email = "test@example.com";
		String name = "테스트 사용자";
		String password = "encodedPassword123";

		// when
		MemberEntity member = MemberEntity.createMember(email, name, password);

		// then
		assertThat(member.getMemberEmail()).isEqualTo(email);
		assertThat(member.getMemberName()).isEqualTo(name);
		assertThat(member.getMemberPassword()).isEqualTo(password);
		assertThat(member.getId()).isNull(); // 아직 저장되지 않았으므로 null
	}

	@Test
	@DisplayName("회원 정보 수정 - updateMemberInfo 메서드")
	void updateMemberInfo_Success() {
		// given
		MemberEntity member = MemberEntity.createMember(
			"original@example.com",
			"원래이름",
			"originalPassword"
		);

		String newName = "수정된이름";
		String newPassword = "newEncodedPassword";

		// when
		member.updateMemberInfo(newName, newPassword);

		// then
		assertThat(member.getMemberName()).isEqualTo(newName);
		assertThat(member.getMemberPassword()).isEqualTo(newPassword);
		// 이메일은 변경되지 않아야 함
		assertThat(member.getMemberEmail()).isEqualTo("original@example.com");
	}

	@Test
	@DisplayName("빌더 패턴을 통한 회원 생성")
	void builder_Success() {
		// given & when
		MemberEntity member = MemberEntity.builder()
			.memberEmail("builder@example.com")
			.memberName("빌더사용자")
			.memberPassword("builderPassword")
			.build();

		// then
		assertThat(member.getMemberEmail()).isEqualTo("builder@example.com");
		assertThat(member.getMemberName()).isEqualTo("빌더사용자");
		assertThat(member.getMemberPassword()).isEqualTo("builderPassword");
	}

	@Test
	@DisplayName("Null 값으로 회원 정보 수정 - 허용되는 경우")
	void updateMemberInfo_WithNull() {
		// given
		MemberEntity member = MemberEntity.createMember(
			"test@example.com",
			"원래이름",
			"originalPassword"
		);

		// when
		member.updateMemberInfo(null, null);

		// then
		assertThat(member.getMemberName()).isNull();
		assertThat(member.getMemberPassword()).isNull();
		assertThat(member.getMemberEmail()).isEqualTo("test@example.com"); // 이메일은 유지
	}
}