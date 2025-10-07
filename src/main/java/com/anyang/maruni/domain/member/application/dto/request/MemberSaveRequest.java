package com.anyang.maruni.domain.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청 DTO")
public class MemberSaveRequest {

	@Schema(description = "회원 이메일", example = "newuser@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 아닙니다.")
	private String memberEmail;

	@Schema(description = "회원 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "이름은 필수입니다.")
	private String memberName;

	@Schema(description = "회원 비밀번호", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "비밀번호는 필수입니다.")
	private String memberPassword;

	// ========== 신규 필드 (Phase 1) ==========

	@Schema(description = "안부 메시지 수신 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "안부 메시지 수신 여부는 필수입니다.")
	private Boolean dailyCheckEnabled;
}