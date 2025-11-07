# Member 도메인

**최종 업데이트**: 2025-11-07
**상태**: ✅ Phase 2 완료

## 📋 개요

회원 가입, 인증, 정보 관리를 담당하는 기반 도메인입니다.

### 핵심 기능
- 회원 가입 및 중복 검증
- 비밀번호 암호화 (BCrypt)
- Spring Security 연동
- JWT 기반 본인 인증
- 보호자 자기 참조 관계 관리
- 안부 메시지 수신 설정

## 🏗️ 주요 엔티티

### MemberEntity
- id: Long
- memberEmail: String (유니크, 로그인 ID)
- memberName: String (회원 이름)
- memberPassword: String (암호화된 비밀번호)
- dailyCheckEnabled: Boolean (안부 메시지 수신 여부, 기본값: false)
- guardian: MemberEntity (내 보호자, 자기 참조)
- managedMembers: List<MemberEntity> (내가 돌보는 사람들, 자기 참조)
- guardianRelation: GuardianRelation (보호자와의 관계)

## 🌐 REST API (9개)

### 회원가입 & 검증

#### 1. 회원가입
```
POST /api/join
Body: {
  "memberEmail": "test@example.com",
  "memberName": "테스트",
  "memberPassword": "password123"
}
```

#### 2. 이메일 중복 확인
```
GET /api/join/email-check?memberEmail=test@example.com
```

### 회원 조회

#### 3. 회원 검색 (이메일 기반)
```
GET /api/members/search?email=elderly@example.com
Headers: Authorization: Bearer {JWT}
```

#### 4. 내 정보 조회
```
GET /api/members/me
Headers: Authorization: Bearer {JWT}
Response: {
  "id": 1,
  "memberEmail": "user@example.com",
  "memberName": "사용자",
  "dailyCheckEnabled": true,
  "guardian": { ... },
  "managedMembers": [ ... ]
}
```

#### 5. 내가 돌보는 사람들 목록 조회
```
GET /api/members/me/managed-members
Headers: Authorization: Bearer {JWT}
```

### 회원 정보 관리

#### 6. 안부 메시지 설정 변경
```
PATCH /api/members/me/daily-check?enabled=true
Headers: Authorization: Bearer {JWT}
```

#### 7. 내 정보 수정
```
PUT /api/members/me
Headers: Authorization: Bearer {JWT}
Body: {
  "memberName": "수정된 이름",
  "memberPassword": "newPassword123"
}
```

#### 8. 내 계정 삭제
```
DELETE /api/members/me
Headers: Authorization: Bearer {JWT}
```

### 보호자 관계

#### 9. 내 보호자 관계 해제
```
DELETE /api/members/me/guardian
Headers: Authorization: Bearer {JWT}
```

## 🔧 핵심 서비스

### MemberService
- `save(MemberSaveRequest)`: 회원가입 (이메일 중복 검증 + 비밀번호 암호화)
- `update(MemberUpdateRequest)`: 회원 정보 수정
- `getMyInfo(email)`: 본인 정보 조회 (JWT 이메일 기반)

### CustomUserDetailsService (Spring Security 연동)
- `loadUserByUsername(email)`: 이메일 기반 회원 조회 및 UserDetails 반환

## 🔧 핵심 메서드

### MemberEntity
- `createMember(email, name, password, dailyCheckEnabled)`: 회원 생성 (정적 팩토리)
- `updateMemberInfo(name, password)`: 회원 정보 수정
- `updateDailyCheckEnabled(enabled)`: 안부 메시지 설정 변경
- `assignGuardian(guardian, relation)`: 보호자 설정
- `removeGuardian()`: 보호자 제거
- `hasGuardian()`: 보호자 존재 여부 확인
- `isGuardianRole()`: 보호자 역할 확인
- `getManagedMembersCount()`: 돌보는 사람 수 조회

### MemberService
- `save(MemberSaveRequest)`: 회원가입 (이메일 중복 검증 + 비밀번호 암호화)
- `isEmailAvailable(email)`: 이메일 중복 확인
- `searchByEmail(email)`: 이메일로 회원 검색
- `getMyProfile(memberId)`: 본인 정보 조회 (보호자 정보 포함)
- `getManagedMembers(memberId)`: 내가 돌보는 사람들 목록
- `updateDailyCheckEnabled(memberId, enabled)`: 안부 메시지 설정 변경
- `update(MemberUpdateRequest)`: 회원 정보 수정
- `deleteById(memberId)`: 회원 삭제
- `findById(memberId)`: ID로 회원 조회

## 🔗 도메인 연동

- **Auth**: Spring Security 인증 + JWT 토큰 발급
- **Guardian**: 보호자 요청 시스템 (GuardianRequest 엔티티)
- **DailyCheck**: 활성 회원 목록 조회 (`dailyCheckEnabled = true`)
- **Conversation**: 대화 메시지 저장

## 📁 패키지 구조

```
member/
├── application/
│   ├── dto/
│   │   ├── request/          # MemberSaveRequest, MemberUpdateRequest, DailyCheckUpdateRequest, MemberLoginRequest
│   │   └── response/         # MemberResponse, EmailCheckResponse
│   ├── service/              # MemberService
│   ├── mapper/               # MemberMapper
│   └── exception/            # MemberNotFoundException
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
- [x] 9개 REST API (회원가입 2개 + 회원 조회 3개 + 정보 관리 3개 + 보호자 1개)
- [x] 보호자 자기 참조 관계 관리
- [x] 안부 메시지 수신 설정
- [x] 회원 검색 기능

**상용 서비스 수준 완성**
