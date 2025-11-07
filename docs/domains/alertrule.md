# AlertRule 도메인

**최종 업데이트**: 2025-11-07
**상태**: ✅ Phase 2 완료 (3종 알고리즘 + Strategy Pattern)

## 📋 개요

이상징후 감지 및 알림 규칙 관리 도메인입니다. 3가지 분석 알고리즘으로 노인의 감정 패턴, 무응답, 위험 키워드를 감지합니다.

### 핵심 기능
- 감정 패턴 분석 (연속 부정 감정 감지)
- 무응답 패턴 분석 (응답률 모니터링)
- 키워드 감지 (긴급/경고 키워드)
- 알림 규칙 CRUD
- 알림 이력 관리

## 🏗️ 주요 엔티티

### AlertRule
```java
- id: Long
- member: MemberEntity
- alertType: AlertType         // EMOTION_PATTERN, NO_RESPONSE, KEYWORD_DETECTION
- alertLevel: AlertLevel       // EMERGENCY, HIGH, MEDIUM, LOW
- condition: AlertCondition    // 감지 조건
- isActive: Boolean
```

### AlertHistory
```java
- id: Long
- alertRule: AlertRule
- member: MemberEntity
- alertLevel: AlertLevel
- alertMessage: String         // 알림 메시지
- detectionDetails: String     // 감지 상세 정보 (JSON)
- isNotificationSent: Boolean
- alertDate: LocalDateTime
```

### AlertType (Enum)
- `EMOTION_PATTERN`: 감정 패턴 분석
- `NO_RESPONSE`: 무응답 패턴 분석
- `KEYWORD_DETECTION`: 키워드 감지

### AlertLevel (Enum)
- `EMERGENCY`: 긴급 (즉시 알림)
- `HIGH`: 높음
- `MEDIUM`: 중간
- `LOW`: 낮음

## 🌐 REST API (9개)

### 알림 규칙 관리

#### 1. 알림 규칙 생성
```
POST /api/alert-rules
Headers: Authorization: Bearer {JWT}
Body: {
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "condition": {
    "consecutiveDays": 3,
    "thresholdCount": null,
    "keywords": null
  }
}
```

#### 2. 알림 규칙 목록 조회
```
GET /api/alert-rules
Headers: Authorization: Bearer {JWT}

Response: [
  {
    "id": 1,
    "alertType": "EMOTION_PATTERN",
    "alertLevel": "HIGH",
    "ruleName": "연속 부정 감정 감지",
    "description": "3일 연속 부정 감정",
    "isActive": true,
    "condition": { ... }
  }
]
```

#### 3. 알림 규칙 상세 조회
```
GET /api/alert-rules/{id}
Headers: Authorization: Bearer {JWT}

Response: {
  "id": 1,
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "ruleName": "연속 부정 감정 감지",
  "description": "3일 연속 부정 감정",
  "isActive": true,
  "condition": { ... }
}
```

#### 4. 알림 규칙 수정
```
PUT /api/alert-rules/{id}
Headers: Authorization: Bearer {JWT}
Body: {
  "ruleName": "수정된 규칙 이름",
  "description": "수정된 설명",
  "alertLevel": "MEDIUM"
}
```

#### 5. 알림 규칙 삭제
```
DELETE /api/alert-rules/{id}
Headers: Authorization: Bearer {JWT}
```

#### 6. 알림 규칙 활성화/비활성화
```
POST /api/alert-rules/{id}/toggle?active=true
Headers: Authorization: Bearer {JWT}

Response: {
  "id": 1,
  "isActive": true,
  ...
}
```

### 알림 이력 관리

#### 7. 알림 이력 조회
```
GET /api/alert-rules/history?days=30
Headers: Authorization: Bearer {JWT}

Response: [
  {
    "id": 1,
    "alertLevel": "HIGH",
    "alertMessage": "3일 연속 부정 감정 감지",
    "detectionDetails": "{...}",
    "alertDate": "2025-11-07T10:00:00",
    "isNotificationSent": true
  }
]
```

#### 8. 알림 상세 조회
```
GET /api/alert-rules/history/{alertId}
Headers: Authorization: Bearer {JWT}

Response: {
  "id": 1,
  "alertLevel": "HIGH",
  "alertMessage": "3일 연속 부정 감정 감지",
  "detectionDetails": "{...}",
  "alertDate": "2025-11-07T10:00:00",
  "isNotificationSent": true
}
```

### 이상징후 감지

#### 9. 수동 이상징후 감지
```
POST /api/alert-rules/detect
Headers: Authorization: Bearer {JWT}

Response: {
  "memberId": 1,
  "detectionResults": [
    {
      "alertType": "EMOTION_PATTERN",
      "detected": true,
      "alertLevel": "HIGH",
      "message": "3일 연속 부정 감정 감지"
    }
  ]
}
```

## 🔧 핵심 서비스

### AlertDetectionService
- `detectAnomalies(memberId)`: 전체 이상징후 감지 (3종 알고리즘)
- `detectKeywordAlert(memberId, message)`: 키워드 즉시 감지

### AlertHistoryService
- `recordAlertHistory(alertRule, member, result)`: 알림 이력 기록
- `getRecentAlertHistory(memberId, days)`: 최근 알림 이력 조회
- `getAlertDetail(alertId, memberId)`: 알림 상세 조회 ✅ 신규

### AlertNotificationService
- `triggerAlert(memberId, alertResult)`: 알림 발송 트리거

## 🎯 3종 감지 알고리즘

### 1. EmotionPatternAnalyzer
```
분석 지표:
- 최근 N일간 NEGATIVE 메시지 비율
- 연속 부정 감정 일수

감지 조건:
- HIGH: 3일 연속 부정 OR 70% 이상 부정
- MEDIUM: 2일 연속 부정 OR 50% 이상 부정
```

### 2. NoResponseAnalyzer
```
분석 지표:
- 최근 N일간 응답률
- 연속 무응답 일수

감지 조건:
- HIGH: 3일 연속 무응답 OR 응답률 30% 미만
- MEDIUM: 2일 연속 무응답 OR 응답률 50% 미만
```

### 3. KeywordAnalyzer
```
긴급 키워드 (EMERGENCY):
- 죽고싶다, 자살, 죽음, 살기싫다

경고 키워드 (HIGH):
- 우울, 외롭다, 힘들다, 슬프다, 고독, 아프다
```

## 🔗 도메인 연동

- **Conversation**: 대화 메시지 분석 → 키워드 감지
- **DailyCheck**: 응답 패턴 분석 → 무응답 감지
- **Guardian**: 보호자 알림 발송
- **Notification**: NotificationHistoryService를 통한 알림 이력 저장 및 조회

## 📁 패키지 구조

```
alertrule/
├── application/
│   ├── dto/
│   ├── service/core/         # AlertDetectionService, AlertHistoryService, etc.
│   ├── service/orchestrator/ # AlertAnalysisOrchestrator
│   └── analyzer/strategy/    # 3종 분석기
├── domain/
│   ├── entity/               # AlertRule, AlertHistory
│   └── repository/
└── presentation/
    └── controller/           # AlertRuleController (9개 API)
```

## ✅ 완성도

- [x] 3종 감지 알고리즘 (감정 패턴, 무응답, 키워드)
- [x] Strategy Pattern 적용 (analyzer/strategy/)
- [x] 알림 규칙 CRUD (생성, 조회, 수정, 삭제, 활성화/비활성화)
- [x] 알림 이력 관리 (AlertHistory 영속화)
- [x] REST API (9개: 규칙 관리 6개 + 이력 관리 2개 + 수동 감지 1개)
- [x] 보호자 알림 연동 (Guardian 도메인)
- [x] Notification 연동 (알림 이력 저장)
- [x] JWT 인증
- [x] TDD 테스트

**상용 서비스 수준 완성**
