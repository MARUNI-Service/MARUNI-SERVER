# Auth 도메인 API 명세서

**JWT Access Token 기반 Stateless 인증 시스템 완전 가이드**

## 📋 개요

Auth 도메인은 MARUNI 프로젝트의 보안 기반을 제공하는 JWT 기반 Stateless 인증 시스템으로, 로그인 및 토큰 기반 API 인증 기능을 제공합니다.

### 🎯 **핵심 기능**
- **Access Token 단일 시스템**: 1시간 유효기간을 가진 JWT 토큰 발급
- **Stateless 인증**: 서버 측 세션 저장소 없이 토큰 자체로 인증 처리
- **Spring Security 통합**: 필터 체인 기반 자동 인증 처리

### 🔐 **보안 특징**
- **JWT 서명 검증**: HMAC-SHA256 기반 토큰 위변조 방지
- **클라이언트 기반 로그아웃**: 토큰 폐기는 클라이언트에서 처리
- **간결한 아키텍처**: Redis 의존성 제거로 단순하고 안정적인 구조

---

## 🌐 API 엔드포인트 목록

| HTTP | 엔드포인트 | 인증 | 설명 |
|------|------------|------|------|
| `POST` | `/api/members/login` | ❌ | 로그인 (Access Token 발급) |
| 기타 | `/api/**` | ✅ JWT | JWT 인증이 필요한 모든 API |

**참고**: 로그아웃 API는 제공하지 않습니다. 클라이언트에서 토큰을 삭제하여 로그아웃을 처리합니다.

---

## 🔐 로그인 API

### 1. 로그인 (Access Token 발급)

#### **POST** `/api/members/login`

사용자 인증을 수행하고 Access Token을 발급합니다.

**Request Body:**
```json
{
  "memberEmail": "string",
  "memberPassword": "string"
}
```

**Request 예시:**
```json
{
  "memberEmail": "user@example.com",
  "memberPassword": "securePassword123"
}
```

**Validation 규칙:**
- `memberEmail`: 필수, 이메일 형식
- `memberPassword`: 필수, 최소 6자 이상

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_LOGIN_SUCCESS",
  "message": "로그인이 완료되었습니다",
  "data": null
}
```

**Response Headers (성공):**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 401 (로그인 실패):**
```json
{
  "success": false,
  "code": "LOGIN_FAIL",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다",
  "data": null
}
```

**Response 400 (잘못된 입력):**
```json
{
  "success": false,
  "code": "INVALID_INPUT_VALUE",
  "message": "입력값이 올바르지 않습니다",
  "data": {
    "fieldErrors": [
      {
        "field": "memberEmail",
        "message": "올바른 이메일 형식이어야 합니다"
      }
    ]
  }
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/members/login" \
  -H "Content-Type: application/json" \
  -d '{
    "memberEmail": "user@example.com",
    "memberPassword": "securePassword123"
  }'
```

**Response로 받은 Authorization 헤더 저장 예시 (JavaScript):**
```javascript
// 로그인 요청
const response = await fetch('http://localhost:8080/api/members/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    memberEmail: 'user@example.com',
    memberPassword: 'securePassword123'
  })
});

// Authorization 헤더에서 토큰 추출
const accessToken = response.headers.get('Authorization');

// 로컬 스토리지에 저장 (또는 메모리, sessionStorage 등)
localStorage.setItem('accessToken', accessToken);
```

---

## 🔑 인증이 필요한 API 호출

### JWT 토큰 사용 방법

모든 보호된 API는 `Authorization` 헤더에 Access Token을 포함하여 요청해야 합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**예시: 보호된 API 호출 (JavaScript)**
```javascript
// 저장된 토큰 가져오기
const accessToken = localStorage.getItem('accessToken');

// API 요청
const response = await fetch('http://localhost:8080/api/conversations/messages', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': accessToken  // Bearer 포함된 전체 값
  },
  body: JSON.stringify({
    message: '안녕하세요'
  })
});
```

**예시: cURL로 보호된 API 호출**
```bash
curl -X GET "http://localhost:8080/api/members/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response 401 (토큰 누락):**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

**Response 403 (토큰 만료):**
```json
{
  "success": false,
  "code": "ACCESS_TOKEN_EXPIRED",
  "message": "Access Token이 만료되었습니다",
  "data": null
}
```

**Response 403 (유효하지 않은 토큰):**
```json
{
  "success": false,
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰입니다",
  "data": null
}
```

---

## 🚪 로그아웃 처리

### 클라이언트 기반 로그아웃

MARUNI 인증 시스템은 **서버 측 로그아웃 API를 제공하지 않습니다**. 로그아웃은 클라이언트에서 토큰을 삭제하여 처리합니다.

#### **로그아웃 구현 예시 (JavaScript)**
```javascript
// 로그아웃: 저장된 토큰 삭제
function logout() {
  localStorage.removeItem('accessToken');
  // 또는 sessionStorage.removeItem('accessToken');

  // 로그인 페이지로 리다이렉트
  window.location.href = '/login';
}
```

#### **로그아웃 특징**
- **즉시 적용**: 토큰 삭제 시 클라이언트에서 즉시 로그아웃 처리
- **서버 부하 없음**: 서버 측 API 호출 불필요
- **Stateless 유지**: 서버 측 상태 관리 없이 JWT 자체로만 인증

#### **보안 고려사항**
- 토큰 삭제 후에도 해당 토큰은 만료 시간(1시간)까지 유효합니다
- 보안이 중요한 경우 로그아웃 후 즉시 브라우저를 종료하는 것을 권장합니다
- 공용 PC 사용 시 반드시 브라우저 시크릿 모드를 사용하거나 전체 세션을 종료하세요

---

## 📊 데이터 모델

### MemberTokenInfo (Value Object)
```json
{
  "memberId": "string",
  "email": "string"
}
```

토큰에 포함되는 사용자 정보를 나타내는 Value Object입니다.

---

## 🔧 JWT 토큰 시스템 구조

### JWT Access Token 상세

**수명**: 1시간 (3600초)

**저장 위치**: 클라이언트 (로컬 스토리지, 세션 스토리지, 또는 메모리)

**용도**: 모든 보호된 API 호출 시 인증

**헤더 형식**: `Authorization: Bearer {token}`

**토큰 구조 (JWT 표준)**:
```
Header.Payload.Signature

예시:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### JWT Payload 포함 정보

```json
{
  "category": "access",
  "email": "user@example.com",
  "iat": 1625097600,
  "exp": 1625101200
}
```

- **category**: 토큰 타입 (`access` 고정)
- **email**: 사용자 이메일
- **iat**: 발급 시간 (Issued At)
- **exp**: 만료 시간 (Expiration Time)

### 보안 특징

- **HMAC-SHA256 서명**: JWT_SECRET_KEY로 서명하여 위변조 방지
- **짧은 수명**: 1시간 만료로 토큰 탈취 시 피해 최소화
- **Stateless**: 서버 측 세션 저장소 없이 토큰 자체로 인증
- **서명 검증**: 모든 요청마다 서명 유효성 자동 검증

---

## ❌ 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `LOGIN_FAIL` | 401 | 로그인 실패 (잘못된 이메일/비밀번호) |
| `UNAUTHORIZED` | 401 | 인증 실패 (토큰 누락) |
| `ACCESS_TOKEN_EXPIRED` | 403 | Access Token 만료 |
| `INVALID_TOKEN` | 403 | 유효하지 않은 토큰 (서명 불일치, 형식 오류 등) |
| `INVALID_INPUT_VALUE` | 400 | 입력값 검증 실패 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 🧪 테스트 시나리오

### 정상 플로우 (전체 인증 사이클)

```bash
# 1. 로그인 (Access Token 발급)
POST /api/members/login
{
  "memberEmail": "test@example.com",
  "memberPassword": "password123"
}
# Response: Authorization 헤더 = Bearer eyJhbGciOiJIUzI1NiIs...

# 2. 토큰 저장 (클라이언트)
# localStorage.setItem('accessToken', 'Bearer eyJhbGciOiJIUzI1NiIs...')

# 3. 보호된 API 호출
GET /api/members/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
# Response: 200 OK (사용자 정보)

# 4. 다른 보호된 API 호출
POST /api/conversations/messages
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
{
  "message": "안녕하세요"
}
# Response: 200 OK (대화 응답)

# 5. 로그아웃 (클라이언트)
# localStorage.removeItem('accessToken')
# -> 이후 보호된 API 호출 시 401 Unauthorized
```

### 에러 케이스

```bash
# 잘못된 로그인 정보
POST /api/members/login
{
  "memberEmail": "wrong@example.com",
  "memberPassword": "wrongpassword"
}
# Response: 401 LOGIN_FAIL

# 토큰 없이 보호된 API 호출
GET /api/members/me
# Response: 401 UNAUTHORIZED

# 만료된 토큰으로 API 호출 (1시간 후)
GET /api/members/me
Authorization: Bearer expired_token
# Response: 403 ACCESS_TOKEN_EXPIRED

# 유효하지 않은 토큰으로 API 호출
GET /api/members/me
Authorization: Bearer invalid_token
# Response: 403 INVALID_TOKEN
```

### 토큰 만료 처리 시나리오

```javascript
// 토큰 만료 시 클라이언트 처리 예시
async function callProtectedAPI() {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch('http://localhost:8080/api/members/me', {
    headers: { 'Authorization': accessToken }
  });

  if (response.status === 403) {
    // 토큰 만료 -> 재로그인 필요
    alert('세션이 만료되었습니다. 다시 로그인해주세요.');
    localStorage.removeItem('accessToken');
    window.location.href = '/login';
  }
}
```

---

## 📋 관련 문서

### 🔗 **연관 API**
- **[Member API](./member-api.md)**: 회원 가입 및 정보 관리
- **[Guardian API](./guardian-api.md)**: 보호자 관계 설정 (JWT 인증 필요)
- **[Conversation API](./conversation-api.md)**: AI 대화 시스템 (JWT 인증 필요)

### 🛠️ **기술 문서**
- **[Auth 도메인 가이드](../domains/auth.md)**: DDD 구조 및 구현 상세 가이드
- **[보안 가이드](../specifications/security-guide.md)**: JWT 보안 구현 원칙
- **[API 설계 가이드](../specifications/api-design-guide.md)**: REST API 설계 원칙

---

## 💡 개발자 가이드

### Authorization 헤더 형식

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**주의**: `Bearer`와 토큰 사이에 공백이 있어야 합니다.

### 토큰 검증 플로우 (JwtAuthenticationFilter)

```
1. Authorization 헤더에서 Access Token 추출
   ↓
2. "Bearer " 접두사 제거
   ↓
3. JWT 형식 검증 (Header.Payload.Signature)
   ↓
4. JWT 서명 검증 (HMAC-SHA256)
   ↓
5. 만료 시간(exp) 검증
   ↓
6. category 필드 검증 ("access"인지 확인)
   ↓
7. 사용자 이메일 추출
   ↓
8. CustomUserDetailsService를 통해 사용자 정보 로드
   ↓
9. SecurityContext에 인증 정보 설정
   ↓
10. 컨트롤러로 요청 전달
```

### 로그인 처리 플로우 (LoginFilter)

```
1. POST /api/members/login 요청 수신
   ↓
2. JSON 요청 본문에서 memberEmail, memberPassword 추출
   ↓
3. Spring Security AuthenticationManager로 인증 시도
   ↓
4. CustomUserDetailsService가 DB에서 사용자 조회
   ↓
5. 비밀번호 검증 (BCrypt)
   ↓
6. 인증 성공 → AuthenticationService.handleLoginSuccess() 호출
   ↓
7. TokenManager.createAccessToken() 호출
   ↓
8. JWTUtil이 JWT 토큰 생성 (HMAC-SHA256 서명)
   ↓
9. Authorization 헤더에 "Bearer {token}" 설정
   ↓
10. 로그인 성공 응답 반환
```

### 클라이언트 구현 체크리스트

- [ ] 로그인 시 Authorization 헤더에서 토큰 추출
- [ ] 토큰을 로컬 스토리지 또는 세션 스토리지에 안전하게 저장
- [ ] 모든 보호된 API 호출 시 `Authorization: Bearer {token}` 헤더 포함
- [ ] 403 응답 시 토큰 만료 처리 (재로그인 유도)
- [ ] 로그아웃 시 저장된 토큰 삭제
- [ ] HTTPS 사용 (토큰 도청 방지)

### 보안 권장사항

#### 클라이언트 측
- **HTTPS 사용 필수**: 토큰 전송 시 암호화
- **XSS 방지**: 신뢰할 수 없는 입력값 sanitize
- **토큰 저장 위치**:
  - 로컬 스토리지: 편리하지만 XSS 취약
  - 세션 스토리지: 탭 종료 시 자동 삭제
  - 메모리: 가장 안전하지만 새로고침 시 재로그인 필요

#### 서버 측
- **JWT_SECRET_KEY**: 강력한 비밀키 사용 (최소 32자)
- **만료 시간**: 적절한 수명 설정 (현재 1시간)
- **서명 알고리즘**: HS256 (HMAC-SHA256) 사용

---

**Auth API는 MARUNI 플랫폼의 간결하고 안정적인 JWT 기반 Stateless 인증 시스템입니다. Access Token 단일 구조로 복잡성을 최소화하면서도 충분한 보안성을 제공합니다.** 🔐
