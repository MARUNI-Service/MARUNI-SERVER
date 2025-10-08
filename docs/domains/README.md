# 도메인 아키텍처 가이드

**MARUNI 7개 핵심 도메인 구조 및 연동 관계**

## 🏗️ 도메인 계층 구조

```
🔐 Foundation Layer (기반 시스템)
├── Member (회원 관리) ✅
└── Auth (JWT 인증) ✅

💬 Core Service Layer (핵심 서비스)
├── Conversation (AI 대화) ✅
├── DailyCheck (스케줄링) ✅
└── Guardian (보호자 관리) ✅

🚨 Integration Layer (통합/알림)
├── AlertRule (이상징후 감지) ✅
└── Notification (알림 서비스) ✅
```

## 📋 도메인별 문서

### 🔐 Foundation Layer
- **[Member](./member.md)**: 회원 가입, 인증, 정보 관리
- **[Auth](./auth.md)**: JWT Stateless 인증 (Access Token Only)

### 💬 Core Service Layer
- **[Conversation](./conversation.md)**: OpenAI GPT-4o 대화 + 감정 분석
- **[DailyCheck](./dailycheck.md)**: 매일 안부 메시지 자동 발송 + 재시도
- **[Guardian](./guardian.md)**: 보호자 관계 설정 + 알림 설정

### 🚨 Integration Layer
- **[AlertRule](./alertrule.md)**: 3종 이상징후 감지 알고리즘
- **[Notification](./notification.md)**: Firebase FCM + 3중 안전망

## 🔄 핵심 데이터 플로우

### 1. 안부 확인 플로우
```
DailyCheck → Notification → Member
(매일 오전 9시 자동 발송)
```

### 2. 대화 분석 플로우
```
Conversation → AlertRule → Guardian → Notification
(이상징후 감지 → 보호자 알림)
```

### 3. 긴급 상황 플로우
```
AlertRule → Guardian → Notification
(키워드 감지 → 즉시 알림)
```

## 🚀 확장 가이드라인

### 새 도메인 추가 시
1. 계층 결정: Foundation / Core Service / Integration
2. 의존 관계 분석: 기존 도메인과의 연결점 파악
3. TDD Red-Green-Blue 적용
4. 도메인 문서 작성

### 기존 도메인 확장 시
1. 해당 도메인 문서 숙지
2. 의존 도메인 영향도 분석
3. 확장 구현 + 테스트
4. 문서 즉시 업데이트

## ⚠️ 의존 관계 주의사항

- **Foundation → Core/Integration**: ❌ 금지 (역방향 의존)
- **Core → Foundation**: ✅ 허용 (하위 계층 의존)
- **Integration → Foundation/Core**: ✅ 허용 (하위 계층 의존)

**모든 도메인은 TDD + DDD 방법론으로 구축되어 확장성과 유지보수성을 보장합니다.** 🚀
