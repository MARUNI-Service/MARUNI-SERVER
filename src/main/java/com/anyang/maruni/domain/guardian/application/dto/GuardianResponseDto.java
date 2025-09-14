package com.anyang.maruni.domain.guardian.application.dto;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보호자 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianResponseDto {

    private Long id;
    private String guardianName;
    private String guardianEmail;
    private String guardianPhone;
    private GuardianRelation relation;
    private NotificationPreference notificationPreference;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GuardianResponseDto from(GuardianEntity entity) {
        return GuardianResponseDto.builder()
            .id(entity.getId())
            .guardianName(entity.getGuardianName())
            .guardianEmail(entity.getGuardianEmail())
            .guardianPhone(entity.getGuardianPhone())
            .relation(entity.getRelation())
            .notificationPreference(entity.getNotificationPreference())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}