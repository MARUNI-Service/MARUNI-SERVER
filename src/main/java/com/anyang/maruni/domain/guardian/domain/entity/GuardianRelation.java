package com.anyang.maruni.domain.guardian.domain.entity;

/**
 * 보호자 관계 유형
 */
public enum GuardianRelation {
    FAMILY("가족"),
    FRIEND("친구"),
    CAREGIVER("돌봄제공자"),
    NEIGHBOR("이웃"),
    OTHER("기타");

    private final String description;

    GuardianRelation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}