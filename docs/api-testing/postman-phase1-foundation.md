# 📋 MARUNI Postman API 테스트 - Phase 1: Foundation Layer

**작성일**: 2025-10-05
**대상**: 회원/인증 시스템 (Member + Auth 도메인)
**난이도**: ⭐ (기초)

---

## 🎯 Phase 1 개요

**Foundation Layer**는 MARUNI 프로젝트의 가장 기본이 되는 **회원 관리 및 JWT 인증 시스템**을 테스트합니다.

### 테스트 목표
- ✅ 회원가입 및 로그인 플로우 검증
- ✅ JWT Access/Refresh Token 발급 및 재발급 검증
- ✅ 인증 기반 API 접근 제어 검증
- ✅ 로그아웃 및 토큰 무효화 검증

### 테스트 순서
```
1. 회원가입
2. 이메일 중복 확인
3. 로그인 (JWT 토큰 발급)
4. 내 정보 조회 (인증 필요)
5. 내 정보 수정 (인증 필요)
6. Access Token 재발급
7. 로그아웃
```

---

## 🔧 사전 준비

### 1. Postman Environment 설정

**Environment 생성**: `MARUNI Local`

**환경 변수 설정**:
```
base_url: http://localhost:8080
access_token: (로그인 후 자동 저장)
refresh_token: (로그인 후 자동 저장)
member_id: (로그인 후 자동 저장)
```

### 2. 서버 실행 확인

```bash
# 애플리케이션 실행 확인
curl http://localhost:8080/actuator/health

# 예상 응답
{
  "status": "UP"
}
```

### 3. Swagger UI 접속 (선택사항)

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📝 API 테스트 시나리오

### ✅ Step 1.1: 회원가입

**목적**: 새로운 회원 계정 생성

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/join`
**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "memberEmail": "test@maruni.com",
  "memberName": "테스트사용자",
  "memberPassword": "password123!"
}
```

**Expected Response** (200 OK):
```json
{
  "code": "M002",
  "message": "회원가입 성공",
  "data": null
}
```

**Postman Tests Script**:
```javascript
pm.test("회원가입 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql("M002");
    console.log("✅ 회원가입 성공");
});
```

**실패 케이스**:

1. **이메일 중복** (409 Conflict):
```json
{
  "code": "E002",
  "message": "이미 존재하는 이메일입니다",
  "data": null
}
```

2. **Validation 실패** (400 Bad Request):
```json
{
  "code": "V001",
  "message": "입력값 유효성 검사 실패",
  "data": {
    "memberEmail": "올바른 이메일 형식이 아닙니다",
    "memberPassword": "비밀번호는 8자 이상이어야 합니다"
  }
}
```

---

### ✅ Step 1.2: 이메일 중복 확인

**목적**: 회원가입 전 이메일 중복 여부 확인

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/join/email-check?memberEmail=test@maruni.com`
**Headers**: 없음 (인증 불필요)

**Expected Response** (200 OK):

**사용 중인 이메일**:
```json
{
  "code": "M001",
  "message": "이메일 중복 확인 완료",
  "data": {
    "available": false,
    "message": "이미 사용 중인 이메일입니다"
  }
}
```

**사용 가능한 이메일**:
```json
{
  "code": "M001",
  "message": "이메일 중복 확인 완료",
  "data": {
    "available": true,
    "message": "사용 가능한 이메일입니다"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("이메일 중복 확인 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    console.log("이메일 사용 가능 여부:", jsonData.data.available);
});
```

---

### ✅ Step 1.3: 로그인 (JWT 토큰 발급) ⭐ **핵심**

**목적**: 로그인하여 Access Token 및 Refresh Token 발급

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/login`
**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "memberEmail": "test@maruni.com",
  "memberPassword": "password123!"
}
```

**Expected Response** (200 OK):
```json
{
  "code": "M003",
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0ZXN0QG1hcnVuaS5jb20iLCJpYXQiOjE2OTU2MjM0MDB9.abc123...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": true
  }
}
```

**Response Headers 확인**:
```
Set-Cookie: refresh=eyJhbGciOiJIUzI1NiIs...; HttpOnly; SameSite=Lax; Path=/; Max-Age=1209600
```

**Postman Tests Script** (자동 토큰 저장):
```javascript
pm.test("로그인 성공 및 토큰 저장", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // Access Token 환경 변수에 저장
    pm.environment.set("access_token", jsonData.data.accessToken);

    console.log("✅ Access Token 저장 완료");
    console.log("Token Type:", jsonData.data.tokenType);
    console.log("만료 시간:", jsonData.data.expiresIn, "초");
});
```

**실패 케이스**:

1. **잘못된 비밀번호** (401 Unauthorized):
```json
{
  "code": "E003",
  "message": "이메일 또는 비밀번호가 일치하지 않습니다",
  "data": null
}
```

2. **존재하지 않는 이메일** (404 Not Found):
```json
{
  "code": "E001",
  "message": "해당 회원을 찾을 수 없습니다",
  "data": null
}
```

---

### ✅ Step 1.4: 내 정보 조회 (JWT 인증 필요) ⭐

**목적**: JWT 토큰이 정상 작동하는지 확인

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/users/me`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "M001",
  "message": "회원 정보 조회 성공",
  "data": {
    "id": 1,
    "memberEmail": "test@maruni.com",
    "memberName": "테스트사용자",
    "createdAt": "2025-10-05T10:30:00"
  }
}
```

**Postman Tests Script** (member_id 저장):
```javascript
pm.test("내 정보 조회 성공 및 ID 저장", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // Member ID 저장 (Phase 2, 3에서 사용)
    pm.environment.set("member_id", jsonData.data.id);

    pm.expect(jsonData.data.memberEmail).to.eql("test@maruni.com");
    console.log("✅ Member ID 저장:", jsonData.data.id);
    console.log("회원 이름:", jsonData.data.memberName);
});
```

**실패 케이스**:

1. **토큰 없음** (401 Unauthorized):
```json
{
  "code": "E004",
  "message": "인증 토큰이 필요합니다",
  "data": null
}
```

2. **토큰 만료** (401 Unauthorized):
```json
{
  "code": "E005",
  "message": "토큰이 만료되었습니다",
  "data": null
}
```

3. **잘못된 토큰** (401 Unauthorized):
```json
{
  "code": "E006",
  "message": "유효하지 않은 토큰입니다",
  "data": null
}
```

---

### ✅ Step 1.5: 내 정보 수정

**목적**: 회원 정보 업데이트 (이름, 비밀번호)

**HTTP Method**: `PUT`
**URL**: `{{base_url}}/api/users/me`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "memberName": "수정된사용자",
  "memberPassword": "newPassword123!"
}
```

**Expected Response** (200 OK):
```json
{
  "code": "M004",
  "message": "회원 정보 수정 성공",
  "data": null
}
```

**Postman Tests Script**:
```javascript
pm.test("회원 정보 수정 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql("M004");
    console.log("✅ 회원 정보 수정 완료");
});
```

**실패 케이스**:

1. **Validation 실패** (400 Bad Request):
```json
{
  "code": "V001",
  "message": "입력값 유효성 검사 실패",
  "data": {
    "memberName": "이름은 2-50자여야 합니다",
    "memberPassword": "비밀번호는 8-20자여야 합니다"
  }
}
```

---

### ✅ Step 1.6: Access Token 재발급

**목적**: Access Token 만료 시 Refresh Token으로 재발급

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/auth/token/refresh`
**Headers**:
```
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
```

**Request Body**: 없음

**Expected Response** (200 OK):
```json
{
  "code": "M006",
  "message": "Access Token 재발급 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.NEW_TOKEN...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": false
  }
}
```

**Postman Tests Script** (토큰 업데이트):
```javascript
pm.test("토큰 재발급 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // 새로운 Access Token으로 업데이트
    pm.environment.set("access_token", jsonData.data.accessToken);

    console.log("✅ Access Token 재발급 완료");
    console.log("새로운 만료 시간:", jsonData.data.expiresIn, "초");
});
```

**실패 케이스**:

1. **Refresh Token 없음** (401 Unauthorized):
```json
{
  "code": "E007",
  "message": "Refresh Token이 필요합니다",
  "data": null
}
```

2. **Refresh Token 만료** (401 Unauthorized):
```json
{
  "code": "E008",
  "message": "Refresh Token이 만료되었습니다. 다시 로그인하세요.",
  "data": null
}
```

3. **Redis에 없는 토큰** (401 Unauthorized):
```json
{
  "code": "E009",
  "message": "유효하지 않은 Refresh Token입니다",
  "data": null
}
```

---

### ✅ Step 1.7: 로그아웃

**목적**: Refresh Token 무효화 및 Access Token 블랙리스트 추가

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/auth/logout`
**Headers**:
```
Authorization: Bearer {{access_token}}
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
```

**Request Body**: 없음

**Expected Response** (200 OK):
```json
{
  "code": "M007",
  "message": "로그아웃 성공",
  "data": null
}
```

**Postman Tests Script**:
```javascript
pm.test("로그아웃 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql("M007");

    console.log("✅ 로그아웃 완료");
    console.log("ℹ️ Refresh Token이 Redis에서 삭제되었습니다");
    console.log("ℹ️ Access Token이 블랙리스트에 추가되었습니다");

    // 환경 변수 정리 (선택사항)
    // pm.environment.unset("access_token");
});
```

**로그아웃 후 동작 확인**:

1. **로그아웃한 Access Token 사용 시** (401 Unauthorized):
```bash
GET /api/users/me
Authorization: Bearer {로그아웃한_토큰}

# 응답
{
  "code": "E010",
  "message": "로그아웃된 토큰입니다",
  "data": null
}
```

2. **로그아웃한 Refresh Token 재발급 시도** (401 Unauthorized):
```bash
POST /api/auth/token/refresh
Cookie: refresh={로그아웃한_토큰}

# 응답
{
  "code": "E009",
  "message": "유효하지 않은 Refresh Token입니다",
  "data": null
}
```

---

## 📊 Phase 1 완료 체크리스트

### API 테스트 완료 여부

```
[ ] Step 1.1: 회원가입 (POST /api/join)
[ ] Step 1.2: 이메일 중복 확인 (GET /api/join/email-check)
[ ] Step 1.3: 로그인 (POST /api/login) ⭐ 핵심
[ ] Step 1.4: 내 정보 조회 (GET /api/users/me) ⭐
[ ] Step 1.5: 내 정보 수정 (PUT /api/users/me)
[ ] Step 1.6: Access Token 재발급 (POST /api/auth/token/refresh)
[ ] Step 1.7: 로그아웃 (POST /api/auth/logout)
```

### 환경 변수 확인

```
[ ] access_token: 저장되어 있음
[ ] member_id: 저장되어 있음
[ ] 로그아웃 후 토큰 무효화 확인
```

### 검증 항목

```
[ ] JWT 토큰 발급/재발급 정상 작동
[ ] 인증 필요한 API에 Authorization 헤더 자동 추가
[ ] 401/403 에러 처리 확인
[ ] Validation 에러 처리 확인
[ ] 로그아웃 후 토큰 블랙리스트 작동 확인
```

---

## 🎯 다음 단계

### Phase 2: Core Service Layer 준비

**Phase 1 완료 후 확인사항**:
1. ✅ `access_token` 환경 변수에 저장되어 있어야 함
2. ✅ `member_id` 환경 변수에 저장되어 있어야 함
3. ✅ 로그아웃하지 않고 Phase 2 진행 (인증 토큰 유지)

**Phase 2에서 테스트할 도메인**:
- ✅ AI 대화 시스템 (Conversation)
- ✅ 보호자 관리 시스템 (Guardian)

**다음 문서**: `postman-phase2-core-service.md`

---

## 🔍 문제 해결 가이드

### 1. 401 Unauthorized 에러

**원인**:
- Authorization 헤더 누락
- 토큰 만료
- 잘못된 토큰 형식

**해결**:
```
1. Authorization 헤더 확인: Bearer {{access_token}}
2. 로그인 다시 수행하여 새 토큰 발급
3. 환경 변수 {{access_token}} 확인
```

### 2. 400 Bad Request (Validation 실패)

**원인**:
- 필수 필드 누락
- 잘못된 데이터 형식
- 길이 제한 초과

**해결**:
```
Response Body의 data 필드에서 구체적인 오류 확인
{
  "data": {
    "memberEmail": "이메일 형식이 아닙니다",
    "memberPassword": "비밀번호는 8자 이상이어야 합니다"
  }
}
```

### 3. 환경 변수가 저장되지 않음

**원인**:
- Tests Script 미실행
- Environment 선택 안 됨

**해결**:
```
1. Postman 우측 상단에서 "MARUNI Local" Environment 선택
2. Tests 탭에서 스크립트 실행 확인
3. Environment 변수 직접 확인
```

### 4. Refresh Token 쿠키가 전송되지 않음

**원인**:
- Postman 쿠키 자동 관리 설정 필요

**해결**:
```
1. Postman Settings → General → "Automatically follow redirects" 체크
2. Cookies 탭에서 수동으로 쿠키 확인 및 추가
3. 또는 Header에 Cookie: refresh=... 직접 추가
```

---

## 📚 참고 문서

- **API 설계 가이드**: `docs/specifications/api-design-guide.md`
- **보안 가이드**: `docs/specifications/security-guide.md`
- **Member 도메인**: `docs/domains/member.md`
- **Auth 도메인**: `docs/domains/auth.md`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

---

**Phase 1 Foundation Layer 테스트 문서 v1.0.0** | 작성: 2025-10-05
