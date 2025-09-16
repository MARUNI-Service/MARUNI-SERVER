# 데이터베이스 설계 가이드

**MARUNI Entity 설계 원칙 및 JPA 구현 표준**

---

## 🗄️ Entity 설계 원칙

### **기본 Entity 구조**
```java
@Entity
@Table(name = "member_table", indexes = {
    @Index(name = "idx_member_email", columnList = "memberEmail", unique = true),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String memberEmail;

    @Column(nullable = false, length = 50)
    private String memberName;

    @Column(length = 200)
    private String memberPassword;
}
```

### **BaseTimeEntity 상속 필수**
```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    protected LocalDateTime updatedAt;
}

// 모든 Entity는 BaseTimeEntity 상속
public class ExampleEntity extends BaseTimeEntity {
    // 구현
}
```

---

## 🔗 연관 관계 매핑

### **Many-to-One (다대일)**
```java
// LAZY 로딩 기본 사용
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "guardian_id")
private GuardianEntity guardian;

// 필수 관계일 경우 nullable = false
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "parent_id", nullable = false)
private ParentEntity parent;
```

### **One-to-Many (일대다)**
```java
// mappedBy 사용, CASCADE 신중히 적용
@OneToMany(mappedBy = "member", cascade = CascadeType.PERSIST)
private List<ConversationEntity> conversations = new ArrayList<>();

// 고아 객체 제거가 필요한 경우
@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ChildEntity> children = new ArrayList<>();
```

### **Many-to-Many (다대다) - 지양**
```java
// ❌ 단순 @ManyToMany 사용 지양
@ManyToMany
private Set<RoleEntity> roles;

// ✅ 중간 엔티티 명시적 관리 권장
@Entity
public class MemberRole extends BaseTimeEntity {
    @ManyToOne
    private MemberEntity member;

    @ManyToOne
    private RoleEntity role;

    private LocalDateTime assignedAt;
}
```

---

## 📋 Column 설계 규칙

### **Column 어노테이션 활용**
```java
// 필수 필드
@Column(nullable = false)
private String requiredField;

// 길이 제한
@Column(length = 100)
private String limitedField;

// 유니크 제약
@Column(unique = true)
private String uniqueField;

// 정밀도가 필요한 숫자
@Column(precision = 19, scale = 2)
private BigDecimal amount;

// 텍스트 길이가 긴 경우
@Lob
@Column(columnDefinition = "TEXT")
private String longText;
```

### **Enum 매핑 규칙**
```java
// ✅ STRING 방식 사용 (가독성, 확장성)
@Enumerated(EnumType.STRING)
@Column(name = "status_type")
private StatusType status;

// ❌ ORDINAL 방식 지양 (순서 변경 위험)
@Enumerated(EnumType.ORDINAL)  // 사용 금지
private StatusType status;

// Enum 정의 예시
public enum GuardianRelation {
    FAMILY("가족"),
    CAREGIVER("간병인"),
    FRIEND("지인"),
    MEDICAL_STAFF("의료진");

    private final String displayName;
}
```

---

## 🏭 정적 팩토리 메서드 패턴

### **Entity 생성 메서드**
```java
@Entity
public class MemberEntity extends BaseTimeEntity {
    // 일반 회원 생성
    public static MemberEntity createRegularMember(String email, String name, String password) {
        return MemberEntity.builder()
            .memberEmail(email)
            .memberName(name)
            .memberPassword(password)
            .build();
    }

    // 소셜 회원 생성
    public static MemberEntity createSocialMember(String email, String name,
                                                 SocialType socialType, String socialId) {
        return MemberEntity.builder()
            .memberEmail(email)
            .memberName(name)
            .socialType(socialType)
            .socialId(socialId)
            .build();
    }
}
```

### **비즈니스 로직 메서드**
```java
public class MemberEntity extends BaseTimeEntity {
    // 상태 변경 메서드
    public void updateMemberInfo(String name, String password) {
        this.memberName = name;
        this.memberPassword = password;
    }

    // 연관관계 편의 메서드
    public void assignGuardian(GuardianEntity guardian) {
        this.guardian = guardian;
        guardian.getMembers().add(this);
    }

    public void removeGuardian() {
        if (this.guardian != null) {
            this.guardian.getMembers().remove(this);
            this.guardian = null;
        }
    }
}
```

---

## 📊 Repository 설계 패턴

### **기본 Repository 인터페이스**
```java
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    // 메서드명으로 쿼리 생성 (단순 조회)
    Optional<MemberEntity> findByMemberEmail(String email);
    boolean existsByMemberEmail(String email);
    List<MemberEntity> findByMemberNameContaining(String name);

    // @Query 사용 (복잡한 쿼리)
    @Query("SELECT m FROM MemberEntity m " +
           "WHERE m.memberEmail = :email AND m.createdAt >= :startDate")
    Optional<MemberEntity> findActiveByEmailAfter(
        @Param("email") String email,
        @Param("startDate") LocalDateTime startDate
    );
}
```

### **페이징 및 정렬**
```java
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    // 페이징 쿼리
    Page<MemberEntity> findByMemberNameContaining(String name, Pageable pageable);

    // Slice 사용 (totalCount 불필요 시)
    Slice<MemberEntity> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);
}
```

### **벌크 연산**
```java
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    // 벌크 업데이트
    @Modifying
    @Query("UPDATE MemberEntity m SET m.lastLoginAt = :loginTime WHERE m.id IN :ids")
    void updateLastLoginTimes(@Param("ids") List<Long> ids, @Param("loginTime") LocalDateTime loginTime);

    // 벌크 삭제
    @Modifying
    @Query("DELETE FROM MemberEntity m WHERE m.createdAt < :cutoffDate")
    void deleteOldMembers(@Param("cutoffDate") LocalDateTime cutoffDate);
}
```

---

## ⚡ 성능 최적화

### **N+1 문제 해결**
```java
// ❌ N+1 문제 발생
public List<MemberResponse> findAllMembers() {
    return memberRepository.findAll().stream()
        .map(member -> {
            // 각 member마다 guardian 조회 쿼리 실행
            return MemberResponse.from(member);
        })
        .collect(Collectors.toList());
}

// ✅ Fetch Join으로 해결
@Query("SELECT m FROM MemberEntity m LEFT JOIN FETCH m.guardian")
List<MemberEntity> findAllWithGuardian();

// ✅ @EntityGraph 사용
@EntityGraph(attributePaths = {"guardian"})
List<MemberEntity> findAll();
```

### **적절한 FetchType 선택**
```java
// 항상 함께 사용되는 관계 - EAGER (신중히 사용)
@ManyToOne(fetch = FetchType.EAGER)
private ParentEntity parent;

// 가끔 사용되는 관계 - LAZY (기본값)
@OneToMany(fetch = FetchType.LAZY)
private List<ChildEntity> children;
```

### **인덱스 설계**
```java
@Table(indexes = {
    // 유니크 인덱스
    @Index(name = "idx_member_email", columnList = "memberEmail", unique = true),

    // 조회용 인덱스
    @Index(name = "idx_created_at", columnList = "createdAt"),

    // 복합 인덱스 (순서 중요)
    @Index(name = "idx_social_type_id", columnList = "socialType, socialId"),

    // 부분 인덱스 (nullable 필드)
    @Index(name = "idx_guardian_id", columnList = "guardian_id")
})
```

---

## 🔒 데이터 무결성

### **유니크 제약 조건**
```java
// Entity 레벨 유니크 제약
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"member_id", "alert_rule_id", "alert_date"})
})
public class AlertHistory extends BaseTimeEntity {
    // 구현
}

// Column 레벨 유니크 제약
@Column(unique = true)
private String memberEmail;
```

### **체크 제약 조건 (Database 레벨)**
```sql
-- PostgreSQL 제약 조건 예시
ALTER TABLE member_table
ADD CONSTRAINT chk_email_format
CHECK (member_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

ALTER TABLE alert_rule
ADD CONSTRAINT chk_alert_level
CHECK (alert_level IN ('LOW', 'MEDIUM', 'HIGH', 'EMERGENCY'));
```

### **외래키 제약 조건**
```java
// 외래키 제약 + CASCADE 옵션
@ManyToOne(cascade = CascadeType.PERSIST)
@JoinColumn(name = "guardian_id",
           foreignKey = @ForeignKey(name = "fk_member_guardian"))
private GuardianEntity guardian;
```

---

## 🎯 Entity 개발 체크리스트

### **새 Entity 생성 시**
- [ ] BaseTimeEntity 상속
- [ ] @Table name 및 indexes 정의
- [ ] @Id, @GeneratedValue 설정
- [ ] @Column nullable, length 적절히 설정
- [ ] @Enumerated(EnumType.STRING) 사용

### **연관관계 설정 시**
- [ ] fetch = FetchType.LAZY 기본 설정
- [ ] mappedBy 양방향 관계 설정
- [ ] CASCADE 옵션 신중히 선택
- [ ] 연관관계 편의 메서드 구현

### **성능 고려사항**
- [ ] 인덱스 설계 (조회 패턴 분석)
- [ ] N+1 문제 방지 (@Query, @EntityGraph)
- [ ] 불필요한 EAGER 로딩 제거
- [ ] 벌크 연산으로 대량 처리

### **데이터 무결성**
- [ ] 유니크 제약 조건 적용
- [ ] 외래키 제약 조건 설정
- [ ] Bean Validation과 연계
- [ ] 비즈니스 규칙 메서드 구현

---

**Version**: v1.0.0 | **Updated**: 2025-09-16