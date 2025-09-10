package com.anyang.maruni.domain.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회원 로그인 응답 DTO")
public class MemberLoginResponse {

	@Schema(description = "회원 고유 ID", example = "1")
	private Long id;

	@Schema(description = "회원 이메일", example = "user@example.com")
	private String memberEmail;

	@Schema(description = "회원 이름", example = "홍길동")
	private String memberName;
}