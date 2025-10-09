# API 설계 가이드

**MARUNI REST API 설계 원칙 및 표준**

## 🌐 RESTful API 원칙

### URL 설계 규칙
```
✅ Good:
GET    /api/members          # 회원 목록
GET    /api/members/1        # 회원 조회
POST   /api/members          # 회원 생성
PUT    /api/members/1        # 회원 수정
DELETE /api/members/1        # 회원 삭제

❌ Bad:
GET    /api/getMember        # 동사 사용
POST   /api/member/create    # 불필요한 동사
GET    /api/members_list     # 언더스코어
```

### HTTP 메서드 사용
```
GET:    조회 (멱등성 O, 캐시 가능)
POST:   생성 (멱등성 X)
PUT:    전체 수정 (멱등성 O)
PATCH:  부분 수정 (멱등성 O)
DELETE: 삭제 (멱등성 O)
```

### HTTP 상태 코드
```
200 OK:            조회/수정 성공
201 Created:       생성 성공
204 No Content:    삭제 성공
400 Bad Request:   잘못된 요청
401 Unauthorized:  인증 실패
403 Forbidden:     권한 없음
404 Not Found:     리소스 없음
409 Conflict:      중복 (이메일 등)
500 Server Error:  서버 오류
```

## 📋 MARUNI API 표준 구조

### CommonApiResponse (전역 래핑)
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "memberEmail": "user@example.com",
    "memberName": "홍길동"
  }
}
```

### 에러 응답
```json
{
  "isSuccess": false,
  "code": "M001",
  "message": "회원을 찾을 수 없습니다",
  "data": null
}
```

### 배열 응답
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": [
    {"id": 1, "name": "홍길동"},
    {"id": 2, "name": "김철수"}
  ]
}
```

## 🎯 Controller 작성 패턴

### 기본 CRUD API
```java
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@AutoApiResponse  // 자동 응답 래핑
@Tag(name = "Member", description = "회원 관리 API")
public class MemberController {
    
    private final MemberService memberService;
    
    // 1. 단건 조회
    @GetMapping("/{id}")
    @Operation(summary = "회원 조회", description = "ID로 회원 정보를 조회합니다")
    @SuccessResponseDescription(implementation = MemberResponse.class)
    @CustomExceptionDescription(errorCodes = {ErrorCode.MEMBER_NOT_FOUND})
    public MemberResponse getById(@PathVariable Long id) {
        return memberService.getById(id);
    }
    
    // 2. 목록 조회
    @GetMapping
    @Operation(summary = "회원 목록 조회")
    @SuccessResponseDescription(implementation = MemberResponse.class, isArray = true)
    public List<MemberResponse> getAll() {
        return memberService.getAll();
    }
    
    // 3. 생성
    @PostMapping
    @Operation(summary = "회원 생성")
    @SuccessCodeAnnotation(SuccessCode.CREATED)  // 201 Created
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return memberService.create(request);
    }
    
    // 4. 수정
    @PutMapping("/{id}")
    @Operation(summary = "회원 수정")
    public void update(
        @PathVariable Long id,
        @Valid @RequestBody MemberUpdateRequest request
    ) {
        memberService.update(id, request);
    }
    
    // 5. 삭제
    @DeleteMapping("/{id}")
    @Operation(summary = "회원 삭제")
    public void delete(@PathVariable Long id) {
        memberService.delete(id);
    }
}
```

### 인증이 필요한 API
```java
@GetMapping("/me")
@Operation(summary = "내 정보 조회")
@SuccessResponseDescription(implementation = MemberResponse.class)
public MemberResponse getMyInfo(
    @AuthenticationPrincipal CustomUserDetails userDetails) {
    
    return memberService.getMyInfo(userDetails.getUsername());
}
```

### 페이징 API
```java
@GetMapping("/page")
@Operation(summary = "회원 목록 페이징 조회")
public Page<MemberResponse> getPage(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "createdAt") String sort,
    @RequestParam(defaultValue = "DESC") String direction
) {
    Pageable pageable = PageRequest.of(
        page, 
        size, 
        Sort.by(Sort.Direction.fromString(direction), sort)
    );
    return memberService.getPage(pageable);
}
```

## 📝 Swagger 문서화

### 필수 어노테이션
```java
@RestController
@Tag(name = "도메인명", description = "도메인 설명")
public class Controller {
    
    @Operation(
        summary = "API 요약",
        description = "상세 설명"
    )
    @SuccessResponseDescription(
        implementation = ResponseDto.class,
        isArray = false  // 배열이면 true
    )
    @CustomExceptionDescription(errorCodes = {
        ErrorCode.NOT_FOUND,
        ErrorCode.DUPLICATE
    })
    public ResponseDto api() { ... }
}
```

### DTO 문서화
```java
public record MemberRequest(
    @Schema(description = "회원 이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String memberEmail,
    
    @Schema(description = "회원 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다")
    String memberName,
    
    @Schema(description = "비밀번호", example = "password123")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    String memberPassword
) {}
```

## 🔐 JWT 인증

### Authorization 헤더
```
Authorization: Bearer {access_token}
```

### Swagger에서 JWT 사용
```
1. Swagger UI 접속: http://localhost:8080/swagger-ui.html
2. 우측 상단 "Authorize" 버튼 클릭
3. Value 입력: Bearer {실제토큰값}
4. "Authorize" 클릭
5. 이후 모든 API에 자동으로 토큰 포함
```

## ✅ Bean Validation

### 자주 쓰는 검증 어노테이션
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

### 커스텀 메시지
```java
public record MemberRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String memberEmail
) {}
```

## 🎯 실전 예시

### 1. 보호자 관리 API (Guardian)
```java
// 보호자 생성
POST /api/guardians
{
  "guardianName": "김보호",
  "guardianEmail": "guardian@example.com",
  "relation": "FAMILY"
}

// 응답
{
  "isSuccess": true,
  "code": "S002",
  "message": "생성되었습니다",
  "data": {
    "id": 1,
    "guardianName": "김보호",
    "guardianEmail": "guardian@example.com",
    "relation": "FAMILY"
  }
}
```

### 2. AI 대화 API (Conversation)
```java
// 메시지 전송
POST /api/conversations/messages
Authorization: Bearer {token}
{
  "messageContent": "오늘 기분이 좋아요"
}

// 응답 (AI 답변 포함)
{
  "isSuccess": true,
  "code": "S001",
  "data": {
    "conversationId": 1,
    "userMessage": "오늘 기분이 좋아요",
    "aiResponse": "좋은 소식이네요! 무슨 일이 있으셨나요?",
    "emotion": "POSITIVE"
  }
}
```

### 3. 에러 응답
```java
// 회원 없음
GET /api/members/999
{
  "isSuccess": false,
  "code": "M001",
  "message": "회원을 찾을 수 없습니다",
  "data": null
}

// 중복 이메일
POST /api/members
{
  "isSuccess": false,
  "code": "M002",
  "message": "이미 존재하는 이메일입니다",
  "data": null
}

// Bean Validation 실패
POST /api/members
{
  "isSuccess": false,
  "code": "C001",
  "message": "이메일은 필수입니다",
  "data": null
}
```

## 🚫 금지 사항

```java
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

// ❌ 예외를 Controller에서 처리
@GetMapping("/{id}")
public MemberResponse getById(@PathVariable Long id) {
    try {
        return memberService.getById(id);
    } catch (Exception e) {
        return null;  // GlobalExceptionHandler에 위임
    }
}

// ✅ Service에서 예외 발생 → 자동 처리
@GetMapping("/{id}")
public MemberResponse getById(@PathVariable Long id) {
    return memberService.getById(id);  // BaseException 발생 시 GlobalExceptionHandler가 처리
}
```

---

**상세 구현: `docs/specifications/coding-standards.md` 참조**
