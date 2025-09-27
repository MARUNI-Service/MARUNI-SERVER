# Guardian 도메인 API 명세서

**보호자 관리 시스템 REST API 완전 가이드**

## 📋 개요

Guardian 도메인은 MARUNI 프로젝트의 핵심 보호자 관리 시스템으로, 보호자-회원 관계 설정, 알림 설정 관리, 보호자별 담당 회원 조회 기능을 제공합니다.

### 🎯 **핵심 기능**
- **보호자 CRUD**: 보호자 생성, 조회, 수정, 비활성화
- **관계 관리**: Guardian-Member 일대다 관계 설정/해제
- **알림 설정**: 4종 알림 채널 지원 (PUSH/EMAIL/SMS/ALL)
- **담당 회원 관리**: 보호자별 담당 회원 목록 조회

### 🔐 **기술 특징**
- **TDD 완전 구현**: Red-Green-Refactor 사이클 완전 적용
- **소프트 삭제**: 물리적 삭제 없이 isActive 플래그 관리
- **관계 무결성**: Guardian-Member 관계의 완전한 생명주기 관리
- **확장 가능한 알림**: NotificationPreference 기반 멀티채널 알림

---

## 🌐 API 엔드포인트 목록

| HTTP | 엔드포인트 | 인증 | 설명 |
|------|------------|------|------|
| `POST` | `/api/guardians` | ❌ | 보호자 생성 |
| `GET` | `/api/guardians/{guardianId}` | ❌ | 보호자 조회 |
| `PUT` | `/api/guardians/{guardianId}` | ❌ | 보호자 정보 수정 |
| `DELETE` | `/api/guardians/{guardianId}` | ❌ | 보호자 비활성화 |
| `POST` | `/api/guardians/{guardianId}/assign` | ✅ JWT | 현재 회원에게 보호자 할당 |
| `DELETE` | `/api/guardians/remove-guardian` | ✅ JWT | 현재 회원의 보호자 관계 해제 |
| `GET` | `/api/guardians/my-guardian` | ✅ JWT | 현재 회원의 보호자 조회 |
| `GET` | `/api/guardians/{guardianId}/members` | ❌ | 보호자가 담당하는 회원 목록 조회 |

---

## 👥 보호자 CRUD API

### 1. 보호자 생성

#### **POST** `/api/guardians`

새로운 보호자를 등록합니다.

**Request Body:**
```json
{
  "guardianName": "string",
  "guardianEmail": "string",
  "guardianPhone": "string",
  "relation": "GuardianRelation",
  "notificationPreference": "NotificationPreference"
}
```

**Request 예시:**
```json
{
  "guardianName": "김보호",
  "guardianEmail": "guardian@example.com",
  "guardianPhone": "010-1234-5678",
  "relation": "FAMILY",
  "notificationPreference": "ALL"
}
```

**Validation 규칙:**
- `guardianName`: 필수, 1자 이상
- `guardianEmail`: 필수, 이메일 형식, 중복 불가
- `guardianPhone`: 선택적
- `relation`: 필수, FAMILY/FRIEND/CAREGIVER/NEIGHBOR/OTHER
- `notificationPreference`: 필수, PUSH/EMAIL/SMS/ALL

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "guardianName": "김보호",
    "guardianEmail": "guardian@example.com",
    "guardianPhone": "010-1234-5678",
    "relation": "FAMILY",
    "notificationPreference": "ALL",
    "isActive": true,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T10:30:00"
  }
}
```

**Response 400 (중복 이메일):**
```json
{
  "success": false,
  "code": "DUPLICATE_GUARDIAN_EMAIL",
  "message": "이미 존재하는 보호자 이메일입니다",
  "data": null
}
```

**Response 400 (입력값 검증 실패):**
```json
{
  "success": false,
  "code": "INVALID_INPUT_VALUE",
  "message": "입력값이 올바르지 않습니다",
  "data": {
    "fieldErrors": [
      {
        "field": "guardianEmail",
        "message": "올바른 이메일 형식이어야 합니다"
      }
    ]
  }
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/guardians" \
  -H "Content-Type: application/json" \
  -d '{
    "guardianName": "김보호",
    "guardianEmail": "guardian@example.com",
    "guardianPhone": "010-1234-5678",
    "relation": "FAMILY",
    "notificationPreference": "ALL"
  }'
```

---

### 2. 보호자 조회

#### **GET** `/api/guardians/{guardianId}`

보호자 정보를 조회합니다.

**Path Parameters:**
- `guardianId` (Long): 보호자 ID

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "guardianName": "김보호",
    "guardianEmail": "guardian@example.com",
    "guardianPhone": "010-1234-5678",
    "relation": "FAMILY",
    "notificationPreference": "ALL",
    "isActive": true,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T10:30:00"
  }
}
```

**Response 404 (보호자 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "보호자를 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/guardians/1"
```

---

### 3. 보호자 정보 수정

#### **PUT** `/api/guardians/{guardianId}`

보호자의 이름과 전화번호를 수정합니다.

**Path Parameters:**
- `guardianId` (Long): 보호자 ID

**Request Body:**
```json
{
  "guardianName": "string",
  "guardianPhone": "string"
}
```

**Request 예시:**
```json
{
  "guardianName": "김보호 수정",
  "guardianPhone": "010-9876-5432"
}
```

**Validation 규칙:**
- `guardianName`: 필수, 1자 이상
- `guardianPhone`: 선택적

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "guardianName": "김보호 수정",
    "guardianEmail": "guardian@example.com",
    "guardianPhone": "010-9876-5432",
    "relation": "FAMILY",
    "notificationPreference": "ALL",
    "isActive": true,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T14:20:00"
  }
}
```

**Response 404 (보호자 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "보호자를 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X PUT "http://localhost:8080/api/guardians/1" \
  -H "Content-Type: application/json" \
  -d '{
    "guardianName": "김보호 수정",
    "guardianPhone": "010-9876-5432"
  }'
```

---

### 4. 보호자 비활성화 (소프트 삭제)

#### **DELETE** `/api/guardians/{guardianId}`

보호자를 비활성화하고 연결된 모든 회원과의 관계를 해제합니다.

**Path Parameters:**
- `guardianId` (Long): 보호자 ID

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": null
}
```

**비활성화 처리 과정:**
1. 보호자와 연결된 모든 회원의 guardian 관계 해제
2. 보호자의 `isActive` 플래그를 `false`로 설정
3. 물리적 삭제는 수행하지 않음 (데이터 보존)

**Response 404 (보호자 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "보호자를 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X DELETE "http://localhost:8080/api/guardians/1"
```

---

## 🔗 관계 관리 API

### 5. 현재 회원에게 보호자 할당

#### **POST** `/api/guardians/{guardianId}/assign`

인증된 현재 회원에게 보호자를 할당합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters:**
- `guardianId` (Long): 할당할 보호자 ID

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": null
}
```

**관계 할당 규칙:**
- 한 회원은 하나의 보호자만 가질 수 있음
- 기존 보호자가 있는 경우 새로운 보호자로 교체
- 보호자는 여러 회원을 담당할 수 있음

**Response 401 (인증 실패):**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

**Response 404 (보호자 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "보호자를 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/guardians/1/assign" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 6. 현재 회원의 보호자 관계 해제

#### **DELETE** `/api/guardians/remove-guardian`

인증된 현재 회원의 보호자 관계를 해제합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": null
}
```

**관계 해제 처리:**
- 회원의 guardian 필드를 null로 설정
- 보호자는 비활성화되지 않음 (다른 회원과의 관계 유지)

**Response 401 (인증 실패):**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

**Response 404 (보호자 관계 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_RELATION_NOT_FOUND",
  "message": "현재 회원에게 할당된 보호자가 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X DELETE "http://localhost:8080/api/guardians/remove-guardian" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 7. 현재 회원의 보호자 조회

#### **GET** `/api/guardians/my-guardian`

인증된 현재 회원의 보호자 정보를 조회합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "guardianName": "김보호",
    "guardianEmail": "guardian@example.com",
    "guardianPhone": "010-1234-5678",
    "relation": "FAMILY",
    "notificationPreference": "ALL",
    "isActive": true,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T10:30:00"
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

**Response 404 (보호자 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "현재 회원에게 할당된 보호자가 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/guardians/my-guardian" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 8. 보호자가 담당하는 회원 목록 조회

#### **GET** `/api/guardians/{guardianId}/members`

특정 보호자가 담당하는 모든 회원 목록을 조회합니다.

**Path Parameters:**
- `guardianId` (Long): 보호자 ID

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": [
    {
      "id": 1,
      "memberName": "김할머니",
      "memberEmail": "elder1@example.com",
      "createdAt": "2025-09-18T09:00:00",
      "updatedAt": "2025-09-18T09:00:00"
    },
    {
      "id": 2,
      "memberName": "이할아버지",
      "memberEmail": "elder2@example.com",
      "createdAt": "2025-09-18T09:30:00",
      "updatedAt": "2025-09-18T09:30:00"
    }
  ]
}
```

**Response 200 (담당 회원 없음):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": []
}
```

**Response 404 (보호자 없음):**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "보호자를 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/guardians/1/members"
```

---

## 📊 데이터 모델

### GuardianRequestDto (보호자 생성)
```json
{
  "guardianName": "string (필수)",
  "guardianEmail": "string (필수, 이메일 형식)",
  "guardianPhone": "string (선택)",
  "relation": "GuardianRelation (필수)",
  "notificationPreference": "NotificationPreference (필수)"
}
```

### GuardianUpdateRequestDto (보호자 수정)
```json
{
  "guardianName": "string (필수)",
  "guardianPhone": "string (선택)"
}
```

### GuardianResponseDto (보호자 응답)
```json
{
  "id": "number (보호자 ID)",
  "guardianName": "string (보호자 이름)",
  "guardianEmail": "string (보호자 이메일)",
  "guardianPhone": "string (보호자 전화번호)",
  "relation": "GuardianRelation (관계)",
  "notificationPreference": "NotificationPreference (알림 설정)",
  "isActive": "boolean (활성 상태)",
  "createdAt": "datetime (생성 시간)",
  "updatedAt": "datetime (수정 시간)"
}
```

### MemberResponse (회원 응답)
```json
{
  "id": "number (회원 ID)",
  "memberName": "string (회원 이름)",
  "memberEmail": "string (회원 이메일)",
  "createdAt": "datetime (생성 시간)",
  "updatedAt": "datetime (수정 시간)"
}
```

---

## 🔧 Enum 정의

### GuardianRelation (보호자 관계)
| 값 | 설명 |
|---|---|
| `FAMILY` | 가족 |
| `FRIEND` | 친구 |
| `CAREGIVER` | 돌봄제공자 |
| `NEIGHBOR` | 이웃 |
| `OTHER` | 기타 |

### NotificationPreference (알림 설정)
| 값 | 설명 |
|---|---|
| `PUSH` | 푸시 알림 (Firebase FCM) |
| `EMAIL` | 이메일 알림 |
| `SMS` | SMS 알림 (Phase 3) |
| `ALL` | 모든 알림 채널 |

---

## ❌ 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `GUARDIAN_NOT_FOUND` | 404 | 보호자를 찾을 수 없음 |
| `MEMBER_NOT_FOUND` | 404 | 회원을 찾을 수 없음 |
| `GUARDIAN_RELATION_NOT_FOUND` | 404 | 보호자 관계를 찾을 수 없음 |
| `DUPLICATE_GUARDIAN_EMAIL` | 400 | 중복된 보호자 이메일 |
| `INVALID_INPUT_VALUE` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 🧪 테스트 시나리오

### 정상 플로우 (완전한 보호자 관리 사이클)
```bash
# 1. 보호자 생성
POST /api/guardians
{
  "guardianName": "김보호",
  "guardianEmail": "guardian@example.com",
  "guardianPhone": "010-1234-5678",
  "relation": "FAMILY",
  "notificationPreference": "ALL"
}
# Response: guardianId=1

# 2. 회원 로그인 및 보호자 할당
POST /api/members/login
{
  "memberEmail": "elder@example.com",
  "memberPassword": "password123"
}
# Response: JWT 토큰 획득

POST /api/guardians/1/assign
Authorization: Bearer {JWT_TOKEN}
# Response: 관계 할당 성공

# 3. 회원의 보호자 조회
GET /api/guardians/my-guardian
Authorization: Bearer {JWT_TOKEN}
# Response: 할당된 보호자 정보

# 4. 보호자가 담당하는 회원 목록 조회
GET /api/guardians/1/members
# Response: 담당 회원 목록 (elder@example.com 포함)

# 5. 보호자 정보 수정
PUT /api/guardians/1
{
  "guardianName": "김보호 수정",
  "guardianPhone": "010-9876-5432"
}
# Response: 수정된 보호자 정보

# 6. 보호자 관계 해제
DELETE /api/guardians/remove-guardian
Authorization: Bearer {JWT_TOKEN}
# Response: 관계 해제 성공

# 7. 보호자 비활성화
DELETE /api/guardians/1
# Response: 보호자 비활성화 완료
```

### 에러 케이스
```bash
# 중복 이메일로 보호자 생성
POST /api/guardians
{
  "guardianName": "김보호2",
  "guardianEmail": "guardian@example.com",  # 이미 존재하는 이메일
  "relation": "FAMILY",
  "notificationPreference": "PUSH"
}
# Response: 400 DUPLICATE_GUARDIAN_EMAIL

# 존재하지 않는 보호자 조회
GET /api/guardians/999
# Response: 404 GUARDIAN_NOT_FOUND

# 인증 없이 보호자 할당 시도
POST /api/guardians/1/assign
# Response: 401 UNAUTHORIZED

# 보호자가 없는 회원의 보호자 조회
GET /api/guardians/my-guardian
Authorization: Bearer {JWT_TOKEN}
# Response: 404 GUARDIAN_NOT_FOUND

# 잘못된 입력값으로 보호자 생성
POST /api/guardians
{
  "guardianName": "",  # 빈 이름
  "guardianEmail": "invalid-email",  # 잘못된 이메일 형식
  "relation": null,  # null 관계
  "notificationPreference": null  # null 알림 설정
}
# Response: 400 INVALID_INPUT_VALUE
```

### 관계 관리 테스트
```bash
# 한 보호자가 여러 회원 담당
# 회원1이 보호자1에게 할당
POST /api/guardians/1/assign (회원1 JWT)

# 회원2도 보호자1에게 할당
POST /api/guardians/1/assign (회원2 JWT)

# 보호자1의 담당 회원 목록 조회
GET /api/guardians/1/members
# Response: [회원1, 회원2]

# 회원1이 보호자 변경 (보호자2로)
POST /api/guardians/2/assign (회원1 JWT)

# 보호자1의 담당 회원 목록 재조회
GET /api/guardians/1/members
# Response: [회원2] (회원1 제외됨)
```

---

## 🔗 도메인 연동

### AlertRule 연동
```bash
# 이상징후 감지 시 보호자 알림 발송
# AlertRule에서 NEGATIVE 감정 패턴 감지
# → Guardian 시스템을 통해 보호자에게 알림 전송
# → NotificationPreference에 따른 멀티채널 알림 발송
```

### Notification 연동
```bash
# 보호자 알림 설정에 따른 알림 발송
# PUSH: Firebase FCM 푸시 알림
# EMAIL: guardian@example.com으로 이메일 발송
# SMS: 010-1234-5678로 SMS 발송 (Phase 3)
# ALL: 모든 채널을 통한 알림 발송
```

### Conversation 연동
```bash
# AI 대화에서 긴급 키워드 감지 시
# "아파요", "도와주세요" 등의 키워드 감지
# → Guardian 시스템을 통해 즉시 보호자 알림
```

### DailyCheck 연동
```bash
# 안부 메시지 무응답 시
# 24시간 이상 응답 없음 감지
# → Guardian 시스템을 통해 보호자 알림 발송
```

---

## 📋 관련 문서

### 🔗 **연관 API**
- **[Member API](./member-api.md)**: 회원 관리 및 Guardian 관계 설정
- **[Auth API](./auth-api.md)**: JWT 인증 (관계 관리 API용)
- **[AlertRule API](./alertrule-api.md)**: 이상징후 감지 시 보호자 알림
- **[Notification API](./notification-api.md)**: 멀티채널 알림 발송

### 🛠️ **기술 문서**
- **[Guardian 도메인 가이드](../domains/guardian.md)**: TDD 구현 및 관계 관리 상세
- **[API 설계 가이드](../specifications/api-design-guide.md)**: REST API 설계 원칙
- **[데이터베이스 설계 가이드](../specifications/database-design-guide.md)**: Guardian-Member 관계 설계

---

## 💡 개발자 가이드

### Guardian-Member 관계 관리
```java
// 보호자 할당 (일대다 관계)
MemberEntity member = findMemberById(memberId);
GuardianEntity guardian = findGuardianById(guardianId);
member.assignGuardian(guardian);  // Member 엔티티에서 관계 관리

// 관계 해제
member.removeGuardian();  // 관계만 해제, 보호자는 유지
```

### 알림 설정 활용
```java
// NotificationPreference에 따른 알림 발송
switch (guardian.getNotificationPreference()) {
    case PUSH -> sendPushNotification(guardian.getId(), message);
    case EMAIL -> sendEmail(guardian.getGuardianEmail(), message);
    case SMS -> sendSms(guardian.getGuardianPhone(), message);
    case ALL -> sendMultiChannelAlert(guardian, message);
}
```

### 소프트 삭제 처리
```java
// 보호자 비활성화 (소프트 삭제)
public void deactivateGuardian(Long guardianId) {
    GuardianEntity guardian = findGuardianById(guardianId);

    // 1. 연결된 모든 회원의 관계 해제
    List<MemberEntity> members = memberRepository.findByGuardian(guardian);
    members.forEach(MemberEntity::removeGuardian);

    // 2. 보호자 비활성화 (물리적 삭제 안함)
    guardian.deactivate();  // isActive = false
    guardianRepository.save(guardian);
}
```

### 관계 무결성 보장
```java
// 보호자 할당 시 기존 관계 자동 해제
public void assignGuardianToMember(Long memberId, Long guardianId) {
    MemberEntity member = findMemberById(memberId);
    GuardianEntity newGuardian = findGuardianById(guardianId);

    // 기존 보호자가 있으면 자동으로 관계 해제됨
    member.assignGuardian(newGuardian);
    memberRepository.save(member);
}
```

---

## 🚀 확장 가능성

### 관계 복잡화
- **다대다 관계**: 한 회원이 여러 보호자를 가질 수 있도록 확장
- **계층적 관계**: 주 보호자, 보조 보호자 등 역할 구분
- **임시 보호자**: 기간 한정 보호자 관계 설정

### 알림 채널 확장
- **Slack 연동**: 팀 워크스페이스를 통한 알림
- **KakaoTalk**: 카카오톡 비즈니스 API 연동
- **LINE**: LINE Notify API 연동
- **Discord**: Discord Webhook 연동

### 권한 관리
- **권한 레벨**: 조회 전용, 부분 관리, 전체 관리 권한 구분
- **기능별 권한**: 대화 조회, 알림 설정, 응급 연락 등 기능별 권한
- **시간 제한**: 특정 시간대에만 접근 가능한 권한

### 분석 기능
- **보호자 활동 분석**: 알림 응답률, 관리 패턴 분석
- **회원-보호자 매칭**: AI 기반 최적 보호자 추천
- **관계 품질 측정**: 상호작용 빈도 기반 관계 품질 평가

---

**Guardian API는 MARUNI 플랫폼의 핵심인 보호자 알림 시스템의 기반이 되는 완성된 관리 시스템입니다. TDD 방법론을 완벽히 적용하여 신뢰성 높은 Guardian-Member 관계 관리와 멀티채널 알림 기능을 제공합니다.** 👥