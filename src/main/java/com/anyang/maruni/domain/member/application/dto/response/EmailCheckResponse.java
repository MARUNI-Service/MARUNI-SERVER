package com.anyang.maruni.domain.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 중복 확인 응답 DTO")
public class EmailCheckResponse {

	@Schema(description = "이메일 사용 가능 여부", example = "true")
	private boolean available;

	@Schema(description = "확인한 이메일 주소", example = "test@example.com")
	private String email;

	public static EmailCheckResponse of(String email, boolean available) {
		return EmailCheckResponse.builder()
			.email(email)
			.available(available)
			.build();
	}
}
