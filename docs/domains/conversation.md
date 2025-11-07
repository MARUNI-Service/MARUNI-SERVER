# Conversation 도메인

**최종 업데이트**: 2025-11-07
**상태**: ✅ Phase 2 완료 (OpenAI GPT-4o 연동 + Rich Domain Model)

## 📋 개요

AI 대화 시스템 도메인입니다. 사용자와 OpenAI GPT-4o 간의 대화를 관리하고, 키워드 기반 감정 분석을 수행합니다.

### 핵심 기능
- OpenAI GPT-4o 기반 AI 응답 생성
- 키워드 기반 감정 분석 (POSITIVE/NEGATIVE/NEUTRAL)
- 멀티턴 대화 컨텍스트 관리
- 대화 및 메시지 영속성 저장
- Rich Domain Model (비즈니스 로직 포함)

## 🏗️ 주요 엔티티

### ConversationEntity (Rich Domain Model)
- id: Long
- memberId: Long (회원 ID)
- startedAt: LocalDateTime (대화 시작 시간)
- messages: List<MessageEntity> (대화에 속한 메시지들, 양방향 연관관계)

**비즈니스 규칙**:
- MAX_DAILY_MESSAGES: 50 (일일 최대 메시지 수)
- MAX_MESSAGE_LENGTH: 500 (메시지 최대 길이)
- isActive(): 마지막 메시지가 24시간 이내인 경우 활성

### MessageEntity
- id: Long
- conversationId: Long (대화 ID)
- conversation: ConversationEntity (JPA 양방향 연관관계)
- type: MessageType (USER_MESSAGE, AI_RESPONSE, SYSTEM_MESSAGE)
- content: String (메시지 내용, TEXT 타입)
- emotion: EmotionType (POSITIVE, NEGATIVE, NEUTRAL)
- createdAt: LocalDateTime

## 🌐 REST API (3개)

### 1. AI 대화 메시지 전송
```
POST /api/conversations/messages
Headers: Authorization: Bearer {JWT}
Body: {
  "content": "오늘 기분이 좋아요"
}

Response: {
  "userMessage": {
    "id": 1,
    "type": "USER_MESSAGE",
    "content": "오늘 기분이 좋아요",
    "emotion": "POSITIVE",
    "createdAt": "2025-11-07T10:00:00"
  },
  "aiResponse": {
    "id": 2,
    "type": "AI_RESPONSE",
    "content": "좋은 하루를 보내고 계시네요!",
    "emotion": "NEUTRAL",
    "createdAt": "2025-11-07T10:00:01"
  }
}

Note: 일일 메시지 한도 50개 (MVP 데모용 검증 비활성화)
```

### 2. 내 대화 전체보기
```
GET /api/conversations/history?days=7
Headers: Authorization: Bearer {JWT}

Response: [
  {
    "id": 1,
    "type": "USER_MESSAGE",
    "content": "오늘 기분이 좋아요",
    "emotion": "POSITIVE",
    "createdAt": "2025-11-07T10:00:00"
  },
  {
    "id": 2,
    "type": "AI_RESPONSE",
    "content": "좋은 하루를 보내고 계시네요!",
    "emotion": "NEUTRAL",
    "createdAt": "2025-11-07T10:00:01"
  }
]
```

### 3. 최신 메시지 조회
```
GET /api/conversations/messages/latest
Headers: Authorization: Bearer {JWT}

Response: {
  "id": 2,
  "type": "AI_RESPONSE",
  "content": "좋은 하루를 보내고 계시네요!",
  "emotion": "NEUTRAL",
  "createdAt": "2025-11-07T10:00:01"
}

Note: 메시지가 없으면 data: null 반환
```

## 🔧 핵심 메서드

### ConversationEntity (Rich Domain Model)
- `createNew(memberId)`: 새 대화 생성 (정적 팩토리)
- `isActive()`: 활성 상태 확인 (마지막 메시지 24시간 이내)
- `addUserMessage(content, emotion)`: 사용자 메시지 추가
- `addAIMessage(content)`: AI 응답 메시지 추가
- `canReceiveMessage()`: 메시지 수신 가능 여부
- `getRecentHistory(count)`: 최근 대화 히스토리 조회

### MessageEntity
- `createUserMessage(conversationId, content, emotion)`: 사용자 메시지 생성 (정적 팩토리)
- `createAIResponse(conversationId, content)`: AI 응답 생성 (정적 팩토리)

### SimpleConversationService
- `processUserMessage(memberId, content)`: 사용자 메시지 처리 및 AI 응답 생성
- `getMyConversationHistory(memberId, days)`: 본인 대화 내역 조회
- `getLatestMessage(memberId)`: 최신 메시지 조회

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
- [x] 키워드 기반 감정 분석
- [x] 멀티턴 대화 컨텍스트
- [x] 대화 영속성 (PostgreSQL)
- [x] Rich Domain Model (비즈니스 로직 내장)
- [x] REST API (3개: 메시지 전송, 대화 이력, 최신 메시지)
- [x] JWT 인증
- [x] TDD 테스트
- [x] 일일 메시지 한도 검증 (MVP: 비활성화)

**상용 서비스 수준 완성**
