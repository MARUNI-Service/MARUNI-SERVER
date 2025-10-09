# 코딩 컨벤션 & 템플릿

**MARUNI 일상 개발용 완전 가이드**

## 📋 1부: 기본 원칙

### 패키지 구조
```
com.anyang.maruni/
├── global/              # 글로벌 설정 (수정 지양)
├── domain/              # 도메인별 DDD 구조
│   ├── {domain}/
│   │   ├── application/    # DTO, Service, Mapper
│   │   ├── domain/         # Entity, Repository 인터페이스
│   │   ├── infrastructure/ # Repository 구현, 외부 연동
│   │   └── presentation/   # Controller
```

### 네이밍 규칙
```
Entity: {Domain}Entity (예: MemberEntity)
Service: {Domain}Service (예: MemberService)
Controller: {Domain}Controller (예: MemberController)
Repository: {Domain}Repository (예: MemberRepository)
DTO: {Domain}{Action}{Request|Response}Dto (예: MemberSaveRequestDto)
Enum: {의미}Type (예: EmotionType, AlertLevel)
```

---

## 📋 2부: 전체 템플릿 (복사해서 사용)

### Entity 템플릿
```java
@Entity
@Table(
    name = "{domain}_table",
    indexes = {
        @Index(name = "idx_{column}", columnList = "{column}")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class {Domain}Entity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    // 정적 팩토리 메서드
    public static {Domain}Entity create{Domain}(String name, MemberEntity member) {
        {Domain}Entity entity = new {Domain}Entity();
        entity.name = name;
        entity.member = member;
        return entity;
    }

    // 비즈니스 메서드
    public void update(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.name = newName;
    }
}
```

### Repository 템플릿
```java
public interface {Domain}Repository extends JpaRepository<{Domain}Entity, Long> {

    // 기본 메서드: findById, save, delete 등 자동 제공

    Optional<{Domain}Entity> findByName(String name);
    List<{Domain}Entity> findByMemberId(Long memberId);
    boolean existsByName(String name);

    // JPQL 쿼리
    @Query("SELECT d FROM {Domain}Entity d WHERE d.member.id = :memberId ORDER BY d.createdAt DESC")
    List<{Domain}Entity> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    // Fetch Join (N+1 방지)
    @Query("SELECT d FROM {Domain}Entity d LEFT JOIN FETCH d.member WHERE d.id = :id")
    Optional<{Domain}Entity> findByIdWithMember(@Param("id") Long id);
}
```

### Service 템플릿
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Domain}Service {

    private final {Domain}Repository repository;

    // 조회 (readOnly = true)
    public {Domain}Response getById(Long id) {
        {Domain}Entity entity = repository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.{DOMAIN}_NOT_FOUND));
        return {Domain}Response.from(entity);
    }

    public List<{Domain}Response> getAll() {
        return repository.findAll().stream()
            .map({Domain}Response::from)
            .toList();
    }

    // 생성 (@Transactional)
    @Transactional
    public {Domain}Response create({Domain}Request request) {
        validateDuplicate(request.getName());

        {Domain}Entity entity = {Domain}Entity.create{Domain}(
            request.getName(),
            request.getMember()
        );

        {Domain}Entity saved = repository.save(entity);
        return {Domain}Response.from(saved);
    }

    // 수정 (@Transactional)
    @Transactional
    public void update(Long id, {Domain}UpdateRequest request) {
        {Domain}Entity entity = repository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.{DOMAIN}_NOT_FOUND));
        entity.update(request.getName());
        // JPA 변경 감지로 자동 UPDATE
    }

    // 삭제 (@Transactional)
    @Transactional
    public void delete(Long id) {
        {Domain}Entity entity = repository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.{DOMAIN}_NOT_FOUND));
        repository.delete(entity);
    }

    private void validateDuplicate(String name) {
        if (repository.existsByName(name)) {
            throw new BaseException(ErrorCode.DUPLICATE_{DOMAIN});
        }
    }
}
```

### Controller 템플릿
```java
@RestController
@RequestMapping("/api/{domains}")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "{Domain}", description = "{Domain} 관리 API")
public class {Domain}Controller {

    private final {Domain}Service service;

    @GetMapping("/{id}")
    @Operation(summary = "{Domain} 조회")
    @SuccessResponseDescription(implementation = {Domain}Response.class)
    public {Domain}Response getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    @Operation(summary = "{Domain} 목록 조회")
    @SuccessResponseDescription(implementation = {Domain}Response.class, isArray = true)
    public List<{Domain}Response> getAll() {
        return service.getAll();
    }

    @PostMapping
    @Operation(summary = "{Domain} 생성")
    @SuccessCodeAnnotation(SuccessCode.CREATED)
    public {Domain}Response create(@Valid @RequestBody {Domain}Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "{Domain} 수정")
    public void update(@PathVariable Long id, @Valid @RequestBody {Domain}UpdateRequest request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "{Domain} 삭제")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

### DTO 템플릿
```java
// Request DTO
public record {Domain}Request(
    @NotBlank(message = "이름은 필수입니다")
    String name,

    @NotNull(message = "회원 ID는 필수입니다")
    Long memberId
) {}

// Response DTO
public record {Domain}Response(
    Long id,
    String name,
    Long memberId,
    LocalDateTime createdAt
) {
    public static {Domain}Response from({Domain}Entity entity) {
        return new {Domain}Response(
            entity.getId(),
            entity.getName(),
            entity.getMember().getId(),
            entity.getCreatedAt()
        );
    }
}

// Update DTO
public record {Domain}UpdateRequest(
    @NotBlank(message = "이름은 필수입니다")
    String name
) {}
```

### Test 템플릿

#### Service Test (Unit)
```java
@ExtendWith(MockitoExtension.class)
class {Domain}ServiceTest {

    @Mock
    private {Domain}Repository repository;

    @InjectMocks
    private {Domain}Service service;

    @Test
    void create_shouldSaveSuccessfully() {
        {Domain}Request request = new {Domain}Request("name", 1L);
        {Domain}Entity entity = {Domain}Entity.create{Domain}("name", member);

        given(repository.existsByName(anyString())).willReturn(false);
        given(repository.save(any({Domain}Entity.class))).willReturn(entity);

        {Domain}Response response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("name");
        verify(repository, times(1)).save(any({Domain}Entity.class));
    }
}
```

#### Repository Test (Integration)
```java
@DataJpaTest
class {Domain}RepositoryTest {

    @Autowired
    private {Domain}Repository repository;

    @Test
    void findByName_shouldReturnEntity() {
        {Domain}Entity entity = {Domain}Entity.create{Domain}("name", member);
        repository.save(entity);

        Optional<{Domain}Entity> found = repository.findByName("name");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("name");
    }
}
```

#### Controller Test (MockMvc)
```java
@WebMvcTest({Domain}Controller.class)
class {Domain}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private {Domain}Service service;

    @Test
    void getById_shouldReturnResponse() throws Exception {
        {Domain}Response response = new {Domain}Response(1L, "name", 1L, LocalDateTime.now());
        given(service.getById(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/{domains}/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("name"));
    }
}
```

---

## 📋 3부: 예외 처리 & 어노테이션

### BaseException 사용
```java
// Service에서 예외 발생
if (member == null) {
    throw new BaseException(ErrorCode.MEMBER_NOT_FOUND);
}

// GlobalExceptionHandler가 자동 처리 → CommonApiResponse로 래핑
```

### ErrorCode 정의
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다"),

    // Member
    MEMBER_NOT_FOUND(404, "M001", "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "M002", "이미 존재하는 이메일입니다");

    private final int status;
    private final String code;
    private final String message;
}
```

### 필수 어노테이션

#### Entity
- `@Entity`, `@Table(name = "테이블명")`
- `@Getter` (필수), `@Setter` (지양)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- `extends BaseTimeEntity` (createdAt, updatedAt 자동 관리)

#### Service
- `@Service`
- `@RequiredArgsConstructor` (생성자 주입)
- `@Transactional(readOnly = true)` (클래스 레벨)
- `@Transactional` (수정 메서드)

#### Controller
- `@RestController`, `@RequestMapping`
- `@RequiredArgsConstructor`
- `@AutoApiResponse` (자동 응답 래핑)
- `@Tag`, `@Operation` (Swagger)

#### DTO
- `@Valid` (Controller 파라미터)
- `@NotBlank`, `@Email`, `@Size` (Bean Validation)

### Bean Validation 자주 쓰는 어노테이션
```java
@NotNull        // null 불가
@NotBlank       // null, "", " " 모두 불가 (문자열 전용)
@NotEmpty       // null, "" 불가 (컬렉션/문자열)
@Email          // 이메일 형식
@Size(min, max) // 길이 제한
@Min(value)     // 최솟값
@Max(value)     // 최댓값
@Pattern(regexp) // 정규식
@Past           // 과거 날짜
@Future         // 미래 날짜
```

---

## 📋 4부: 자주 쓰는 쿼리 패턴

```java
// 1. 존재 확인
boolean exists = repository.existsByEmail(email);

// 2. 카운트
long count = repository.countByMemberId(memberId);

// 3. 페이징
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
Page<Entity> page = repository.findAll(pageable);

// 4. DTO Projection
@Query("SELECT new com.anyang.maruni.dto.SimpleDto(d.id, d.name) FROM {Domain}Entity d")
List<SimpleDto> findAllSimple();

// 5. Bulk Update
@Modifying
@Query("UPDATE {Domain}Entity d SET d.name = :name WHERE d.id IN :ids")
void bulkUpdateName(@Param("name") String name, @Param("ids") List<Long> ids);

// 6. Fetch Join (N+1 방지)
@Query("SELECT d FROM {Domain}Entity d LEFT JOIN FETCH d.member WHERE d.id = :id")
Optional<{Domain}Entity> findByIdWithMember(@Param("id") Long id);
```

---

## 🚫 금지 사항

```java
// ❌ Setter 남용 (Entity 불변성 위반)
member.setName("new name");

// ✅ 비즈니스 메서드 사용
member.updateProfile("new name");

// ❌ 양방향 연관관계 (복잡도 증가)
@OneToMany(mappedBy = "member")
private List<ConversationEntity> conversations;

// ✅ 단방향 연관관계 (필요시만 조회)
@ManyToOne(fetch = FetchType.LAZY)
private MemberEntity member;

// ❌ EAGER 로딩 (N+1 문제)
@ManyToOne(fetch = FetchType.EAGER)
private GuardianEntity guardian;

// ✅ LAZY 로딩 + Fetch Join
@ManyToOne(fetch = FetchType.LAZY)
private GuardianEntity guardian;

// ❌ Entity 직접 반환
@GetMapping("/{id}")
public MemberEntity getById(@PathVariable Long id) {
    return memberRepository.findById(id).orElseThrow();
}

// ✅ DTO 반환
@GetMapping("/{id}")
public MemberResponse getById(@PathVariable Long id) {
    return memberService.getById(id);
}
```

---

## 🚀 새 도메인 추가 체크리스트

```
1. [ ] docs/domains/{domain}.md 가이드 작성
2. [ ] Entity 생성 (BaseTimeEntity 상속)
3. [ ] Repository 인터페이스 생성
4. [ ] Service 생성 (@Transactional)
5. [ ] DTO 생성 (Request/Response)
6. [ ] Controller 생성 (@AutoApiResponse)
7. [ ] 테스트 작성 (TDD Red-Green-Blue)
8. [ ] CLAUDE.md 업데이트
```

---

**관련 가이드:**
- API 설계: `api-design-guide.md`
- DB 설계: `database-design-guide.md`
- 테스트: `testing-guide.md`
