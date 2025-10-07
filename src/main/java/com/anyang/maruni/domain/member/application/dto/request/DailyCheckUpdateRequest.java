package com.anyang.maruni.domain.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "안부 메시지 설정 변경 요청 DTO")
public class DailyCheckUpdateRequest {

	@Schema(description = "안부 메시지 수신 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "안부 메시지 수신 여부는 필수입니다.")
	private Boolean dailyCheckEnabled;
}
