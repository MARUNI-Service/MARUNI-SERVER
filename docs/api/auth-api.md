# Auth 도메인 API 명세서

**JWT 토큰 기반 인증/인가 시스템 REST API 완전 가이드**

## 📋 개요

Auth 도메인은 MARUNI 프로젝트의 보안 기반을 제공하는 JWT 기반 인증/인가 시스템으로, 토큰 발급, 갱신, 무효화 기능을 제공합니다.

### 🎯 **핵심 기능**
- **이중 토큰 시스템**: Access Token(1시간) + Refresh Token(24시간) 분리 보안
- **JWT 토큰 관리**: 발급, 검증, 갱신, 무효화 완전 자동화
- **Redis 기반 저장**: Refresh Token + 블랙리스트 관리

### 🔐 **보안 특징**
- **HttpOnly 쿠키**: XSS 공격 방지를 위한 Refresh Token 저장
- **토큰 블랙리스트**: 로그아웃된 Access Token 즉시 무효화
- **자동 TTL 관리**: Redis 기반 토큰 생명주기 관리

---

## 🌐 API 엔드포인트 목록

| HTTP | 엔드포인트 | 인증 | 설명 |
|------|------------|------|------|
| `POST` | `/api/members/login` | ❌ | 로그인 (토큰 발급) |
| `POST` | `/api/auth/token/refresh` | 🔄 Refresh Token | Access Token 재발급 |
| `POST` | `/api/auth/token/refresh/full` | 🔄 Refresh Token | 전체 토큰 재발급 |
| `POST` | `/api/auth/logout` | ✅ JWT | 로그아웃 (토큰 무효화) |

---

## 🔐 로그인 API

### 1. 로그인 (토큰 발급)

#### **POST** `/api/members/login`

사용자 인증을 수행하고 Access Token과 Refresh Token을 발급합니다.

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
Set-Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; HttpOnly; Secure; SameSite=Strict; Max-Age=86400
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

---

## 🔄 토큰 관리 API

### 2. Access Token 재발급

#### **POST** `/api/auth/token/refresh`

Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. (일반적인 갱신)

**Request Headers:**
```
Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_TOKEN_REISSUE_SUCCESS",
  "message": "토큰이 재발급되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": false
  }
}
```

**Response Headers (성공):**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 401 (토큰 오류):**
```json
{
  "success": false,
  "code": "INVALID_REFRESH_TOKEN",
  "message": "유효하지 않은 Refresh Token입니다",
  "data": null
}
```

**Response 401 (토큰 만료):**
```json
{
  "success": false,
  "code": "REFRESH_TOKEN_EXPIRED",
  "message": "Refresh Token이 만료되었습니다",
  "data": null
}
```

**Response 401 (토큰 누락):**
```json
{
  "success": false,
  "code": "REFRESH_TOKEN_NOT_FOUND",
  "message": "Refresh Token을 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/auth/token/refresh" \
  -H "Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 3. 전체 토큰 재발급 (보안 강화)

#### **POST** `/api/auth/token/refresh/full`

Refresh Token을 사용하여 Access Token과 Refresh Token을 모두 재발급받습니다. (보안 강화 갱신)

**Request Headers:**
```
Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_TOKEN_REISSUE_FULL_SUCCESS",
  "message": "모든 토큰이 재발급되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": true
  }
}
```

**Response Headers (성공):**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Set-Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; HttpOnly; Secure; SameSite=Strict; Max-Age=86400
```

**Response 401 (토큰 오류):**
```json
{
  "success": false,
  "code": "INVALID_REFRESH_TOKEN",
  "message": "유효하지 않은 Refresh Token입니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/auth/token/refresh/full" \
  -H "Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 4. 로그아웃 (토큰 무효화)

#### **POST** `/api/auth/logout`

현재 사용자의 모든 토큰을 무효화하고 로그아웃을 처리합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_LOGOUT_SUCCESS",
  "message": "로그아웃이 완료되었습니다",
  "data": null
}
```

**Response Headers (성공):**
```
Set-Cookie: refresh=; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT
```

**Response 401 (인증 실패):**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/auth/logout" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📊 데이터 모델

### TokenResponse
```json
{
  "accessToken": "string (JWT token)",
  "tokenType": "string (Bearer)",
  "expiresIn": "number (seconds)",
  "refreshTokenIncluded": "boolean"
}
```

### MemberTokenInfo (Value Object)
```json
{
  "memberId": "string",
  "email": "string"
}
```

### RefreshToken Entity (Redis)
```json
{
  "memberId": "string (Primary Key)",
  "token": "string (JWT token)",
  "ttl": "number (seconds)"
}
```

---

## 🔧 토큰 시스템 구조

### JWT 토큰 종류
1. **Access Token**
   - 수명: 1시간 (3600초)
   - 저장: HTTP Authorization 헤더
   - 용도: 모든 API 호출 시 인증
   - 형식: `Bearer eyJhbGciOiJIUzI1NiIs...`

2. **Refresh Token**
   - 수명: 24시간 (86400초)
   - 저장: HttpOnly 쿠키 + Redis
   - 용도: Access Token 재발급
   - 형식: `eyJhbGciOiJIUzI1NiIs...`

### 토큰 획득 방법
1. **초기 발급**: `POST /api/members/login`
2. **Access Token 갱신**: `POST /api/auth/token/refresh`
3. **전체 토큰 갱신**: `POST /api/auth/token/refresh/full`

### 보안 특징
- **이중 토큰 시스템**: 짧은 수명 Access Token + 긴 수명 Refresh Token
- **HttpOnly 쿠키**: XSS 공격 차단
- **토큰 블랙리스트**: 로그아웃된 Access Token 즉시 무효화
- **Redis TTL**: 자동 만료를 통한 토큰 생명주기 관리
- **CSRF 보호**: SameSite=Strict 쿠키 속성

---

## ❌ 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `LOGIN_FAIL` | 401 | 로그인 실패 (잘못된 이메일/비밀번호) |
| `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 Refresh Token |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token 만료 |
| `REFRESH_TOKEN_NOT_FOUND` | 401 | Refresh Token 누락 |
| `ACCESS_TOKEN_EXPIRED` | 403 | Access Token 만료 |
| `TOKEN_BLACKLISTED` | 403 | 블랙리스트된 토큰 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `INVALID_INPUT_VALUE` | 400 | 입력값 검증 실패 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 🧪 테스트 시나리오

### 정상 플로우 (전체 인증 사이클)
```bash
# 1. 로그인 (토큰 발급)
POST /api/members/login
{
  "memberEmail": "test@example.com",
  "memberPassword": "password123"
}
# Response: Authorization 헤더 + refresh 쿠키 설정

# 2. 보호된 API 호출
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

# 3. Access Token 갱신 (1시간 후)
POST /api/auth/token/refresh
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
# Response: 새로운 Authorization 헤더

# 4. 보안 강화 갱신 (전체 토큰 교체)
POST /api/auth/token/refresh/full
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
# Response: 새로운 Authorization 헤더 + 새로운 refresh 쿠키

# 5. 로그아웃 (토큰 무효화)
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
# Response: 토큰 무효화 및 쿠키 만료
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

# 만료된 Refresh Token으로 갱신 시도
POST /api/auth/token/refresh
Cookie: refresh=expired_token
# Response: 401 REFRESH_TOKEN_EXPIRED

# Refresh Token 없이 갱신 시도
POST /api/auth/token/refresh
# Response: 401 REFRESH_TOKEN_NOT_FOUND

# 블랙리스트된 Access Token으로 API 호출
GET /api/users/me
Authorization: Bearer blacklisted_token
# Response: 403 TOKEN_BLACKLISTED

# 로그아웃 후 토큰 재사용 시도
POST /api/auth/token/refresh
Cookie: refresh=revoked_token
# Response: 401 INVALID_REFRESH_TOKEN
```

### 보안 시나리오
```bash
# Access Token 탈취 대응 (짧은 수명으로 피해 최소화)
# - 1시간 후 자동 만료
# - 로그아웃 시 블랙리스트 추가로 즉시 무효화

# Refresh Token 탈취 대응
# - HttpOnly 쿠키로 JavaScript 접근 차단
# - 새 로그인 시 기존 토큰 자동 무효화
# - 서버 측 revoke 기능으로 즉시 차단

# 동시 로그인 제어
# - 회원당 하나의 Refresh Token만 Redis에 저장
# - 새 로그인 시 기존 토큰 자동 교체
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

### Refresh Token 쿠키 형식
```
Cookie: refresh=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 토큰 검증 플로우
```
1. Authorization 헤더에서 Access Token 추출
2. JWT 형식 및 만료 시간 검증
3. 블랙리스트 확인 (Redis)
4. 토큰 서명 검증 (HMAC-SHA256)
5. 사용자 정보 추출 및 SecurityContext 설정
```

### Refresh Token 갱신 플로우
```
1. 쿠키에서 Refresh Token 추출
2. JWT 형식 및 만료 시간 검증
3. Redis 저장된 토큰과 일치 여부 확인
4. 새로운 Access Token 생성
5. (전체 갱신 시) 새로운 Refresh Token 생성 및 기존 토큰 교체
```

### 로그아웃 처리 플로우
```
1. Access Token을 블랙리스트에 추가 (만료 시간까지)
2. Redis에서 Refresh Token 삭제
3. Refresh Token 쿠키 만료 설정
4. 로그아웃 완료 응답
```

---

**Auth API는 MARUNI 플랫폼의 모든 보안 요구사항을 만족하는 완성된 JWT 기반 인증/인가 시스템입니다. 이중 토큰 구조와 Redis 기반 토큰 관리를 통해 높은 보안성과 사용자 경험을 동시에 제공합니다.** 🔐