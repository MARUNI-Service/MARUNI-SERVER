package com.anyang.maruni.domain.member.domain.entity;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
	@Index(name = "idx_member_email", columnList = "memberEmail")
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

	// Guardian 관계 추가 (다대일)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guardian_id")
	private GuardianEntity guardian;

	// 푸시 알림 토큰 (Firebase FCM 토큰)
	@Column(name = "push_token", length = 1000)
	private String pushToken;

	public static MemberEntity createMember(String email, String name, String password) {
		return MemberEntity.builder()
			.memberEmail(email)
			.memberName(name)
			.memberPassword(password)
			.build();
	}

	public void updateMemberInfo(String name, String password) {
		this.memberName = name;
		this.memberPassword = password;
	}

	// Guardian 관계 비즈니스 메서드
	public void assignGuardian(GuardianEntity guardian) {
		this.guardian = guardian;
	}

	public void removeGuardian() {
		this.guardian = null;
	}

	// 푸시 토큰 관리 메서드
	public void updatePushToken(String pushToken) {
		this.pushToken = pushToken;
	}

	public void removePushToken() {
		this.pushToken = null;
	}

	public boolean hasPushToken() {
		return this.pushToken != null && !this.pushToken.trim().isEmpty();
	}
}
