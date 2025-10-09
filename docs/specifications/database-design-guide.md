# 데이터베이스 설계 가이드

**MARUNI JPA Entity 설계 패턴**

## 🗄️ 핵심 원칙

### 1. BaseTimeEntity 상속
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**사용**:
```java
@Entity
public class MemberEntity extends BaseTimeEntity {
    // createdAt, updatedAt 자동 관리
}
```

### 2. 정적 팩토리 메서드
```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String memberEmail;
    private String memberName;
    
    // ✅ 정적 팩토리 메서드
    public static MemberEntity createRegularMember(
        String email, String name, String password) {
        
        MemberEntity member = new MemberEntity();
        member.memberEmail = email;
        member.memberName = name;
        member.memberPassword = password;
        return member;
    }
}
```

### 3. Setter 대신 비즈니스 메서드
```java
// ❌ Bad: Setter 사용
member.setName("new name");

// ✅ Good: 비즈니스 메서드
public void updateProfile(String name) {
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("이름은 필수입니다");
    }
    this.memberName = name;
}
```

## 🔗 연관관계 매핑

### 다대일 (@ManyToOne)
```java
@Entity
public class ConversationEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ✅ LAZY 로딩 (기본값이 EAGER이므로 명시 필요)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
}
```

### 일대다 (@OneToMany) - 지양
```java
// ❌ 양방향 연관관계 (복잡도 증가, 성능 저하)
@Entity
public class MemberEntity {
    
    @OneToMany(mappedBy = "member")
    private List<ConversationEntity> conversations;
}

// ✅ 단방향 연관관계 (필요시 쿼리로 조회)
@Repository
public interface ConversationRepository {
    List<ConversationEntity> findByMemberId(Long memberId);
}
```

### Enum 매핑
```java
@Entity
public class AlertRuleEntity extends BaseTimeEntity {
    
    @Enumerated(EnumType.STRING)  // ✅ STRING 사용 (변경에 안전)
    @Column(nullable = false)
    private AlertType alertType;
}

// Enum 정의
public enum AlertType {
    EMOTION_PATTERN,
    NO_RESPONSE,
    KEYWORD_DETECTION
}
```

## 📋 Entity 전체 템플릿

```java
@Entity
@Table(
    name = "member_table",
    indexes = {
        @Index(name = "idx_member_email", columnList = "memberEmail")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email", columnNames = "memberEmail")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String memberEmail;
    
    @Column(nullable = false, length = 50)
    private String memberName;
    
    @Column(nullable = false)
    private String memberPassword;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private GuardianEntity guardian;
    
    @Column(length = 200)
    private String pushToken;
    
    // 정적 팩토리 메서드
    public static MemberEntity createRegularMember(
        String email, String name, String password) {
        
        MemberEntity member = new MemberEntity();
        member.memberEmail = email;
        member.memberName = name;
        member.memberPassword = password;
        return member;
    }
    
    // 비즈니스 메서드
    public void updateProfile(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.memberName = name;
    }
    
    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }
    
    public void assignGuardian(GuardianEntity guardian) {
        this.guardian = guardian;
    }
}
```

## 🔍 인덱스 전략

### 자주 조회되는 컬럼
```java
@Table(
    name = "member_table",
    indexes = {
        @Index(name = "idx_member_email", columnList = "memberEmail"),
        @Index(name = "idx_created_at", columnList = "createdAt")
    }
)
public class MemberEntity { ... }
```

### 복합 인덱스
```java
@Table(
    name = "daily_check_record",
    indexes = {
        @Index(
            name = "idx_member_checkdate",
            columnList = "memberId, checkDate"
        )
    }
)
public class DailyCheckRecord { ... }
```

### 유니크 제약
```java
// 방법 1: @Column
@Column(unique = true)
private String memberEmail;

// 방법 2: @Table (권장 - 이름 지정 가능)
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_email",
            columnNames = "memberEmail"
        )
    }
)
```

## 🎯 복합키 설계

### Embeddable 사용
```java
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DailyCheckId implements Serializable {
    
    private Long memberId;
    private LocalDate checkDate;
}

@Entity
public class DailyCheckRecord extends BaseTimeEntity {
    
    @EmbeddedId
    private DailyCheckId id;
    
    @Column(nullable = false)
    private boolean success;
}
```

## 🔧 JPA 설정

### application.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 개발: update, 운영: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        default_batch_fetch_size: 100  # N+1 방지
    show-sql: true  # 개발: true, 운영: false
```

### Application 클래스
```java
@SpringBootApplication
@EnableJpaAuditing  // BaseTimeEntity 활성화
public class MaruniApplication { ... }
```

## 🚫 안티패턴

```java
// ❌ EAGER 로딩 (N+1 문제)
@ManyToOne(fetch = FetchType.EAGER)
private GuardianEntity guardian;

// ✅ LAZY 로딩 + Fetch Join
@ManyToOne(fetch = FetchType.LAZY)
private GuardianEntity guardian;

@Query("SELECT c FROM ConversationEntity c " +
       "LEFT JOIN FETCH c.member " +
       "WHERE c.id = :id")
Optional<ConversationEntity> findByIdWithMember(@Param("id") Long id);

// ❌ Setter 노출
@Setter
public class MemberEntity { ... }

// ✅ 비즈니스 메서드
public void updateProfile(String name) { ... }

// ❌ 양방향 연관관계 남용
@OneToMany(mappedBy = "member")
private List<ConversationEntity> conversations;

// ✅ 필요시 쿼리로 조회
List<ConversationEntity> conversations = 
    conversationRepository.findByMemberId(memberId);
```

## 📊 실전 예시

### 1. 일대다 관계 (Guardian - Member)
```java
// Guardian (일)
@Entity
public class GuardianEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 양방향 연관관계 없음 (단방향만)
}

// Member (다)
@Entity
public class MemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private GuardianEntity guardian;
}

// Repository에서 조회
public interface MemberRepository {
    List<MemberEntity> findByGuardianId(Long guardianId);
}
```

### 2. Enum + 인덱스
```java
@Entity
@Table(
    name = "alert_rule",
    indexes = {
        @Index(name = "idx_member_type", columnList = "memberId, alertType"),
        @Index(name = "idx_active", columnList = "isActive")
    }
)
public class AlertRuleEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long memberId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;
    
    @Column(nullable = false)
    private boolean isActive = true;
}
```

---

**성능 최적화: `docs/specifications/performance-guide.md` 참조**
