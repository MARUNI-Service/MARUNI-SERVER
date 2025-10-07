package com.anyang.maruni.domain.guardian.application.dto;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRequest;
import com.anyang.maruni.domain.guardian.domain.entity.RequestStatus;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 보호자 요청 응답 DTO
 */
@Getter
@Builder
@Schema(description = "보호자 요청 응답 DTO")
public class GuardianRequestResponse {

	@Schema(description = "요청 ID", example = "1")
	private Long id;

	@Schema(description = "요청자 정보")
	private MemberInfo requester;

	@Schema(description = "보호자 정보")
	private MemberInfo guardian;

	@Schema(description = "관계", example = "FAMILY")
	private GuardianRelation relation;

	@Schema(description = "요청 상태", example = "PENDING")
	private RequestStatus status;

	@Schema(description = "요청 생성 시간")
	private LocalDateTime createdAt;

	/**
	 * GuardianRequest Entity → GuardianRequestResponse DTO 변환
	 *
	 * @param request GuardianRequest Entity
	 * @return GuardianRequestResponse DTO
	 */
	public static GuardianRequestResponse from(GuardianRequest request) {
		return GuardianRequestResponse.builder()
			.id(request.getId())
			.requester(MemberInfo.from(request.getRequester()))
			.guardian(MemberInfo.from(request.getGuardian()))
			.relation(request.getRelation())
			.status(request.getStatus())
			.createdAt(request.getCreatedAt())
			.build();
	}

	/**
	 * 회원 간단 정보 DTO
	 */
	@Getter
	@Builder
	@Schema(description = "회원 간단 정보")
	public static class MemberInfo {
		@Schema(description = "회원 ID")
		private Long id;

		@Schema(description = "회원 이름")
		private String name;

		@Schema(description = "회원 이메일")
		private String email;

		/**
		 * MemberEntity → MemberInfo 변환
		 *
		 * @param member MemberEntity
		 * @return MemberInfo DTO
		 */
		public static MemberInfo from(MemberEntity member) {
			return MemberInfo.builder()
				.id(member.getId())
				.name(member.getMemberName())
				.email(member.getMemberEmail())
				.build();
		}
	}
}
