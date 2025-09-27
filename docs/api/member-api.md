# Member 도메인 API 명세서

**회원 관리 시스템 REST API 완전 가이드**

## 📋 개요

Member 도메인은 MARUNI 프로젝트의 기반이 되는 회원 관리 시스템으로, 회원가입, 인증, 프로필 관리 기능을 제공합니다.

### 🎯 **핵심 기능**
- **회원 가입**: 이메일 중복 검증, 비밀번호 암호화
- **본인 정보 관리**: JWT 기반 안전한 CRUD
- **Spring Security 연동**: 인증/인가 통합 시스템

### 🔐 **보안 특징**
- **BCrypt 암호화**: 비밀번호 단방향 암호화
- **JWT 인증**: Access Token 기반 API 접근
- **본인 확인**: 토큰 기반 본인 데이터만 접근 가능

---

## 🌐 API 엔드포인트 목록

| HTTP | 엔드포인트 | 인증 | 설명 |
|------|------------|------|------|
| `POST` | `/api/join` | ❌ | 회원가입 |
| `GET` | `/api/join/email-check` | ❌ | 이메일 중복 확인 |
| `GET` | `/api/users/me` | ✅ JWT | 내 정보 조회 |
| `PUT` | `/api/users/me` | ✅ JWT | 내 정보 수정 |
| `DELETE` | `/api/users/me` | ✅ JWT | 내 계정 삭제 |

---

## 🔐 회원가입 API

### 1. 회원가입

#### **POST** `/api/join`

새로운 회원을 등록합니다.

**Request Body:**
```json
{
  "memberEmail": "string",
  "memberName": "string",
  "memberPassword": "string"
}
```

**Request 예시:**
```json
{
  "memberEmail": "user@example.com",
  "memberName": "김할머니",
  "memberPassword": "securePassword123"
}
```

**Validation 규칙:**
- `memberEmail`: 필수, 이메일 형식, 중복 불가
- `memberName`: 필수, 최소 1자 이상
- `memberPassword`: 필수, 최소 6자 이상

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_JOIN_SUCCESS",
  "message": "회원가입이 완료되었습니다",
  "data": null
}
```

**Response 409 (이메일 중복):**
```json
{
  "success": false,
  "code": "DUPLICATE_EMAIL",
  "message": "이미 존재하는 이메일입니다",
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
curl -X POST "http://localhost:8080/api/join" \
  -H "Content-Type: application/json" \
  -d '{
    "memberEmail": "user@example.com",
    "memberName": "김할머니",
    "memberPassword": "securePassword123"
  }'
```

---

### 2. 이메일 중복 확인

#### **GET** `/api/join/email-check`

회원가입 전 이메일 중복 여부를 확인합니다.

**Query Parameters:**
- `memberEmail` (string, required): 확인할 이메일 주소

**Request 예시:**
```
GET /api/join/email-check?memberEmail=user@example.com
```

**Response 200 (사용 가능):**
```json
{
  "success": true,
  "code": "EMAIL_AVAILABLE",
  "message": "사용 가능한 이메일입니다",
  "data": {
    "available": true,
    "email": "user@example.com"
  }
}
```

**Response 409 (이미 사용 중):**
```json
{
  "success": false,
  "code": "DUPLICATE_EMAIL",
  "message": "이미 존재하는 이메일입니다",
  "data": {
    "available": false,
    "email": "user@example.com"
  }
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/join/email-check?memberEmail=user@example.com"
```

---

## 👤 회원 정보 관리 API

### 3. 내 정보 조회

#### **GET** `/api/users/me`

JWT 토큰을 통해 인증된 사용자의 정보를 조회합니다.

**Request Headers:**
```
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "조회되었습니다",
  "data": {
    "id": 1,
    "memberName": "김할머니",
    "memberEmail": "user@example.com",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
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

**Response 403 (토큰 만료):**
```json
{
  "success": false,
  "code": "TOKEN_EXPIRED",
  "message": "토큰이 만료되었습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 4. 내 정보 수정

#### **PUT** `/api/users/me`

JWT 토큰을 통해 인증된 사용자의 정보를 수정합니다.

**Request Headers:**
```
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

**Request Body:**
```json
{
  "memberName": "string",
  "memberPassword": "string"
}
```

**Request 예시:**
```json
{
  "memberName": "김할머니 수정",
  "memberPassword": "newSecurePassword456"
}
```

**Validation 규칙:**
- `memberName`: 필수, 최소 1자 이상
- `memberPassword`: 필수, 최소 6자 이상

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_UPDATE_SUCCESS",
  "message": "회원정보가 수정되었습니다",
  "data": {
    "id": 1,
    "memberName": "김할머니 수정",
    "memberEmail": "user@example.com",
    "updatedAt": "2025-01-15T14:30:00"
  }
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
        "field": "memberPassword",
        "message": "비밀번호는 최소 6자 이상이어야 합니다"
      }
    ]
  }
}
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
curl -X PUT "http://localhost:8080/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "memberName": "김할머니 수정",
    "memberPassword": "newSecurePassword456"
  }'
```

---

### 5. 내 계정 삭제

#### **DELETE** `/api/users/me`

JWT 토큰을 통해 인증된 사용자의 계정을 삭제합니다.

**Request Headers:**
```
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "MEMBER_DELETE_SUCCESS",
  "message": "계정이 삭제되었습니다",
  "data": null
}
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

**Response 404 (회원 없음):**
```json
{
  "success": false,
  "code": "MEMBER_NOT_FOUND",
  "message": "회원을 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X DELETE "http://localhost:8080/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📊 데이터 모델

### MemberEntity
```json
{
  "id": "number (Long)",
  "memberEmail": "string (unique)",
  "memberName": "string",
  "memberPassword": "string (BCrypt 암호화)",
  "socialType": "enum (GOOGLE, KAKAO, NAVER)",
  "socialId": "string (nullable)",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### MemberResponse
```json
{
  "id": "number",
  "memberName": "string",
  "memberEmail": "string",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## 🔧 인증 및 보안

### JWT 토큰 인증
Member API의 보호된 엔드포인트는 JWT Access Token이 필요합니다.

**Authorization 헤더 형식:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 토큰 획득 방법
1. **로그인**: `POST /api/members/login` (Auth 도메인)
2. **토큰 재발급**: `POST /api/auth/token/refresh` (Auth 도메인)

### 보안 특징
- **비밀번호 암호화**: BCryptPasswordEncoder 사용
- **SQL Injection 방지**: JPA Repository 사용
- **본인 확인**: JWT에서 추출한 memberId로만 접근
- **HTTPS 권장**: 운영 환경에서 필수

---

## ❌ 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `DUPLICATE_EMAIL` | 409 | 이메일 중복 |
| `MEMBER_NOT_FOUND` | 404 | 회원을 찾을 수 없음 |
| `INVALID_INPUT_VALUE` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `TOKEN_EXPIRED` | 403 | 토큰 만료 |
| `FORBIDDEN` | 403 | 권한 부족 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 🧪 테스트 시나리오

### 정상 플로우
```bash
# 1. 이메일 중복 확인
GET /api/join/email-check?memberEmail=test@example.com

# 2. 회원가입
POST /api/join
{
  "memberEmail": "test@example.com",
  "memberName": "테스트사용자",
  "memberPassword": "password123"
}

# 3. 로그인 (Auth 도메인)
POST /api/members/login
{
  "email": "test@example.com",
  "password": "password123"
}

# 4. 내 정보 조회
GET /api/users/me
Authorization: Bearer {토큰}

# 5. 내 정보 수정
PUT /api/users/me
Authorization: Bearer {토큰}
{
  "memberName": "수정된이름",
  "memberPassword": "newPassword456"
}
```

### 에러 케이스
```bash
# 이메일 중복 에러
POST /api/join
{
  "memberEmail": "existing@example.com", # 이미 존재하는 이메일
  "memberName": "테스트",
  "memberPassword": "password123"
}
# Response: 409 DUPLICATE_EMAIL

# 인증 없이 보호된 API 호출
GET /api/users/me
# Response: 401 UNAUTHORIZED

# 잘못된 토큰으로 API 호출
GET /api/users/me
Authorization: Bearer invalid_token
# Response: 403 TOKEN_EXPIRED
```

---

## 📋 관련 문서

### 🔗 **연관 API**
- **[Auth API](./auth-api.md)**: JWT 토큰 발급 및 관리
- **[Guardian API](./guardian-api.md)**: 보호자 관계 설정
- **[Conversation API](./conversation-api.md)**: AI 대화 시스템

### 🛠️ **기술 문서**
- **[Member 도메인 가이드](../domains/member.md)**: 구현 상세 가이드
- **[API 설계 가이드](../specifications/api-design-guide.md)**: REST API 설계 원칙
- **[보안 가이드](../specifications/security-guide.md)**: JWT 보안 구현

---

**Member API는 MARUNI 플랫폼의 모든 사용자 관리 기능의 기반이 되는 핵심 API입니다. 안전하고 직관적인 회원 관리 기능을 제공하여 노인 돌봄 서비스의 신뢰성을 보장합니다.** 🚀