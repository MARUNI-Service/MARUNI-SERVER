package com.anyang.maruni.domain.guardian.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Guardian Entity TDD Red 테스트
 *
 * 일대다 관계 기반 Guardian Entity 테스트
 * - Guardian 생성 및 기본 동작
 * - 비즈니스 로직 메서드
 * - 상태 변경 메서드
 */
@DisplayName("Guardian Entity 테스트")
class GuardianEntityTest {

    @Test
    @DisplayName("보호자를 생성할 수 있다")
    void createGuardian_shouldCreateValidGuardian() {
        // Given
        String name = "김보호자";
        String email = "guardian@example.com";
        String phone = "010-1234-5678";
        GuardianRelation relation = GuardianRelation.FAMILY;
        NotificationPreference preference = NotificationPreference.ALL;

        // When
        GuardianEntity guardian = GuardianEntity.createGuardian(name, email, phone, relation, preference);

        // Then
        assertThat(guardian).isNotNull();
        assertThat(guardian.getGuardianName()).isEqualTo(name);
        assertThat(guardian.getGuardianEmail()).isEqualTo(email);
        assertThat(guardian.getGuardianPhone()).isEqualTo(phone);
        assertThat(guardian.getRelation()).isEqualTo(relation);
        assertThat(guardian.getNotificationPreference()).isEqualTo(preference);
        assertThat(guardian.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("보호자 알림 설정을 변경할 수 있다")
    void updateNotificationPreference_shouldUpdatePreference() {
        // Given
        GuardianEntity guardian = GuardianEntity.createGuardian(
            "김보호자", "guardian@example.com", "010-1234-5678",
            GuardianRelation.FAMILY, NotificationPreference.PUSH
        );
        NotificationPreference newPreference = NotificationPreference.ALL;

        // When
        guardian.updateNotificationPreference(newPreference);

        // Then
        assertThat(guardian.getNotificationPreference()).isEqualTo(newPreference);
    }

    @Test
    @DisplayName("보호자를 비활성화할 수 있다")
    void deactivate_shouldSetInactive() {
        // Given
        GuardianEntity guardian = GuardianEntity.createGuardian(
            "김보호자", "guardian@example.com", "010-1234-5678",
            GuardianRelation.FAMILY, NotificationPreference.PUSH
        );

        // When
        guardian.deactivate();

        // Then
        assertThat(guardian.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("보호자 정보를 수정할 수 있다")
    void updateGuardianInfo_shouldUpdateInfo() {
        // Given
        GuardianEntity guardian = GuardianEntity.createGuardian(
            "김보호자", "guardian@example.com", "010-1234-5678",
            GuardianRelation.FAMILY, NotificationPreference.PUSH
        );
        String newName = "박보호자";
        String newPhone = "010-9876-5432";

        // When
        guardian.updateGuardianInfo(newName, newPhone);

        // Then
        assertThat(guardian.getGuardianName()).isEqualTo(newName);
        assertThat(guardian.getGuardianPhone()).isEqualTo(newPhone);
    }
}