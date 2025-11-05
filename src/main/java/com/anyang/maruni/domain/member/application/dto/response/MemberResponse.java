package com.anyang.maruni.domain.member.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 정보 응답 DTO")
public class MemberResponse {

	@Schema(description = "회원 고유 ID", example = "1")
	private Long id;

	@Schema(description = "회원 이름", example = "홍길동")
	private String memberName;

	@Schema(description = "회원 이메일", example = "hong@example.com")
	private String memberEmail;

	@Schema(description = "안부 메시지 수신 여부", example = "true")
	private Boolean dailyCheckEnabled;

	@Schema(description = "보호자 정보 (없으면 null)")
	private GuardianInfo guardian;

	@Schema(description = "내가 돌보는 사람들 (없으면 빈 배열)")
	private List<ManagedMemberInfo> managedMembers;

	@Schema(description = "생성일시")
	private LocalDateTime createdAt;

	@Schema(description = "수정일시")
	private LocalDateTime updatedAt;

	// ========== 중첩 DTO ==========

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(description = "보호자 정보")
	public static class GuardianInfo {
		@Schema(description = "보호자 ID")
		private Long memberId;

		@Schema(description = "보호자 이름")
		private String memberName;

		@Schema(description = "보호자 이메일")
		private String memberEmail;

		@Schema(description = "관계", example = "FAMILY")
		private GuardianRelation relation;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(description = "돌봄 대상 정보")
	public static class ManagedMemberInfo {
		@Schema(description = "돌봄 대상 ID")
		private Long memberId;

		@Schema(description = "돌봄 대상 이름")
		private String memberName;

		@Schema(description = "돌봄 대상 이메일")
		private String memberEmail;

		@Schema(description = "관계", example = "FAMILY")
		private GuardianRelation relation;

		@Schema(description = "안부 메시지 수신 여부")
		private Boolean dailyCheckEnabled;

		@Schema(description = "최근 안부 메시지 응답 시각")
		private LocalDateTime lastDailyCheckAt;
	}

	// ========== 정적 팩토리 메서드 ==========

	/**
	 * 기본 변환 (기존 호환)
	 */
	public static MemberResponse from(MemberEntity entity) {
		return MemberResponse.builder()
			.id(entity.getId())
			.memberName(entity.getMemberName())
			.memberEmail(entity.getMemberEmail())
			.dailyCheckEnabled(entity.getDailyCheckEnabled())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	/**
	 * 역할 정보 포함 변환 (신규)
	 */
	public static MemberResponse fromWithRoles(MemberEntity entity) {
		return MemberResponse.builder()
			.id(entity.getId())
			.memberName(entity.getMemberName())
			.memberEmail(entity.getMemberEmail())
			.dailyCheckEnabled(entity.getDailyCheckEnabled())
			.guardian(toGuardianInfo(entity))
			.managedMembers(toManagedMemberInfoList(entity))
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	private static GuardianInfo toGuardianInfo(MemberEntity entity) {
		if (entity.getGuardian() == null) {
			return null;
		}

		MemberEntity guardian = entity.getGuardian();
		return GuardianInfo.builder()
			.memberId(guardian.getId())
			.memberName(guardian.getMemberName())
			.memberEmail(guardian.getMemberEmail())
			.relation(entity.getGuardianRelation())
			.build();
	}

	private static List<ManagedMemberInfo> toManagedMemberInfoList(MemberEntity entity) {
		return entity.getManagedMembers().stream()
			.map(managedMember -> ManagedMemberInfo.builder()
				.memberId(managedMember.getId())
				.memberName(managedMember.getMemberName())
				.memberEmail(managedMember.getMemberEmail())
				.relation(managedMember.getGuardianRelation())
				.dailyCheckEnabled(managedMember.getDailyCheckEnabled())
				.lastDailyCheckAt(null)  // TODO: DailyCheckRecord에서 조회
				.build())
			.collect(Collectors.toList());
	}
}