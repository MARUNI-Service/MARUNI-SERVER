package com.anyang.maruni.domain.guardian.application.dto;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보호자 생성 요청 DTO
 */
@Schema(description = "보호자 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRequestDto {

    @Schema(description = "보호자 이름", example = "김보호", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "보호자 이름은 필수입니다")
    private String guardianName;

    @Schema(description = "보호자 이메일 주소", example = "guardian@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "올바른 이메일 형식이어야 합니다")
    @NotBlank(message = "이메일은 필수입니다")
    private String guardianEmail;

    @Schema(description = "보호자 전화번호", example = "010-1234-5678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String guardianPhone;

    @Schema(description = "보호자와 회원의 관계", example = "FAMILY", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"FAMILY", "FRIEND", "CAREGIVER", "NEIGHBOR", "OTHER"})
    @NotNull(message = "관계는 필수입니다")
    private GuardianRelation relation;

    @Schema(description = "알림 수신 설정", example = "ALL", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PUSH", "EMAIL", "SMS", "ALL"})
    @NotNull(message = "알림 설정은 필수입니다")
    private NotificationPreference notificationPreference;
}