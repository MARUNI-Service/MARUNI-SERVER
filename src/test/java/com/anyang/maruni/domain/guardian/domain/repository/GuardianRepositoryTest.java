package com.anyang.maruni.domain.guardian.domain.repository;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.entity.NotificationPreference;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Guardian Repository TDD Red 테스트
 *
 * 일대다 관계 기반 Repository 테스트
 * - 기본 CRUD 동작
 * - JPA 쿼리 메서드
 * - Guardian-Member 관계 쿼리
 */
@DataJpaTest
@DisplayName("Guardian Repository 테스트")
class GuardianRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("활성 보호자 목록을 조회할 수 있다")
    void findByIsActiveTrueOrderByCreatedAtDesc_shouldReturnActiveGuardians() {
        // Given
        GuardianEntity activeGuardian = GuardianEntity.createGuardian(
            "김활성보호자", "active@example.com", "010-1111-1111",
            GuardianRelation.FAMILY, NotificationPreference.ALL
        );

        GuardianEntity inactiveGuardian = GuardianEntity.createGuardian(
            "김비활성보호자", "inactive@example.com", "010-2222-2222",
            GuardianRelation.FRIEND, NotificationPreference.PUSH
        );
        inactiveGuardian.deactivate();

        guardianRepository.save(activeGuardian);
        guardianRepository.save(inactiveGuardian);
        entityManager.flush();
        entityManager.clear();

        // When
        List<GuardianEntity> activeGuardians = guardianRepository.findByIsActiveTrueOrderByCreatedAtDesc();

        // Then
        assertThat(activeGuardians).hasSize(1);
        assertThat(activeGuardians.get(0).getGuardianName()).isEqualTo("김활성보호자");
        assertThat(activeGuardians.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("이메일로 활성 보호자를 조회할 수 있다")
    void findByGuardianEmailAndIsActiveTrue_shouldReturnActiveGuardian() {
        // Given
        String email = "guardian@example.com";
        GuardianEntity guardian = GuardianEntity.createGuardian(
            "김보호자", email, "010-1234-5678",
            GuardianRelation.FAMILY, NotificationPreference.PUSH
        );
        guardianRepository.save(guardian);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<GuardianEntity> foundGuardian = guardianRepository.findByGuardianEmailAndIsActiveTrue(email);

        // Then
        assertThat(foundGuardian).isPresent();
        assertThat(foundGuardian.get().getGuardianEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("보호자가 담당하는 회원 수를 조회할 수 있다")
    void countMembersByGuardianId_shouldReturnMemberCount() {
        // Given
        GuardianEntity guardian = GuardianEntity.createGuardian(
            "김보호자", "guardian@example.com", "010-1234-5678",
            GuardianRelation.FAMILY, NotificationPreference.ALL
        );
        guardianRepository.save(guardian);

        MemberEntity member1 = MemberEntity.createRegularMember(
            "member1@example.com", "김회원1", "password123"
        );
        member1.assignGuardian(guardian);
        memberRepository.save(member1);

        MemberEntity member2 = MemberEntity.createRegularMember(
            "member2@example.com", "김회원2", "password123"
        );
        member2.assignGuardian(guardian);
        memberRepository.save(member2);

        entityManager.flush();
        entityManager.clear();

        // When
        long memberCount = guardianRepository.countMembersByGuardianId(guardian.getId());

        // Then
        assertThat(memberCount).isEqualTo(2);
    }
}