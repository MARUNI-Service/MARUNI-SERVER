package com.anyang.maruni.domain.member.domain.entity;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member_table", indexes = {
	@Index(name = "idx_member_email", columnList = "memberEmail"),
	@Index(name = "idx_guardian_member_id", columnList = "guardian_member_id"),
	@Index(name = "idx_daily_check_enabled", columnList = "dailyCheckEnabled")
})
public class MemberEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String memberEmail;

	@Column(nullable = false)
	private String memberName;

	@Column(nullable = false)
	private String memberPassword;

	// ========== 기존 필드 ==========

	// 푸시 알림 토큰 (Firebase FCM 토큰)
	@Column(name = "push_token", length = 1000)
	private String pushToken;

	// ========== 신규 필드 ==========

	/**
	 * 안부 메시지 수신 여부
	 * true: 매일 오전 9시 안부 메시지 수신
	 * false: 안부 메시지 수신 안 함
	 */
	@Column(name = "daily_check_enabled", nullable = false)
	@Builder.Default
	private Boolean dailyCheckEnabled = false;

	/**
	 * 내 보호자 (자기 참조 ManyToOne)
	 * null: 보호자가 없음
	 * not null: 보호자가 있음
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guardian_member_id")
	private MemberEntity guardian;

	/**
	 * 내가 돌보는 사람들 (자기 참조 OneToMany)
	 * 비어있음: 보호자 역할 안 함
	 * 1개 이상: 보호자 역할 수행 중
	 */
	@OneToMany(mappedBy = "guardian", fetch = FetchType.LAZY)
	@Builder.Default
	private List<MemberEntity> managedMembers = new ArrayList<>();

	/**
	 * 보호자와의 관계
	 * guardian이 null이면 이 값도 null
	 * guardian이 있으면 관계 타입 (FAMILY, FRIEND, CAREGIVER 등)
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "guardian_relation")
	private GuardianRelation guardianRelation;

	// ========== 정적 팩토리 메서드 ==========

	/**
	 * 일반 회원 생성
	 */
	public static MemberEntity createMember(String email, String name, String password, Boolean dailyCheckEnabled) {
		return MemberEntity.builder()
			.memberEmail(email)
			.memberName(name)
			.memberPassword(password)
			.dailyCheckEnabled(dailyCheckEnabled)
			.build();
	}

	/**
	 * 기존 호환성 유지 (dailyCheckEnabled 기본값 false)
	 */
	public static MemberEntity createMember(String email, String name, String password) {
		return createMember(email, name, password, false);
	}

	// ========== 비즈니스 메서드 ==========

	/**
	 * 회원 정보 수정
	 */
	public void updateMemberInfo(String name, String password) {
		this.memberName = name;
		this.memberPassword = password;
	}

	/**
	 * 푸시 토큰 관리 (기존)
	 */
	public void updatePushToken(String pushToken) {
		this.pushToken = pushToken;
	}

	public void removePushToken() {
		this.pushToken = null;
	}

	public boolean hasPushToken() {
		return this.pushToken != null && !this.pushToken.trim().isEmpty();
	}

	/**
	 * 안부 메시지 설정 변경
	 */
	public void updateDailyCheckEnabled(Boolean enabled) {
		this.dailyCheckEnabled = enabled;
	}

	/**
	 * 보호자 설정
	 */
	public void assignGuardian(MemberEntity guardian, GuardianRelation relation) {
		if (guardian == null) {
			throw new IllegalArgumentException("Guardian cannot be null");
		}
		if (guardian.getId().equals(this.id)) {
			throw new IllegalArgumentException("Cannot assign self as guardian");
		}
		this.guardian = guardian;
		this.guardianRelation = relation;
	}

	/**
	 * 보호자 제거
	 */
	public void removeGuardian() {
		this.guardian = null;
		this.guardianRelation = null;
	}

	/**
	 * 보호자가 있는지 확인
	 */
	public boolean hasGuardian() {
		return this.guardian != null;
	}

	/**
	 * 보호자 역할을 하는지 확인
	 */
	public boolean isGuardianRole() {
		return !this.managedMembers.isEmpty();
	}

	/**
	 * 돌보는 사람 수 조회
	 */
	public int getManagedMembersCount() {
		return this.managedMembers.size();
	}
}
