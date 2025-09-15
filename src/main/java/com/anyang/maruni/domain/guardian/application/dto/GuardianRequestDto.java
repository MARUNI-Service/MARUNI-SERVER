package com.anyang.maruni.domain.guardian.application.dto;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRequestDto {

    @NotBlank(message = "보호자 이름은 필수입니다")
    private String guardianName;

    @Email(message = "올바른 이메일 형식이어야 합니다")
    @NotBlank(message = "이메일은 필수입니다")
    private String guardianEmail;

    private String guardianPhone;

    @NotNull(message = "관계는 필수입니다")
    private GuardianRelation relation;

    @NotNull(message = "알림 설정은 필수입니다")
    private NotificationPreference notificationPreference;
}