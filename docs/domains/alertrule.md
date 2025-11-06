# AlertRule 도메인

**최종 업데이트**: 2025-10-09
**상태**: ✅ Phase 1 완료 (3종 알고리즘 구현)

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

## 🌐 REST API

### 1. 알림 규칙 생성
```
POST /api/alert-rules
Headers: Authorization: Bearer {JWT}
Body: {
  "alertType": "EMOTION_PATTERN",
  "alertLevel": "HIGH",
  "condition": { ... }
}
```

### 2. 알림 규칙 목록 조회
```
GET /api/alert-rules
```

### 3. 알림 규칙 수정
```
PUT /api/alert-rules/{id}
```

### 4. 알림 규칙 삭제
```
DELETE /api/alert-rules/{id}
```

### 5. 알림 규칙 활성화/비활성화
```
POST /api/alert-rules/{id}/toggle?active=true
```

### 6. 알림 이력 조회
```
GET /api/alert-rules/history?days=30
```

### 7. 알림 상세 조회 ✅ 신규
```
GET /api/alert-rules/history/{alertId}
```

### 8. 수동 이상징후 감지
```
POST /api/alert-rules/detect
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
    └── controller/           # AlertRuleController (8개 API)
```

## ✅ 완성도

- [x] 3종 감지 알고리즘
- [x] Strategy Pattern 적용
- [x] 알림 규칙 CRUD
- [x] 알림 이력 관리
- [x] REST API (8개)
- [x] 보호자 알림 연동
- [x] JWT 인증

**상용 서비스 수준 완성**
