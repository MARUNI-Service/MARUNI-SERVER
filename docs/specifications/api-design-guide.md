# API 설계 가이드

**MARUNI REST API 설계 규칙 및 표준**

---

## 🌐 REST API 설계 원칙

### **URL 패턴 규칙**
```
✅ RESTful URL 패턴
GET    /api/users              # 사용자 목록 조회
GET    /api/users/{id}         # 특정 사용자 조회
POST   /api/users              # 사용자 생성
PUT    /api/users/{id}         # 사용자 수정
DELETE /api/users/{id}         # 사용자 삭제

✅ 복합 리소스 패턴
POST   /api/conversations/messages    # 대화 메시지 전송
GET    /api/alert-rules               # 알림 규칙 목록
POST   /api/alert-rules/{id}/toggle   # 알림 규칙 토글

❌ 잘못된 패턴
/api/getUserById
/api/user/create
/api/deleteUser
```

### **HTTP 메서드 사용 규칙**
```yaml
GET: 조회 (Idempotent, Safe)
POST: 생성, 복합 액션 (Non-Idempotent)
PUT: 전체 수정 (Idempotent)
PATCH: 부분 수정 (선택적)
DELETE: 삭제 (Idempotent)
```

---

## 📋 표준 응답 형식

### **CommonApiResponse 구조**
```json
// 성공 응답
{
  "code": "M001",
  "message": "회원 정보 조회 성공",
  "data": {
    "id": 1,
    "memberName": "홍길동",
    "memberEmail": "user@example.com"
  }
}

// 실패 응답
{
  "code": "E001",
  "message": "해당 회원을 찾을 수 없습니다",
  "data": null
}

// 유효성 검사 실패
{
  "code": "V001",
  "message": "입력값 유효성 검사 실패",
  "data": {
    "memberEmail": "이메일 형식이 아닙니다",
    "memberName": "이름은 필수입니다"
  }
}
```

### **페이징 응답 형식**
```json
{
  "code": "M001",
  "message": "회원 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "memberName": "홍길동"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  }
}
```

---

## 🎯 Controller 표준 구현

### **Controller 클래스 템플릿**
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse  // 자동 응답 래핑
@Tag(name = "회원 관리 API", description = "사용자 CRUD API")
@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
public class UserApiController {
    private final MemberService memberService;

    // 구현
}
```

### **CRUD 메서드 표준 구현**

#### **생성 (CREATE)**
```java
@Operation(summary = "사용자 생성", description = "새로운 사용자를 등록합니다.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "생성 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 유효성 실패"),
    @ApiResponse(responseCode = "409", description = "이메일 중복")
})
@PostMapping
@SuccessCodeAnnotation(SuccessCode.MEMBER_CREATED)
public void create(@Valid @RequestBody MemberSaveRequest request) {
    memberService.save(request);
}
```

#### **조회 (READ)**
```java
@Operation(summary = "사용자 목록 조회")
@GetMapping
@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
public List<MemberResponse> findAll() {
    return memberService.findAll();
}

@Operation(summary = "특정 사용자 조회")
@GetMapping("/{id}")
@SuccessCodeAnnotation(SuccessCode.MEMBER_VIEW)
public MemberResponse findById(@PathVariable Long id) {
    return memberService.findById(id);
}
```

#### **수정 (UPDATE)**
```java
@Operation(summary = "사용자 정보 수정")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "수정 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 유효성 실패"),
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
})
@PutMapping("/{id}")
@SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
public void update(
    @PathVariable Long id,
    @Valid @RequestBody MemberUpdateRequest request
) {
    request.setId(id);
    memberService.update(request);
}
```

#### **삭제 (DELETE)**
```java
@Operation(summary = "사용자 삭제")
@DeleteMapping("/{id}")
@SuccessCodeAnnotation(SuccessCode.MEMBER_DELETED)
public void delete(@PathVariable Long id) {
    memberService.deleteById(id);
}
```

### **복합 액션 구현**
```java
@Operation(summary = "대화 메시지 전송")
@PostMapping("/messages")
@SuccessCodeAnnotation(SuccessCode.SUCCESS)
public ConversationResponseDto sendMessage(
    @AuthenticationPrincipal MemberEntity member,
    @Valid @RequestBody ConversationRequestDto request
) {
    return conversationService.processUserMessage(member.getId(), request.getContent());
}
```

---

## 📝 DTO 설계 표준

### **Request DTO 표준**
```java
@Getter
@Setter
@Schema(description = "회원가입 요청 DTO")
public class MemberSaveRequest {
    @Schema(
        description = "회원 이메일",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    private String memberEmail;

    @Schema(description = "회원 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String memberName;

    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다.")
    private String memberPassword;
}
```

### **Response DTO 표준**
```java
@Getter
@Builder
@Schema(description = "회원 정보 응답 DTO")
public class MemberResponse {
    @Schema(description = "회원 ID", example = "1")
    private Long id;

    @Schema(description = "회원 이메일", example = "user@example.com")
    private String memberEmail;

    @Schema(description = "회원 이름", example = "홍길동")
    private String memberName;

    @Schema(description = "가입일시", example = "2025-09-16T10:30:00")
    private LocalDateTime createdAt;

    // 정적 변환 메서드
    public static MemberResponse from(MemberEntity entity) {
        return MemberResponse.builder()
            .id(entity.getId())
            .memberEmail(entity.getMemberEmail())
            .memberName(entity.getMemberName())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    // 리스트 변환 메서드
    public static List<MemberResponse> fromList(List<MemberEntity> entities) {
        return entities.stream()
            .map(MemberResponse::from)
            .collect(Collectors.toList());
    }
}
```

---

## 📚 Swagger 문서화 표준

### **API 문서화 필수 어노테이션**
```java
// 컨트롤러 레벨
@Tag(name = "회원 관리 API", description = "사용자 CRUD 관련 API")
@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)

// 메서드 레벨
@Operation(
    summary = "사용자 정보 수정",
    description = "요청 본문의 정보로 사용자 정보를 수정합니다."
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "수정 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 유효성 실패"),
    @ApiResponse(responseCode = "404", description = "해당 사용자가 존재하지 않음")
})
@SuccessCodeAnnotation(SuccessCode.MEMBER_UPDATED)
```

### **DTO 문서화**
```java
// 클래스 레벨
@Schema(description = "회원가입 요청 DTO")

// 필드 레벨
@Schema(
    description = "회원 이메일",
    example = "user@example.com",
    requiredMode = Schema.RequiredMode.REQUIRED
)
```

---

## 🔒 인증 및 권한

### **JWT 인증 적용**
```java
// 인증 필요한 API
@GetMapping("/profile")
public MemberResponse getProfile(@AuthenticationPrincipal MemberEntity member) {
    return memberService.getProfile(member.getId());
}

// 인증 불필요한 API (SecurityConfig에서 permitAll 설정)
@PostMapping("/join")
public void join(@Valid @RequestBody MemberSaveRequest request) {
    memberService.save(request);
}
```

### **권한 레벨별 접근 제어**
```java
// 관리자 전용 API
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<MemberResponse> getAllUsers() {
    return memberService.findAll();
}

// 본인 또는 Guardian 접근 가능
@PreAuthorize("@memberService.canAccess(#memberId, authentication.name)")
@GetMapping("/users/{memberId}/health")
public HealthResponse getHealthInfo(@PathVariable Long memberId) {
    return healthService.getHealthInfo(memberId);
}
```

---

## ⚡ 성능 최적화

### **페이징 처리**
```java
@GetMapping
public Page<MemberResponse> findAll(
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
    Pageable pageable
) {
    return memberService.findAll(pageable);
}
```

### **캐싱 적용**
```java
@GetMapping("/{id}")
@Cacheable(value = "members", key = "#id")
public MemberResponse findById(@PathVariable Long id) {
    return memberService.findById(id);
}
```

### **조건부 요청 처리**
```java
@GetMapping("/{id}")
public ResponseEntity<MemberResponse> findById(
    @PathVariable Long id,
    @RequestHeader(value = "If-None-Match", required = false) String etag
) {
    MemberResponse member = memberService.findById(id);
    String currentEtag = generateEtag(member);

    if (currentEtag.equals(etag)) {
        return ResponseEntity.notModified().build();
    }

    return ResponseEntity.ok()
        .eTag(currentEtag)
        .body(member);
}
```

---

## 🎯 API 개발 체크리스트

### **Controller 개발 시**
- [ ] @AutoApiResponse 적용
- [ ] @Tag, @Operation 문서화 완료
- [ ] @Valid Bean Validation 적용
- [ ] RESTful URL 패턴 준수
- [ ] 적절한 HTTP 메서드 사용

### **DTO 개발 시**
- [ ] Bean Validation 어노테이션 적용
- [ ] Swagger @Schema 문서화
- [ ] 정적 from() 변환 메서드 구현
- [ ] Immutable 객체 설계

### **응답 처리 시**
- [ ] CommonApiResponse 자동 래핑 확인
- [ ] SuccessCode 적절히 설정
- [ ] 예외는 GlobalExceptionHandler 위임
- [ ] 페이징 응답 형식 준수

### **보안 고려사항**
- [ ] JWT 인증 적용 여부 확인
- [ ] 권한 레벨 검증 로직 구현
- [ ] 민감한 정보 응답 제외
- [ ] CORS 정책 확인

---

**Version**: v1.0.0 | **Updated**: 2025-09-16