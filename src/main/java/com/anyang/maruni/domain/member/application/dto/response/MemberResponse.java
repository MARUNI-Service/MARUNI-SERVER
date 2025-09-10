package com.anyang.maruni.domain.member.application.dto.response;

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


	public static MemberResponse fromEntity(MemberEntity memberEntity) {
		return MemberResponse.builder()
			.id(memberEntity.getId())
			.memberName(memberEntity.getMemberName())
			.memberEmail(memberEntity.getMemberEmail())
			.build();
	}
}