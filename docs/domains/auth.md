# Auth 도메인

**최종 업데이트**: 2025-10-09
**상태**: ✅ Stateless JWT 완성 (Access Token Only)

## 📋 개요

JWT 기반 Stateless 인증 시스템입니다. Access Token 단일 토큰으로 간결한 인증을 제공합니다.

### 핵심 기능
- Access Token 발급 (1시간 유효)
- JWT 검증 및 인증 처리
- Spring Security 필터 체인 통합
- 클라이언트 기반 로그아웃

## 🏗️ 주요 구조

### TokenManager 인터페이스 (Domain Layer)
```java
- createAccessToken(memberId, email): JWT 생성
- extractAccessToken(request): 헤더에서 토큰 추출
- getEmail(token): 토큰에서 이메일 추출
- isAccessToken(token): Access Token 검증
```

### JWTUtil 구현체 (Global Layer)
- TokenManager 인터페이스 구현
- HMAC-SHA256 서명
- 토큰 생성/검증/파싱

## 🔧 인증 플로우

### 1. 로그인
```
POST /api/auth/login
Body: { "email": "user@example.com", "password": "password123" }
↓
Spring Security AuthenticationManager
↓
LoginFilter: 인증 성공
↓
AuthenticationService.handleLoginSuccess()
↓
Response Header: Authorization: Bearer {token}
```

### 2. API 요청 인증
```
GET /api/users/me
Headers: Authorization: Bearer {token}
↓
JwtAuthenticationFilter: 토큰 추출 및 검증
↓
CustomUserDetailsService: 사용자 로드
↓
SecurityContext: 인증 정보 설정
↓
Controller: 인증된 사용자 정보 접근
```

### 3. 로그아웃 (클라이언트 처리)
```javascript
// 클라이언트에서 토큰 삭제
localStorage.removeItem('access_token');
window.location.href = '/login';
```

## 🔗 주요 컴포넌트

### AuthenticationService (Application Layer)
- `handleLoginSuccess()`: 로그인 성공 시 Access Token 발급

### JwtAuthenticationFilter (Global Security)
- Spring Security 필터로 모든 요청의 JWT 검증

### MemberTokenInfo (Value Object)
- 토큰 발급에 필요한 회원 정보 (memberId, email)

## ⚙️ 설정

### application-security.yml
```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}  # 1시간
```

### 환경 변수 (.env)
```bash
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000  # 1시간 (밀리초)
```

## 📁 패키지 구조

```
auth/
├── application/
│   └── service/              # AuthenticationService
├── domain/
│   ├── service/              # TokenManager 인터페이스
│   └── vo/                   # MemberTokenInfo

global/security/
├── JWTUtil.java              # TokenManager 구현체
├── JwtAuthenticationFilter.java
├── LoginFilter.java
└── AuthenticationEventHandler.java
```

## 📈 보안 특성

- ✅ **Stateless**: 서버 상태 저장 없음, 수평 확장 용이
- ✅ **표준 기반**: JWT RFC 7519 준수
- ✅ **짧은 수명**: 1시간 토큰으로 탈취 위험 최소화
- ✅ **CSRF 방어**: Custom Header 사용으로 자동 방어

## ✅ 완성도

- [x] Access Token 발급/검증
- [x] Spring Security 통합
- [x] JWT 필터 체인
- [x] 클라이언트 로그아웃
- [x] DDD 의존성 역전 구조

**상용 서비스 수준 완성**
