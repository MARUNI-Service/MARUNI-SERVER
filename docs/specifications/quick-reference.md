# 빠른 참조 가이드

**자주 사용하는 코드 템플릿 및 체크리스트**

---

## 🔥 자주 사용하는 템플릿

### **Entity 기본 구조**
```java
@Entity
@Table(name = "table_name", indexes = {
    @Index(name = "idx_field", columnList = "field")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExampleEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requiredField;

    public static ExampleEntity createExample(String field) {
        return ExampleEntity.builder()
            .requiredField(field)
            .build();
    }

    public void updateField(String newValue) {
        this.requiredField = newValue;
    }
}
```

### **Service 기본 구조**
```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {
    private final ExampleRepository repository;

    @Transactional
    public void save(ExampleRequestDto req) {
        ExampleEntity entity = ExampleEntity.createExample(req.getField());
        repository.save(entity);
    }

    public ExampleResponseDto findById(Long id) {
        ExampleEntity entity = repository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND));
        return ExampleResponseDto.from(entity);
    }
}
```

### **Controller 기본 구조**
```java
@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Example API", description = "예시 API")
public class ExampleController {
    private final ExampleService service;

    @Operation(summary = "생성")
    @PostMapping
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void create(@Valid @RequestBody ExampleRequestDto req) {
        service.save(req);
    }

    @Operation(summary = "조회")
    @GetMapping("/{id}")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public ExampleResponseDto findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
```

### **DTO 기본 구조**
```java
// Request DTO
@Getter
@Setter
@Schema(description = "요청 DTO")
public class ExampleRequestDto {
    @Schema(description = "필드", example = "example")
    @NotBlank(message = "필드는 필수입니다")
    private String field;
}

// Response DTO
@Getter
@Builder
@Schema(description = "응답 DTO")
public class ExampleResponseDto {
    private Long id;
    private String field;
    private LocalDateTime createdAt;

    public static ExampleResponseDto from(ExampleEntity entity) {
        return ExampleResponseDto.builder()
            .id(entity.getId())
            .field(entity.getRequiredField())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
```

### **테스트 기본 구조**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ExampleService 테스트")
class ExampleServiceTest {
    @Mock private ExampleRepository repository;
    @InjectMocks private ExampleService service;

    @Test
    @DisplayName("생성 성공 - 정상적인 요청으로 생성한다")
    void save_ValidRequest_Success() {
        // given
        ExampleRequestDto req = createValidRequest();

        // when
        assertDoesNotThrow(() -> service.save(req));

        // then
        verify(repository).save(any(ExampleEntity.class));
    }

    private ExampleRequestDto createValidRequest() {
        ExampleRequestDto req = new ExampleRequestDto();
        req.setField("test");
        return req;
    }
}
```

---

## ✅ 체크리스트

### **새 Entity 생성 시**
- [ ] BaseTimeEntity 상속
- [ ] @Table name 및 indexes 정의
- [ ] @Id, @GeneratedValue 설정
- [ ] 정적 팩토리 메서드 구현
- [ ] 비즈니스 로직 메서드 추가
- [ ] public setter 제거

### **새 Service 생성 시**
- [ ] @Slf4j 추가
- [ ] @Transactional(readOnly = true) 기본 설정
- [ ] 쓰기 작업에 @Transactional 명시
- [ ] BaseException 활용한 예외 처리
- [ ] DTO 변환 로직 포함

### **새 Controller 생성 시**
- [ ] @AutoApiResponse 적용
- [ ] @Tag 문서화 추가
- [ ] @Valid Bean Validation 적용
- [ ] @Operation 각 메서드 문서화
- [ ] @SuccessCodeAnnotation 적용
- [ ] RESTful URL 패턴 준수

### **새 DTO 생성 시**
- [ ] Bean Validation 어노테이션
- [ ] Swagger @Schema 문서화
- [ ] 정적 from() 메서드 구현
- [ ] Immutable 객체 설계

### **테스트 작성 시**
- [ ] @ExtendWith(MockitoExtension.class)
- [ ] @DisplayName 한글 설명
- [ ] Given-When-Then 패턴
- [ ] 테스트 메서드명 명확히 작성
- [ ] Mock 객체 적절히 활용

---

## 🏷️ 자주 사용하는 어노테이션

### **Entity 어노테이션**
```java
// 기본 구조
@Entity
@Table(name = "table_name")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder

// 필드 어노테이션
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(nullable = false, unique = true, length = 100)
@Enumerated(EnumType.STRING)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
```

### **Service 어노테이션**
```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 클래스 레벨
@Transactional                   // 메서드 레벨 (쓰기 작업)
```

### **Controller 어노테이션**
```java
@RestController
@RequestMapping("/api/path")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "API명", description = "설명")

// 메서드 레벨
@Operation(summary = "요약")
@GetMapping("/{id}")
@SuccessCodeAnnotation(SuccessCode.SUCCESS)
```

### **DTO 어노테이션**
```java
// Validation
@NotBlank(message = "메시지")
@NotNull @Email @Size(min = 1, max = 100)

// Swagger
@Schema(description = "설명", example = "예시")

// Lombok
@Getter @Setter @Builder
```

---

## 🔧 자주 사용하는 설정

### **application.yml 핵심 설정**
```yaml
spring:
  profiles:
    active: dev
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}
```

### **Docker Compose 기본 구조**
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: maruni
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
```

---

## 🚨 자주 하는 실수

### **Entity 관련**
```java
// ❌ 잘못된 예시
public class BadEntity {
    private String field;

    // setter 사용
    public void setField(String field) {
        this.field = field;
    }
}

// ✅ 올바른 예시
public class GoodEntity extends BaseTimeEntity {
    private String field;

    // 비즈니스 메서드 사용
    public void updateField(String field) {
        this.field = field;
    }

    // 정적 팩토리 메서드
    public static GoodEntity createGood(String field) {
        return GoodEntity.builder().field(field).build();
    }
}
```

### **Service 관련**
```java
// ❌ 트랜잭션 누락
@Service
public class BadService {
    public void save(Request req) { } // 트랜잭션 없음
}

// ✅ 올바른 트랜잭션 설정
@Service
@Transactional(readOnly = true)
public class GoodService {
    @Transactional
    public void save(Request req) { }
}
```

### **Controller 관련**
```java
// ❌ 수동 응답 래핑
@RestController
public class BadController {
    public CommonApiResponse<?> create(Request req) {
        service.save(req);
        return CommonApiResponse.success(SuccessCode.SUCCESS);
    }
}

// ✅ 자동 응답 래핑
@RestController
@AutoApiResponse
public class GoodController {
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void create(@Valid @RequestBody Request req) {
        service.save(req);
    }
}
```

---

## 📞 트러블슈팅

### **자주 발생하는 문제**
```bash
# JWT 인증 실패
→ Authorization 헤더: "Bearer {token}" 형식 확인

# API 응답 래핑 안됨
→ @AutoApiResponse 어노테이션 확인

# JPA N+1 문제
→ @Query fetch join 또는 @EntityGraph 사용

# Docker 컨테이너 연결 실패
→ .env 파일 환경변수 확인
→ docker-compose ps로 상태 확인
```

### **개발 명령어**
```bash
# 전체 환경 시작
docker-compose up -d

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew build

# 로그 확인
docker-compose logs -f app
```

---

**Version**: v1.0.0 | **Updated**: 2025-09-16