package com.anyang.maruni.domain.member.domain.entity;

import com.anyang.maruni.global.entity.BaseTimeEntity;
import com.anyang.maruni.global.oauth2.domain.entity.SocialType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member_table", indexes = {
	@Index(name = "idx_member_email", columnList = "memberEmail"),
	@Index(name = "idx_social_type_id", columnList = "socialType, socialId"),
	@Index(name = "idx_role", columnList = "role")
})
public class MemberEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String memberEmail;

	@Column
	private String memberName;

	@Column
	private String memberPassword;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private SocialType socialType;

	@Column(nullable = true)
	private String socialId;

	public static MemberEntity createRegularMember(String email, String name, String password) {
		return MemberEntity.builder()
			.memberEmail(email)
			.memberName(name)
			.memberPassword(password)
			.build();
	}

	public static MemberEntity createSocialMember(String email, String name, String password, SocialType socialType, String socialId) {
		return MemberEntity.builder()
			.memberEmail(email)
			.memberName(name)
			.memberPassword(password)
			.socialType(socialType)
			.socialId(socialId)
			.build();
	}

	public void updateMemberInfo(String name, String password) {
		this.memberName = name;
		this.memberPassword = password;
	}

	public void updateSocialInfo(SocialType socialType, String socialId) {
		this.socialType = socialType;
		this.socialId = socialId;
	}


}
