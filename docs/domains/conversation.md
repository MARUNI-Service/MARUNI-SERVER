# Conversation 도메인

**최종 업데이트**: 2025-10-09
**상태**: ✅ Phase 1 완료 (OpenAI GPT-4o 연동)

## 📋 개요

AI 대화 시스템 도메인입니다. 사용자와 OpenAI GPT-4o 간의 대화를 관리하고, 키워드 기반 감정 분석을 수행합니다.

### 핵심 기능
- OpenAI GPT-4o 기반 AI 응답 생성
- 키워드 기반 감정 분석 (POSITIVE/NEGATIVE/NEUTRAL)
- 멀티턴 대화 컨텍스트 관리
- 대화 및 메시지 영속성 저장

## 🏗️ 주요 엔티티

### ConversationEntity
```java
- id: Long
- memberId: Long              // 회원 ID
- isActive: Boolean            // 활성 상태
- createdAt/updatedAt: LocalDateTime
```

### MessageEntity
```java
- id: Long
- conversationId: Long         // 대화 ID
- type: MessageType            // USER_MESSAGE, AI_RESPONSE, SYSTEM_MESSAGE
- content: String              // 메시지 내용
- emotion: EmotionType         // POSITIVE, NEGATIVE, NEUTRAL (사용자 메시지만)
- createdAt: LocalDateTime
```

## 🌐 REST API

### 1. AI 대화 메시지 전송
```
POST /api/conversations/messages
Headers: Authorization: Bearer {JWT}
Body: {
  "content": "오늘 기분이 좋아요"
}

Response: {
  "userMessage": { ... },
  "aiResponse": { ... }
}
```

### 2. 내 대화 전체보기 ✅ 신규
```
GET /api/conversations/history?days=7
Headers: Authorization: Bearer {JWT}

Response: [
  {
    "id": 1,
    "type": "USER_MESSAGE",
    "content": "...",
    "emotion": "POSITIVE",
    "createdAt": "2025-10-09T10:00:00"
  },
  ...
]
```

## 🔧 핵심 서비스

### SimpleConversationService
- `processUserMessage(memberId, content)`: 사용자 메시지 처리 및 AI 응답 생성
- `processSystemMessage(memberId, message)`: 시스템 메시지 저장 (안부 메시지용)
- `getMyConversationHistory(memberId, days)`: 본인 대화 내역 조회 ✅ 신규

### ConversationManager
- `findOrCreateActive(memberId)`: 활성 대화 조회 또는 생성

### MessageProcessor
- `processMessage(conversation, content)`: 메시지 처리 (AI 응답 + 감정 분석)

## 🤖 AI 통합

### OpenAI 설정 (application.yml)
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
          max-tokens: 100
```

### 환경 변수 (.env)
```bash
OPENAI_API_KEY=your_openai_api_key_here
```

## 📊 감정 분석

### KeywordBasedEmotionAnalyzer
- 긍정 키워드: 좋다, 행복, 즐겁다, 감사, 기쁘다, 편안, 뿌듯
- 부정 키워드: 슬프다, 우울, 외롭다, 힘들다, 아프다, 걱정, 불안

## 🔗 도메인 연동

- **DailyCheck**: 매일 안부 메시지 전송 → `processSystemMessage()`
- **AlertRule**: 대화 분석을 통한 이상징후 감지

## 📁 패키지 구조

```
conversation/
├── application/
│   ├── dto/                  # Request/Response DTO
│   ├── service/              # SimpleConversationService
│   └── mapper/               # ConversationMapper
├── domain/
│   ├── entity/               # ConversationEntity, MessageEntity
│   ├── repository/           # ConversationRepository, MessageRepository
│   └── port/                 # AIResponsePort, EmotionAnalysisPort
├── infrastructure/
│   ├── ai/                   # OpenAIResponseAdapter
│   └── analyzer/             # KeywordBasedEmotionAnalyzer
└── presentation/
    └── controller/           # ConversationController
```

## ✅ 완성도

- [x] OpenAI GPT-4o 연동
- [x] 감정 분석
- [x] 멀티턴 대화
- [x] 대화 영속성
- [x] REST API (2개)
- [x] JWT 인증
- [x] TDD 테스트

**상용 서비스 수준 완성**
