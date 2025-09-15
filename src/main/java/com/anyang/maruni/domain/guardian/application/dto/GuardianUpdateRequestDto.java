package com.anyang.maruni.domain.guardian.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보호자 정보 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "보호자 정보 수정 요청")
public class GuardianUpdateRequestDto {

    @NotBlank(message = "보호자 이름은 필수입니다")
    @Schema(description = "보호자 이름", example = "김보호자")
    private String guardianName;

    @Schema(description = "보호자 전화번호", example = "010-1234-5678")
    private String guardianPhone;
}