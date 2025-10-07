package com.anyang.maruni.domain.guardian.application.dto;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 보호자 요청 생성 DTO
 */
@Getter
@Setter
@Schema(description = "보호자 요청 생성 DTO")
public class GuardianRequestDto {

	@Schema(description = "보호자 회원 ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "보호자 ID는 필수입니다")
	private Long guardianId;

	@Schema(description = "관계", example = "FAMILY", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "관계는 필수입니다")
	private GuardianRelation relation;
}
