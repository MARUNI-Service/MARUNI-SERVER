# Member 도메인

**최종 업데이트**: 2025-10-09
**상태**: ✅ Phase 1 완료

## 📋 개요

회원 가입, 인증, 정보 관리를 담당하는 기반 도메인입니다.

### 핵심 기능
- 회원 가입 및 중복 검증
- 비밀번호 암호화 (BCrypt)
- Spring Security 연동
- JWT 기반 본인 인증

## 🏗️ 주요 엔티티

### MemberEntity
```java
- id: Long
- memberEmail: String        // 로그인 ID (유니크)
- memberName: String          // 회원 이름
- memberPassword: String      // 암호화된 비밀번호
- guardian: GuardianEntity    // 보호자 (다대일)
- pushToken: String           // FCM 푸시 토큰
```

## 🌐 REST API

### 1. 회원가입
```
POST /api/join
Body: {
  "memberEmail": "test@example.com",
  "memberName": "테스트",
  "memberPassword": "password123"
}
```

### 2. 이메일 중복 확인
```
GET /api/join/email-check?memberEmail=test@example.com
```

### 3. 내 정보 조회
```
GET /api/users/me
Headers: Authorization: Bearer {JWT}
```

### 4. 내 정보 수정
```
PUT /api/users/me
Headers: Authorization: Bearer {JWT}
Body: {
  "memberName": "수정된 이름",
  "memberPassword": "newPassword123"
}
```

### 5. 내 계정 삭제
```
DELETE /api/users/me
Headers: Authorization: Bearer {JWT}
```

## 🔧 핵심 서비스

### MemberService
- `save(MemberSaveRequest)`: 회원가입 (이메일 중복 검증 + 비밀번호 암호화)
- `update(MemberUpdateRequest)`: 회원 정보 수정
- `getMyInfo(email)`: 본인 정보 조회 (JWT 이메일 기반)

### CustomUserDetailsService (Spring Security 연동)
- `loadUserByUsername(email)`: 이메일 기반 회원 조회 및 UserDetails 반환

## 🔗 도메인 연동

- **Auth**: Spring Security 인증 + JWT 토큰 발급
- **Guardian**: 보호자 관계 설정/해제 (`assignGuardian`, `removeGuardian`)
- **DailyCheck**: 활성 회원 목록 조회 (`findActiveMemberIds()`)
- **Notification**: 푸시 토큰 관리 (`updatePushToken`, `removePushToken`)

## 📁 패키지 구조

```
member/
├── application/
│   ├── dto/                  # Request/Response DTO
│   ├── service/              # MemberService
│   └── mapper/               # MemberMapper
├── domain/
│   ├── entity/               # MemberEntity
│   └── repository/           # MemberRepository
├── infrastructure/
│   └── security/             # CustomUserDetails, CustomUserDetailsService
└── presentation/
    └── controller/           # JoinApiController, MemberApiController
```

## ✅ 완성도

- [x] 회원 가입 및 중복 검증
- [x] 비밀번호 암호화 (BCrypt)
- [x] Spring Security 연동
- [x] JWT 인증 (본인 정보만 접근)
- [x] 5개 REST API
- [x] 보호자 관계 관리
- [x] 푸시 토큰 관리

**상용 서비스 수준 완성**
