# Guardian 도메인

**최종 업데이트**: 2025-11-07
**상태**: ✅ Phase 2 완성 (보호자 요청 시스템 + Member 자기 참조)

## 📋 개요

보호자 요청 및 관계 관리 시스템입니다. Member 자기 참조 방식으로 보호자 관계를 구현합니다.

### 핵심 기능
- 보호자 요청 생성 (노인 → 보호자)
- 보호자 요청 수락/거절
- Member 간 보호자 관계 설정
- 요청 상태 관리 (PENDING, ACCEPTED, REJECTED)

## 🏗️ 주요 엔티티

### GuardianRequest
- id: Long
- requester: MemberEntity (요청한 사람, 노인)
- guardian: MemberEntity (요청받은 사람, 보호자)
- relation: GuardianRelation (관계 타입)
- status: RequestStatus (PENDING, ACCEPTED, REJECTED)
- createdAt/updatedAt: LocalDateTime

**유니크 제약**: (requester_id, guardian_id) 중복 요청 방지

### GuardianRelation (Enum)
- `FAMILY`: 가족
- `FRIEND`: 친구
- `CAREGIVER`: 돌봄제공자
- `NEIGHBOR`: 이웃
- `OTHER`: 기타

### RequestStatus (Enum)
- `PENDING`: 대기 중
- `ACCEPTED`: 수락됨
- `REJECTED`: 거절됨

## 🌐 REST API (4개)

### 1. 보호자 요청 생성
```
POST /api/guardians/requests
Headers: Authorization: Bearer {JWT} (요청자 토큰)
Body: {
  "guardianId": 2,
  "relation": "FAMILY"
}

Response: {
  "id": 1,
  "requesterId": 1,
  "requesterName": "김순자",
  "guardianId": 2,
  "guardianName": "김영희",
  "relation": "FAMILY",
  "status": "PENDING",
  "createdAt": "2025-11-07T10:00:00"
}

Note:
- 이미 보호자가 있으면 400 에러
- 중복 요청 시 409 에러
- 본인에게 요청 시 400 에러
```

### 2. 내가 받은 보호자 요청 목록 조회
```
GET /api/guardians/requests
Headers: Authorization: Bearer {JWT} (보호자 토큰)

Response: [
  {
    "id": 1,
    "requesterId": 1,
    "requesterName": "김순자",
    "guardianId": 2,
    "guardianName": "김영희",
    "relation": "FAMILY",
    "status": "PENDING",
    "createdAt": "2025-11-07T10:00:00"
  }
]

Note: PENDING 상태의 요청만 반환
```

### 3. 보호자 요청 수락
```
POST /api/guardians/requests/{requestId}/accept
Headers: Authorization: Bearer {JWT} (보호자 토큰)

Response: 200 OK

Note:
- 요청 상태가 ACCEPTED로 변경
- MemberEntity.guardian 필드에 보호자 설정
- MemberEntity.guardianRelation 필드에 관계 저장
- Notification 이력 저장
```

### 4. 보호자 요청 거절
```
POST /api/guardians/requests/{requestId}/reject
Headers: Authorization: Bearer {JWT} (보호자 토큰)

Response: 200 OK

Note: 요청 상태가 REJECTED로 변경
```

## 🔧 핵심 메서드

### GuardianRequest
- `createRequest(requester, guardian, relation)`: 보호자 요청 생성 (정적 팩토리)
- `accept()`: 요청 수락 (PENDING → ACCEPTED)
- `reject()`: 요청 거절 (PENDING → REJECTED)

### GuardianRelationService
- `sendRequest(requesterId, guardianId, relation)`: 보호자 요청 생성
- `getReceivedRequests(guardianId)`: 받은 요청 목록 조회
- `acceptRequest(requestId, guardianId)`: 요청 수락 후 Member 관계 설정
- `rejectRequest(requestId, guardianId)`: 요청 거절
- `removeGuardian(memberId)`: 보호자 관계 해제

### Member 연동 (MemberEntity)
- `assignGuardian(guardian, relation)`: 보호자 설정
- `removeGuardian()`: 보호자 제거
- `hasGuardian()`: 보호자 존재 여부
- `getManagedMembers()`: 내가 돌보는 사람들

## 🔗 도메인 연동

- **Member**: MemberEntity.guardian 자기 참조 (보호자 관계)
- **Member**: MemberEntity.managedMembers 자기 참조 (돌보는 사람들)
- **AlertRule**: 보호자에게 이상징후 알림 발송
- **Notification**: 보호자 요청/수락/거절 알림 이력 저장

## 📁 패키지 구조

```
guardian/
├── application/
│   ├── dto/                  # GuardianRequestDto, GuardianRequestResponse
│   └── service/              # GuardianRelationService
├── domain/
│   ├── entity/               # GuardianRequest, GuardianRelation (Enum), RequestStatus (Enum)
│   └── repository/           # GuardianRequestRepository
└── presentation/
    └── controller/           # GuardianRelationController (4개 API)
```

## ✅ 완성도

- [x] 보호자 요청 시스템 (요청/수락/거절)
- [x] Member 자기 참조 보호자 관계
- [x] 요청 상태 관리 (PENDING, ACCEPTED, REJECTED)
- [x] 중복 요청 방지 (유니크 제약)
- [x] REST API (4개: 요청 생성, 받은 요청 조회, 수락, 거절)
- [x] Notification 연동 (요청/수락/거절 알림)
- [x] TDD 테스트

**상용 서비스 수준 완성**
