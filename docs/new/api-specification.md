# API 명세서

**MARUNI 프로젝트 REST API 완전 명세**

---

**버전**: 1.0.0
**상태**: 설계
**기반 문서**: user-journey.md
**API 설계 원칙**: docs/specifications/api-design-guide.md

---

## 📋 목차

1. [인증 API](#1-인증-api)
2. [회원 관리 API](#2-회원-관리-api)
3. [보호자 관계 API](#3-보호자-관계-api)
4. [안부 메시지 API](#4-안부-메시지-api)
5. [대화 API](#5-대화-api)
6. [알림 설정 API](#6-알림-설정-api)
7. [이상징후 API](#7-이상징후-api)

---

## 공통 사항

### Base URL
```
http://localhost:8080/api
```

### 인증 방식
- **JWT Bearer Token** (Access Token Only)
- Header: `Authorization: Bearer {access_token}`
- 유효 시간: 1시간
- 로그아웃: 클라이언트에서 토큰 삭제

### 공통 응답 구조
```json
{
  "code": "SUCCESS_CODE",
  "message": "성공 메시지",
  "data": { }
}
```

### 공통 에러 응답
```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지",
  "data": null
}
```

### 주요 에러 코드
| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `INVALID_INPUT_VALUE` | 400 | 잘못된 입력 값 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류 |

---

## 1. 인증 API

### 1.1 회원가입

**Journey 1 - Phase 1: 최초 회원가입**

```
POST /auth/signup
```

**Request Body**
```json
{
  "memberEmail": "grandma@example.com",
  "memberPassword": "password123!",
  "memberName": "김순자",
  "dailyCheckEnabled": true
}
```

**Request 필드 설명**
| 필드 | 타입 | 필수 | 설명 | 검증 규칙 |
|------|------|------|------|-----------|
| `memberEmail` | String | ✅ | 이메일 주소 | 이메일 형식, 중복 불가 |
| `memberPassword` | String | ✅ | 비밀번호 | 8자 이상, 영문+숫자+특수문자 |
| `memberName` | String | ✅ | 이름 | 2-50자 |
| `dailyCheckEnabled` | Boolean | ✅ | 안부 메시지 수신 여부 | true/false |

**Response 200 OK**
```json
{
  "code": "MEMBER_CREATED",
  "message": "회원가입이 완료되었습니다",
  "data": {
    "memberId": 1,
    "memberEmail": "grandma@example.com",
    "memberName": "김순자",
    "dailyCheckEnabled": true,
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

**Error Responses**
- `400 INVALID_INPUT_VALUE`: 입력 값 검증 실패
- `409 EMAIL_DUPLICATED`: 이메일 중복

---

### 1.2 로그인

**Journey 1 - Phase 1: 로그인**

```
POST /auth/login
```

**Request Body**
```json
{
  "memberEmail": "grandma@example.com",
  "memberPassword": "password123!"
}
```

**Response 200 OK**
```json
{
  "code": "LOGIN_SUCCESS",
  "message": "로그인에 성공했습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "member": {
      "memberId": 1,
      "memberEmail": "grandma@example.com",
      "memberName": "김순자",
      "dailyCheckEnabled": true,
      "hasGuardian": false,
      "managedMembersCount": 0
    }
  }
}
```

**Response 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | String | JWT 액세스 토큰 (1시간 유효) |
| `tokenType` | String | 토큰 타입 (항상 "Bearer") |
| `expiresIn` | Integer | 토큰 만료 시간 (초 단위) |
| `member.hasGuardian` | Boolean | 보호자가 등록되어 있는지 여부 |
| `member.managedMembersCount` | Integer | 내가 돌보는 사람 수 |

**Error Responses**
- `401 LOGIN_FAIL`: 이메일 또는 비밀번호 불일치

---

## 2. 회원 관리 API

### 2.1 내 프로필 조회

**Journey 1 - Phase 2: 메인 화면**

```
GET /members/me
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "프로필 조회 성공",
  "data": {
    "memberId": 1,
    "memberEmail": "grandma@example.com",
    "memberName": "김순자",
    "dailyCheckEnabled": true,
    "pushToken": "fcm_token_xyz...",
    "guardian": {
      "memberId": 2,
      "memberName": "김영희",
      "memberEmail": "guardian@example.com",
      "relation": "FAMILY"
    },
    "managedMembers": [],
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

**Response 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `guardian` | Object\|null | 내 보호자 정보 (없으면 null) |
| `guardian.relation` | String | 관계 (FAMILY, FRIEND, CAREGIVER 등) |
| `managedMembers` | Array | 내가 돌보는 사람들 목록 |

---

### 2.2 프로필 수정

```
PATCH /members/me
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "memberName": "김순자",
  "dailyCheckEnabled": false
}
```

**Response 200 OK**
```json
{
  "code": "MEMBER_UPDATED",
  "message": "프로필이 수정되었습니다",
  "data": {
    "memberId": 1,
    "memberName": "김순자",
    "dailyCheckEnabled": false,
    "updatedAt": "2025-01-16T14:30:00"
  }
}
```

---

### 2.3 푸시 토큰 등록

**Journey 1 - Phase 1: FCM 토큰 등록**

```
POST /members/me/push-token
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "pushToken": "fcm_device_token_xyz123..."
}
```

**Response 200 OK**
```json
{
  "code": "PUSH_TOKEN_REGISTERED",
  "message": "푸시 알림이 활성화되었습니다",
  "data": {
    "pushToken": "fcm_device_token_xyz123...",
    "registeredAt": "2025-01-15T10:05:00"
  }
}
```

---

### 2.4 푸시 토큰 삭제

```
DELETE /members/me/push-token
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "PUSH_TOKEN_REMOVED",
  "message": "푸시 알림이 비활성화되었습니다",
  "data": null
}
```

---

## 3. 보호자 관계 API

### 3.1 회원 검색 (이메일)

**Journey 2 - Phase 1: 보호자 등록 - 이메일 검색**

```
GET /members/search?email={email}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `email` | String | ✅ | 검색할 이메일 주소 |

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "회원 검색 성공",
  "data": {
    "memberId": 2,
    "memberName": "김영희",
    "memberEmail": "guardian@example.com",
    "createdAt": "2025-01-10T09:00:00"
  }
}
```

**Error Responses**
- `404 MEMBER_NOT_FOUND`: 해당 이메일의 회원이 없음

---

### 3.2 보호자 등록 요청

**Journey 2 - Phase 2: 보호자 연결 요청**

```
POST /members/me/guardian/request
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "guardianMemberId": 2,
  "relation": "FAMILY"
}
```

**Request 필드 설명**
| 필드 | 타입 | 필수 | 설명 | 허용 값 |
|------|------|------|------|---------|
| `guardianMemberId` | Long | ✅ | 보호자로 등록할 회원 ID | - |
| `relation` | String | ✅ | 관계 | FAMILY, FRIEND, CAREGIVER, MEDICAL_STAFF, OTHER |

**Response 200 OK**
```json
{
  "code": "GUARDIAN_REQUEST_SENT",
  "message": "보호자 등록 요청이 전송되었습니다",
  "data": {
    "requestId": 101,
    "requestedTo": {
      "memberId": 2,
      "memberName": "김영희",
      "memberEmail": "guardian@example.com"
    },
    "relation": "FAMILY",
    "status": "PENDING",
    "requestedAt": "2025-01-16T15:00:00"
  }
}
```

**Error Responses**
- `400 ALREADY_HAS_GUARDIAN`: 이미 보호자가 등록되어 있음
- `400 CANNOT_BE_SELF_GUARDIAN`: 자기 자신을 보호자로 등록할 수 없음
- `404 MEMBER_NOT_FOUND`: 요청 대상 회원을 찾을 수 없음

---

### 3.3 받은 보호자 요청 목록 조회

**Journey 2 - Phase 3: 보호자 요청 수락**

```
GET /members/me/guardian/requests/received
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "받은 요청 목록 조회 성공",
  "data": {
    "requests": [
      {
        "requestId": 101,
        "requester": {
          "memberId": 1,
          "memberName": "김순자",
          "memberEmail": "grandma@example.com"
        },
        "relation": "FAMILY",
        "status": "PENDING",
        "requestedAt": "2025-01-16T15:00:00"
      }
    ]
  }
}
```

---

### 3.4 보호자 요청 수락

**Journey 2 - Phase 3: 보호자 요청 수락**

```
POST /members/me/guardian/requests/{requestId}/accept
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `requestId` | Long | ✅ | 보호자 요청 ID |

**Response 200 OK**
```json
{
  "code": "GUARDIAN_REQUEST_ACCEPTED",
  "message": "보호자 등록이 완료되었습니다",
  "data": {
    "requestId": 101,
    "elderlyCareRecipient": {
      "memberId": 1,
      "memberName": "김순자",
      "memberEmail": "grandma@example.com",
      "dailyCheckEnabled": true
    },
    "guardian": {
      "memberId": 2,
      "memberName": "김영희",
      "memberEmail": "guardian@example.com"
    },
    "relation": "FAMILY",
    "acceptedAt": "2025-01-16T15:30:00"
  }
}
```

**Error Responses**
- `404 REQUEST_NOT_FOUND`: 요청을 찾을 수 없음
- `403 NOT_REQUEST_RECIPIENT`: 요청 수신자가 아님
- `400 REQUEST_ALREADY_PROCESSED`: 이미 처리된 요청

---

### 3.5 보호자 요청 거절

```
POST /members/me/guardian/requests/{requestId}/reject
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "GUARDIAN_REQUEST_REJECTED",
  "message": "보호자 요청이 거절되었습니다",
  "data": {
    "requestId": 101,
    "rejectedAt": "2025-01-16T15:30:00"
  }
}
```

---

### 3.6 보호자 관계 해제

**Journey 2 - Phase 4: 보호자 관계 해제**

```
DELETE /members/me/guardian
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "GUARDIAN_REMOVED",
  "message": "보호자 관계가 해제되었습니다",
  "data": {
    "removedGuardian": {
      "memberId": 2,
      "memberName": "김영희"
    },
    "removedAt": "2025-01-20T10:00:00"
  }
}
```

**Error Responses**
- `404 GUARDIAN_NOT_FOUND`: 등록된 보호자가 없음

---

### 3.7 내가 돌보는 사람 목록 조회

**Journey 3 - 보호자가 여러 노인 돌봄**

```
GET /members/me/managed-members
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "돌봄 대상 목록 조회 성공",
  "data": {
    "managedMembers": [
      {
        "memberId": 1,
        "memberName": "김순자",
        "memberEmail": "grandma@example.com",
        "relation": "FAMILY",
        "dailyCheckEnabled": true,
        "lastDailyCheckAt": "2025-01-16T09:00:00",
        "lastConversationAt": "2025-01-16T14:30:00",
        "registeredAt": "2025-01-16T15:30:00"
      },
      {
        "memberId": 3,
        "memberName": "이철수",
        "memberEmail": "grandpa@example.com",
        "relation": "FRIEND",
        "dailyCheckEnabled": true,
        "lastDailyCheckAt": "2025-01-16T09:00:00",
        "lastConversationAt": null,
        "registeredAt": "2025-01-10T10:00:00"
      }
    ],
    "totalCount": 2
  }
}
```

---

### 3.8 돌봄 대상 상세 조회

```
GET /members/{memberId}/care-details
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `memberId` | Long | ✅ | 조회할 회원 ID |

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "돌봄 대상 상세 조회 성공",
  "data": {
    "member": {
      "memberId": 1,
      "memberName": "김순자",
      "memberEmail": "grandma@example.com",
      "dailyCheckEnabled": true
    },
    "relation": "FAMILY",
    "recentDailyChecks": [
      {
        "date": "2025-01-16",
        "responded": true,
        "respondedAt": "2025-01-16T09:15:00",
        "responseMessage": "좋아요"
      },
      {
        "date": "2025-01-15",
        "responded": true,
        "respondedAt": "2025-01-15T09:30:00",
        "responseMessage": "네, 잘 지냈어요"
      }
    ],
    "recentAlerts": [
      {
        "alertId": 501,
        "alertType": "EMOTION_PATTERN",
        "alertLevel": "MEDIUM",
        "message": "3일 연속 부정적 감정 패턴 감지",
        "detectedAt": "2025-01-16T20:00:00"
      }
    ],
    "conversationSummary": {
      "totalMessages": 45,
      "lastConversationAt": "2025-01-16T14:30:00",
      "recentEmotionTrend": "NEGATIVE"
    }
  }
}
```

**Error Responses**
- `404 MEMBER_NOT_FOUND`: 회원을 찾을 수 없음
- `403 NOT_GUARDIAN`: 해당 회원의 보호자가 아님

---

## 4. 안부 메시지 API

### 4.1 안부 메시지 스케줄 설정 조회

**Journey 1 - Phase 3: 안부 메시지 설정**

```
GET /daily-checks/schedule
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "스케줄 조회 성공",
  "data": {
    "enabled": true,
    "scheduledTime": "09:00",
    "timezone": "Asia/Seoul",
    "cronExpression": "0 0 9 * * *"
  }
}
```

---

### 4.2 안부 메시지 발송 (스케줄러 자동 실행)

**Journey 1 - Phase 4: 안부 메시지 수신**

```
POST /daily-checks/send (Internal API - 스케줄러 전용)
```

**스케줄러가 자동으로 호출하는 내부 API**
- 매일 오전 9시 Cron 스케줄러가 실행
- dailyCheckEnabled=true인 모든 회원에게 메시지 전송
- 중복 방지: DB 제약 조건 (member_id, check_date)

---

### 4.3 내 안부 메시지 목록 조회

**Journey 1 - Phase 4: 안부 메시지 수신**

```
GET /daily-checks/me?page=0&size=10
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | ❌ | 10 | 페이지 크기 |

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "안부 메시지 목록 조회 성공",
  "data": {
    "content": [
      {
        "dailyCheckId": 1001,
        "checkDate": "2025-01-16",
        "message": "오늘도 좋은 하루 보내세요!",
        "responded": true,
        "respondedAt": "2025-01-16T09:15:00",
        "responseMessage": "좋아요",
        "createdAt": "2025-01-16T09:00:00"
      },
      {
        "dailyCheckId": 1000,
        "checkDate": "2025-01-15",
        "message": "안녕하세요, 오늘 기분은 어떠세요?",
        "responded": false,
        "respondedAt": null,
        "responseMessage": null,
        "createdAt": "2025-01-15T09:00:00"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 10,
      "totalElements": 15,
      "totalPages": 2
    }
  }
}
```

---

### 4.4 안부 메시지 응답

**Journey 1 - Phase 5: 안부 메시지 응답**

```
POST /daily-checks/{dailyCheckId}/respond
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `dailyCheckId` | Long | ✅ | 안부 메시지 ID |

**Request Body**
```json
{
  "responseMessage": "네, 오늘도 잘 지내고 있어요"
}
```

**Response 200 OK**
```json
{
  "code": "DAILY_CHECK_RESPONDED",
  "message": "안부 응답이 등록되었습니다",
  "data": {
    "dailyCheckId": 1001,
    "responseMessage": "네, 오늘도 잘 지내고 있어요",
    "respondedAt": "2025-01-16T09:15:00"
  }
}
```

**Error Responses**
- `404 DAILY_CHECK_NOT_FOUND`: 안부 메시지를 찾을 수 없음
- `403 NOT_MESSAGE_RECIPIENT`: 본인의 안부 메시지가 아님
- `400 ALREADY_RESPONDED`: 이미 응답한 메시지

---

## 5. 대화 API

### 5.1 대화 세션 시작

**Journey 1 - Phase 6: AI 대화**

```
POST /conversations
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body** (Optional)
```json
{
  "initialMessage": "오늘 기분이 안 좋아요"
}
```

**Response 200 OK**
```json
{
  "code": "CONVERSATION_CREATED",
  "message": "대화 세션이 시작되었습니다",
  "data": {
    "conversationId": 5001,
    "memberId": 1,
    "createdAt": "2025-01-16T14:00:00",
    "firstMessage": {
      "messageId": 10001,
      "messageType": "USER",
      "content": "오늘 기분이 안 좋아요",
      "emotionType": "NEGATIVE",
      "createdAt": "2025-01-16T14:00:00"
    },
    "aiResponse": {
      "messageId": 10002,
      "messageType": "AI",
      "content": "무슨 일이 있으셨나요? 편하게 말씀해 주세요.",
      "emotionType": null,
      "createdAt": "2025-01-16T14:00:05"
    }
  }
}
```

---

### 5.2 메시지 전송

**Journey 1 - Phase 6: AI 대화 계속**

```
POST /conversations/{conversationId}/messages
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `conversationId` | Long | ✅ | 대화 세션 ID |

**Request Body**
```json
{
  "content": "아들이 전화를 안 받아요"
}
```

**Response 200 OK**
```json
{
  "code": "MESSAGE_SENT",
  "message": "메시지가 전송되었습니다",
  "data": {
    "userMessage": {
      "messageId": 10003,
      "messageType": "USER",
      "content": "아들이 전화를 안 받아요",
      "emotionType": "NEGATIVE",
      "createdAt": "2025-01-16T14:02:00"
    },
    "aiResponse": {
      "messageId": 10004,
      "messageType": "AI",
      "content": "마음이 많이 아프시겠어요. 아드님께서 바쁘신 걸 수도 있으니 조금 기다려보시는 건 어떨까요?",
      "emotionType": null,
      "createdAt": "2025-01-16T14:02:05"
    }
  }
}
```

**Error Responses**
- `404 CONVERSATION_NOT_FOUND`: 대화 세션을 찾을 수 없음
- `403 NOT_CONVERSATION_OWNER`: 본인의 대화가 아님

---

### 5.3 대화 목록 조회

```
GET /conversations?page=0&size=10
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "대화 목록 조회 성공",
  "data": {
    "content": [
      {
        "conversationId": 5001,
        "lastMessage": "조금 기다려보시는 건 어떨까요?",
        "lastMessageAt": "2025-01-16T14:02:05",
        "messageCount": 6,
        "createdAt": "2025-01-16T14:00:00"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 10,
      "totalElements": 12,
      "totalPages": 2
    }
  }
}
```

---

### 5.4 대화 상세 조회

```
GET /conversations/{conversationId}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "대화 상세 조회 성공",
  "data": {
    "conversationId": 5001,
    "memberId": 1,
    "messages": [
      {
        "messageId": 10001,
        "messageType": "USER",
        "content": "오늘 기분이 안 좋아요",
        "emotionType": "NEGATIVE",
        "createdAt": "2025-01-16T14:00:00"
      },
      {
        "messageId": 10002,
        "messageType": "AI",
        "content": "무슨 일이 있으셨나요?",
        "emotionType": null,
        "createdAt": "2025-01-16T14:00:05"
      }
    ],
    "createdAt": "2025-01-16T14:00:00"
  }
}
```

---

## 6. 알림 설정 API

### 6.1 알림 설정 조회

**Journey 1 - Phase 7: 설정**

```
GET /members/me/notification-settings
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "알림 설정 조회 성공",
  "data": {
    "pushEnabled": true,
    "dailyCheckEnabled": true,
    "alertEnabled": true,
    "conversationEnabled": true
  }
}
```

---

### 6.2 알림 설정 변경

```
PATCH /members/me/notification-settings
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "pushEnabled": true,
  "dailyCheckEnabled": false,
  "alertEnabled": true,
  "conversationEnabled": true
}
```

**Response 200 OK**
```json
{
  "code": "NOTIFICATION_SETTINGS_UPDATED",
  "message": "알림 설정이 변경되었습니다",
  "data": {
    "pushEnabled": true,
    "dailyCheckEnabled": false,
    "alertEnabled": true,
    "conversationEnabled": true,
    "updatedAt": "2025-01-16T16:00:00"
  }
}
```

---

## 7. 이상징후 API

### 7.1 이상징후 알림 목록 조회

**Journey 3 - 보호자 알림 수신**

```
GET /alerts?page=0&size=10
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | ❌ | 0 | 페이지 번호 |
| `size` | Integer | ❌ | 10 | 페이지 크기 |
| `memberId` | Long | ❌ | null | 특정 회원의 알림만 필터링 |
| `alertType` | String | ❌ | null | 알림 타입 필터 (EMOTION_PATTERN, NO_RESPONSE, KEYWORD) |

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "이상징후 알림 목록 조회 성공",
  "data": {
    "content": [
      {
        "alertId": 501,
        "member": {
          "memberId": 1,
          "memberName": "김순자"
        },
        "alertType": "EMOTION_PATTERN",
        "alertLevel": "MEDIUM",
        "message": "3일 연속 부정적 감정 패턴 감지",
        "detectedAt": "2025-01-16T20:00:00",
        "isRead": false,
        "details": {
          "consecutiveDays": 3,
          "emotionTrend": "NEGATIVE",
          "recentMessages": [
            "기분이 안 좋아요",
            "외로워요",
            "아무도 안 찾아와요"
          ]
        }
      },
      {
        "alertId": 502,
        "member": {
          "memberId": 3,
          "memberName": "이철수"
        },
        "alertType": "NO_RESPONSE",
        "alertLevel": "HIGH",
        "message": "3일 연속 안부 메시지 무응답",
        "detectedAt": "2025-01-16T20:30:00",
        "isRead": false,
        "details": {
          "consecutiveDays": 3,
          "lastResponseDate": "2025-01-13"
        }
      }
    ],
    "pageable": {
      "page": 0,
      "size": 10,
      "totalElements": 8,
      "totalPages": 1
    }
  }
}
```

---

### 7.2 이상징후 알림 읽음 처리

```
POST /alerts/{alertId}/read
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "ALERT_MARKED_AS_READ",
  "message": "알림을 읽음 처리했습니다",
  "data": {
    "alertId": 501,
    "isRead": true,
    "readAt": "2025-01-16T21:00:00"
  }
}
```

---

### 7.3 이상징후 감지 규칙 조회

```
GET /alert-rules
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Response 200 OK**
```json
{
  "code": "SUCCESS",
  "message": "감지 규칙 조회 성공",
  "data": {
    "rules": [
      {
        "ruleId": 1,
        "alertType": "EMOTION_PATTERN",
        "name": "부정적 감정 패턴 감지",
        "description": "연속된 날 동안 부정적 감정이 감지되면 알림",
        "enabled": true,
        "threshold": {
          "consecutiveDays": 3,
          "emotionType": "NEGATIVE"
        }
      },
      {
        "ruleId": 2,
        "alertType": "NO_RESPONSE",
        "name": "안부 무응답 감지",
        "description": "연속된 날 동안 안부 메시지에 응답하지 않으면 알림",
        "enabled": true,
        "threshold": {
          "consecutiveDays": 3
        }
      },
      {
        "ruleId": 3,
        "alertType": "KEYWORD",
        "name": "긴급 키워드 감지",
        "description": "특정 긴급 키워드 감지 시 즉시 알림",
        "enabled": true,
        "keywords": ["죽고 싶", "힘들", "외로", "아프", "도와줘"]
      }
    ]
  }
}
```

---

### 7.4 이상징후 감지 규칙 수정

```
PATCH /alert-rules/{ruleId}
```

**Headers**
```
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "enabled": false,
  "threshold": {
    "consecutiveDays": 5
  }
}
```

**Response 200 OK**
```json
{
  "code": "ALERT_RULE_UPDATED",
  "message": "감지 규칙이 수정되었습니다",
  "data": {
    "ruleId": 1,
    "enabled": false,
    "threshold": {
      "consecutiveDays": 5
    },
    "updatedAt": "2025-01-16T22:00:00"
  }
}
```

---

## 부록: 관계형 다이어그램

### Guardian 관계 (Relation) 타입
```
FAMILY          가족
FRIEND          지인
CAREGIVER       간병인
MEDICAL_STAFF   의료진
OTHER           기타
```

### 감정 타입 (EmotionType)
```
POSITIVE        긍정
NEUTRAL         중립
NEGATIVE        부정
```

### 알림 타입 (AlertType)
```
EMOTION_PATTERN  감정 패턴 이상
NO_RESPONSE      안부 무응답
KEYWORD          긴급 키워드 감지
```

### 알림 레벨 (AlertLevel)
```
LOW             낮음
MEDIUM          중간
HIGH            높음
EMERGENCY       긴급
```

---

**Version**: 1.0.0
**Updated**: 2025-10-07
**Status**: 설계 (구현 대기)
