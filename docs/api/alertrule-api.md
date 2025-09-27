# AlertRule 도메인 API 명세서

**이상징후 감지 알림 규칙 관리 시스템 REST API 완전 가이드**

## 📋 개요

AlertRule 도메인은 MARUNI 프로젝트의 핵심 기능인 노인들의 이상징후를 자동으로 감지하고 보호자에게 실시간 알림을 제공하는 시스템입니다.

### 🎯 **핵심 기능**
- **3종 이상징후 감지**: 감정패턴/무응답/키워드 분석 알고리즘
- **실시간 알림 발송**: 보호자에게 즉시 알림 전송
- **알림 규칙 관리**: 개별 맞춤형 감지 규칙 설정
- **이력 추적**: 모든 감지 및 알림 발송 기록 관리

### 🔐 **기술 특징**
- **Strategy Pattern**: 확장 가능한 3종 분석기 구조
- **Facade Pattern**: SRP 준수하며 기존 API 호환성 유지
- **실시간 처리**: 키워드 감지 시 즉시 알림 발송
- **성능 최적화**: 인덱스 최적화 및 N+1 문제 방지

---

## 🌐 API 엔드포인트 목록

| HTTP | 엔드포인트 | 인증 | 설명 |
|------|------------|------|------|
| `POST` | `/api/alert-rules` | ✅ JWT | 알림 규칙 생성 |
| `GET` | `/api/alert-rules` | ✅ JWT | 알림 규칙 목록 조회 |
| `GET` | `/api/alert-rules/{id}` | ✅ JWT | 알림 규칙 상세 조회 |
| `PUT` | `/api/alert-rules/{id}` | ✅ JWT | 알림 규칙 수정 |
| `DELETE` | `/api/alert-rules/{id}` | ✅ JWT | 알림 규칙 삭제 |
| `POST` | `/api/alert-rules/{id}/toggle` | ✅ JWT | 알림 규칙 활성화/비활성화 |
| `GET` | `/api/alert-rules/history` | ✅ JWT | 알림 이력 조회 |
| `POST` | `/api/alert-rules/detect` | ✅ JWT | 수동 이상징후 감지 |

---

## 🚨 알림 규칙 관리 API

### 1. 알림 규칙 생성

#### **POST** `/api/alert-rules`

새로운 이상징후 감지 알림 규칙을 생성합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Request Body:**
```json
{
  "alertType": "AlertType",
  "alertLevel": "AlertLevel",
  "ruleName": "string",
  "condition": "AlertConditionDto",
  "description": "string",
  "active": "boolean"
}
```

**Request 예시 (감정패턴 규칙):**
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

**Request 예시 (키워드 감지 규칙):**
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

**Request 예시 (무응답 규칙):**
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

**Validation 규칙:**
- `alertType`: 필수, EMOTION_PATTERN/NO_RESPONSE/KEYWORD_DETECTION
- `alertLevel`: 필수, LOW/MEDIUM/HIGH/EMERGENCY
- `ruleName`: 필수, 1-100자
- `condition`: 필수, 알림 유형에 따른 조건 설정
- `description`: 선택, 최대 255자
- `active`: 선택, 기본값 true

**Response 200 (성공):**
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

**Response 400 (입력값 검증 실패):**
```json
{
  "success": false,
  "code": "INVALID_INPUT_VALUE",
  "message": "입력값이 올바르지 않습니다",
  "data": {
    "fieldErrors": [
      {
        "field": "ruleName",
        "message": "알림 규칙 이름은 1~100자여야 합니다"
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
curl -X POST "http://localhost:8080/api/alert-rules" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

---

### 2. 알림 규칙 목록 조회

#### **GET** `/api/alert-rules`

현재 회원의 모든 알림 규칙 목록을 조회합니다.

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
  "data": [
    {
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
    },
    {
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
  ]
}
```

**Response 200 (규칙 없음):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": []
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/alert-rules" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 3. 알림 규칙 상세 조회

#### **GET** `/api/alert-rules/{id}`

특정 알림 규칙의 상세 정보를 조회합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters:**
- `id` (Long): 알림 규칙 ID

**Response 200 (성공):**
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

**Response 404 (규칙 없음):**
```json
{
  "success": false,
  "code": "ALERT_RULE_NOT_FOUND",
  "message": "알림 규칙을 찾을 수 없습니다",
  "data": null
}
```

**Response 403 (접근 권한 없음):**
```json
{
  "success": false,
  "code": "ALERT_RULE_ACCESS_DENIED",
  "message": "해당 알림 규칙에 접근할 권한이 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/alert-rules/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 4. 알림 규칙 수정

#### **PUT** `/api/alert-rules/{id}`

기존 알림 규칙의 설정을 수정합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Path Parameters:**
- `id` (Long): 알림 규칙 ID

**Request Body:**
```json
{
  "ruleName": "string",
  "description": "string",
  "alertLevel": "AlertLevel"
}
```

**Request 예시:**
```json
{
  "ruleName": "수정된 연속 부정감정 감지 규칙",
  "description": "3일 연속 부정적 감정 감지 시 보호자에게 즉시 알림을 발송합니다 (수정됨)",
  "alertLevel": "EMERGENCY"
}
```

**Validation 규칙:**
- `ruleName`: 필수, 1-100자
- `description`: 선택, 최대 255자
- `alertLevel`: 필수, LOW/MEDIUM/HIGH/EMERGENCY

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "alertType": "EMOTION_PATTERN",
    "alertLevel": "EMERGENCY",
    "ruleName": "수정된 연속 부정감정 감지 규칙",
    "ruleDescription": "3일 연속 부정적 감정 감지 시 보호자에게 즉시 알림을 발송합니다 (수정됨)",
    "condition": {
      "consecutiveDays": 3,
      "thresholdCount": null,
      "keywords": null,
      "description": "3일 연속 부정감정 감지"
    },
    "isActive": true,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T14:20:00"
  }
}
```

**cURL 예시:**
```bash
curl -X PUT "http://localhost:8080/api/alert-rules/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "ruleName": "수정된 연속 부정감정 감지 규칙",
    "description": "3일 연속 부정적 감정 감지 시 보호자에게 즉시 알림을 발송합니다 (수정됨)",
    "alertLevel": "EMERGENCY"
  }'
```

---

### 5. 알림 규칙 삭제

#### **DELETE** `/api/alert-rules/{id}`

알림 규칙을 삭제합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters:**
- `id` (Long): 알림 규칙 ID

**Response 200 (성공):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": null
}
```

**삭제 처리 과정:**
1. 해당 알림 규칙의 모든 이력 기록 유지
2. 규칙 자체는 물리적 삭제 (isActive 비활성화 아님)
3. 관련된 실시간 감지 작업 중단

**Response 404 (규칙 없음):**
```json
{
  "success": false,
  "code": "ALERT_RULE_NOT_FOUND",
  "message": "알림 규칙을 찾을 수 없습니다",
  "data": null
}
```

**cURL 예시:**
```bash
curl -X DELETE "http://localhost:8080/api/alert-rules/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 6. 알림 규칙 활성화/비활성화

#### **POST** `/api/alert-rules/{id}/toggle`

알림 규칙의 활성화 상태를 토글합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters:**
- `id` (Long): 알림 규칙 ID

**Query Parameters:**
- `active` (boolean): 활성화 상태 (true/false)

**Request 예시:**
```
POST /api/alert-rules/1/toggle?active=false
```

**Response 200 (성공):**
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
    "isActive": false,
    "createdAt": "2025-09-18T10:30:00",
    "updatedAt": "2025-09-18T15:00:00"
  }
}
```

**활성화/비활성화 처리:**
- **비활성화**: 해당 규칙의 이상징후 감지 중단
- **활성화**: 즉시 이상징후 감지 재개
- **이력 유지**: 비활성화되어도 기존 알림 이력은 유지

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/alert-rules/1/toggle?active=false" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📊 알림 이력 및 감지 API

### 7. 알림 이력 조회

#### **GET** `/api/alert-rules/history`

회원의 이상징후 감지 이력을 조회합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Query Parameters:**
- `days` (int): 조회 기간 (기본값: 30일)

**Request 예시:**
```
GET /api/alert-rules/history?days=7
```

**Response 200 (성공):**
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
      "detectionDetails": "{\"consecutiveDays\": 3, \"emotions\": [\"NEGATIVE\", \"NEGATIVE\", \"NEGATIVE\"], \"messages\": [\"우울해요\", \"슬퍼요\", \"힘들어요\"]}",
      "isNotificationSent": true,
      "notificationSentAt": "2025-09-18T10:35:00",
      "notificationResult": "성공적으로 보호자에게 알림을 발송했습니다",
      "alertDate": "2025-09-18T10:30:00",
      "createdAt": "2025-09-18T10:30:00"
    },
    {
      "id": 2,
      "alertRuleId": 2,
      "alertType": "KEYWORD_DETECTION",
      "alertLevel": "EMERGENCY",
      "alertMessage": "긴급 키워드 감지: '아파요'",
      "detectionDetails": "{\"keyword\": \"아파요\", \"message\": \"배가 아파요\", \"detectedAt\": \"2025-09-17T15:20:00\"}",
      "isNotificationSent": true,
      "notificationSentAt": "2025-09-17T15:20:05",
      "notificationResult": "즉시 보호자 알림 발송 완료",
      "alertDate": "2025-09-17T15:20:00",
      "createdAt": "2025-09-17T15:20:00"
    }
  ]
}
```

**Response 200 (이력 없음):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": []
}
```

**cURL 예시:**
```bash
curl -X GET "http://localhost:8080/api/alert-rules/history?days=7" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 8. 수동 이상징후 감지

#### **POST** `/api/alert-rules/detect`

회원에 대해 수동으로 이상징후 감지를 실행합니다.

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
    "memberId": 1,
    "detectionTime": "2025-09-18T16:00:00",
    "totalAnomaliesDetected": 2,
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
      },
      {
        "alertType": "NO_RESPONSE",
        "alertLevel": "MEDIUM",
        "isAlert": true,
        "message": "2일 연속 안부 메시지 무응답",
        "severity": 65,
        "details": {
          "noResponseDays": 2,
          "lastResponseDate": "2025-09-16T09:00:00",
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

**Response 200 (이상징후 없음):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "memberId": 1,
    "detectionTime": "2025-09-18T16:00:00",
    "totalAnomaliesDetected": 0,
    "alertResults": [],
    "notificationStatus": {
      "guardianNotified": false,
      "notificationChannels": [],
      "notificationTime": null
    }
  }
}
```

**감지 처리 과정:**
1. 현재 활성화된 모든 알림 규칙 조회
2. 3종 분석기로 동시 이상징후 감지 실행
3. 감지된 이상징후의 알림 레벨별 우선순위 정렬
4. 보호자 알림 발송 (HIGH/EMERGENCY 레벨)
5. 알림 이력 기록 및 결과 반환

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/alert-rules/detect" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📊 데이터 모델

### AlertRuleCreateRequestDto (알림 규칙 생성)
```json
{
  "alertType": "AlertType (필수)",
  "alertLevel": "AlertLevel (필수)",
  "ruleName": "string (필수, 1-100자)",
  "condition": "AlertConditionDto (필수)",
  "description": "string (선택, 최대 255자)",
  "active": "boolean (기본값: true)"
}
```

### AlertRuleUpdateRequestDto (알림 규칙 수정)
```json
{
  "ruleName": "string (필수, 1-100자)",
  "description": "string (선택, 최대 255자)",
  "alertLevel": "AlertLevel (필수)"
}
```

### AlertConditionDto (알림 조건)
```json
{
  "consecutiveDays": "number (감정패턴/무응답용)",
  "thresholdCount": "number (키워드 감지용)",
  "keywords": "string (키워드 목록, 쉼표 구분)",
  "description": "string (조건 설명)"
}
```

### AlertRuleResponseDto (알림 규칙 응답)
```json
{
  "id": "number (알림 규칙 ID)",
  "alertType": "AlertType (알림 유형)",
  "alertLevel": "AlertLevel (알림 레벨)",
  "ruleName": "string (규칙 이름)",
  "ruleDescription": "string (규칙 설명)",
  "condition": "AlertConditionDto (알림 조건)",
  "isActive": "boolean (활성 상태)",
  "createdAt": "datetime (생성 시간)",
  "updatedAt": "datetime (수정 시간)"
}
```

### AlertHistoryResponseDto (알림 이력 응답)
```json
{
  "id": "number (이력 ID)",
  "alertRuleId": "number (알림 규칙 ID)",
  "alertType": "AlertType (알림 유형)",
  "alertLevel": "AlertLevel (알림 레벨)",
  "alertMessage": "string (알림 메시지)",
  "detectionDetails": "string (JSON 형태 상세 정보)",
  "isNotificationSent": "boolean (알림 발송 여부)",
  "notificationSentAt": "datetime (알림 발송 시간)",
  "notificationResult": "string (발송 결과)",
  "alertDate": "datetime (알림 날짜)",
  "createdAt": "datetime (생성 시간)"
}
```

### AlertDetectionResultDto (감지 결과 응답)
```json
{
  "memberId": "number (회원 ID)",
  "detectionTime": "datetime (감지 시간)",
  "totalAnomaliesDetected": "number (감지된 이상징후 수)",
  "alertResults": "array<AlertResult> (상세 감지 결과)",
  "notificationStatus": "NotificationStatus (알림 발송 상태)"
}
```

---

## 🔧 Enum 정의

### AlertType (알림 유형)
| 값 | 설명 | 감지 방식 |
|---|---|---|
| `EMOTION_PATTERN` | 감정패턴 | 연속적인 부정적 감정 감지 |
| `NO_RESPONSE` | 무응답 | 일정 기간 응답 없음 |
| `KEYWORD_DETECTION` | 키워드감지 | 위험 키워드 포함된 응답 |

### AlertLevel (알림 레벨)
| 값 | 우선순위 | 설명 | 대응 방식 |
|---|---|---|---|
| `LOW` | 1 | 낮음 | 정보성 알림 |
| `MEDIUM` | 2 | 보통 | 주의 관찰 필요 |
| `HIGH` | 3 | 높음 | 빠른 확인 필요 |
| `EMERGENCY` | 4 | 긴급 | 즉시 대응 필요 |

---

## ❌ 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `ALERT_RULE_NOT_FOUND` | 404 | 알림 규칙을 찾을 수 없음 |
| `ALERT_RULE_ACCESS_DENIED` | 403 | 알림 규칙 접근 권한 없음 |
| `ALERT_RULE_CREATION_FAILED` | 400 | 알림 규칙 생성 실패 |
| `INVALID_ALERT_CONDITION` | 400 | 잘못된 알림 조건 |
| `UNSUPPORTED_ALERT_TYPE` | 400 | 지원하지 않는 알림 유형 |
| `INVALID_INPUT_VALUE` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 🧪 테스트 시나리오

### 정상 플로우 (완전한 이상징후 감지 사이클)
```bash
# 1. 회원 로그인 및 JWT 토큰 획득
POST /api/members/login
{
  "memberEmail": "elder@example.com",
  "memberPassword": "password123"
}
# Response: JWT 토큰

# 2. 감정패턴 알림 규칙 생성
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
{
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "ruleName": "연속 부정감정 감지",
  "condition": {
    "consecutiveDays": 3,
    "description": "3일 연속 부정감정"
  },
  "description": "3일 연속 부정적 감정 시 보호자 알림"
}
# Response: alertRuleId=1

# 3. 키워드 감지 규칙 생성 (긴급)
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
{
  "alertType": "KEYWORD_DETECTION",
  "alertLevel": "EMERGENCY",
  "ruleName": "긴급 키워드 감지",
  "condition": {
    "keywords": "아파요,도와주세요,병원,119",
    "thresholdCount": 1
  },
  "description": "긴급 키워드 즉시 감지"
}
# Response: alertRuleId=2

# 4. 무응답 규칙 생성
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
{
  "alertType": "NO_RESPONSE",
  "alertLevel": "MEDIUM",
  "ruleName": "무응답 감지",
  "condition": {
    "consecutiveDays": 2
  },
  "description": "2일 연속 무응답 시 알림"
}
# Response: alertRuleId=3

# 5. 생성된 규칙 목록 조회
GET /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
# Response: 3개 규칙 목록

# 6. 수동 이상징후 감지 실행
POST /api/alert-rules/detect
Authorization: Bearer {JWT_TOKEN}
# Response: 현재 상태 기반 이상징후 감지 결과

# 7. 최근 7일 알림 이력 조회
GET /api/alert-rules/history?days=7
Authorization: Bearer {JWT_TOKEN}
# Response: 감지된 이상징후 이력

# 8. 특정 규칙 비활성화
POST /api/alert-rules/2/toggle?active=false
Authorization: Bearer {JWT_TOKEN}
# Response: 키워드 감지 규칙 비활성화

# 9. 규칙 수정
PUT /api/alert-rules/1
Authorization: Bearer {JWT_TOKEN}
{
  "ruleName": "강화된 연속 부정감정 감지",
  "description": "3일 연속 부정감정 시 즉시 보호자 알림 (강화됨)",
  "alertLevel": "EMERGENCY"
}
# Response: 수정된 규칙 정보

# 10. 규칙 삭제
DELETE /api/alert-rules/3
Authorization: Bearer {JWT_TOKEN}
# Response: 무응답 규칙 삭제 완료
```

### 에러 케이스
```bash
# 잘못된 알림 유형으로 규칙 생성
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
{
  "alertType": "INVALID_TYPE",  # 존재하지 않는 타입
  "alertLevel": "HIGH",
  "ruleName": "잘못된 규칙"
}
# Response: 400 UNSUPPORTED_ALERT_TYPE

# 조건 없이 감정패턴 규칙 생성
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
{
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "ruleName": "조건 없는 규칙",
  "condition": {
    "consecutiveDays": null  # 필수 조건 누락
  }
}
# Response: 400 INVALID_ALERT_CONDITION

# 존재하지 않는 규칙 조회
GET /api/alert-rules/999
Authorization: Bearer {JWT_TOKEN}
# Response: 404 ALERT_RULE_NOT_FOUND

# 다른 회원의 규칙에 접근 시도
GET /api/alert-rules/1
Authorization: Bearer {OTHER_MEMBER_JWT}
# Response: 403 ALERT_RULE_ACCESS_DENIED

# 인증 없이 API 호출
GET /api/alert-rules
# Response: 401 UNAUTHORIZED

# 너무 긴 규칙 이름으로 생성
POST /api/alert-rules
Authorization: Bearer {JWT_TOKEN}
{
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "ruleName": "매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우매우긴규칙이름",  # 100자 초과
  "condition": {
    "consecutiveDays": 3
  }
}
# Response: 400 INVALID_INPUT_VALUE
```

### 실시간 이상징후 감지 테스트
```bash
# AI 대화에서 긴급 키워드 발생
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "배가 너무 아파요, 도와주세요"
}
# 자동 처리: 키워드 감지 → 즉시 보호자 알림 → 이력 기록

# 연속 부정 감정 시나리오
# Day 1
POST /api/conversations/messages
{
  "content": "오늘 정말 우울해요"
}
# Day 2
POST /api/conversations/messages
{
  "content": "너무 슬프고 힘들어요"
}
# Day 3
POST /api/conversations/messages
{
  "content": "아무것도 하기 싫어요"
}
# 자동 처리: 3일 연속 NEGATIVE → HIGH 레벨 알림 → 보호자 통지

# 무응답 패턴 시나리오
# 2일 연속 DailyCheck 무응답
# 자동 처리: NoResponseAnalyzer 감지 → MEDIUM 레벨 알림
```

---

## 🔗 도메인 연동

### Conversation 연동 (실시간 키워드 감지)
```bash
# AI 대화 중 긴급 키워드 감지
# "아파요", "도와주세요", "119" 등 키워드 포함 메시지 입력
# → KeywordAnalyzer 실시간 감지
# → EMERGENCY 레벨 즉시 알림
# → Guardian 시스템을 통한 보호자 알림 발송
```

### Guardian 연동 (보호자 알림 발송)
```bash
# 이상징후 감지 시 보호자별 알림 설정에 따른 발송
# Guardian의 NotificationPreference 기반:
# - PUSH: Firebase FCM 푸시 알림
# - EMAIL: guardian@example.com 이메일 발송
# - SMS: 010-1234-5678 SMS 발송 (Phase 3)
# - ALL: 모든 채널 동시 발송
```

### DailyCheck 연동 (무응답 패턴 분석)
```bash
# 안부 메시지 무응답 패턴 분석
# DailyCheckRecord 기반 무응답 일수 계산
# NoResponseAnalyzer가 연속 무응답 감지
# → 설정된 임계값 초과 시 보호자 알림
```

### Member/Auth 연동 (권한 관리)
```bash
# JWT 기반 회원별 알림 규칙 관리
# 회원은 자신의 알림 규칙만 생성/조회/수정/삭제 가능
# 보호자는 담당 회원의 알림 이력 조회 가능 (Phase 3)
```

---

## 📋 관련 문서

### 🔗 **연관 API**
- **[Member API](./member-api.md)**: 회원 인증 (JWT 토큰 필요)
- **[Auth API](./auth-api.md)**: JWT 토큰 관리
- **[Conversation API](./conversation-api.md)**: AI 대화 기반 감정 분석
- **[Guardian API](./guardian-api.md)**: 보호자 알림 발송
- **[Notification API](./notification-api.md)**: 멀티채널 알림 시스템

### 🛠️ **기술 문서**
- **[AlertRule 도메인 가이드](../domains/alertrule.md)**: Strategy/Facade Pattern 구현 상세
- **[API 설계 가이드](../specifications/api-design-guide.md)**: REST API 설계 원칙
- **[아키텍처 가이드](../specifications/architecture-guide.md)**: DDD + Clean Architecture
- **[성능 가이드](../specifications/performance-guide.md)**: N+1 문제 방지 및 인덱스 최적화

---

## 💡 개발자 가이드

### 새로운 분석기 추가
```java
@Component
public class NewPatternAnalyzer implements AnomalyAnalyzer {
    @Override
    public AlertType getSupportedType() {
        return AlertType.NEW_PATTERN;  // AlertType enum에 추가 필요
    }

    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        // 새로운 패턴 분석 로직 구현
        return AlertResult.createFromAnalysis(/* ... */);
    }
}
```

### 알림 조건 확장
```java
// AlertCondition에 새로운 조건 필드 추가
@Embeddable
public class AlertCondition {
    private Integer consecutiveDays;
    private Integer thresholdCount;
    private String keywords;
    private Integer newConditionField;  // 새 조건 추가

    // 새 조건 평가 메서드 추가
    public boolean evaluateNewCondition(/* parameters */) {
        // 새 조건 평가 로직
    }
}
```

### 성능 최적화
```java
// N+1 문제 방지를 위한 fetch join
@Query("SELECT ar FROM AlertRule ar " +
       "JOIN FETCH ar.member m " +
       "LEFT JOIN FETCH m.guardian " +
       "WHERE ar.member.id = :memberId AND ar.isActive = true")
List<AlertRule> findActiveRulesWithMemberAndGuardian(@Param("memberId") Long memberId);

// 배치 처리 최적화
@Transactional
public void batchProcessAlerts(List<AlertResult> results) {
    List<AlertHistory> histories = results.stream()
        .map(this::convertToHistory)
        .toList();
    alertHistoryRepository.saveAll(histories);  // 배치 insert
}
```

### 권한 검증 추가
```java
// 알림 규칙 접근 권한 검증
private void validateRuleAccess(AlertRule alertRule, MemberEntity member) {
    if (!alertRule.getMember().getId().equals(member.getId())) {
        throw new AlertRuleAccessDeniedException(alertRule.getId(), member.getId());
    }
}
```

---

## 🚀 확장 가능성

### 머신러닝 모델 연동
- **AI 기반 이상징후 감지**: TensorFlow/PyTorch 모델 통합
- **개인화 알고리즘**: 개별 회원 패턴 학습 기반 감지
- **예측 모델**: 위험도 예측 및 예방적 알림

### 실시간 처리 강화
- **WebSocket**: 실시간 이상징후 감지 결과 스트리밍
- **Event Sourcing**: 이상징후 감지 이벤트 기반 아키텍처
- **Message Queue**: 대용량 알림 처리를 위한 비동기 처리

### 알림 채널 확장
- **KakaoTalk**: 카카오톡 비즈니스 API 연동
- **LINE**: LINE Notify API 연동
- **Slack**: 팀 워크스페이스 알림 연동
- **Discord**: Discord Webhook 알림

### 분석 고도화
- **다중 패턴 분석**: 여러 이상징후 패턴 조합 분석
- **시계열 분석**: 장기간 패턴 변화 추이 분석
- **컨텍스트 분석**: 날씨, 계절, 이벤트 등 외부 요인 반영

---

**AlertRule API는 MARUNI 플랫폼의 핵심 가치인 '실시간 이상징후 감지 및 보호자 알림'을 구현하는 완성된 시스템입니다. Strategy Pattern과 Facade Pattern을 적용하여 확장 가능하면서도 유지보수가 용이한 구조로 설계되었으며, 3종 분석 알고리즘을 통해 노인들의 안전을 실시간으로 모니터링합니다.** 🚨