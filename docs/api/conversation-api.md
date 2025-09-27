# Conversation 도메인 API 명세서

**OpenAI GPT-4o 기반 AI 대화 시스템 REST API 완전 가이드**

## 📋 개요

Conversation 도메인은 MARUNI 프로젝트의 핵심 AI 대화 시스템으로, 노인 돌봄에 특화된 OpenAI GPT-4o 기반 멀티턴 대화 및 감정 분석 기능을 제공합니다.

### 🎯 **핵심 기능**
- **OpenAI GPT-4o 연동**: Spring AI 기반 실제 AI 응답 생성
- **멀티턴 대화 지원**: 대화 히스토리 기반 개인화된 AI 응답
- **키워드 감정 분석**: POSITIVE/NEGATIVE/NEUTRAL 3단계 자동 분석
- **노인 돌봄 특화**: 연령대, 성격, 건강 관심사 반영된 대화

### 🔐 **기술 특징**
- **Clean Architecture**: Port-Adapter 패턴으로 확장 가능한 구조
- **Rich Domain Model**: ConversationEntity 중심 비즈니스 로직
- **대화 컨텍스트**: 사용자 프로필 + 대화 히스토리 기반 개인화
- **실시간 처리**: 평균 2-3초 응답 시간 (OpenAI API 포함)

---

## 🌐 API 엔드포인트 목록

| HTTP | 엔드포인트 | 인증 | 설명 |
|------|------------|------|------|
| `POST` | `/api/conversations/messages` | ✅ JWT | AI 대화 메시지 전송 |

---

## 💬 AI 대화 API

### 1. AI 대화 메시지 전송

#### **POST** `/api/conversations/messages`

사용자 메시지를 전송하고 OpenAI GPT-4o 기반 AI 응답을 받습니다. 키워드 기반 감정 분석 및 멀티턴 대화를 지원합니다.

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Request Body:**
```json
{
  "content": "string"
}
```

**Request 예시:**
```json
{
  "content": "안녕하세요, 오늘 기분이 좋아요!"
}
```

**Validation 규칙:**
- `content`: 필수, 1자 이상 500자 이하
- 일일 메시지 한도: 50개
- 대화 세션: 24시간 이내 마지막 메시지가 있으면 기존 대화 연결

**Response 200 (성공 - 첫 대화):**
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

**Response 200 (성공 - 멀티턴 대화):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공적으로 처리되었습니다",
  "data": {
    "conversationId": 1,
    "userMessage": {
      "id": 5,
      "type": "USER_MESSAGE",
      "content": "네, 손자가 놀러와서 기뻤어요",
      "emotion": "POSITIVE",
      "createdAt": "2025-09-18T14:20:00"
    },
    "aiMessage": {
      "id": 6,
      "type": "AI_RESPONSE",
      "content": "70대이신 어르신이 손자분과 좋은 시간을 보내셨다니 정말 따뜻하네요! 지난번에 말씀하신 산책도 함께 하셨나요?",
      "emotion": "NEUTRAL",
      "createdAt": "2025-09-18T14:20:03"
    }
  }
}
```

**🔍 멀티턴 대화 특징:**
- AI가 사용자의 연령대(70대) 기억
- 이전 대화 내용(산책) 참조
- 개인화된 공감적 응답 생성

**Response 400 (입력값 검증 실패):**
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

**Response 401 (인증 실패):**
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "data": null
}
```

**Response 429 (메시지 한도 초과):**
```json
{
  "success": false,
  "code": "MESSAGE_LIMIT_EXCEEDED",
  "message": "일일 메시지 한도(50개)를 초과했습니다",
  "data": null
}
```

**Response 500 (AI 응답 생성 실패):**
```json
{
  "success": false,
  "code": "AI_RESPONSE_GENERATION_FAILED",
  "message": "AI 응답 생성에 실패했습니다",
  "data": {
    "fallbackResponse": "안녕하세요! 어떻게 지내세요?"
  }
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:8080/api/conversations/messages" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "content": "안녕하세요, 오늘 기분이 좋아요!"
  }'
```

---

## 📊 데이터 모델

### ConversationRequestDto
```json
{
  "content": "string (1-500자 필수)"
}
```

### ConversationResponseDto
```json
{
  "conversationId": "number (대화 세션 ID)",
  "userMessage": "MessageDto (사용자 메시지)",
  "aiMessage": "MessageDto (AI 응답 메시지)"
}
```

### MessageDto
```json
{
  "id": "number (메시지 ID)",
  "type": "string (USER_MESSAGE|AI_RESPONSE|SYSTEM_MESSAGE)",
  "content": "string (메시지 내용)",
  "emotion": "string (POSITIVE|NEGATIVE|NEUTRAL)",
  "createdAt": "datetime (ISO 8601)"
}
```

### ConversationEntity (내부 모델)
```json
{
  "id": "number (대화 ID)",
  "memberId": "number (회원 ID)",
  "startedAt": "datetime (대화 시작 시간)",
  "messages": "array<MessageEntity> (메시지 목록)"
}
```

---

## 🤖 AI 시스템 구조

### OpenAI GPT-4o 설정
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

### 대화 컨텍스트 구성
```json
{
  "currentMessage": "사용자 메시지",
  "recentHistory": [
    {
      "type": "USER_MESSAGE",
      "content": "이전 사용자 메시지",
      "emotion": "POSITIVE"
    },
    {
      "type": "AI_RESPONSE",
      "content": "이전 AI 응답"
    }
  ],
  "memberProfile": {
    "ageGroup": "70대",
    "personalityType": "일반",
    "healthConcerns": [],
    "recentEmotionPattern": "POSITIVE"
  },
  "currentEmotion": "POSITIVE"
}
```

### 감정 분석 키워드
```yaml
emotion:
  keywords:
    negative: ["슬프", "우울", "아프", "힘들", "외로", "무서", "걱정", "답답"]
    positive: ["좋", "행복", "기쁘", "감사", "즐거", "만족", "고마"]
```

---

## 🔧 비즈니스 규칙

### 대화 세션 관리
- **활성 대화**: 마지막 메시지가 24시간 이내인 경우
- **새 대화**: 24시간 이상 비활성 상태이거나 첫 대화인 경우
- **대화 연결**: 회원별로 가장 최근 대화를 활성 대화로 간주

### 메시지 제한
- **일일 한도**: 회원당 50개 메시지
- **메시지 길이**: 최대 500자
- **응답 시간**: 평균 2-3초 (OpenAI API 호출 포함)

### 감정 분석 우선순위
1. **부정적 키워드**: 우선 순위 높음 (정신 건강 모니터링)
2. **긍정적 키워드**: 중간 우선순위
3. **중립**: 키워드가 없거나 판단 불가능한 경우

---

## ❌ 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `INVALID_INPUT_VALUE` | 400 | 입력값 검증 실패 (메시지 길이 등) |
| `UNAUTHORIZED` | 401 | JWT 토큰 인증 실패 |
| `MESSAGE_LIMIT_EXCEEDED` | 429 | 일일 메시지 한도 초과 |
| `AI_RESPONSE_GENERATION_FAILED` | 500 | OpenAI API 호출 실패 |
| `CONVERSATION_SERVICE_ERROR` | 500 | 대화 처리 서비스 오류 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 🧪 테스트 시나리오

### 정상 플로우 (멀티턴 대화)
```bash
# 1. 첫 대화 시작
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "안녕하세요, 처음 사용해봐요"
}
# Response: conversationId=1, AI 환영 메시지

# 2. 감정 상태 공유
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "오늘 손자가 놀러와서 너무 좋았어요"
}
# Response: conversationId=1, AI 공감 응답 + emotion=POSITIVE

# 3. 건강 상태 문의
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "요즘 허리가 조금 아파요"
}
# Response: conversationId=1, AI 건강 관련 조언 + emotion=NEGATIVE

# 4. 이전 대화 참조 확인
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "손자 이야기 더 들려드릴게요"
}
# Response: conversationId=1, AI가 이전 손자 이야기 기억하며 응답
```

### 에러 케이스
```bash
# 메시지 길이 초과
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "500자를 초과하는 매우 긴 메시지..."
}
# Response: 400 INVALID_INPUT_VALUE

# 빈 메시지
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": ""
}
# Response: 400 INVALID_INPUT_VALUE

# 인증 토큰 없음
POST /api/conversations/messages
{
  "content": "안녕하세요"
}
# Response: 401 UNAUTHORIZED

# 일일 메시지 한도 초과 (51번째 메시지)
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "51번째 메시지"
}
# Response: 429 MESSAGE_LIMIT_EXCEEDED

# OpenAI API 장애 시
POST /api/conversations/messages
Authorization: Bearer {JWT_TOKEN}
{
  "content": "API 장애 테스트"
}
# Response: 500 AI_RESPONSE_GENERATION_FAILED + fallback 응답
```

### 감정 분석 테스트
```bash
# 긍정 감정
POST /api/conversations/messages
{
  "content": "오늘 정말 행복하고 기분이 좋아요"
}
# Response: emotion=POSITIVE

# 부정 감정 (우선 처리)
POST /api/conversations/messages
{
  "content": "요즘 많이 슬프고 우울해요"
}
# Response: emotion=NEGATIVE

# 중립 감정
POST /api/conversations/messages
{
  "content": "오늘 날씨가 어떤가요?"
}
# Response: emotion=NEUTRAL
```

---

## 🔗 도메인 연동

### DailyCheck 연동
```bash
# DailyCheck에서 자동 발송된 안부 메시지가 대화 시스템에 기록됨
# 사용자가 "네, 잘 지내요"라고 응답하면 AI가 후속 대화 진행
```

### AlertRule 연동
```bash
# Conversation에서 부정 감정 메시지 감지 시 AlertRule로 이벤트 전송
# AlertRule이 패턴 분석하여 보호자에게 알림 발송 결정
```

### Guardian 연동
```bash
# 긴급 키워드 감지 시 ("아파요", "도와주세요" 등)
# Guardian 시스템을 통해 보호자에게 즉시 알림 발송
```

---

## 📋 관련 문서

### 🔗 **연관 API**
- **[Member API](./member-api.md)**: 회원 인증 (JWT 토큰 필요)
- **[Auth API](./auth-api.md)**: JWT 토큰 관리
- **[Guardian API](./guardian-api.md)**: 보호자 알림 연동
- **[AlertRule API](./alertrule-api.md)**: 이상징후 감지 연동

### 🛠️ **기술 문서**
- **[Conversation 도메인 가이드](../domains/conversation.md)**: Clean Architecture 구현 상세
- **[API 설계 가이드](../specifications/api-design-guide.md)**: REST API 설계 원칙
- **[아키텍처 가이드](../specifications/architecture-guide.md)**: DDD + Clean Architecture

---

## 💡 개발자 가이드

### OpenAI API 환경 변수
```bash
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o
OPENAI_TEMPERATURE=0.7
OPENAI_MAX_TOKENS=100
```

### 대화 컨텍스트 활용
```java
// 멀티턴 대화를 위한 컨텍스트 구성
ConversationContext context = ConversationContext.forUserMessage(
    userMessage,
    recentHistory,  // 최근 5턴 대화
    memberProfile,  // 사용자 프로필 (연령대, 성격 등)
    emotion        // 현재 감정 상태
);

// 컨텍스트 기반 AI 응답 생성
String aiResponse = aiResponsePort.generateResponse(context);
```

### 감정 분석 활용
```java
// 키워드 기반 감정 분석
EmotionType emotion = emotionAnalysisPort.analyzeEmotion(userMessage);

// 부정 감정 감지 시 AlertRule 이벤트 발행
if (emotion == EmotionType.NEGATIVE) {
    eventPublisher.publishEvent(new NegativeEmotionDetectedEvent(memberId, userMessage));
}
```

### 비즈니스 규칙 확장
```java
// ConversationEntity의 도메인 로직 활용
ConversationEntity conversation = conversationManager.findOrCreateActive(memberId);

// 메시지 추가 시 자동 검증
MessageEntity userMessage = conversation.addUserMessage(content, emotion);
// - 메시지 길이 검증
// - 일일 한도 검증
// - 대화 활성 상태 검증
```

### API 응답 최적화
```java
// 정적 팩토리 메서드 활용
ConversationResponseDto response = ConversationResponseDto.from(messageExchangeResult);

// 지연 로딩 최적화
@EntityGraph(attributePaths = {"messages"})
Optional<ConversationEntity> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
```

---

## 🚀 확장 가능성

### AI 모델 교체
- **GPT-4 Turbo**: 더 빠른 응답 속도
- **Claude 3**: 다른 AI 모델 비교 테스트
- **커스텀 모델**: 노인 돌봄 특화 Fine-tuning 모델

### 고급 감정 분석
- **ML 기반 분석**: TensorFlow/PyTorch 모델 연동
- **음성 감정 분석**: 음성 메시지 지원 시
- **표정 분석**: 화상 통화 기능 추가 시

### 실시간 대화
- **WebSocket**: 실시간 채팅 인터페이스
- **SSE**: 스트리밍 AI 응답
- **모바일 푸시**: 즉시 알림 기능

### 개인화 강화
- **학습 기능**: 대화 패턴 학습으로 응답 개선
- **건강 데이터 연동**: IoT 기기 데이터 활용
- **가족 정보 활용**: 가족 구성원 정보 기반 대화

---

**Conversation API는 MARUNI 플랫폼의 핵심인 AI 대화 시스템으로, OpenAI GPT-4o 기반 멀티턴 대화와 감정 분석을 통해 노인들에게 따뜻하고 개인화된 AI 돌봄 서비스를 제공합니다. Clean Architecture와 Rich Domain Model을 통해 확장 가능하고 유지보수가 용이한 구조로 설계되었습니다.** 🤖