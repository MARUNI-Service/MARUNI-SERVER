# Guardian 도메인

**최종 업데이트**: 2025-10-09
**상태**: ✅ Phase 2 완성 (TDD 완전 사이클)

## 📋 개요

보호자 관리 및 Member 관계 설정 시스템입니다.

### 핵심 기능
- 보호자 CRUD
- Member-Guardian 관계 설정/해제
- 알림 설정 관리 (PUSH/EMAIL/SMS/ALL)
- 소프트 삭제 (isActive)

## 🏗️ 주요 엔티티

### GuardianEntity
```java
- id: Long
- guardianName: String        // 보호자 이름
- guardianEmail: String       // 보호자 이메일 (유니크)
- guardianPhone: String       // 보호자 전화번호
- relation: GuardianRelation  // 관계 (가족/친구/돌봄제공자 등)
- notificationPreference: NotificationPreference  // 알림 설정
- isActive: Boolean           // 활성 상태
- members: List<MemberEntity> // 담당 회원 (일대다)
```

### GuardianRelation (Enum)
- `FAMILY`: 가족
- `FRIEND`: 친구
- `CAREGIVER`: 돌봄제공자
- `NEIGHBOR`: 이웃
- `OTHER`: 기타

### NotificationPreference (Enum)
- `PUSH`: 푸시알림 (Firebase FCM)
- `EMAIL`: 이메일
- `SMS`: SMS (Phase 3)
- `ALL`: 모든 알림

## 🌐 REST API

### 1. 보호자 생성
```
POST /api/guardians
Body: {
  "guardianName": "김보호",
  "guardianEmail": "guardian@example.com",
  "guardianPhone": "010-1234-5678",
  "relation": "FAMILY",
  "notificationPreference": "ALL"
}
```

### 2. 보호자 조회
```
GET /api/guardians/{guardianId}
```

### 3. 보호자 정보 수정
```
PUT /api/guardians/{guardianId}
```

### 4. 보호자 비활성화
```
DELETE /api/guardians/{guardianId}
```

### 5. 회원에게 보호자 할당
```
POST /api/guardians/{guardianId}/members/{memberId}
```

### 6. 회원의 보호자 관계 해제
```
DELETE /api/guardians/members/{memberId}/guardian
```

### 7. 보호자가 담당하는 회원 목록 조회
```
GET /api/guardians/{guardianId}/members
```

## 🔧 핵심 서비스

### GuardianService
- `createGuardian()`: 보호자 생성 (이메일 중복 검증)
- `assignGuardianToMember()`: Member-Guardian 관계 설정
- `removeGuardianFromMember()`: Member-Guardian 관계 해제
- `deactivateGuardian()`: 보호자 비활성화 (연결된 모든 Member 해제)
- `getMembersByGuardian()`: 보호자가 담당하는 회원 목록

## 🔗 도메인 연동

- **Member**: MemberEntity.guardian 필드로 일대다 관계
- **AlertRule**: 보호자 알림 발송
- **Notification**: NotificationPreference 기반 알림 채널 선택

## 🧪 테스트 완성도

- ✅ **11개 테스트 시나리오**: Entity(4) + Repository(3) + Service(4)
- ✅ **TDD 완전 사이클**: Red → Green → Blue
- ✅ **100% 통과**: 모든 테스트 성공

## 📁 패키지 구조

```
guardian/
├── application/
│   ├── dto/                  # Request/Response DTO
│   ├── service/              # GuardianService
│   └── exception/            # GuardianNotFoundException
├── domain/
│   ├── entity/               # GuardianEntity, GuardianRelation, NotificationPreference
│   └── repository/           # GuardianRepository
└── presentation/
    └── controller/           # GuardianController (7개 API)
```

## ✅ 완성도

- [x] 보호자 CRUD
- [x] Member-Guardian 관계 관리
- [x] 4종 알림 채널 설정
- [x] 소프트 삭제
- [x] 7개 REST API
- [x] TDD 완전 적용

**상용 서비스 수준 완성**
