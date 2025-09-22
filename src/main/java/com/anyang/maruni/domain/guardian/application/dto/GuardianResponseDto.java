package com.anyang.maruni.domain.guardian.application.dto;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보호자 응답 DTO
 */
@Schema(description = "보호자 정보 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianResponseDto {

    @Schema(description = "보호자 ID", example = "1")
    private Long id;

    @Schema(description = "보호자 이름", example = "김보호")
    private String guardianName;

    @Schema(description = "보호자 이메일", example = "guardian@example.com")
    private String guardianEmail;

    @Schema(description = "보호자 전화번호", example = "010-1234-5678")
    private String guardianPhone;

    @Schema(description = "보호자와 회원의 관계", example = "FAMILY")
    private GuardianRelation relation;

    @Schema(description = "알림 수신 설정", example = "ALL")
    private NotificationPreference notificationPreference;

    @Schema(description = "활성 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "생성 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2024-01-16T14:20:00")
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