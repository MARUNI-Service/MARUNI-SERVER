# MARUNI API 완전 테스트 가이드 (Postman)

**흐름별 단계적 API 테스트 시나리오 및 요청 형식**

---

## 📋 목차

1. [테스트 환경 설정](#-테스트-환경-설정)
2. [Phase 1: Foundation Layer (기반)](#-phase-1-foundation-layer-기반)
3. [Phase 2: Core Service Layer (핵심 서비스)](#-phase-2-core-service-layer-핵심-서비스)
4. [Phase 3: Integration Layer (통합/알림)](#-phase-3-integration-layer-통합알림)
5. [완전한 통합 시나리오](#-완전한-통합-시나리오)
6. [에러 케이스 테스트](#-에러-케이스-테스트)

---

## 🔧 테스트 환경 설정

### Postman Environment 설정

**Environment Name**: `MARUNI Local`

| Variable | Type | Initial Value | Current Value |
|----------|------|---------------|---------------|
| `base_url` | default | `http://localhost:8080` | `http://localhost:8080` |
| `access_token` | secret | - | (로그인 후 자동 설정) |
| `member_id` | default | - | (회원가입 후 자동 설정) |
| `guardian_id` | default | - | (보호자 생성 후 자동 설정) |

### Postman Collection 구성

```
MARUNI API Tests/
├── 1. Foundation Layer/
│   ├── 1-1. 회원가입
│   ├── 1-2. 이메일 중복 확인
│   ├── 1-3. 로그인
│   └── 1-4. 회원 관리
├── 2. Core Service Layer/
│   ├── 2-1. AI 대화
│   ├── 2-2. 보호자 관리
│   └── 2-3. 보호자-회원 관계
└── 3. Integration Layer/
    ├── 3-1. 알림 규칙 관리
    ├── 3-2. 이상징후 감지
    └── 3-3. 알림 이력
```

---

## 🔐 Phase 1: Foundation Layer (기반)

**목표**: 회원 가입 → 인증 → JWT 토큰 획득 → 본인 정보 관리

### 📍 Step 1-1: 이메일 중복 확인

**Request**
```http
GET {{base_url}}/api/join/email-check?memberEmail=test@example.com
```

**Headers**
```
Content-Type: application/json
```

**Expected Response (200 OK - 사용 가능)**
```json
{
  "success": true,
  "code": "MEMBER_EMAIL_CHECK_OK",
  "message": "사용 가능한 이메일입니다",
  "data": null
}
```

**Expected Response (409 Conflict - 중복)**
```json
{
  "success": false,
  "code": "DUPLICATE_EMAIL",
  "message": "이미 존재하는 이메일입니다",
  "data": null
}
```

---

### 📍 Step 1-2: 회원가입

**Request**
```http
POST {{base_url}}/api/join
```

**Headers**
```
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "memberEmail": "test@example.com",
  "memberName": "김할머니",
  "memberPassword": "password123"
}
```

**Tests (Postman Script)**
```javascript
// 회원가입 성공 시 이메일 저장
if (pm.response.code === 200) {
    pm.environment.set("test_member_email", "test@example.com");
}

// 응답 검증
pm.test("회원가입 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.code).to.eql("MEMBER_CREATED");
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "MEMBER_CREATED",
  "message": "회원가입이 완료되었습니다",
  "data": null
}
```

---

### 📍 Step 1-3: 로그인 (JWT 토큰 획득)

**Request**
```http
POST {{base_url}}/api/members/login
```

**Headers**
```
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "memberEmail": "test@example.com",
  "memberPassword": "password123"
}
```

**Tests (Postman Script)**
```javascript
// 로그인 성공 시 Authorization 헤더에서 토큰 추출
if (pm.response.code === 200) {
    var authHeader = pm.response.headers.get("Authorization");

    if (authHeader) {
        // Authorization 헤더 전체를 저장 ("Bearer eyJhbGci...")
        pm.environment.set("access_token", authHeader);
        console.log("✅ Access Token 저장 완료:", authHeader.substring(0, 20) + "...");
    }
}

// 응답 검증
pm.test("로그인 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});

pm.test("Authorization 헤더 존재 확인", function () {
    pm.response.to.have.header("Authorization");
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "MEMBER_LOGIN_SUCCESS",
  "message": "로그인이 완료되었습니다",
  "data": null
}
```

**Response Headers**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 📍 Step 1-4: 내 정보 조회 (JWT 인증 테스트)

**Request**
```http
GET {{base_url}}/api/users/me
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Tests (Postman Script)**
```javascript
pm.test("내 정보 조회 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.data).to.have.property("id");
    pm.expect(jsonData.data).to.have.property("memberEmail");
    pm.expect(jsonData.data.memberEmail).to.eql("test@example.com");

    // member_id 환경변수에 저장 (이후 테스트에서 사용)
    pm.environment.set("member_id", jsonData.data.id);
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "MEMBER_VIEW",
  "message": "조회되었습니다",
  "data": {
    "id": 1,
    "memberName": "김할머니",
    "memberEmail": "test@example.com",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
```

---

### 📍 Step 1-5: 내 정보 수정

**Request**
```http
PUT {{base_url}}/api/users/me
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "memberName": "김할머니 수정",
  "memberPassword": "newPassword456"
}
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "MEMBER_UPDATED",
  "message": "회원정보가 수정되었습니다",
  "data": {
    "id": 1,
    "memberName": "김할머니 수정",
    "memberEmail": "test@example.com",
    "updatedAt": "2025-01-15T14:20:00"
  }
}
```

---

## 💬 Phase 2: Core Service Layer (핵심 서비스)

**목표**: AI 대화 → 보호자 등록 → 관계 설정

### 📍 Step 2-1: AI 대화 메시지 전송 (첫 대화)

**Request**
```http
POST {{base_url}}/api/conversations/messages
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "content": "안녕하세요, 오늘 기분이 좋아요!"
}
```

**Tests (Postman Script)**
```javascript
pm.test("AI 대화 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.data).to.have.property("conversationId");
    pm.expect(jsonData.data).to.have.property("userMessage");
    pm.expect(jsonData.data).to.have.property("aiMessage");

    // conversationId 저장
    pm.environment.set("conversation_id", jsonData.data.conversationId);
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "conversationId": 1,
    "userMessage": {
      "id": 1,
      "type": "USER_MESSAGE",
      "content": "안녕하세요, 오늘 기분이 좋아요!",
      "emotion": "POSITIVE",
      "createdAt": "2025-09-18T10:30:00"
    },
    "aiMessage": {
      "id": 2,
      "type": "AI_RESPONSE",
      "content": "안녕하세요! 기분이 좋으시다니 정말 다행이에요. 오늘 특별한 일이 있으셨나요?",
      "emotion": "NEUTRAL",
      "createdAt": "2025-09-18T10:30:03"
    }
  }
}
```

---

### 📍 Step 2-2: AI 대화 메시지 전송 (부정 감정)

**Request**
```http
POST {{base_url}}/api/conversations/messages
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "content": "요즘 많이 슬프고 우울해요"
}
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "conversationId": 1,
    "userMessage": {
      "id": 3,
      "type": "USER_MESSAGE",
      "content": "요즘 많이 슬프고 우울해요",
      "emotion": "NEGATIVE",
      "createdAt": "2025-09-18T11:00:00"
    },
    "aiMessage": {
      "id": 4,
      "type": "AI_RESPONSE",
      "content": "그러셨군요. 힘드신 일이 있으신가봐요. 좋은 일이 생길 거예요.",
      "emotion": "NEUTRAL",
      "createdAt": "2025-09-18T11:00:03"
    }
  }
}
```

---

### 📍 Step 2-3: 보호자 생성

**Request**
```http
POST {{base_url}}/api/guardians
```

**Headers**
```
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "guardianName": "김보호",
  "guardianEmail": "guardian@example.com",
  "guardianPhone": "010-1234-5678",
  "relation": "FAMILY",
  "notificationPreference": "ALL"
}
```

**Tests (Postman Script)**
```javascript
pm.test("보호자 생성 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.data).to.have.property("id");

    // guardian_id 저장
    pm.environment.set("guardian_id", jsonData.data.id);
});
```

**Expected Response (200 OK)**
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

---

### 📍 Step 2-4: 현재 회원에게 보호자 할당

**Request**
```http
POST {{base_url}}/api/guardians/{{guardian_id}}/assign
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Tests (Postman Script)**
```javascript
pm.test("보호자 할당 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": null
}
```

---

### 📍 Step 2-5: 내 보호자 조회

**Request**
```http
GET {{base_url}}/api/guardians/my-guardian
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Expected Response (200 OK)**
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

---

### 📍 Step 2-6: 보호자가 담당하는 회원 목록 조회

**Request**
```http
GET {{base_url}}/api/guardians/{{guardian_id}}/members
```

**Headers**
```
Content-Type: application/json
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": [
    {
      "id": 1,
      "memberName": "김할머니 수정",
      "memberEmail": "test@example.com",
      "createdAt": "2025-09-18T09:00:00",
      "updatedAt": "2025-09-18T14:20:00"
    }
  ]
}
```

---

## 🚨 Phase 3: Integration Layer (통합/알림)

**목표**: 알림 규칙 생성 → 이상징후 감지 → 알림 이력 확인

### 📍 Step 3-1: 감정패턴 알림 규칙 생성

**Request**
```http
POST {{base_url}}/api/alert-rules
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "ruleName": "연속 부정감정 감지 규칙",
  "condition": {
    "consecutiveDays": 3,
    "thresholdCount": null,
    "keywords": null,
    "description": "3일 연속 부정감정 감지"
  },
  "description": "3일 연속 부정적 감정 감지 시 보호자에게 HIGH 레벨 알림을 발송합니다",
  "active": true
}
```

**Tests (Postman Script)**
```javascript
pm.test("알림 규칙 생성 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.data).to.have.property("id");

    // alert_rule_id 저장
    pm.environment.set("alert_rule_emotion_id", jsonData.data.id);
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "alertType": "EMOTION_PATTERN",
    "alertLevel": "HIGH",
    "ruleName": "연속 부정감정 감지 규칙",
    "ruleDescription": "3일 연속 부정적 감정 감지 시 보호자에게 HIGH 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": 3,
      "thresholdCount": null,
      "keywords": null,
      "description": "3일 연속 부정감정 감지"
    },
    "isActive": true,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T10:30:00"
  }
}
```

---

### 📍 Step 3-2: 키워드 감지 알림 규칙 생성 (긴급)

**Request**
```http
POST {{base_url}}/api/alert-rules
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "alertType": "KEYWORD_DETECTION",
  "alertLevel": "EMERGENCY",
  "ruleName": "긴급 키워드 감지 규칙",
  "condition": {
    "consecutiveDays": null,
    "thresholdCount": 1,
    "keywords": "아파요,도와주세요,병원,119,응급실",
    "description": "긴급 키워드 즉시 감지"
  },
  "description": "긴급 상황 키워드 감지 시 즉시 EMERGENCY 알림 발송",
  "active": true
}
```

**Tests (Postman Script)**
```javascript
pm.test("긴급 알림 규칙 생성 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.environment.set("alert_rule_keyword_id", jsonData.data.id);
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 2,
    "alertType": "KEYWORD_DETECTION",
    "alertLevel": "EMERGENCY",
    "ruleName": "긴급 키워드 감지 규칙",
    "ruleDescription": "긴급 상황 키워드 감지 시 즉시 EMERGENCY 알림 발송",
    "condition": {
      "consecutiveDays": null,
      "thresholdCount": 1,
      "keywords": "아파요,도와주세요,병원,119,응급실",
      "description": "긴급 키워드 즉시 감지"
    },
    "isActive": true,
    "createdAt": "2025-09-18T11:00:00",
    "updatedAt": "2025-09-18T11:00:00"
  }
}
```

---

### 📍 Step 3-3: 무응답 패턴 알림 규칙 생성

**Request**
```http
POST {{base_url}}/api/alert-rules
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body (raw JSON)**
```json
{
  "alertType": "NO_RESPONSE",
  "alertLevel": "MEDIUM",
  "ruleName": "무응답 패턴 감지 규칙",
  "condition": {
    "consecutiveDays": 2,
    "thresholdCount": null,
    "keywords": null,
    "description": "2일 연속 무응답"
  },
  "description": "2일 연속 안부 메시지에 응답하지 않을 때 알림 발송",
  "active": true
}
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 3,
    "alertType": "NO_RESPONSE",
    "alertLevel": "MEDIUM",
    "ruleName": "무응답 패턴 감지 규칙",
    "ruleDescription": "2일 연속 안부 메시지에 응답하지 않을 때 알림 발송",
    "condition": {
      "consecutiveDays": 2,
      "thresholdCount": null,
      "keywords": null,
      "description": "2일 연속 무응답"
    },
    "isActive": true,
    "createdAt": "2025-09-18T11:30:00",
    "updatedAt": "2025-09-18T11:30:00"
  }
}
```

---

### 📍 Step 3-4: 알림 규칙 목록 조회

**Request**
```http
GET {{base_url}}/api/alert-rules
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Tests (Postman Script)**
```javascript
pm.test("알림 규칙 목록 조회 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.at.least(3);
});
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": [
    {
      "id": 1,
      "alertType": "EMOTION_PATTERN",
      "alertLevel": "HIGH",
      "ruleName": "연속 부정감정 감지 규칙",
      "ruleDescription": "3일 연속 부정적 감정 감지 시 보호자에게 HIGH 레벨 알림을 발송합니다",
      "condition": { /* ... */ },
      "isActive": true,
      "createdAt": "2025-09-18T10:30:00",
      "updatedAt": "2025-09-18T10:30:00"
    },
    {
      "id": 2,
      "alertType": "KEYWORD_DETECTION",
      "alertLevel": "EMERGENCY",
      "ruleName": "긴급 키워드 감지 규칙",
      "ruleDescription": "긴급 상황 키워드 감지 시 즉시 EMERGENCY 알림 발송",
      "condition": { /* ... */ },
      "isActive": true,
      "createdAt": "2025-09-18T11:00:00",
      "updatedAt": "2025-09-18T11:00:00"
    },
    {
      "id": 3,
      "alertType": "NO_RESPONSE",
      "alertLevel": "MEDIUM",
      "ruleName": "무응답 패턴 감지 규칙",
      "ruleDescription": "2일 연속 안부 메시지에 응답하지 않을 때 알림 발송",
      "condition": { /* ... */ },
      "isActive": true,
      "createdAt": "2025-09-18T11:30:00",
      "updatedAt": "2025-09-18T11:30:00"
    }
  ]
}
```

---

### 📍 Step 3-5: 수동 이상징후 감지 실행

**Request**
```http
POST {{base_url}}/api/alert-rules/detect
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Tests (Postman Script)**
```javascript
pm.test("이상징후 감지 실행 성공", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
    pm.expect(jsonData.data).to.have.property("memberId");
    pm.expect(jsonData.data).to.have.property("detectionTime");
    pm.expect(jsonData.data).to.have.property("totalAnomaliesDetected");
    pm.expect(jsonData.data).to.have.property("alertResults");
});
```

**Expected Response (200 OK - 이상징후 감지됨)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "memberId": 1,
    "detectionTime": "2025-09-18T16:00:00",
    "totalAnomaliesDetected": 1,
    "alertResults": [
      {
        "alertType": "EMOTION_PATTERN",
        "alertLevel": "HIGH",
        "isAlert": true,
        "message": "3일 연속 부정적 감정 감지",
        "severity": 85,
        "details": {
          "consecutiveDays": 3,
          "recentEmotions": ["NEGATIVE", "NEGATIVE", "NEGATIVE"],
          "analysisDate": "2025-09-18T16:00:00"
        }
      }
    ],
    "notificationStatus": {
      "guardianNotified": true,
      "notificationChannels": ["PUSH", "EMAIL"],
      "notificationTime": "2025-09-18T16:00:05"
    }
  }
}
```

---

### 📍 Step 3-6: 알림 이력 조회

**Request**
```http
GET {{base_url}}/api/alert-rules/history?days=7
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Expected Response (200 OK)**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": [
    {
      "id": 1,
      "alertRuleId": 1,
      "alertType": "EMOTION_PATTERN",
      "alertLevel": "HIGH",
      "alertMessage": "3일 연속 부정적 감정 감지",
      "detectionDetails": "{\"consecutiveDays\": 3, \"emotions\": [\"NEGATIVE\", \"NEGATIVE\", \"NEGATIVE\"]}",
      "isNotificationSent": true,
      "notificationSentAt": "2025-09-18T16:00:05",
      "notificationResult": "성공적으로 보호자에게 알림을 발송했습니다",
      "alertDate": "2025-09-18T16:00:00",
      "createdAt": "2025-09-18T16:00:00"
    }
  ]
}
```

---

## 🔄 완전한 통합 시나리오

**시나리오**: 신규 회원 가입 → AI 대화 → 보호자 설정 → 이상징후 감지 → 알림 발송

### 전체 플로우 요약

```
1. 회원가입 (test2@example.com)
   ↓
2. 로그인 → JWT 토큰 획득
   ↓
3. 보호자 생성 (guardian2@example.com)
   ↓
4. 보호자-회원 관계 설정
   ↓
5. 알림 규칙 생성 (긴급 키워드)
   ↓
6. AI 대화 - 긴급 키워드 포함 ("배가 아파요")
   ↓
7. 이상징후 자동 감지 → 보호자 알림 발송
   ↓
8. 알림 이력 확인
```

### Step-by-Step 실행 순서

**1. 새 회원 가입**
```http
POST {{base_url}}/api/join
Body: { "memberEmail": "test2@example.com", "memberName": "이할아버지", "memberPassword": "password456" }
```

**2. 로그인**
```http
POST {{base_url}}/api/members/login
Body: { "memberEmail": "test2@example.com", "memberPassword": "password456" }
```

**3. 보호자 생성**
```http
POST {{base_url}}/api/guardians
Body: { "guardianName": "이보호", "guardianEmail": "guardian2@example.com", "guardianPhone": "010-9876-5432", "relation": "FAMILY", "notificationPreference": "PUSH" }
```

**4. 보호자 할당**
```http
POST {{base_url}}/api/guardians/{{guardian_id}}/assign
Headers: Authorization: {{access_token}}
```

**5. 긴급 키워드 알림 규칙 생성**
```http
POST {{base_url}}/api/alert-rules
Body: { "alertType": "KEYWORD_DETECTION", "alertLevel": "EMERGENCY", "ruleName": "긴급 알림", "condition": { "keywords": "아파요,도와주세요", "thresholdCount": 1 }, "active": true }
```

**6. AI 대화 - 긴급 키워드 포함**
```http
POST {{base_url}}/api/conversations/messages
Body: { "content": "배가 아파요, 도와주세요" }
```

**7. 수동 이상징후 감지 (자동 감지 검증용)**
```http
POST {{base_url}}/api/alert-rules/detect
Headers: Authorization: {{access_token}}
```

**8. 알림 이력 확인**
```http
GET {{base_url}}/api/alert-rules/history?days=1
Headers: Authorization: {{access_token}}
```

---

## ❌ 에러 케이스 테스트

### Error Case 1: 인증 실패 (401 Unauthorized)

**Request**
```http
GET {{base_url}}/api/users/me
```

**Headers**
```
Authorization: Bearer invalid_token_here
Content-Type: application/json
```

**Expected Response (401)**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

---

### Error Case 2: 토큰 없이 보호된 API 호출 (401)

**Request**
```http
GET {{base_url}}/api/users/me
```

**Headers**
```
Content-Type: application/json
```

**Expected Response (401)**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

---

### Error Case 3: 중복 이메일로 회원가입 (409 Conflict)

**Request**
```http
POST {{base_url}}/api/join
```

**Body**
```json
{
  "memberEmail": "test@example.com",
  "memberName": "중복테스트",
  "memberPassword": "password123"
}
```

**Expected Response (409)**
```json
{
  "success": false,
  "code": "DUPLICATE_EMAIL",
  "message": "이미 존재하는 이메일입니다",
  "data": null
}
```

---

### Error Case 4: 잘못된 로그인 정보 (401)

**Request**
```http
POST {{base_url}}/api/members/login
```

**Body**
```json
{
  "memberEmail": "test@example.com",
  "memberPassword": "wrongpassword"
}
```

**Expected Response (401)**
```json
{
  "success": false,
  "code": "LOGIN_FAIL",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다",
  "data": null
}
```

---

### Error Case 5: 입력값 검증 실패 (400 Bad Request)

**Request**
```http
POST {{base_url}}/api/join
```

**Body**
```json
{
  "memberEmail": "invalid-email-format",
  "memberName": "",
  "memberPassword": "short"
}
```

**Expected Response (400)**
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
      },
      {
        "field": "memberName",
        "message": "이름은 필수입니다"
      },
      {
        "field": "memberPassword",
        "message": "비밀번호는 최소 6자 이상이어야 합니다"
      }
    ]
  }
}
```

---

### Error Case 6: 존재하지 않는 리소스 (404 Not Found)

**Request**
```http
GET {{base_url}}/api/guardians/99999
```

**Expected Response (404)**
```json
{
  "success": false,
  "code": "GUARDIAN_NOT_FOUND",
  "message": "보호자를 찾을 수 없습니다",
  "data": null
}
```

---

### Error Case 7: 메시지 길이 초과 (400)

**Request**
```http
POST {{base_url}}/api/conversations/messages
```

**Headers**
```
Authorization: {{access_token}}
Content-Type: application/json
```

**Body**
```json
{
  "content": "이것은 500자를 초과하는 매우 긴 메시지입니다... (500자 이상 텍스트)"
}
```

**Expected Response (400)**
```json
{
  "success": false,
  "code": "INVALID_INPUT_VALUE",
  "message": "입력값이 올바르지 않습니다",
  "data": {
    "fieldErrors": [
      {
        "field": "content",
        "message": "메시지는 500자를 초과할 수 없습니다"
      }
    ]
  }
}
```

---

## 📊 Postman Collection 자동화 스크립트

### Pre-request Script (Collection Level)

```javascript
// 공통 변수 확인
if (!pm.environment.get("base_url")) {
    pm.environment.set("base_url", "http://localhost:8080");
}

// 타임스탬프 추가 (필요 시)
pm.globals.set("timestamp", new Date().toISOString());
```

---

### Tests Script (Collection Level)

```javascript
// 모든 응답에 대한 공통 검증
pm.test("Response time is less than 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

pm.test("Response has success field", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("success");
    pm.expect(jsonData).to.have.property("code");
    pm.expect(jsonData).to.have.property("message");
});

// HTTP 상태 코드별 로깅
if (pm.response.code >= 400) {
    console.error("❌ API Error:", pm.response.code, pm.response.json().message);
} else {
    console.log("✅ API Success:", pm.response.code, pm.info.requestName);
}
```

---

## 🎯 추천 테스트 순서

### 초기 설정 (한 번만 실행)
1. Environment 생성 (`MARUNI Local`)
2. Base URL 설정 (`http://localhost:8080`)

### 기본 플로우 테스트 (순서대로)
1. ✅ Phase 1: Foundation Layer
   - Step 1-1 → Step 1-2 → Step 1-3 → Step 1-4 → Step 1-5
2. ✅ Phase 2: Core Service Layer
   - Step 2-1 → Step 2-2 → Step 2-3 → Step 2-4 → Step 2-5 → Step 2-6
3. ✅ Phase 3: Integration Layer
   - Step 3-1 → Step 3-2 → Step 3-3 → Step 3-4 → Step 3-5 → Step 3-6

### 통합 시나리오 테스트
4. ✅ 완전한 통합 시나리오 (Step 1~8)

### 에러 케이스 검증
5. ✅ 에러 케이스 1~7 (독립 실행)

---

## 📝 주요 참고 사항

### JWT 토큰 관리
- **유효기간**: 1시간 (3600초)
- **재발급**: 만료 시 로그인 재실행 필요
- **저장 위치**: Postman Environment Variable (`access_token`)
- **헤더 형식**: `Authorization: Bearer {token}` (전체 값 사용)

### API 응답 구조
모든 API는 다음 공통 구조를 사용합니다:
```json
{
  "success": boolean,
  "code": "string",
  "message": "string",
  "data": object | array | null
}
```

### 환경 변수 활용
- `{{base_url}}`: 기본 URL
- `{{access_token}}`: JWT Access Token (Authorization 헤더 전체)
- `{{member_id}}`: 현재 로그인 회원 ID
- `{{guardian_id}}`: 생성된 보호자 ID
- `{{conversation_id}}`: 대화 세션 ID
- `{{alert_rule_emotion_id}}`: 감정패턴 알림 규칙 ID
- `{{alert_rule_keyword_id}}`: 키워드 알림 규칙 ID

### 주의사항
1. **순서 준수**: 테스트는 반드시 Phase 순서대로 진행
2. **토큰 저장**: 로그인 후 반드시 Authorization 헤더에서 토큰 추출 확인
3. **환경 초기화**: 새로운 테스트 시작 시 Environment 변수 초기화 권장
4. **서버 상태**: 테스트 전 서버 실행 상태 확인 (`http://localhost:8080/actuator/health`)

---

**MARUNI API 완전 테스트 가이드 v1.0**
**Updated**: 2025-10-06 | **Status**: Production Ready
