# 아키텍처 가이드

**MARUNI DDD 아키텍처 패턴 및 구조 설계 가이드**

---

## 🏛️ DDD 계층 구조

### **도메인 계층 분류**
```
🔐 Foundation Layer (기반 시스템)
├── Member (회원 관리)
└── Auth (JWT 인증)

💬 Core Service Layer (핵심 서비스)
├── Conversation (AI 대화)
├── DailyCheck (스케줄링)
└── Guardian (보호자 관리)

🚨 Integration Layer (통합/알림)
├── AlertRule (이상징후 감지)
└── Notification (알림 서비스)
```

### **의존성 방향 규칙**
```
Presentation ─→ Application ─→ Domain
     ↓              ↓           ↑
Infrastructure ─────┴───────────┘

❌ Domain Layer는 다른 계층에 의존하면 안됨
✅ 모든 의존성은 Domain Layer로 향해야 함
```

---

## 📁 Package 구조

### **표준 DDD Package 구조**
```
com.anyang.maruni.domain.{domain}/
├── application/                # Application Layer
│   ├── dto/                    # Data Transfer Objects
│   │   ├── request/           # Request DTOs
│   │   └── response/          # Response DTOs
│   ├── service/               # Application Services
│   └── mapper/                # DTO-Entity 매핑 (선택적)
├── domain/                    # Domain Layer
│   ├── entity/               # Domain Entities
│   └── repository/           # Repository Interfaces
├── infrastructure/           # Infrastructure Layer
│   └── {기술별구현체}/         # 기술 특화 구현체
└── presentation/             # Presentation Layer
    └── controller/           # REST Controllers
```

### **실제 도메인 예시**
```
com.anyang.maruni.domain.member/
├── application/
│   ├── dto/
│   │   ├── request/MemberSaveRequest.java
│   │   └── response/MemberResponse.java
│   ├── service/MemberService.java
│   └── mapper/MemberMapper.java
├── domain/
│   ├── entity/MemberEntity.java
│   └── repository/MemberRepository.java
├── infrastructure/
│   └── security/
│       ├── CustomUserDetails.java
│       └── CustomUserDetailsService.java
└── presentation/
    └── controller/UserApiController.java
```

---

## 🎯 계층별 구현 규칙

### **Domain Layer (핵심 비즈니스 로직)**

#### **Entity 설계 원칙**
```java
// ✅ 올바른 Entity 구현
@Entity
@Table(name = "member_table")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정적 팩토리 메서드로 생성
    public static MemberEntity createRegularMember(String email, String name) {
        return MemberEntity.builder()
            .memberEmail(email)
            .memberName(name)
            .build();
    }

    // 비즈니스 로직 메서드
    public void updateMemberInfo(String name) {
        this.memberName = name;
    }
}
```

#### **Repository 설계 원칙**
```java
// ✅ 도메인 친화적인 메서드명
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    Optional<MemberEntity> findByMemberEmail(String email);
    boolean existsByMemberEmail(String email);

    // 복잡한 쿼리는 @Query 사용
    @Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :email")
    Optional<MemberEntity> findActiveByMemberEmail(@Param("email") String email);
}
```

### **Application Layer (유스케이스 조정)**

#### **Service 설계 원칙**
```java
// ✅ 트랜잭션 경계 및 도메인 조정
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional // 쓰기 작업만 명시
    public void save(MemberSaveRequest req) {
        // 1. 비즈니스 규칙 검증
        validateEmailDuplication(req.getMemberEmail());

        // 2. 도메인 객체 생성 (정적 팩토리 메서드 사용)
        MemberEntity entity = MemberEntity.createRegularMember(
            req.getMemberEmail(), req.getMemberName());

        // 3. 영속화
        memberRepository.save(entity);
    }

    private void validateEmailDuplication(String email) {
        if (memberRepository.existsByMemberEmail(email)) {
            throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}
```

#### **DTO 설계 원칙**
```java
// ✅ Request DTO
@Getter
@Setter
@Schema(description = "회원가입 요청 DTO")
public class MemberSaveRequest {
    @Schema(description = "회원 이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    private String memberEmail;
}

// ✅ Response DTO
@Getter
@Builder
@Schema(description = "회원 정보 응답 DTO")
public class MemberResponse {
    private Long id;
    private String memberEmail;

    // 정적 변환 메서드
    public static MemberResponse from(MemberEntity entity) {
        return MemberResponse.builder()
            .id(entity.getId())
            .memberEmail(entity.getMemberEmail())
            .build();
    }
}
```

### **Presentation Layer (외부 인터페이스)**

#### **Controller 설계 원칙**
```java
// ✅ HTTP 요청 처리 및 응답
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "사용자 CRUD API")
public class UserApiController {
    private final MemberService memberService;

    @Operation(summary = "사용자 정보 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/{id}")
    @SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
    public void update(
        @PathVariable Long id,
        @Valid @RequestBody MemberUpdateRequest req
    ) {
        req.setId(id);
        memberService.update(req);
    }
}
```

### **Infrastructure Layer (기술 구현체)**

#### **Infrastructure 구현 원칙**
```java
// ✅ 도메인 인터페이스 구현
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberService memberService; // Application Service 의존

    @Override
    public UserDetails loadUserByUsername(String email) {
        MemberResponse member = memberService.findByEmail(email);
        return new CustomUserDetails(member);
    }
}
```

---

## 🔄 도메인간 상호작용 패턴

### **도메인간 데이터 플로우**
```
📱 안부 확인 플로우:
DailyCheck → Notification → Member

💬 대화 분석 플로우:
Conversation → AlertRule → Guardian → Notification

🚨 긴급 상황 플로우:
AlertRule → Guardian → Notification (즉시 발송)
```

### **도메인간 통신 방법**

#### **1. Application Service 경유 (권장)**
```java
// ✅ Application Service를 통한 안전한 통신
@Service
public class AlertRuleService {
    private final MemberService memberService;
    private final GuardianService guardianService;
    private final NotificationService notificationService;

    public void detectAnomalies(Long memberId) {
        // 1. Member 정보 조회
        MemberResponse member = memberService.findById(memberId);

        // 2. Guardian 정보 조회
        GuardianResponse guardian = guardianService.findByMemberId(memberId);

        // 3. 알림 발송
        notificationService.sendPushNotification(guardian.getId(), "알림 메시지");
    }
}
```

#### **2. 이벤트 기반 통신 (향후 확장)**
```java
// ✅ Spring Events를 활용한 느슨한 결합
@EventListener
public void handleNewMessage(MessageCreatedEvent event) {
    alertRuleService.analyzeMessage(event.getMessage());
}
```

---

## ⚡ 성능 고려사항

### **연관관계 최적화**
```java
// ❌ N+1 문제 발생
public List<MemberResponse> findAllMembers() {
    return memberRepository.findAll().stream()
        .map(MemberResponse::from)
        .collect(Collectors.toList());
}

// ✅ Fetch Join으로 최적화
@Query("SELECT m FROM MemberEntity m LEFT JOIN FETCH m.guardian")
List<MemberEntity> findAllWithGuardian();
```

### **트랜잭션 최적화**
```java
// ✅ 읽기 전용 트랜잭션 기본 설정
@Transactional(readOnly = true)
public class MemberService {

    // ✅ 쓰기 작업에만 별도 설정
    @Transactional
    public void save(MemberSaveRequest req) {
        // 구현
    }
}
```

---

## 🎯 아키텍처 체크리스트

### **새 도메인 추가 시**
- [ ] DDD 4계층 구조 준수
- [ ] Domain Layer 순수성 유지 (외부 의존성 없음)
- [ ] 의존성 방향 올바른 설정
- [ ] 정적 팩토리 메서드 구현
- [ ] BaseTimeEntity 상속

### **도메인간 통신 시**
- [ ] Application Service 경유 통신
- [ ] 직접적인 Repository 접근 금지
- [ ] DTO 변환 후 데이터 전달
- [ ] 순환 의존성 방지

### **성능 최적화 시**
- [ ] N+1 문제 해결 (Fetch Join)
- [ ] 읽기 전용 트랜잭션 적용
- [ ] 불필요한 연관관계 로딩 방지
- [ ] 적절한 캐싱 전략 적용

---

**Version**: v1.0.0 | **Updated**: 2025-09-16