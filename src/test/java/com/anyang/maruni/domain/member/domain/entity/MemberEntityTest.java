package com.anyang.maruni.domain.member.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.anyang.maruni.global.oauth2.domain.entity.SocialType;

@ActiveProfiles("test")
@DisplayName("MemberEntity 도메인 로직 테스트")
class MemberEntityTest {

	@Test
	@DisplayName("일반 회원 생성 - createRegularMember 정적 팩토리 메서드")
	void createRegularMember_Success() {
		// given
		String email = "test@example.com";
		String name = "테스트 사용자";
		String password = "encodedPassword123";

		// when
		MemberEntity member = MemberEntity.createRegularMember(email, name, password);

		// then
		assertThat(member.getMemberEmail()).isEqualTo(email);
		assertThat(member.getMemberName()).isEqualTo(name);
		assertThat(member.getMemberPassword()).isEqualTo(password);
		assertThat(member.getSocialType()).isNull();
		assertThat(member.getSocialId()).isNull();
		assertThat(member.getId()).isNull(); // 아직 저장되지 않았으므로 null
	}

	@Test
	@DisplayName("소셜 회원 생성 - createSocialMember 정적 팩토리 메서드")
	void createSocialMember_Success() {
		// given
		String email = "social@example.com";
		String name = "소셜 사용자";
		String password = "encodedSocialPassword";
		SocialType socialType = SocialType.GOOGLE;
		String socialId = "google123456";

		// when
		MemberEntity member = MemberEntity.createSocialMember(email, name, password, socialType, socialId);

		// then
		assertThat(member.getMemberEmail()).isEqualTo(email);
		assertThat(member.getMemberName()).isEqualTo(name);
		assertThat(member.getMemberPassword()).isEqualTo(password);
		assertThat(member.getSocialType()).isEqualTo(socialType);
		assertThat(member.getSocialId()).isEqualTo(socialId);
		assertThat(member.getId()).isNull(); // 아직 저장되지 않았으므로 null
	}

	@Test
	@DisplayName("회원 정보 수정 - updateMemberInfo 메서드")
	void updateMemberInfo_Success() {
		// given
		MemberEntity member = MemberEntity.createRegularMember(
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
	@DisplayName("소셜 정보 수정 - updateSocialInfo 메서드")
	void updateSocialInfo_Success() {
		// given
		MemberEntity member = MemberEntity.createRegularMember(
			"regular@example.com",
			"일반사용자",
			"password"
		);

		SocialType newSocialType = SocialType.KAKAO;
		String newSocialId = "kakao789012";

		// when
		member.updateSocialInfo(newSocialType, newSocialId);

		// then
		assertThat(member.getSocialType()).isEqualTo(newSocialType);
		assertThat(member.getSocialId()).isEqualTo(newSocialId);
		// 기존 정보는 유지되어야 함
		assertThat(member.getMemberEmail()).isEqualTo("regular@example.com");
		assertThat(member.getMemberName()).isEqualTo("일반사용자");
		assertThat(member.getMemberPassword()).isEqualTo("password");
	}

	@Test
	@DisplayName("빌더 패턴을 통한 회원 생성")
	void builder_Success() {
		// given & when
		MemberEntity member = MemberEntity.builder()
			.memberEmail("builder@example.com")
			.memberName("빌더사용자")
			.memberPassword("builderPassword")
			.socialType(SocialType.NAVER)
			.socialId("naver345678")
			.build();

		// then
		assertThat(member.getMemberEmail()).isEqualTo("builder@example.com");
		assertThat(member.getMemberName()).isEqualTo("빌더사용자");
		assertThat(member.getMemberPassword()).isEqualTo("builderPassword");
		assertThat(member.getSocialType()).isEqualTo(SocialType.NAVER);
		assertThat(member.getSocialId()).isEqualTo("naver345678");
	}

	@Test
	@DisplayName("Null 값으로 회원 정보 수정 - 허용되는 경우")
	void updateMemberInfo_WithNull() {
		// given
		MemberEntity member = MemberEntity.createRegularMember(
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

	@Test
	@DisplayName("Null 값으로 소셜 정보 수정")
	void updateSocialInfo_WithNull() {
		// given
		MemberEntity member = MemberEntity.createSocialMember(
			"social@example.com",
			"소셜사용자",
			"password",
			SocialType.GOOGLE,
			"google123"
		);

		// when
		member.updateSocialInfo(null, null);

		// then
		assertThat(member.getSocialType()).isNull();
		assertThat(member.getSocialId()).isNull();
		// 기존 정보는 유지
		assertThat(member.getMemberEmail()).isEqualTo("social@example.com");
		assertThat(member.getMemberName()).isEqualTo("소셜사용자");
		assertThat(member.getMemberPassword()).isEqualTo("password");
	}
}