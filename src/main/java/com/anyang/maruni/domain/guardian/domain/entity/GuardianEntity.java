package com.anyang.maruni.domain.guardian.domain.entity;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 보호자 엔티티 (TDD Red - 기본 구조)
 */
@Entity
@Table(name = "guardian")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String guardianName;
    private String guardianEmail;
    private String guardianPhone;

    @Enumerated(EnumType.STRING)
    private GuardianRelation relation;

    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference;

    @Builder.Default
    private Boolean isActive = true;

    // 일대다 관계: Guardian → Members
    @OneToMany(mappedBy = "guardian", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberEntity> members = new ArrayList<>();

    // TDD Green: 실제 구현
    public static GuardianEntity createGuardian(
        String name, String email, String phone,
        GuardianRelation relation, NotificationPreference preference) {
        return GuardianEntity.builder()
            .guardianName(name)
            .guardianEmail(email)
            .guardianPhone(phone)
            .relation(relation)
            .notificationPreference(preference)
            .isActive(true)
            .build();
    }

    public void updateNotificationPreference(NotificationPreference preference) {
        this.notificationPreference = preference;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateGuardianInfo(String name, String phone) {
        this.guardianName = name;
        this.guardianPhone = phone;
    }
}