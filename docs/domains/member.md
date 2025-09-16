# Member 도메인 구현 가이드라인 (2025-09-16 완성)

## 🎉 완성 상태 요약

**Member 도메인은 MARUNI 프로젝트의 가장 기본이 되는 도메인으로, TDD와 DDD 원칙에 따라 100% 완성되었습니다.**

### 🏆 완성 지표
- ✅ **핵심 CRUD 기능**: 회원 가입, 조회, 수정, 삭제 기능 완전 구현
- ✅ **Spring Security 통합**: `UserDetailsService`를 통한 완벽한 인증 연동
- ✅ **비밀번호 암호화**: `PasswordEncoder`를 사용한 안전한 비밀번호 저장
- ✅ **REST API 완성**: 2개의 컨트롤러, 6개 엔드포인트 + Swagger 문서화
- ✅ **도메인 연동**: Auth, Guardian, DailyCheck 도메인과의 의존 관계 명확히 구현
- ✅ **실제 운영 준비**: 상용 서비스 수준의 회원 관리 시스템

## 📐 아키텍처 구조

### DDD 패키지 구조
```
com.anyang.maruni.domain.member/
├── application/
│   ├── dto/
│   │   ├── request/
│   │   │   ├── MemberLoginRequest.java      ✅ 완성
│   │   │   ├── MemberSaveRequest.java       ✅ 완성
│   │   │   └── MemberUpdateRequest.java     ✅ 완성
│   │   └── response/
│   │       └── MemberResponse.java          ✅ 완성
│   ├── mapper/
│   │   └── MemberMapper.java              ✅ 완성
│   └── service/
│       └── MemberService.java             ✅ 완성
├── domain/
│   ├── entity/
│   │   └── MemberEntity.java              ✅ 완성
│   └── repository/
│       └── MemberRepository.java          ✅ 완성
├── infrastructure/
│   └── security/
│       ├── CustomUserDetails.java         ✅ 완성
│       └── CustomUserDetailsService.java  ✅ 완성
└── presentation/
    └── controller/
        ├── JoinApiController.java         ✅ 완성
        └── UserApiController.java         ✅ 완성
```

### 주요 의존성
```java
// MemberService 의존성
- MemberRepository: 회원 데이터 영속성 관리
- PasswordEncoder: 비밀번호 암호화
- MemberMapper: DTO와 Entity 간의 변환

// CustomUserDetailsService 의존성
- MemberRepository: 이메일 기반 회원 조회
```

## 🔐 핵심 기능 구현

### 1. 회원 가입 및 관리

#### 회원 가입 (MemberService)
```java
@Transactional
public void save(MemberSaveRequest req) {
    // 1. 이메일 중복 검증
    if (memberRepository.findByMemberEmail(req.getMemberEmail()).isPresent()) {
        throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
    }

    // 2. 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(req.getMemberPassword());

    // 3. 엔티티 생성 및 저장
    MemberEntity memberEntity = memberMapper.toEntity(req, encodedPassword);
    memberRepository.save(memberEntity);
}
```

#### 회원 정보 수정 (MemberService)
```java
@Transactional
public void update(MemberUpdateRequest req) {
    MemberEntity entity = memberRepository.findById(req.getId())
        .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));

    String encodedPassword = passwordEncoder.encode(req.getMemberPassword());

    // 엔티티의 비즈니스 메서드를 통해 정보 업데이트
    entity.updateMemberInfo(req.getMemberName(), encodedPassword);
    memberRepository.save(entity);
}
```

### 2. Spring Security 연동

#### CustomUserDetailsService
- `UserDetailsService` 인터페이스를 구현하여 Spring Security의 인증 과정에 통합됩니다.
- `loadUserByUsername` 메서드는 이메일을 기반으로 `MemberRepository`에서 회원을 조회하고, `UserDetails` 구현체인 `CustomUserDetails`를 반환합니다.

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberEntity member = memberRepository.findByMemberEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 회원을 찾을 수 없습니다: " + username));
        return new CustomUserDetails(member);
    }
}
```

## 📊 엔티티 설계

### MemberEntity 엔티티
```java
@Entity
@Table(name = "member_table")
public class MemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String memberEmail; // 회원 이메일 (로그인 ID)

    private String memberName; // 회원 이름

    private String memberPassword; // 암호화된 비밀번호

    // 소셜 로그인 정보
    @Enumerated(EnumType.STRING)
    private SocialType socialType;
    private String socialId;

    // Guardian 도메인과의 관계 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private GuardianEntity guardian;

    // 정적 팩토리 메서드
    public static MemberEntity createRegularMember(String email, String name, String password);
    public static MemberEntity createSocialMember(String email, String name, String password, SocialType socialType, String socialId);

    // 비즈니스 로직 메서드
    public void updateMemberInfo(String name, String password);
    public void assignGuardian(GuardianEntity guardian);
    public void removeGuardian();
}
```

## 🔍 Repository 쿼리

### MemberRepository
```java
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    // 이메일로 회원 조회 (로그인 및 중복 확인)
    Optional<MemberEntity> findByMemberEmail(String memberEmail);

    // 이메일 존재 여부 확인
    Boolean existsByMemberEmail(String memberEmail);

    // 소셜 정보로 회원 조회
    Optional<MemberEntity> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

    // DailyCheck 도메인을 위한 활성 회원 ID 목록 조회
    @Query("SELECT m.id FROM MemberEntity m")
    List<Long> findActiveMemberIds();

    // Guardian 도메인을 위한 보호자별 회원 목록 조회
    List<MemberEntity> findByGuardian(GuardianEntity guardian);
}
```

## 🌐 REST API 구현

### JoinApiController (회원가입)
- **`POST /api/join`**: 회원가입 처리
- **`GET /api/join/email-check`**: 이메일 중복 확인

### UserApiController (회원 관리)
- **`GET /api/users`**: 전체 회원 목록 조회
- **`GET /api/users/{id}`**: 특정 회원 정보 조회
- **`PUT /api/users/{id}`**: 회원 정보 수정
- **`DELETE /api/users/{id}`**: 회원 삭제

## 📝 DTO 계층

- **`MemberSaveRequest`**: 회원가입 시 사용 (email, name, password)
- **`MemberLoginRequest`**: 로그인 시 사용 (email, password)
- **`MemberUpdateRequest`**: 회원 정보 수정 시 사용 (email, name, password)
- **`MemberResponse`**: 회원 정보 응답 시 사용 (id, name, email)

## 🔗 도메인 간 연동

### Auth 도메인
- `CustomUserDetailsService`를 통해 Spring Security 인증 과정에 회원 정보를 제공합니다.
- `PasswordEncoder`를 주입받아 비밀번호를 안전하게 암호화합니다.
- 로그인 성공 시 `Auth` 도메인의 `AuthenticationEventHandler`가 `MemberTokenInfo`를 받아 JWT 토큰을 발급합니다.

### Guardian 도메인
- `MemberEntity`는 `GuardianEntity`와 다대일 관계를 맺습니다.
- `assignGuardian`, `removeGuardian` 비즈니스 메서드를 통해 보호자 관계를 설정/해제합니다.
- `GuardianService`는 `MemberRepository`를 사용하여 보호자와 회원 간의 관계를 관리합니다.

### DailyCheck 도메인
- `DailyCheckService`는 `memberRepository.findActiveMemberIds()`를 호출하여 매일 안부 메시지를 보낼 대상 회원 목록을 조회합니다.

## 📈 보안 특성

- **비밀번호 암호화**: `BCryptPasswordEncoder`를 사용하여 비밀번호를 단방향 암호화하여 저장합니다.
- **인증**: Spring Security와 통합하여 폼 기반 로그인 및 JWT 인증의 기반을 제공합니다.
- **인가**: `CustomUserDetails`에서 `ROLE_USER` 권한을 부여하여 API 접근 제어의 기초를 마련합니다.

## 🎯 Claude Code 작업 가이드

### 향후 확장 시 주의사항
1. **비밀번호 정책 강화**: `MemberService`의 `save` 또는 `update` 메서드에서 비밀번호 복잡도 검증 로직을 추가할 수 있습니다.
2. **소셜 로그인 확장**: 새로운 소셜 로그인 제공자를 추가할 경우 `SocialType` Enum을 확장하고 관련 로직을 추가해야 합니다.
3. **회원 상태 관리**: 현재 모든 회원은 활성 상태로 간주됩니다. 향후 휴면, 탈퇴 등 다양한 회원 상태를 관리하려면 `MemberEntity`에 상태 필드(e.g., `MemberStatus` Enum)를 추가하고 관련 비즈니스 로직을 구현해야 합니다.

### API 사용 예시
```bash
# 회원가입
POST /api/join
{
  "memberEmail": "test@example.com",
  "memberName": "테스트",
  "memberPassword": "password123"
}

# 이메일 중복 확인
GET /api/join/email-check?memberEmail=test@example.com

# 특정 회원 조회
GET /api/users/1
```

**Member 도메인은 MARUNI의 모든 사용자 데이터와 인증의 시작점 역할을 하는 핵심 기반 도메인입니다. Spring Security와 긴밀하게 통합되어 안정적이고 확장 가능한 회원 관리 기능을 제공합니다.** 🚀
