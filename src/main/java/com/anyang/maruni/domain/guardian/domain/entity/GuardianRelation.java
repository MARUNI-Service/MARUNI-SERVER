package com.anyang.maruni.domain.guardian.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 보호자 관계 유형
 */
@Schema(description = "보호자와 회원의 관계 유형")
public enum GuardianRelation {
    @Schema(description = "가족")
    FAMILY("가족"),

    @Schema(description = "친구")
    FRIEND("친구"),

    @Schema(description = "돌봄제공자")
    CAREGIVER("돌봄제공자"),

    @Schema(description = "이웃")
    NEIGHBOR("이웃"),

    @Schema(description = "기타")
    OTHER("기타");

    private final String description;

    GuardianRelation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}