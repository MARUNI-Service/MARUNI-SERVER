package com.anyang.maruni.domain.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원 로그인 요청 DTO")
public class MemberLoginRequest {

	@Schema(description = "회원 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 아닙니다.")
	private String memberEmail;

	@Schema(description = "회원 비밀번호", example = "securePassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "비밀번호는 필수입니다.")
	private String memberPassword;
}