# 📋 MARUNI Postman API 테스트 - Phase 3: Integration Layer

**작성일**: 2025-10-05
**대상**: 이상징후 감지 시스템 (AlertRule) + 알림 시스템 (Notification)
**난이도**: ⭐⭐⭐ (고급)

---

## 🎯 Phase 3 개요

**Integration Layer**는 MARUNI 프로젝트의 **핵심 가치**인 **이상징후 자동 감지 및 보호자 실시간 알림** 시스템을 테스트합니다.

### 테스트 목표
- ✅ 알림 규칙 생성 및 관리 (CRUD)
- ✅ 3종 이상징후 감지 알고리즘 검증
  - 감정 패턴 분석 (EMOTION_PATTERN)
  - 무응답 패턴 분석 (NO_RESPONSE)
  - 키워드 감지 (KEYWORD_DETECTION)
- ✅ 수동 이상징후 감지 실행 및 결과 확인
- ✅ 알림 이력 추적 및 조회
- ✅ 보호자 알림 발송 시뮬레이션

### 테스트 순서
```
Part A: 알림 규칙 관리 (AlertRule CRUD)
  1. 감정 패턴 알림 규칙 생성
  2. 무응답 패턴 알림 규칙 생성
  3. 키워드 감지 알림 규칙 생성
  4. 알림 규칙 목록 조회
  5. 알림 규칙 상세 조회
  6. 알림 규칙 수정
  7. 알림 규칙 활성화/비활성화

Part B: 이상징후 감지 시스템
  8. 수동 이상징후 감지 실행 (전체 분석)
  9. 감지 결과 확인 및 검증
  10. 알림 이력 조회 (최근 30일)
  11. 특정 알림 규칙 이력 조회

Part C: 통합 시나리오 테스트
  12. 부정 감정 메시지 연속 전송 (자동 감지 트리거)
  13. 긴급 키워드 메시지 전송 (즉시 알림)
  14. 알림 규칙 삭제
```

---

## 🔧 사전 준비

### 1. Phase 1, 2 완료 확인

**필수 환경 변수**:
```
✅ access_token: JWT 인증 토큰
✅ member_id: 로그인한 회원 ID
✅ conversation_id: 대화 세션 ID (Phase 2에서 생성)
✅ guardian_id_family: 가족 보호자 ID (Phase 2에서 생성)
```

**필수 선행 작업**:
```
✅ Phase 2.3: 부정적 감정 메시지 전송 완료
✅ Phase 2.9: 회원에게 보호자 할당 완료
```

### 2. 추가 환경 변수 준비

**Postman Environment에 추가할 변수**:
```
alert_rule_id_emotion: (감정 패턴 규칙 ID, 자동 저장)
alert_rule_id_noresponse: (무응답 패턴 규칙 ID, 자동 저장)
alert_rule_id_keyword: (키워드 감지 규칙 ID, 자동 저장)
```

### 3. AlertRule 도메인 이해

**AlertType (알림 유형)**:
```
EMOTION_PATTERN: 연속적인 부정 감정 패턴 감지
NO_RESPONSE: 일정 기간 무응답 패턴 감지
KEYWORD_DETECTION: 위험 키워드 즉시 감지
```

**AlertLevel (알림 레벨)**:
```
LOW: 낮음 (정보성 알림)
MEDIUM: 보통 (주의 관찰 필요)
HIGH: 높음 (빠른 확인 필요)
EMERGENCY: 긴급 (즉시 대응 필요)
```

---

## 📝 Part A: 알림 규칙 관리 (AlertRule CRUD)

### ✅ Step 3.1: 감정 패턴 알림 규칙 생성 ⭐

**목적**: 3일 연속 부정 감정 감지 시 HIGH 레벨 알림

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "ruleName": "3일 연속 부정감정 감지",
  "ruleDescription": "연속 3일간 부정적 감정이 감지되면 보호자에게 HIGH 레벨 알림을 발송합니다",
  "condition": {
    "consecutiveDays": 3,
    "thresholdCount": null,
    "keywords": null,
    "description": "3일 연속 부정 감정 패턴"
  }
}
```

**Expected Response** (200 OK):
```json
{
  "code": "A001",
  "message": "알림 규칙 생성 성공",
  "data": {
    "id": 1,
    "alertType": "EMOTION_PATTERN",
    "alertLevel": "HIGH",
    "ruleName": "3일 연속 부정감정 감지",
    "ruleDescription": "연속 3일간 부정적 감정이 감지되면 보호자에게 HIGH 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": 3,
      "thresholdCount": null,
      "keywords": null,
      "description": "3일 연속 부정 감정 패턴"
    },
    "isActive": true,
    "createdAt": "2025-10-05T12:00:00",
    "updatedAt": "2025-10-05T12:00:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("감정 패턴 알림 규칙 생성 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // AlertRule ID 저장
    pm.environment.set("alert_rule_id_emotion", jsonData.data.id);

    // 규칙 검증
    pm.expect(jsonData.data.alertType).to.eql("EMOTION_PATTERN");
    pm.expect(jsonData.data.alertLevel).to.eql("HIGH");
    pm.expect(jsonData.data.isActive).to.be.true;
    pm.expect(jsonData.data.condition.consecutiveDays).to.eql(3);

    console.log("✅ 감정 패턴 알림 규칙 생성");
    console.log("Rule ID:", jsonData.data.id);
    console.log("조건: 3일 연속 부정 감정");
    console.log("알림 레벨: HIGH");
});
```

**비즈니스 로직**:
- ✅ 사용자가 3일 연속 부정적 감정(NEGATIVE) 메시지 전송 시
- ✅ EmotionPatternAnalyzer가 자동 감지
- ✅ 보호자에게 HIGH 레벨 알림 자동 발송
- ✅ AlertHistory에 기록 저장

---

### ✅ Step 3.2: 무응답 패턴 알림 규칙 생성

**목적**: 2일 연속 무응답 시 MEDIUM 레벨 알림

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "alertType": "NO_RESPONSE",
  "alertLevel": "MEDIUM",
  "ruleName": "2일 연속 무응답 감지",
  "ruleDescription": "연속 2일간 안부 메시지에 응답하지 않으면 보호자에게 MEDIUM 레벨 알림을 발송합니다",
  "condition": {
    "consecutiveDays": null,
    "thresholdCount": 2,
    "keywords": null,
    "description": "2일 연속 무응답 패턴"
  }
}
```

**Expected Response** (200 OK):
```json
{
  "code": "A001",
  "message": "알림 규칙 생성 성공",
  "data": {
    "id": 2,
    "alertType": "NO_RESPONSE",
    "alertLevel": "MEDIUM",
    "ruleName": "2일 연속 무응답 감지",
    "ruleDescription": "연속 2일간 안부 메시지에 응답하지 않으면 보호자에게 MEDIUM 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": null,
      "thresholdCount": 2,
      "keywords": null,
      "description": "2일 연속 무응답 패턴"
    },
    "isActive": true,
    "createdAt": "2025-10-05T12:05:00",
    "updatedAt": "2025-10-05T12:05:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("무응답 패턴 알림 규칙 생성 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // AlertRule ID 저장
    pm.environment.set("alert_rule_id_noresponse", jsonData.data.id);

    pm.expect(jsonData.data.alertType).to.eql("NO_RESPONSE");
    pm.expect(jsonData.data.alertLevel).to.eql("MEDIUM");
    pm.expect(jsonData.data.condition.thresholdCount).to.eql(2);

    console.log("✅ 무응답 패턴 알림 규칙 생성");
    console.log("Rule ID:", jsonData.data.id);
    console.log("조건: 2일 연속 무응답");
    console.log("알림 레벨: MEDIUM");
});
```

**비즈니스 로직**:
- ✅ DailyCheck 시스템이 매일 오전 9시 안부 메시지 발송
- ✅ 2일 연속 응답 없을 시 NoResponseAnalyzer가 자동 감지
- ✅ 보호자에게 MEDIUM 레벨 알림 자동 발송

---

### ✅ Step 3.3: 키워드 감지 알림 규칙 생성 ⭐⚠️

**목적**: 긴급 키워드 즉시 감지 및 EMERGENCY 레벨 알림

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "alertType": "KEYWORD_DETECTION",
  "alertLevel": "EMERGENCY",
  "ruleName": "긴급 키워드 즉시 감지",
  "ruleDescription": "자살, 죽고싶다, 도와줘 등 긴급 키워드 감지 시 즉시 EMERGENCY 레벨 알림을 발송합니다",
  "condition": {
    "consecutiveDays": null,
    "thresholdCount": null,
    "keywords": "자살,죽고싶다,도와줘,위험,응급",
    "description": "긴급 키워드 즉시 감지"
  }
}
```

**Expected Response** (200 OK):
```json
{
  "code": "A001",
  "message": "알림 규칙 생성 성공",
  "data": {
    "id": 3,
    "alertType": "KEYWORD_DETECTION",
    "alertLevel": "EMERGENCY",
    "ruleName": "긴급 키워드 즉시 감지",
    "ruleDescription": "자살, 죽고싶다, 도와줘 등 긴급 키워드 감지 시 즉시 EMERGENCY 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": null,
      "thresholdCount": null,
      "keywords": "자살,죽고싶다,도와줘,위험,응급",
      "description": "긴급 키워드 즉시 감지"
    },
    "isActive": true,
    "createdAt": "2025-10-05T12:10:00",
    "updatedAt": "2025-10-05T12:10:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("키워드 감지 알림 규칙 생성 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // AlertRule ID 저장
    pm.environment.set("alert_rule_id_keyword", jsonData.data.id);

    pm.expect(jsonData.data.alertType).to.eql("KEYWORD_DETECTION");
    pm.expect(jsonData.data.alertLevel).to.eql("EMERGENCY");
    pm.expect(jsonData.data.condition.keywords).to.exist;

    console.log("✅ 키워드 감지 알림 규칙 생성");
    console.log("Rule ID:", jsonData.data.id);
    console.log("감지 키워드:", jsonData.data.condition.keywords);
    console.log("알림 레벨: EMERGENCY (즉시 발송)");
});
```

**비즈니스 로직**:
- ⚠️ **실시간 감지**: 메시지 전송 즉시 KeywordAnalyzer 작동
- ⚠️ **최우선 처리**: EMERGENCY 레벨로 즉시 알림 발송
- ⚠️ **보호자 즉시 대응**: 모든 알림 채널 동시 활용

**키워드 목록** (쉼표로 구분):
```
자살, 죽고싶다, 도와줘, 위험, 응급, 고통, 견딜수없어
```

---

### ✅ Step 3.4: 알림 규칙 목록 조회

**목적**: 생성한 모든 알림 규칙 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/alert-rules`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "A002",
  "message": "알림 규칙 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "alertType": "EMOTION_PATTERN",
      "alertLevel": "HIGH",
      "ruleName": "3일 연속 부정감정 감지",
      "ruleDescription": "연속 3일간 부정적 감정이 감지되면 보호자에게 HIGH 레벨 알림을 발송합니다",
      "condition": {
        "consecutiveDays": 3,
        "thresholdCount": null,
        "keywords": null,
        "description": "3일 연속 부정 감정 패턴"
      },
      "isActive": true,
      "createdAt": "2025-10-05T12:00:00",
      "updatedAt": "2025-10-05T12:00:00"
    },
    {
      "id": 2,
      "alertType": "NO_RESPONSE",
      "alertLevel": "MEDIUM",
      "ruleName": "2일 연속 무응답 감지",
      "ruleDescription": "연속 2일간 안부 메시지에 응답하지 않으면 보호자에게 MEDIUM 레벨 알림을 발송합니다",
      "condition": {
        "consecutiveDays": null,
        "thresholdCount": 2,
        "keywords": null,
        "description": "2일 연속 무응답 패턴"
      },
      "isActive": true,
      "createdAt": "2025-10-05T12:05:00",
      "updatedAt": "2025-10-05T12:05:00"
    },
    {
      "id": 3,
      "alertType": "KEYWORD_DETECTION",
      "alertLevel": "EMERGENCY",
      "ruleName": "긴급 키워드 즉시 감지",
      "ruleDescription": "자살, 죽고싶다, 도와줘 등 긴급 키워드 감지 시 즉시 EMERGENCY 레벨 알림을 발송합니다",
      "condition": {
        "consecutiveDays": null,
        "thresholdCount": null,
        "keywords": "자살,죽고싶다,도와줘,위험,응급",
        "description": "긴급 키워드 즉시 감지"
      },
      "isActive": true,
      "createdAt": "2025-10-05T12:10:00",
      "updatedAt": "2025-10-05T12:10:00"
    }
  ]
}
```

**Postman Tests Script**:
```javascript
pm.test("알림 규칙 목록 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // 3개 규칙 확인
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.at.least(3);

    console.log("✅ 알림 규칙 목록:", jsonData.data.length, "개");
    jsonData.data.forEach(rule => {
        console.log(`- [${rule.alertLevel}] ${rule.ruleName} (${rule.alertType})`);
    });
});
```

---

### ✅ Step 3.5: 알림 규칙 상세 조회

**목적**: 특정 알림 규칙의 상세 정보 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/alert-rules/{{alert_rule_id_emotion}}`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "A002",
  "message": "알림 규칙 조회 성공",
  "data": {
    "id": 1,
    "alertType": "EMOTION_PATTERN",
    "alertLevel": "HIGH",
    "ruleName": "3일 연속 부정감정 감지",
    "ruleDescription": "연속 3일간 부정적 감정이 감지되면 보호자에게 HIGH 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": 3,
      "thresholdCount": null,
      "keywords": null,
      "description": "3일 연속 부정 감정 패턴"
    },
    "isActive": true,
    "createdAt": "2025-10-05T12:00:00",
    "updatedAt": "2025-10-05T12:00:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("알림 규칙 상세 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    const savedRuleId = pm.environment.get("alert_rule_id_emotion");
    pm.expect(jsonData.data.id).to.eql(parseInt(savedRuleId));

    console.log("✅ 알림 규칙 상세 정보:", jsonData.data.ruleName);
});
```

---

### ✅ Step 3.6: 알림 규칙 수정

**목적**: 알림 규칙 조건 변경 (3일 → 2일)

**HTTP Method**: `PUT`
**URL**: `{{base_url}}/api/alert-rules/{{alert_rule_id_emotion}}`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "ruleName": "2일 연속 부정감정 감지 (수정됨)",
  "ruleDescription": "연속 2일간 부정적 감정이 감지되면 보호자에게 HIGH 레벨 알림을 발송합니다",
  "alertLevel": "HIGH",
  "condition": {
    "consecutiveDays": 2,
    "thresholdCount": null,
    "keywords": null,
    "description": "2일 연속 부정 감정 패턴 (수정됨)"
  }
}
```

**Expected Response** (200 OK):
```json
{
  "code": "A003",
  "message": "알림 규칙 수정 성공",
  "data": {
    "id": 1,
    "alertType": "EMOTION_PATTERN",
    "alertLevel": "HIGH",
    "ruleName": "2일 연속 부정감정 감지 (수정됨)",
    "ruleDescription": "연속 2일간 부정적 감정이 감지되면 보호자에게 HIGH 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": 2,
      "thresholdCount": null,
      "keywords": null,
      "description": "2일 연속 부정 감정 패턴 (수정됨)"
    },
    "isActive": true,
    "createdAt": "2025-10-05T12:00:00",
    "updatedAt": "2025-10-05T12:20:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("알림 규칙 수정 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.data.condition.consecutiveDays).to.eql(2);
    pm.expect(jsonData.data.ruleName).to.include("수정됨");

    console.log("✅ 알림 규칙 수정 완료");
    console.log("변경 전: 3일 연속 → 변경 후: 2일 연속");
});
```

---

### ✅ Step 3.7: 알림 규칙 활성화/비활성화

**목적**: 알림 규칙 일시 중지 (비활성화)

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules/{{alert_rule_id_keyword}}/toggle?active=false`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Request Body**: 없음

**Expected Response** (200 OK):
```json
{
  "code": "A004",
  "message": "알림 규칙 비활성화 성공",
  "data": {
    "id": 3,
    "alertType": "KEYWORD_DETECTION",
    "alertLevel": "EMERGENCY",
    "ruleName": "긴급 키워드 즉시 감지",
    "ruleDescription": "자살, 죽고싶다, 도와줘 등 긴급 키워드 감지 시 즉시 EMERGENCY 레벨 알림을 발송합니다",
    "condition": {
      "consecutiveDays": null,
      "thresholdCount": null,
      "keywords": "자살,죽고싶다,도와줘,위험,응급",
      "description": "긴급 키워드 즉시 감지"
    },
    "isActive": false,
    "createdAt": "2025-10-05T12:10:00",
    "updatedAt": "2025-10-05T12:25:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("알림 규칙 비활성화 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.data.isActive).to.be.false;

    console.log("✅ 알림 규칙 비활성화 완료");
    console.log("이제 이 규칙은 이상징후 감지에 적용되지 않음");
});
```

**재활성화**:
```http
POST {{base_url}}/api/alert-rules/{{alert_rule_id_keyword}}/toggle?active=true
```

---

## 📝 Part B: 이상징후 감지 시스템

### ✅ Step 3.8: 수동 이상징후 감지 실행 (전체 분석) ⭐⚠️

**목적**: 3종 알고리즘으로 현재 회원 상태 종합 분석

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules/detect`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Request Body**: 없음

**Expected Response** (200 OK):
```json
{
  "code": "A005",
  "message": "이상징후 감지 완료",
  "data": {
    "detectionTimestamp": "2025-10-05T12:30:00",
    "totalAlertsDetected": 1,
    "alerts": [
      {
        "alertType": "EMOTION_PATTERN",
        "alertLevel": "HIGH",
        "alertMessage": "2일 연속 부정적 감정이 감지되었습니다",
        "detectionDetails": {
          "consecutiveDays": 2,
          "negativeEmotionCount": 2,
          "analysisStartDate": "2025-10-04",
          "analysisEndDate": "2025-10-05",
          "detectedMessages": [
            {
              "date": "2025-10-04",
              "content": "오늘 너무 슬프고 우울해요...",
              "emotion": "NEGATIVE"
            },
            {
              "date": "2025-10-05",
              "content": "여전히 힘들어요...",
              "emotion": "NEGATIVE"
            }
          ]
        },
        "ruleId": 1,
        "ruleName": "2일 연속 부정감정 감지 (수정됨)",
        "notificationSent": true,
        "guardianNotified": [
          {
            "guardianId": 1,
            "guardianName": "김보호_수정",
            "notificationChannel": "PUSH",
            "sentAt": "2025-10-05T12:30:02"
          }
        ]
      }
    ],
    "summary": {
      "emotionPattern": {
        "analyzed": true,
        "alertTriggered": true,
        "consecutiveDays": 2
      },
      "noResponse": {
        "analyzed": true,
        "alertTriggered": false,
        "message": "정상 응답 패턴"
      },
      "keywordDetection": {
        "analyzed": false,
        "alertTriggered": false,
        "message": "규칙 비활성화됨"
      }
    }
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("수동 이상징후 감지 실행 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.data.alerts).to.be.an('array');

    console.log("✅ 이상징후 감지 완료");
    console.log("감지된 알림:", jsonData.data.totalAlertsDetected, "건");

    if (jsonData.data.totalAlertsDetected > 0) {
        jsonData.data.alerts.forEach(alert => {
            console.log(`⚠️ [${alert.alertLevel}] ${alert.alertMessage}`);
            console.log(`   - 알림 발송: ${alert.notificationSent ? '완료' : '실패'}`);
            if (alert.guardianNotified) {
                alert.guardianNotified.forEach(guardian => {
                    console.log(`   - 보호자 ${guardian.guardianName}에게 ${guardian.notificationChannel} 알림 발송`);
                });
            }
        });
    }

    console.log("\n📊 분석 요약:");
    console.log("- 감정 패턴 분석:", jsonData.data.summary.emotionPattern.analyzed ? '완료' : '미실행');
    console.log("- 무응답 패턴 분석:", jsonData.data.summary.noResponse.analyzed ? '완료' : '미실행');
    console.log("- 키워드 감지:", jsonData.data.summary.keywordDetection.analyzed ? '완료' : '미실행');
});
```

**비즈니스 로직**:
1. **EmotionPatternAnalyzer**: Phase 2에서 전송한 부정 감정 메시지 분석
2. **NoResponseAnalyzer**: DailyCheck 기록 기반 무응답 패턴 분석
3. **KeywordAnalyzer**: 비활성화 상태 (Step 3.7에서 비활성화)
4. **보호자 알림 발송**: Phase 2에서 할당한 보호자에게 자동 발송
5. **AlertHistory 기록**: 감지 이력 자동 저장

---

### ✅ Step 3.9: 감지 결과 확인 및 검증

**목적**: 감지된 알림의 상세 정보 검증

**수동 검증 항목**:

```javascript
// Response에서 확인할 항목
1. ✅ totalAlertsDetected > 0
2. ✅ alerts 배열에 EMOTION_PATTERN 포함
3. ✅ alertLevel = "HIGH"
4. ✅ notificationSent = true
5. ✅ guardianNotified 배열에 보호자 정보 포함
6. ✅ summary.emotionPattern.alertTriggered = true
```

**예상 시나리오**:
- ✅ Phase 2.3에서 부정 감정 메시지 전송
- ✅ Step 3.1에서 2일 연속 부정 감정 규칙 생성 (수정 후)
- ✅ 추가 부정 감정 메시지 전송 필요 시 Phase 2.3 반복
- ✅ Step 3.8 실행 시 감정 패턴 감지

---

### ✅ Step 3.10: 알림 이력 조회 (최근 30일) ⭐

**목적**: 발송된 모든 알림 이력 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/alert-rules/history?days=30`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "A006",
  "message": "알림 이력 조회 성공",
  "data": [
    {
      "id": 1,
      "alertRuleId": 1,
      "alertRuleName": "2일 연속 부정감정 감지 (수정됨)",
      "alertType": "EMOTION_PATTERN",
      "alertLevel": "HIGH",
      "alertMessage": "2일 연속 부정적 감정이 감지되었습니다",
      "detectionDetails": "{\"consecutiveDays\":2,\"negativeEmotionCount\":2}",
      "isNotificationSent": true,
      "notificationSentAt": "2025-10-05T12:30:02",
      "notificationResult": "보호자 '김보호_수정'에게 PUSH 알림 발송 완료",
      "alertDate": "2025-10-05T00:00:00",
      "createdAt": "2025-10-05T12:30:00"
    }
  ]
}
```

**Postman Tests Script**:
```javascript
pm.test("알림 이력 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.data).to.be.an('array');

    console.log("✅ 알림 이력:", jsonData.data.length, "건");

    jsonData.data.forEach((history, index) => {
        console.log(`\n📋 알림 ${index + 1}:`);
        console.log(`- 유형: ${history.alertType}`);
        console.log(`- 레벨: ${history.alertLevel}`);
        console.log(`- 메시지: ${history.alertMessage}`);
        console.log(`- 발송 여부: ${history.isNotificationSent ? '성공' : '실패'}`);
        if (history.isNotificationSent) {
            console.log(`- 발송 시간: ${history.notificationSentAt}`);
            console.log(`- 발송 결과: ${history.notificationResult}`);
        }
    });
});
```

**필터링 옵션**:
```http
# 최근 7일
GET {{base_url}}/api/alert-rules/history?days=7

# 최근 1일
GET {{base_url}}/api/alert-rules/history?days=1
```

---

### ✅ Step 3.11: 특정 알림 규칙 이력 조회

**목적**: 특정 알림 규칙의 감지 이력만 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/alert-rules/{{alert_rule_id_emotion}}/history?days=30`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "A006",
  "message": "알림 규칙별 이력 조회 성공",
  "data": [
    {
      "id": 1,
      "alertRuleId": 1,
      "alertRuleName": "2일 연속 부정감정 감지 (수정됨)",
      "alertType": "EMOTION_PATTERN",
      "alertLevel": "HIGH",
      "alertMessage": "2일 연속 부정적 감정이 감지되었습니다",
      "detectionDetails": "{\"consecutiveDays\":2,\"negativeEmotionCount\":2}",
      "isNotificationSent": true,
      "notificationSentAt": "2025-10-05T12:30:02",
      "notificationResult": "보호자 '김보호_수정'에게 PUSH 알림 발송 완료",
      "alertDate": "2025-10-05T00:00:00",
      "createdAt": "2025-10-05T12:30:00"
    }
  ]
}
```

**Postman Tests Script**:
```javascript
pm.test("특정 알림 규칙 이력 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    const savedRuleId = pm.environment.get("alert_rule_id_emotion");

    jsonData.data.forEach(history => {
        pm.expect(history.alertRuleId).to.eql(parseInt(savedRuleId));
    });

    console.log("✅ 알림 규칙 이력:", jsonData.data.length, "건");
});
```

---

## 📝 Part C: 통합 시나리오 테스트

### ✅ Step 3.12: 부정 감정 메시지 연속 전송 (자동 감지 트리거)

**목적**: 실제 이상징후 감지 플로우 시뮬레이션

**시나리오**:
1. 오늘 부정 감정 메시지 전송
2. 내일 부정 감정 메시지 전송 (시뮬레이션)
3. 자동 감지 실행
4. 보호자 알림 발송

**Step 1: 첫 번째 부정 감정 메시지**

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/conversations/messages`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "content": "오늘도 힘들고 외로워요. 아무도 찾아오지 않네요."
}
```

**Step 2: 두 번째 부정 감정 메시지 (다음날 시뮬레이션)**

**Request Body**:
```json
{
  "content": "여전히 우울하고 슬퍼요. 혼자 있으니 너무 답답해요."
}
```

**Step 3: 수동 감지 실행**

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules/detect`

**Expected Result**:
- ✅ EMOTION_PATTERN 알림 감지
- ✅ HIGH 레벨 알림 발송
- ✅ 보호자에게 자동 알림

**Postman Tests Script**:
```javascript
pm.test("부정 감정 연속 감지 및 알림 발송 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    const emotionAlert = jsonData.data.alerts.find(a => a.alertType === "EMOTION_PATTERN");
    pm.expect(emotionAlert).to.exist;
    pm.expect(emotionAlert.notificationSent).to.be.true;

    console.log("✅ 부정 감정 패턴 감지 및 보호자 알림 완료");
    console.log("감지 조건: 2일 연속 부정 감정");
    console.log("알림 레벨: HIGH");
});
```

---

### ✅ Step 3.13: 긴급 키워드 메시지 전송 (즉시 알림) ⚠️⚠️

**목적**: 키워드 감지 즉시 알림 시스템 검증

**사전 작업**: Step 3.7에서 비활성화한 키워드 규칙 재활성화

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules/{{alert_rule_id_keyword}}/toggle?active=true`

**긴급 키워드 메시지 전송**:

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/conversations/messages`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "content": "너무 힘들어서 죽고싶어요. 도와주세요."
}
```

**Expected Response** (200 OK):
```json
{
  "code": "SUCCESS",
  "message": "메시지 전송 성공",
  "data": {
    "conversationId": 1,
    "userMessage": {
      "id": 15,
      "type": "USER_MESSAGE",
      "content": "너무 힘들어서 죽고싶어요. 도와주세요.",
      "emotion": "NEGATIVE",
      "createdAt": "2025-10-05T13:00:00"
    },
    "aiMessage": {
      "id": 16,
      "type": "AI_RESPONSE",
      "content": "지금 많이 힘드신 것 같아요. 잠시만 기다려주세요. 도움을 요청하고 있습니다.",
      "emotion": "NEUTRAL",
      "createdAt": "2025-10-05T13:00:03"
    }
  }
}
```

**자동 감지 실행 (즉시)**:

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/alert-rules/detect`

**Expected Response**:
```json
{
  "code": "A005",
  "message": "이상징후 감지 완료",
  "data": {
    "totalAlertsDetected": 2,
    "alerts": [
      {
        "alertType": "KEYWORD_DETECTION",
        "alertLevel": "EMERGENCY",
        "alertMessage": "긴급 키워드가 감지되었습니다: 죽고싶어요, 도와주세요",
        "detectionDetails": {
          "detectedKeywords": ["죽고싶어요", "도와주세요"],
          "messageContent": "너무 힘들어서 죽고싶어요. 도와주세요.",
          "detectionTime": "2025-10-05T13:00:00"
        },
        "ruleId": 3,
        "ruleName": "긴급 키워드 즉시 감지",
        "notificationSent": true,
        "guardianNotified": [
          {
            "guardianId": 1,
            "guardianName": "김보호_수정",
            "notificationChannel": "PUSH",
            "sentAt": "2025-10-05T13:00:01"
          }
        ]
      },
      {
        "alertType": "EMOTION_PATTERN",
        "alertLevel": "HIGH",
        "alertMessage": "2일 연속 부정적 감정이 감지되었습니다",
        "notificationSent": true
      }
    ]
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("긴급 키워드 즉시 감지 및 EMERGENCY 알림 발송", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    const keywordAlert = jsonData.data.alerts.find(a => a.alertType === "KEYWORD_DETECTION");
    pm.expect(keywordAlert).to.exist;
    pm.expect(keywordAlert.alertLevel).to.eql("EMERGENCY");
    pm.expect(keywordAlert.notificationSent).to.be.true;

    console.log("⚠️⚠️ 긴급 키워드 감지!");
    console.log("감지된 키워드:", keywordAlert.detectionDetails.detectedKeywords);
    console.log("알림 레벨: EMERGENCY (최우선 처리)");
    console.log("보호자 알림: 즉시 발송 완료");
});
```

**중요**:
- ⚠️⚠️ **실제 운영 시**: 긴급 키워드 감지 시 전문 상담사 연결 시스템 연동 필요
- ⚠️⚠️ **응급 상황**: 119 자동 신고 시스템 연동 고려

---

### ✅ Step 3.14: 알림 규칙 삭제

**목적**: 테스트 후 알림 규칙 정리

**HTTP Method**: `DELETE`
**URL**: `{{base_url}}/api/alert-rules/{{alert_rule_id_emotion}}`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "A007",
  "message": "알림 규칙 삭제 성공",
  "data": null
}
```

**Postman Tests Script**:
```javascript
pm.test("알림 규칙 삭제 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql("A007");

    console.log("✅ 알림 규칙 삭제 완료");
});
```

**참고**:
- ⚠️ 알림 규칙 삭제 시 연결된 AlertHistory는 삭제되지 않음 (이력 보존)
- ⚠️ 삭제된 규칙은 이상징후 감지에 더 이상 적용되지 않음

---

## 📊 Phase 3 완료 체크리스트

### Part A: 알림 규칙 관리

```
[ ] Step 3.1: 감정 패턴 알림 규칙 생성 ⭐
[ ] Step 3.2: 무응답 패턴 알림 규칙 생성
[ ] Step 3.3: 키워드 감지 알림 규칙 생성 ⭐⚠️
[ ] Step 3.4: 알림 규칙 목록 조회
[ ] Step 3.5: 알림 규칙 상세 조회
[ ] Step 3.6: 알림 규칙 수정
[ ] Step 3.7: 알림 규칙 활성화/비활성화
```

### Part B: 이상징후 감지 시스템

```
[ ] Step 3.8: 수동 이상징후 감지 실행 ⭐⚠️
[ ] Step 3.9: 감지 결과 확인 및 검증
[ ] Step 3.10: 알림 이력 조회 (최근 30일) ⭐
[ ] Step 3.11: 특정 알림 규칙 이력 조회
```

### Part C: 통합 시나리오 테스트

```
[ ] Step 3.12: 부정 감정 메시지 연속 전송 (자동 감지 트리거)
[ ] Step 3.13: 긴급 키워드 메시지 전송 (즉시 알림) ⚠️⚠️
[ ] Step 3.14: 알림 규칙 삭제
```

### 환경 변수 확인

```
[ ] alert_rule_id_emotion: 저장되어 있음
[ ] alert_rule_id_noresponse: 저장되어 있음
[ ] alert_rule_id_keyword: 저장되어 있음
```

### 검증 항목

```
[ ] 3종 알림 규칙 생성 성공
[ ] 이상징후 감지 알고리즘 정상 작동
[ ] 보호자 알림 발송 성공
[ ] 알림 이력 저장 및 조회 정상
[ ] 긴급 키워드 즉시 감지 작동
[ ] 알림 규칙 활성화/비활성화 정상
```

---

## 🎯 전체 플로우 종합 테스트

### 완전한 엔드투엔드 시나리오

**1단계: 회원 준비 (Phase 1)**
```
✅ 회원가입
✅ 로그인 (JWT 토큰 발급)
✅ 내 정보 조회
```

**2단계: 대화 및 보호자 설정 (Phase 2)**
```
✅ AI 대화 시작 (대화 세션 생성)
✅ 부정 감정 메시지 전송 (Day 1)
✅ 보호자 생성 (가족)
✅ 회원에게 보호자 할당
```

**3단계: 알림 규칙 설정 (Phase 3)**
```
✅ 감정 패턴 알림 규칙 생성 (2일 연속)
✅ 키워드 감지 알림 규칙 생성
```

**4단계: 이상징후 발생 및 감지**
```
✅ 부정 감정 메시지 전송 (Day 2)
✅ 수동 이상징후 감지 실행
✅ EMOTION_PATTERN 알림 감지
✅ 보호자에게 HIGH 레벨 알림 자동 발송
```

**5단계: 긴급 상황 대응**
```
✅ 긴급 키워드 메시지 전송 ("죽고싶어요")
✅ 즉시 감지 실행
✅ EMERGENCY 레벨 알림 즉시 발송
✅ 알림 이력 조회 및 확인
```

**예상 결과**:
- ✅ 총 2건의 알림 감지 (EMOTION_PATTERN + KEYWORD_DETECTION)
- ✅ 보호자에게 총 2건의 알림 발송
- ✅ AlertHistory에 2건의 이력 저장

---

## 🔍 문제 해결 가이드

### 1. 이상징후가 감지되지 않음

**원인**:
- 알림 규칙 조건 미충족
- 알림 규칙 비활성화 상태
- 대화 데이터 부족

**해결**:
```bash
# 1. 알림 규칙 활성화 확인
GET /api/alert-rules
→ isActive: true 확인

# 2. 부정 감정 메시지 추가 전송
POST /api/conversations/messages
{
  "content": "우울하고 힘들어요"
}

# 3. 조건 완화 (3일 → 2일)
PUT /api/alert-rules/{id}
{
  "condition": {
    "consecutiveDays": 2
  }
}

# 4. 수동 감지 재실행
POST /api/alert-rules/detect
```

### 2. 보호자 알림이 발송되지 않음

**원인**:
- 보호자 할당 안 됨
- Notification 서비스 오류

**해결**:
```bash
# 1. 보호자 할당 확인
GET /api/guardians/{guardianId}/members
→ 회원 ID 포함 여부 확인

# 2. 보호자 재할당
POST /api/guardians/{guardianId}/members/{memberId}

# 3. 서버 로그 확인
docker-compose logs -f app | grep "Notification"
```

### 3. 키워드 감지가 작동하지 않음

**원인**:
- 키워드 규칙 비활성화
- 키워드 대소문자/띄어쓰기 불일치

**해결**:
```bash
# 1. 키워드 규칙 활성화 확인
POST /api/alert-rules/{id}/toggle?active=true

# 2. 키워드 목록 확인
GET /api/alert-rules/{id}
→ condition.keywords 확인

# 3. 정확한 키워드 사용
"죽고싶다" → "죽고싶어요" (변형 포함)
```

### 4. AlertHistory가 저장되지 않음

**원인**:
- 중복 알림 방지 (동일 날짜 중복 발송 차단)
- DB 제약 조건

**해결**:
```bash
# 1. 기존 이력 확인
GET /api/alert-rules/history?days=1

# 2. 다음날 시뮬레이션
# (테스트 환경에서는 시간 변경 또는 다른 알림 규칙 사용)

# 3. DB 제약 조건 확인
# alert_history 테이블의 unique constraint:
# (member_id, alert_rule_id, alert_date)
```

---

## 📚 참고 문서

- **AlertRule 도메인**: `docs/domains/alertrule.md`
- **Conversation 도메인**: `docs/domains/conversation.md`
- **Guardian 도메인**: `docs/domains/guardian.md`
- **Notification 도메인**: `docs/domains/notification.md`
- **API 설계 가이드**: `docs/specifications/api-design-guide.md`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

---

## 🎉 전체 테스트 완료

**축하합니다! MARUNI 프로젝트의 모든 API 테스트를 완료했습니다.**

### 완료된 테스트 범위

**Phase 1: Foundation Layer (7개 API)**
- ✅ 회원 관리 시스템 (Member)
- ✅ JWT 인증 시스템 (Auth)

**Phase 2: Core Service Layer (12개 API)**
- ✅ AI 대화 시스템 (Conversation)
- ✅ 보호자 관리 시스템 (Guardian)

**Phase 3: Integration Layer (14개 API)**
- ✅ 이상징후 감지 시스템 (AlertRule)
- ✅ 알림 발송 시스템 (Notification)

**총 33개 API 엔드포인트 테스트 완료!**

### 다음 단계

**1. Postman Collection 내보내기**
```
File → Export → Collection v2.1
→ MARUNI_API_Test_Collection.json
```

**2. Newman으로 자동화 테스트**
```bash
npm install -g newman
newman run MARUNI_API_Test_Collection.json \
  --environment MARUNI_Local.postman_environment.json
```

**3. CI/CD 통합**
```yaml
# .github/workflows/api-test.yml
- name: Run API Tests
  run: newman run postman/MARUNI_API_Test_Collection.json
```

---

**Phase 3 Integration Layer 테스트 문서 v1.0.0** | 작성: 2025-10-05
