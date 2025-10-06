# Phase 1 상세 개발 계획

**MARUNI Foundation Layer - Member + Auth 도메인**

---

**버전**: 1.0.0
**작성일**: 2025-10-07
**예상 기간**: 1주 (5일)
**우선순위**: P0 (최우선)

---

## 📋 목차

1. [개요](#1-개요)
2. [Day 1-2: MemberEntity 스키마 변경](#2-day-1-2-memberentity-스키마-변경)
3. [Day 3-4: MemberService + Controller](#3-day-3-4-memberservice--controller)
4. [Day 5: Auth 도메인 수정](#4-day-5-auth-도메인-수정)
5. [TDD 테스트 계획](#5-tdd-테스트-계획)

---

## 1. 개요

### 1.1 목표

- ✅ GuardianEntity를 제거하고 MemberEntity로 통합
- ✅ 자기 참조 관계 구현 (guardian, managedMembers)
- ✅ dailyCheckEnabled 필드 추가
- ✅ 회원 검색 API 구현
- ✅ 로그인 응답에 역할 정보 추가

### 1.2 완료 기준

```
✅ MemberEntity 스키마 변경 완료
✅ Migration 스크립트 실행 성공
✅ MemberRepository 쿼리 메서드 동작
✅ MemberService 신규 메서드 동작
✅ MemberApiController 신규 API 동작
✅ Auth 로그인 응답에 역할 정보 포함
✅ 모든 단위 테스트 통과
```

---

## 2. Day 1-2: MemberEntity 스키마 변경

### 2.1 MemberEntity 스키마 설계

**파일 경로**: `src/main/java/com/anyang/maruni/domain/member/domain/entity/MemberEntity.java`

**변경 사항**:

```java
package com.anyang.maruni.domain.member.domain.entity;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;  // ✅ Guardian enum 재사용
import com.anyang.maruni.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member_table", indexes = {
    @Index(name = "idx_member_email", columnList = "memberEmail"),
    @Index(name = "idx_guardian_member_id", columnList = "guardian_member_id"),  // ✅ 신규
    @Index(name = "idx_daily_check_enabled", columnList = "dailyCheckEnabled")   // ✅ 신규
})
public class MemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String memberEmail;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String memberPassword;

    // ========== 기존 필드 ==========
    @Column(name = "push_token", length = 1000)
    private String pushToken;

    // ========== 신규 필드 ==========

    /**
     * 안부 메시지 수신 여부
     * true: 매일 오전 9시 안부 메시지 수신
     * false: 안부 메시지 수신 안 함
     */
    @Column(name = "daily_check_enabled", nullable = false)
    @Builder.Default
    private Boolean dailyCheckEnabled = false;

    /**
     * 내 보호자 (자기 참조 ManyToOne)
     * null: 보호자가 없음
     * not null: 보호자가 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_member_id")
    private MemberEntity guardian;

    /**
     * 내가 돌보는 사람들 (자기 참조 OneToMany)
     * 비어있음: 보호자 역할 안 함
     * 1개 이상: 보호자 역할 수행 중
     */
    @OneToMany(mappedBy = "guardian", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberEntity> managedMembers = new ArrayList<>();

    /**
     * 보호자와의 관계
     * guardian이 null이면 이 값도 null
     * guardian이 있으면 관계 타입 (FAMILY, FRIEND, CAREGIVER 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_relation")
    private GuardianRelation guardianRelation;

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 일반 회원 생성 (안부 메시지 수신)
     */
    public static MemberEntity createMember(String email, String name, String password, Boolean dailyCheckEnabled) {
        return MemberEntity.builder()
            .memberEmail(email)
            .memberName(name)
            .memberPassword(password)
            .dailyCheckEnabled(dailyCheckEnabled)
            .build();
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 회원 정보 수정
     */
    public void updateMemberInfo(String name, String password) {
        this.memberName = name;
        this.memberPassword = password;
    }

    /**
     * 푸시 토큰 관리 (기존)
     */
    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public void removePushToken() {
        this.pushToken = null;
    }

    public boolean hasPushToken() {
        return this.pushToken != null && !this.pushToken.trim().isEmpty();
    }

    /**
     * 안부 메시지 설정 변경
     */
    public void updateDailyCheckEnabled(Boolean enabled) {
        this.dailyCheckEnabled = enabled;
    }

    /**
     * 보호자 설정
     */
    public void assignGuardian(MemberEntity guardian, GuardianRelation relation) {
        if (guardian == null) {
            throw new IllegalArgumentException("Guardian cannot be null");
        }
        if (guardian.getId().equals(this.id)) {
            throw new IllegalArgumentException("Cannot assign self as guardian");
        }
        this.guardian = guardian;
        this.guardianRelation = relation;
    }

    /**
     * 보호자 제거
     */
    public void removeGuardian() {
        this.guardian = null;
        this.guardianRelation = null;
    }

    /**
     * 보호자가 있는지 확인
     */
    public boolean hasGuardian() {
        return this.guardian != null;
    }

    /**
     * 보호자 역할을 하는지 확인
     */
    public boolean isGuardianRole() {
        return !this.managedMembers.isEmpty();
    }

    /**
     * 돌보는 사람 수 조회
     */
    public int getManagedMembersCount() {
        return this.managedMembers.size();
    }
}
```

**주요 변경 사항**:
1. ✅ `dailyCheckEnabled` 필드 추가
2. ✅ `guardian` 필드를 MemberEntity로 변경 (자기 참조)
3. ✅ `managedMembers` 필드 추가 (자기 참조 OneToMany)
4. ✅ `guardianRelation` 필드 추가
5. ✅ 인덱스 2개 추가 (guardian_member_id, daily_check_enabled)
6. ✅ 비즈니스 메서드 추가 (assignGuardian, removeGuardian 등)

---

### 2.2 Migration 스크립트

**파일 경로**: `src/main/resources/db/migration/V2_0_0__migrate_guardian_to_member.sql`

```sql
-- ========================================
-- MARUNI v2.0.0 Guardian → Member Migration
-- ========================================
-- 작성일: 2025-10-07
-- 목적: GuardianEntity를 MemberEntity로 통합
-- 주의: 실행 전 반드시 데이터베이스 백업!
-- ========================================

-- Step 1: member_table에 신규 컬럼 추가
ALTER TABLE member_table
ADD COLUMN daily_check_enabled BOOLEAN DEFAULT false NOT NULL,
ADD COLUMN guardian_member_id BIGINT,
ADD COLUMN guardian_relation VARCHAR(50);

-- Step 2: 인덱스 추가
CREATE INDEX idx_guardian_member_id ON member_table(guardian_member_id);
CREATE INDEX idx_daily_check_enabled ON member_table(daily_check_enabled);

-- Step 3: Guardian 데이터를 Member로 마이그레이션
-- Guardian의 이메일로 회원이 이미 있으면 스킵, 없으면 신규 생성
INSERT INTO member_table (
    member_email,
    member_name,
    member_password,  -- 임시 비밀번호 (나중에 변경 필요)
    daily_check_enabled,
    created_at,
    updated_at
)
SELECT
    g.guardian_email,
    g.guardian_name,
    '$2a$10$TEMP_PASSWORD_HASH',  -- 임시 bcrypt 해시 (변경 필요)
    false,  -- 보호자는 기본적으로 안부 메시지 수신 안 함
    g.created_at,
    g.updated_at
FROM guardian g
WHERE NOT EXISTS (
    SELECT 1 FROM member_table m WHERE m.member_email = g.guardian_email
);

-- Step 4: 기존 Member의 guardian_id를 guardian_member_id로 변환
UPDATE member_table m
SET guardian_member_id = (
    SELECT m2.id
    FROM member_table m2
    JOIN guardian g ON g.guardian_email = m2.member_email
    WHERE g.id = (
        -- 기존 guardian 테이블 참조 (guardian_id 컬럼이 있다고 가정)
        SELECT m3.guardian_id
        FROM (SELECT id, guardian_id FROM member_table) m3
        WHERE m3.id = m.id
    )
),
guardian_relation = (
    SELECT g.relation::VARCHAR
    FROM guardian g
    WHERE g.id = (
        SELECT m4.guardian_id
        FROM (SELECT id, guardian_id FROM member_table) m4
        WHERE m4.id = m.id
    )
)
WHERE EXISTS (
    SELECT 1 FROM (SELECT id, guardian_id FROM member_table) m5
    WHERE m5.id = m.id AND m5.guardian_id IS NOT NULL
);

-- Step 5: 외래키 제약 조건 추가 (guardian_member_id → member_table.id)
ALTER TABLE member_table
ADD CONSTRAINT fk_member_guardian
FOREIGN KEY (guardian_member_id)
REFERENCES member_table(id)
ON DELETE SET NULL;

-- Step 6: 기존 guardian_id 컬럼 제거 (있다면)
-- ALTER TABLE member_table DROP COLUMN IF EXISTS guardian_id;

-- Step 7: Guardian 테이블 백업 후 삭제
-- 주의: 실제 운영에서는 백업 후 삭제할 것
-- CREATE TABLE guardian_backup AS SELECT * FROM guardian;
-- DROP TABLE guardian;

-- ========================================
-- Migration 완료
-- ========================================
-- 검증 쿼리:
-- SELECT COUNT(*) FROM member_table WHERE guardian_member_id IS NOT NULL;
-- SELECT COUNT(*) FROM member_table WHERE daily_check_enabled = true;
-- ========================================
```

**Migration 주의 사항**:
1. ⚠️ **백업 필수**: Migration 전 전체 DB 백업
2. ⚠️ **임시 비밀번호**: Guardian → Member 전환 시 임시 비밀번호 설정, 첫 로그인 시 변경 필요
3. ⚠️ **테스트 환경**: 먼저 테스트 DB에서 실행 후 검증
4. ⚠️ **롤백 계획**: Migration 실패 시 롤백 스크립트 준비

---

### 2.3 MemberRepository 확장

**파일 경로**: `src/main/java/com/anyang/maruni/domain/member/domain/repository/MemberRepository.java`

```java
package com.anyang.maruni.domain.member.domain.repository;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    // ========== 기존 메서드 (재사용) ==========

    Optional<MemberEntity> findByMemberEmail(String email);

    boolean existsByMemberEmail(String email);

    // ========== 신규 메서드 (Phase 1) ==========

    /**
     * 안부 메시지 수신자 ID 목록 조회 (DailyCheck용)
     */
    @Query("SELECT m.id FROM MemberEntity m WHERE m.dailyCheckEnabled = true")
    List<Long> findDailyCheckEnabledMemberIds();

    /**
     * 보호자의 돌봄 대상 조회
     * @param guardian 보호자 MemberEntity
     * @return 돌봄 대상 목록
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.guardian = :guardian")
    List<MemberEntity> findByGuardian(@Param("guardian") MemberEntity guardian);

    /**
     * 회원 검색 (이메일 기반)
     * @param email 검색할 이메일
     * @return 회원 정보
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :email")
    Optional<MemberEntity> searchByEmail(@Param("email") String email);

    /**
     * 안부 메시지 수신자 전체 조회 (엔티티)
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.dailyCheckEnabled = true")
    List<MemberEntity> findAllByDailyCheckEnabled();

    /**
     * 보호자 ID로 돌봄 대상 조회 (성능 최적화)
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.guardian.id = :guardianId")
    List<MemberEntity> findManagedMembersByGuardianId(@Param("guardianId") Long guardianId);
}
```

---

### 2.4 GuardianRelation Enum 재사용

**파일 경로**: `src/main/java/com/anyang/maruni/domain/guardian/domain/entity/GuardianRelation.java`

**기존 코드 그대로 사용** (변경 없음):

```java
package com.anyang.maruni.domain.guardian.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuardianRelation {
    FAMILY("가족"),
    FRIEND("지인"),
    CAREGIVER("간병인"),
    MEDICAL_STAFF("의료진"),
    OTHER("기타");

    private final String displayName;
}
```

---

## 3. Day 3-4: MemberService + Controller

### 3.1 MemberService 확장

**파일 경로**: `src/main/java/com/anyang/maruni/domain/member/application/service/MemberService.java`

**신규 메서드 추가**:

```java
package com.anyang.maruni.domain.member.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.member.application.dto.request.MemberSaveRequest;
import com.anyang.maruni.domain.member.application.dto.request.MemberUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.mapper.MemberMapper;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    // ========== 기존 메서드 (재사용) ==========

    @Transactional
    public MemberResponse save(MemberSaveRequest req) {
        // 기존 로직 유지
        // ... (생략)
    }

    public List<MemberResponse> findAll() { /* ... */ }
    public MemberResponse findById(Long id) { /* ... */ }

    @Transactional
    public void update(MemberUpdateRequest req) { /* ... */ }

    @Transactional
    public void deleteById(Long id) { /* ... */ }

    public boolean isEmailAvailable(String memberEmail) { /* ... */ }

    // ========== 신규 메서드 (Phase 1) ==========

    /**
     * 회원 검색 (이메일 기반)
     */
    public MemberResponse searchByEmail(String email) {
        MemberEntity member = memberRepository.searchByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));

        return memberMapper.toResponse(member);
    }

    /**
     * 내가 돌보는 사람들 목록 조회
     */
    public List<MemberResponse> getManagedMembers(Long guardianId) {
        MemberEntity guardian = memberRepository.findById(guardianId)
            .orElseThrow(() -> memberNotFound());

        List<MemberEntity> managedMembers = memberRepository.findByGuardian(guardian);

        return managedMembers.stream()
            .map(memberMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * 안부 메시지 설정 변경
     */
    @Transactional
    public MemberResponse updateDailyCheckEnabled(Long memberId, Boolean enabled) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> memberNotFound());

        member.updateDailyCheckEnabled(enabled);
        memberRepository.save(member);

        log.info("Daily check enabled updated: memberId={}, enabled={}", memberId, enabled);

        return memberMapper.toResponse(member);
    }

    /**
     * 내 프로필 조회 (역할 정보 포함)
     * API 명세서의 GET /members/me 응답 형식
     */
    public MemberResponse getMyProfile(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> memberNotFound());

        // MemberResponse에 guardian, managedMembers 정보 포함
        return memberMapper.toResponseWithRoles(member);
    }

    // ========== Private Helper Methods ==========

    private BaseException memberNotFound() {
        return new BaseException(ErrorCode.MEMBER_NOT_FOUND);
    }
}
```

---

### 3.2 MemberResponse 확장

**파일 경로**: `src/main/java/com/anyang/maruni/domain/member/application/dto/response/MemberResponse.java`

**변경 사항**:

```java
package com.anyang.maruni.domain.member.application.dto.response;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 정보 응답 DTO")
public class MemberResponse {

    @Schema(description = "회원 고유 ID", example = "1")
    private Long id;

    @Schema(description = "회원 이름", example = "홍길동")
    private String memberName;

    @Schema(description = "회원 이메일", example = "hong@example.com")
    private String memberEmail;

    // ========== 신규 필드 ==========

    @Schema(description = "안부 메시지 수신 여부", example = "true")
    private Boolean dailyCheckEnabled;

    @Schema(description = "푸시 토큰 등록 여부", example = "true")
    private Boolean hasPushToken;

    @Schema(description = "보호자 정보 (없으면 null)")
    private GuardianInfo guardian;

    @Schema(description = "내가 돌보는 사람들 (없으면 빈 배열)")
    private List<ManagedMemberInfo> managedMembers;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    // ========== 중첩 DTO ==========

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "보호자 정보")
    public static class GuardianInfo {
        @Schema(description = "보호자 ID")
        private Long memberId;

        @Schema(description = "보호자 이름")
        private String memberName;

        @Schema(description = "보호자 이메일")
        private String memberEmail;

        @Schema(description = "관계", example = "FAMILY")
        private GuardianRelation relation;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "돌봄 대상 정보")
    public static class ManagedMemberInfo {
        @Schema(description = "돌봄 대상 ID")
        private Long memberId;

        @Schema(description = "돌봄 대상 이름")
        private String memberName;

        @Schema(description = "돌봄 대상 이메일")
        private String memberEmail;

        @Schema(description = "관계", example = "FAMILY")
        private GuardianRelation relation;

        @Schema(description = "안부 메시지 수신 여부")
        private Boolean dailyCheckEnabled;

        @Schema(description = "최근 안부 메시지 응답 시각")
        private LocalDateTime lastDailyCheckAt;
    }

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 기본 변환 (기존 호환)
     */
    public static MemberResponse from(MemberEntity entity) {
        return MemberResponse.builder()
            .id(entity.getId())
            .memberName(entity.getMemberName())
            .memberEmail(entity.getMemberEmail())
            .dailyCheckEnabled(entity.getDailyCheckEnabled())
            .hasPushToken(entity.hasPushToken())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    /**
     * 역할 정보 포함 변환 (신규)
     */
    public static MemberResponse fromWithRoles(MemberEntity entity) {
        return MemberResponse.builder()
            .id(entity.getId())
            .memberName(entity.getMemberName())
            .memberEmail(entity.getMemberEmail())
            .dailyCheckEnabled(entity.getDailyCheckEnabled())
            .hasPushToken(entity.hasPushToken())
            .guardian(toGuardianInfo(entity))
            .managedMembers(toManagedMemberInfoList(entity))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private static GuardianInfo toGuardianInfo(MemberEntity entity) {
        if (entity.getGuardian() == null) {
            return null;
        }

        MemberEntity guardian = entity.getGuardian();
        return GuardianInfo.builder()
            .memberId(guardian.getId())
            .memberName(guardian.getMemberName())
            .memberEmail(guardian.getMemberEmail())
            .relation(entity.getGuardianRelation())
            .build();
    }

    private static List<ManagedMemberInfo> toManagedMemberInfoList(MemberEntity entity) {
        return entity.getManagedMembers().stream()
            .map(managedMember -> ManagedMemberInfo.builder()
                .memberId(managedMember.getId())
                .memberName(managedMember.getMemberName())
                .memberEmail(managedMember.getMemberEmail())
                .relation(managedMember.getGuardianRelation())
                .dailyCheckEnabled(managedMember.getDailyCheckEnabled())
                .lastDailyCheckAt(null)  // TODO: DailyCheckRecord에서 조회
                .build())
            .collect(Collectors.toList());
    }
}
```

---

### 3.3 MemberSaveRequest 확장

**파일 경로**: `src/main/java/com/anyang/maruni/domain/member/application/dto/request/MemberSaveRequest.java`

**변경 사항**:

```java
package com.anyang.maruni.domain.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청 DTO")
public class MemberSaveRequest {

    @Schema(description = "회원 이메일", example = "newuser@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    private String memberEmail;

    @Schema(description = "회원 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이름은 필수입니다.")
    private String memberName;

    @Schema(description = "회원 비밀번호", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String memberPassword;

    // ========== 신규 필드 ==========

    @Schema(description = "안부 메시지 수신 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean dailyCheckEnabled;
}
```

---

### 3.4 MemberApiController 확장

**파일 경로**: `src/main/java/com/anyang/maruni/domain/member/presentation/controller/MemberApiController.java`

**신규 API 추가**:

```java
package com.anyang.maruni.domain.member.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.anyang.maruni.domain.member.application.dto.request.MemberUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetails;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/members")  // ✅ 경로 변경 (users → members)
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "JWT 기반 본인 정보 관리 및 검색 API")
public class MemberApiController {

    private final MemberService memberService;

    // ========== 기존 API (재사용) ==========

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "역할 정보(보호자, 돌봄 대상) 포함")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
    public MemberResponse getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        // ✅ 변경: toResponseWithRoles 사용
        return memberService.getMyProfile(userDetails.getMemberId());
    }

    @PutMapping("/me")
    @Operation(summary = "내 정보 수정")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
    public MemberResponse updateMyInfo(
            @RequestBody MemberUpdateRequest req,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        req.setId(userDetails.getMemberId());
        memberService.update(req);
        return memberService.findById(userDetails.getMemberId());
    }

    @DeleteMapping("/me")
    @Operation(summary = "내 계정 삭제")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.MEMBER_DELETED)
    public void deleteMyAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.deleteById(userDetails.getMemberId());
    }

    // ========== 신규 API (Phase 1) ==========

    /**
     * 회원 검색 (이메일)
     * API 명세서: GET /members/search?email={email}
     */
    @GetMapping("/search")
    @Operation(
        summary = "회원 검색 (이메일)",
        description = "보호자 등록을 위한 회원 검색 API"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public MemberResponse searchByEmail(
            @Parameter(description = "검색할 이메일", required = true)
            @RequestParam String email) {
        return memberService.searchByEmail(email);
    }

    /**
     * 내가 돌보는 사람들 목록 조회
     * API 명세서: GET /members/me/managed-members
     */
    @GetMapping("/me/managed-members")
    @Operation(
        summary = "내가 돌보는 사람들 목록 조회",
        description = "보호자 역할로 돌보는 사람들의 목록 조회"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public List<MemberResponse> getManagedMembers(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return memberService.getManagedMembers(userDetails.getMemberId());
    }

    /**
     * 안부 메시지 설정 변경
     * API 명세서: PATCH /members/me (통합)
     */
    @PatchMapping("/me/daily-check")
    @Operation(
        summary = "안부 메시지 설정 변경",
        description = "안부 메시지 수신 ON/OFF 설정"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 변경 성공")
    })
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
    public MemberResponse updateDailyCheckEnabled(
            @Parameter(description = "안부 메시지 수신 여부", required = true)
            @RequestParam Boolean enabled,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return memberService.updateDailyCheckEnabled(userDetails.getMemberId(), enabled);
    }

    /**
     * 푸시 토큰 등록 (기존 기능 유지)
     */
    @PostMapping("/me/push-token")
    @Operation(summary = "푸시 토큰 등록")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void registerPushToken(
            @RequestParam String pushToken,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        // TODO: MemberService에 pushToken 등록 메서드 추가 필요
    }
}
```

---

## 4. Day 5: Auth 도메인 수정

### 4.1 AuthenticationEventHandler 응답 수정

**파일 경로**: `src/main/java/com/anyang/maruni/domain/auth/application/AuthenticationService.java`

**로그인 응답에 역할 정보 추가**:

```java
package com.anyang.maruni.domain.auth.application;

import com.anyang.maruni.domain.auth.domain.service.TokenManager;
import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.security.AuthenticationEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticationEventHandler {

    private final TokenManager tokenManager;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;  // ✅ 추가

    @Override
    public void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberTokenInfo) {
        try {
            String accessToken = tokenManager.generateAccessToken(memberTokenInfo);

            // ✅ Member 정보 조회하여 역할 정보 추가
            MemberEntity member = memberRepository.findById(memberTokenInfo.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", accessToken);
            data.put("tokenType", "Bearer");
            data.put("expiresIn", 3600);  // 1시간

            // ✅ Member 정보 (역할 포함)
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("memberId", member.getId());
            memberInfo.put("memberEmail", member.getMemberEmail());
            memberInfo.put("memberName", member.getMemberName());
            memberInfo.put("dailyCheckEnabled", member.getDailyCheckEnabled());
            memberInfo.put("hasGuardian", member.hasGuardian());
            memberInfo.put("managedMembersCount", member.getManagedMembersCount());

            data.put("member", memberInfo);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json; charset=UTF-8");

            String json = objectMapper.writeValueAsString(CommonApiResponse.success(data));
            response.getWriter().write(json);

        } catch (IOException e) {
            log.error("로그인 응답 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("로그인 응답 처리 실패", e);
        }
    }
}
```

---

## 5. TDD 테스트 계획

### 5.1 MemberEntity 테스트

**파일 경로**: `src/test/java/com/anyang/maruni/domain/member/domain/entity/MemberEntityTest.java`

```java
package com.anyang.maruni.domain.member.domain.entity;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MemberEntityTest {

    @Test
    @DisplayName("회원 생성 시 dailyCheckEnabled 기본값은 false")
    void createMember_defaultDailyCheckEnabled() {
        // given & when
        MemberEntity member = MemberEntity.createMember(
            "test@example.com", "테스터", "password", false
        );

        // then
        assertThat(member.getDailyCheckEnabled()).isFalse();
    }

    @Test
    @DisplayName("보호자 설정 성공")
    void assignGuardian_success() {
        // given
        MemberEntity elderly = MemberEntity.createMember(
            "elderly@example.com", "노인", "password", true
        );
        elderly.setId(1L);

        MemberEntity guardian = MemberEntity.createMember(
            "guardian@example.com", "보호자", "password", false
        );
        guardian.setId(2L);

        // when
        elderly.assignGuardian(guardian, GuardianRelation.FAMILY);

        // then
        assertThat(elderly.hasGuardian()).isTrue();
        assertThat(elderly.getGuardian()).isEqualTo(guardian);
        assertThat(elderly.getGuardianRelation()).isEqualTo(GuardianRelation.FAMILY);
    }

    @Test
    @DisplayName("자기 자신을 보호자로 설정하면 예외 발생")
    void assignGuardian_selfAssignment_throwsException() {
        // given
        MemberEntity member = MemberEntity.createMember(
            "test@example.com", "테스터", "password", true
        );
        member.setId(1L);

        // when & then
        assertThatThrownBy(() -> member.assignGuardian(member, GuardianRelation.FAMILY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot assign self as guardian");
    }

    @Test
    @DisplayName("보호자 제거 성공")
    void removeGuardian_success() {
        // given
        MemberEntity elderly = MemberEntity.createMember(
            "elderly@example.com", "노인", "password", true
        );
        elderly.setId(1L);

        MemberEntity guardian = MemberEntity.createMember(
            "guardian@example.com", "보호자", "password", false
        );
        guardian.setId(2L);

        elderly.assignGuardian(guardian, GuardianRelation.FAMILY);

        // when
        elderly.removeGuardian();

        // then
        assertThat(elderly.hasGuardian()).isFalse();
        assertThat(elderly.getGuardian()).isNull();
        assertThat(elderly.getGuardianRelation()).isNull();
    }

    @Test
    @DisplayName("안부 메시지 설정 변경 성공")
    void updateDailyCheckEnabled_success() {
        // given
        MemberEntity member = MemberEntity.createMember(
            "test@example.com", "테스터", "password", false
        );

        // when
        member.updateDailyCheckEnabled(true);

        // then
        assertThat(member.getDailyCheckEnabled()).isTrue();
    }
}
```

### 5.2 MemberRepository 테스트

**파일 경로**: `src/test/java/com/anyang/maruni/domain/member/domain/repository/MemberRepositoryTest.java`

```java
package com.anyang.maruni.domain.member.domain.repository;

import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("안부 메시지 수신자 ID 목록 조회 성공")
    void findDailyCheckEnabledMemberIds_success() {
        // given
        MemberEntity member1 = MemberEntity.createMember(
            "elderly1@example.com", "노인1", "password", true
        );
        MemberEntity member2 = MemberEntity.createMember(
            "elderly2@example.com", "노인2", "password", true
        );
        MemberEntity member3 = MemberEntity.createMember(
            "guardian@example.com", "보호자", "password", false
        );

        memberRepository.saveAll(List.of(member1, member2, member3));

        // when
        List<Long> result = memberRepository.findDailyCheckEnabledMemberIds();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(member1.getId(), member2.getId());
        assertThat(result).doesNotContain(member3.getId());
    }

    @Test
    @DisplayName("보호자의 돌봄 대상 조회 성공")
    void findByGuardian_success() {
        // given
        MemberEntity guardian = MemberEntity.createMember(
            "guardian@example.com", "보호자", "password", false
        );
        memberRepository.save(guardian);

        MemberEntity elderly1 = MemberEntity.createMember(
            "elderly1@example.com", "노인1", "password", true
        );
        elderly1.assignGuardian(guardian, GuardianRelation.FAMILY);

        MemberEntity elderly2 = MemberEntity.createMember(
            "elderly2@example.com", "노인2", "password", true
        );
        elderly2.assignGuardian(guardian, GuardianRelation.FRIEND);

        memberRepository.saveAll(List.of(elderly1, elderly2));

        // when
        List<MemberEntity> result = memberRepository.findByGuardian(guardian);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("memberEmail")
            .contains("elderly1@example.com", "elderly2@example.com");
    }

    @Test
    @DisplayName("이메일로 회원 검색 성공")
    void searchByEmail_success() {
        // given
        MemberEntity member = MemberEntity.createMember(
            "test@example.com", "테스터", "password", true
        );
        memberRepository.save(member);

        // when
        var result = memberRepository.searchByEmail("test@example.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getMemberEmail()).isEqualTo("test@example.com");
    }
}
```

### 5.3 MemberService 테스트

**파일 경로**: `src/test/java/com/anyang/maruni/domain/member/application/service/MemberServiceTest.java`

```java
package com.anyang.maruni.domain.member.application.service;

import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.mapper.MemberMapper;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.global.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberMapper memberMapper;

    @Test
    @DisplayName("이메일로 회원 검색 성공")
    void searchByEmail_success() {
        // given
        String email = "test@example.com";
        MemberEntity member = MemberEntity.createMember(email, "테스터", "password", true);
        MemberResponse expected = MemberResponse.from(member);

        given(memberRepository.searchByEmail(email)).willReturn(Optional.of(member));
        given(memberMapper.toResponse(member)).willReturn(expected);

        // when
        MemberResponse result = memberService.searchByEmail(email);

        // then
        assertThat(result).isEqualTo(expected);
        verify(memberRepository).searchByEmail(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 검색 시 예외 발생")
    void searchByEmail_notFound_throwsException() {
        // given
        String email = "notfound@example.com";
        given(memberRepository.searchByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.searchByEmail(email))
            .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("내가 돌보는 사람들 목록 조회 성공")
    void getManagedMembers_success() {
        // given
        Long guardianId = 1L;
        MemberEntity guardian = MemberEntity.createMember(
            "guardian@example.com", "보호자", "password", false
        );
        guardian.setId(guardianId);

        List<MemberEntity> managedMembers = List.of(
            MemberEntity.createMember("elderly1@example.com", "노인1", "password", true),
            MemberEntity.createMember("elderly2@example.com", "노인2", "password", true)
        );

        given(memberRepository.findById(guardianId)).willReturn(Optional.of(guardian));
        given(memberRepository.findByGuardian(guardian)).willReturn(managedMembers);

        // when
        List<MemberResponse> result = memberService.getManagedMembers(guardianId);

        // then
        assertThat(result).hasSize(2);
        verify(memberRepository).findByGuardian(guardian);
    }

    @Test
    @DisplayName("안부 메시지 설정 변경 성공")
    void updateDailyCheckEnabled_success() {
        // given
        Long memberId = 1L;
        MemberEntity member = MemberEntity.createMember(
            "test@example.com", "테스터", "password", false
        );
        member.setId(memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.save(any())).willReturn(member);

        // when
        memberService.updateDailyCheckEnabled(memberId, true);

        // then
        assertThat(member.getDailyCheckEnabled()).isTrue();
        verify(memberRepository).save(member);
    }
}
```

---

## 6. 체크리스트

### Day 1-2: 스키마 변경

- [ ] MemberEntity 필드 추가 (dailyCheckEnabled, guardian, managedMembers, guardianRelation)
- [ ] MemberEntity 비즈니스 메서드 추가 (assignGuardian, removeGuardian 등)
- [ ] MemberRepository 쿼리 메서드 추가 (findDailyCheckEnabledMemberIds 등)
- [ ] Migration 스크립트 작성 및 테스트
- [ ] MemberEntity 단위 테스트 작성 (Red-Green-Blue)
- [ ] MemberRepository 통합 테스트 작성

### Day 3-4: Service + Controller

- [ ] MemberResponse 확장 (guardian, managedMembers 필드)
- [ ] MemberSaveRequest 확장 (dailyCheckEnabled 필드)
- [ ] MemberService 신규 메서드 구현
- [ ] MemberApiController 신규 API 추가
- [ ] MemberService 단위 테스트 작성
- [ ] MemberApiController 통합 테스트 작성

### Day 5: Auth 도메인

- [ ] AuthenticationService 로그인 응답 수정
- [ ] 로그인 API 테스트 (역할 정보 포함 확인)

### 최종 검증

- [ ] 모든 단위 테스트 통과
- [ ] 모든 통합 테스트 통과
- [ ] Swagger UI에서 신규 API 확인
- [ ] Postman/Insomnia로 실제 요청 테스트
- [ ] 기존 API 호환성 확인 (Breaking Change 없음)

---

**Version**: 1.0.0
**Updated**: 2025-10-07
**Status**: 개발 대기
**Next Step**: Day 1 스키마 변경 시작
