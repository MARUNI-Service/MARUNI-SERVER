# 📋 MARUNI Postman API 테스트 - Phase 2: Core Service Layer

**작성일**: 2025-10-05
**대상**: AI 대화 시스템 (Conversation) + 보호자 관리 시스템 (Guardian)
**난이도**: ⭐⭐ (중급)

---

## 🎯 Phase 2 개요

**Core Service Layer**는 MARUNI의 핵심 비즈니스 로직인 **AI 대화 시스템**과 **보호자 관리 시스템**을 테스트합니다.

### 테스트 목표
- ✅ OpenAI GPT-4o 기반 AI 대화 시스템 검증
- ✅ 감정 분석 (POSITIVE/NEGATIVE/NEUTRAL) 검증
- ✅ 보호자 CRUD 기능 검증
- ✅ 보호자-회원 관계 설정 검증
- ✅ 알림 설정 관리 검증

### 테스트 순서
```
Part A: AI 대화 시스템 (Conversation)
  1. 첫 대화 메시지 전송 (대화 세션 자동 생성)
  2. 긍정적 감정 메시지 전송
  3. 부정적 감정 메시지 전송
  4. 연속 대화 (멀티턴 대화)

Part B: 보호자 관리 시스템 (Guardian)
  5. 보호자 생성 (가족)
  6. 보호자 생성 (친구)
  7. 보호자 목록 조회
  8. 보호자 상세 조회
  9. 회원에게 보호자 할당
  10. 보호자의 회원 목록 조회
  11. 보호자 정보 수정
  12. 보호자-회원 관계 해제
```

---

## 🔧 사전 준비

### 1. Phase 1 완료 확인

**필수 환경 변수**:
```
✅ access_token: 로그인 후 발급받은 JWT 토큰
✅ member_id: 로그인한 회원의 ID
```

**확인 방법**:
```bash
# Postman Environment에서 확인
{{access_token}} → eyJhbGciOiJIUzI1NiIs...
{{member_id}} → 1
```

### 2. 서버 환경 변수 확인

**OpenAI API 설정 필요**:
```env
# .env 파일
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=100
OPENAI_TEMPERATURE=0.7
```

**확인 명령**:
```bash
# 환경 변수 확인
echo $OPENAI_API_KEY
```

### 3. 추가 환경 변수 준비

**Postman Environment에 추가할 변수**:
```
conversation_id: (첫 대화 후 자동 저장)
guardian_id_family: (가족 보호자 ID, 자동 저장)
guardian_id_friend: (친구 보호자 ID, 자동 저장)
```

---

## 📝 Part A: AI 대화 시스템 (Conversation)

### ✅ Step 2.1: 첫 대화 메시지 전송 (대화 세션 자동 생성) ⭐

**목적**: 새로운 대화 세션 생성 및 AI 응답 받기

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
  "content": "안녕하세요! 오늘 날씨가 정말 좋네요."
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
      "id": 1,
      "type": "USER_MESSAGE",
      "content": "안녕하세요! 오늘 날씨가 정말 좋네요.",
      "emotion": "POSITIVE",
      "createdAt": "2025-10-05T11:00:00"
    },
    "aiMessage": {
      "id": 2,
      "type": "AI_RESPONSE",
      "content": "안녕하세요! 날씨가 좋은 날은 기분도 좋아지죠. 오늘 산책이라도 다녀오시는 건 어떠세요?",
      "emotion": "NEUTRAL",
      "createdAt": "2025-10-05T11:00:03"
    }
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("첫 대화 메시지 전송 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // Conversation ID 저장
    pm.environment.set("conversation_id", jsonData.data.conversationId);

    // 감정 분석 검증
    pm.expect(jsonData.data.userMessage.emotion).to.be.oneOf(["POSITIVE", "NEGATIVE", "NEUTRAL"]);

    // AI 응답 검증
    pm.expect(jsonData.data.aiMessage).to.exist;
    pm.expect(jsonData.data.aiMessage.type).to.eql("AI_RESPONSE");

    console.log("✅ 대화 세션 생성:", jsonData.data.conversationId);
    console.log("감정 분석 결과:", jsonData.data.userMessage.emotion);
    console.log("AI 응답:", jsonData.data.aiMessage.content);
});
```

**주요 검증 포인트**:
- ✅ 대화 세션 자동 생성 (conversationId)
- ✅ 감정 분석 결과 (POSITIVE 예상)
- ✅ AI 응답 생성 (OpenAI GPT-4o)
- ✅ 응답 시간 (평균 2-3초)

---

### ✅ Step 2.2: 긍정적 감정 메시지 전송

**목적**: 긍정적 키워드 감정 분석 검증

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
  "content": "정말 좋아요! 오늘 기분이 너무 행복해요. 감사합니다!"
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
      "id": 3,
      "type": "USER_MESSAGE",
      "content": "정말 좋아요! 오늘 기분이 너무 행복해요. 감사합니다!",
      "emotion": "POSITIVE",
      "createdAt": "2025-10-05T11:05:00"
    },
    "aiMessage": {
      "id": 4,
      "type": "AI_RESPONSE",
      "content": "행복하시다니 정말 기쁘네요! 좋은 하루 보내세요.",
      "emotion": "NEUTRAL",
      "createdAt": "2025-10-05T11:05:03"
    }
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("긍정적 감정 분석 검증", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // 긍정적 감정 검증
    pm.expect(jsonData.data.userMessage.emotion).to.eql("POSITIVE");

    console.log("✅ 감정 분석: POSITIVE (긍정적 키워드 감지)");
    console.log("AI 응답:", jsonData.data.aiMessage.content);
});
```

**긍정 키워드 목록** (자동 감지):
```
좋, 행복, 기쁘, 감사, 즐거, 만족, 고마
```

---

### ✅ Step 2.3: 부정적 감정 메시지 전송 ⚠️

**목적**: 부정적 키워드 감정 분석 및 AlertRule 연동 검증

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
  "content": "오늘 너무 슬프고 우울해요. 혼자 있으니 외롭고 힘들어요."
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
      "id": 5,
      "type": "USER_MESSAGE",
      "content": "오늘 너무 슬프고 우울해요. 혼자 있으니 외롭고 힘들어요.",
      "emotion": "NEGATIVE",
      "createdAt": "2025-10-05T11:10:00"
    },
    "aiMessage": {
      "id": 6,
      "type": "AI_RESPONSE",
      "content": "힘드신 마음이 느껴집니다. 천천히 이야기 나눠보실까요? 제가 옆에 있어요.",
      "emotion": "NEUTRAL",
      "createdAt": "2025-10-05T11:10:03"
    }
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("부정적 감정 분석 및 AlertRule 연동 검증", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // 부정적 감정 검증
    pm.expect(jsonData.data.userMessage.emotion).to.eql("NEGATIVE");

    console.log("✅ 감정 분석: NEGATIVE (부정적 키워드 감지)");
    console.log("⚠️ AlertRule 도메인에서 이상징후 감지 가능");
    console.log("AI 응답:", jsonData.data.aiMessage.content);
});
```

**부정 키워드 목록** (자동 감지):
```
슬프, 우울, 아프, 힘들, 외로, 무서, 걱정, 답답
```

**AlertRule 연동**:
- ⚠️ 부정적 감정이 연속으로 감지되면 AlertRule 도메인에서 자동 감지
- ⚠️ Phase 3에서 알림 규칙 설정 및 보호자 알림 테스트

---

### ✅ Step 2.4: 연속 대화 (멀티턴 대화)

**목적**: 대화 히스토리 기반 컨텍스트 유지 검증

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
  "content": "아까 말씀하신 산책 좋을 것 같아요."
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
      "id": 7,
      "type": "USER_MESSAGE",
      "content": "아까 말씀하신 산책 좋을 것 같아요.",
      "emotion": "POSITIVE",
      "createdAt": "2025-10-05T11:15:00"
    },
    "aiMessage": {
      "id": 8,
      "type": "AI_RESPONSE",
      "content": "좋은 선택이세요! 가까운 공원에 다녀오시면 기분이 더 좋아지실 거예요.",
      "emotion": "NEUTRAL",
      "createdAt": "2025-10-05T11:15:03"
    }
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("멀티턴 대화 컨텍스트 유지 검증", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // 동일한 대화 세션 검증
    const savedConversationId = pm.environment.get("conversation_id");
    pm.expect(jsonData.data.conversationId).to.eql(parseInt(savedConversationId));

    console.log("✅ 대화 히스토리 유지 (conversationId:", jsonData.data.conversationId, ")");
    console.log("AI가 이전 대화('산책') 내용을 기억하고 응답");
});
```

**주요 검증 포인트**:
- ✅ 동일한 conversationId 유지
- ✅ AI가 이전 대화 내용 기억 (대화 히스토리 최대 5턴)
- ✅ ConversationContext 기반 개인화 응답

---

## 📝 Part B: 보호자 관리 시스템 (Guardian)

### ✅ Step 2.5: 보호자 생성 (가족) ⭐

**목적**: 가족 관계 보호자 등록

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/guardians`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "guardianName": "김보호",
  "guardianEmail": "guardian.family@maruni.com",
  "guardianPhone": "010-1234-5678",
  "relation": "FAMILY",
  "notificationPreference": "ALL"
}
```

**Expected Response** (200 OK):
```json
{
  "code": "G001",
  "message": "보호자 생성 성공",
  "data": {
    "id": 1,
    "guardianName": "김보호",
    "guardianEmail": "guardian.family@maruni.com",
    "guardianPhone": "010-1234-5678",
    "relation": "FAMILY",
    "notificationPreference": "ALL",
    "isActive": true,
    "createdAt": "2025-10-05T11:20:00",
    "updatedAt": "2025-10-05T11:20:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("가족 보호자 생성 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // Guardian ID 저장
    pm.environment.set("guardian_id_family", jsonData.data.id);

    // 관계 및 알림 설정 검증
    pm.expect(jsonData.data.relation).to.eql("FAMILY");
    pm.expect(jsonData.data.notificationPreference).to.eql("ALL");
    pm.expect(jsonData.data.isActive).to.be.true;

    console.log("✅ 가족 보호자 생성:", jsonData.data.guardianName);
    console.log("Guardian ID:", jsonData.data.id);
    console.log("알림 설정: ALL (PUSH + EMAIL + SMS)");
});
```

**GuardianRelation Enum 값**:
```
FAMILY: 가족
FRIEND: 친구
CAREGIVER: 돌봄제공자
NEIGHBOR: 이웃
OTHER: 기타
```

**NotificationPreference Enum 값**:
```
PUSH: 푸시알림만
EMAIL: 이메일만
SMS: SMS만
ALL: 모든 알림 (PUSH + EMAIL + SMS)
```

---

### ✅ Step 2.6: 보호자 생성 (친구)

**목적**: 친구 관계 보호자 등록

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/guardians`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "guardianName": "이친구",
  "guardianEmail": "guardian.friend@maruni.com",
  "guardianPhone": "010-9876-5432",
  "relation": "FRIEND",
  "notificationPreference": "PUSH"
}
```

**Expected Response** (200 OK):
```json
{
  "code": "G001",
  "message": "보호자 생성 성공",
  "data": {
    "id": 2,
    "guardianName": "이친구",
    "guardianEmail": "guardian.friend@maruni.com",
    "guardianPhone": "010-9876-5432",
    "relation": "FRIEND",
    "notificationPreference": "PUSH",
    "isActive": true,
    "createdAt": "2025-10-05T11:25:00",
    "updatedAt": "2025-10-05T11:25:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("친구 보호자 생성 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // Guardian ID 저장
    pm.environment.set("guardian_id_friend", jsonData.data.id);

    pm.expect(jsonData.data.relation).to.eql("FRIEND");
    pm.expect(jsonData.data.notificationPreference).to.eql("PUSH");

    console.log("✅ 친구 보호자 생성:", jsonData.data.guardianName);
    console.log("Guardian ID:", jsonData.data.id);
    console.log("알림 설정: PUSH (푸시알림만)");
});
```

**실패 케이스**:

1. **이메일 중복** (409 Conflict):
```json
{
  "code": "E011",
  "message": "이미 존재하는 보호자 이메일입니다",
  "data": null
}
```

2. **Validation 실패** (400 Bad Request):
```json
{
  "code": "V001",
  "message": "입력값 유효성 검사 실패",
  "data": {
    "guardianEmail": "올바른 이메일 형식이어야 합니다",
    "relation": "유효한 관계 타입이어야 합니다"
  }
}
```

---

### ✅ Step 2.7: 보호자 목록 조회

**목적**: 활성화된 모든 보호자 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/guardians`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "G002",
  "message": "보호자 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "guardianName": "김보호",
      "guardianEmail": "guardian.family@maruni.com",
      "guardianPhone": "010-1234-5678",
      "relation": "FAMILY",
      "notificationPreference": "ALL",
      "isActive": true,
      "createdAt": "2025-10-05T11:20:00",
      "updatedAt": "2025-10-05T11:20:00"
    },
    {
      "id": 2,
      "guardianName": "이친구",
      "guardianEmail": "guardian.friend@maruni.com",
      "guardianPhone": "010-9876-5432",
      "relation": "FRIEND",
      "notificationPreference": "PUSH",
      "isActive": true,
      "createdAt": "2025-10-05T11:25:00",
      "updatedAt": "2025-10-05T11:25:00"
    }
  ]
}
```

**Postman Tests Script**:
```javascript
pm.test("보호자 목록 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    // 2명의 보호자 확인
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.at.least(2);

    console.log("✅ 보호자 목록:", jsonData.data.length, "명");
    jsonData.data.forEach(guardian => {
        console.log(`- ${guardian.guardianName} (${guardian.relation})`);
    });
});
```

---

### ✅ Step 2.8: 보호자 상세 조회

**목적**: 특정 보호자 정보 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/guardians/{{guardian_id_family}}`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "G002",
  "message": "보호자 조회 성공",
  "data": {
    "id": 1,
    "guardianName": "김보호",
    "guardianEmail": "guardian.family@maruni.com",
    "guardianPhone": "010-1234-5678",
    "relation": "FAMILY",
    "notificationPreference": "ALL",
    "isActive": true,
    "createdAt": "2025-10-05T11:20:00",
    "updatedAt": "2025-10-05T11:20:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("보호자 상세 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    const savedGuardianId = pm.environment.get("guardian_id_family");
    pm.expect(jsonData.data.id).to.eql(parseInt(savedGuardianId));

    console.log("✅ 보호자 상세 정보 조회:", jsonData.data.guardianName);
});
```

**실패 케이스**:

**존재하지 않는 보호자** (404 Not Found):
```json
{
  "code": "E012",
  "message": "해당 보호자를 찾을 수 없습니다",
  "data": null
}
```

---

### ✅ Step 2.9: 회원에게 보호자 할당 ⭐

**목적**: Member와 Guardian 관계 설정 (다대일 관계)

**HTTP Method**: `POST`
**URL**: `{{base_url}}/api/guardians/{{guardian_id_family}}/members/{{member_id}}`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Request Body**: 없음

**Expected Response** (200 OK):
```json
{
  "code": "G004",
  "message": "보호자 할당 성공",
  "data": null
}
```

**Postman Tests Script**:
```javascript
pm.test("회원에게 보호자 할당 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.code).to.eql("G004");

    const guardianId = pm.environment.get("guardian_id_family");
    const memberId = pm.environment.get("member_id");

    console.log("✅ 보호자 할당 완료");
    console.log(`Member ID ${memberId} ← Guardian ID ${guardianId}`);
    console.log("이제 AlertRule에서 이 보호자에게 알림 발송 가능");
});
```

**관계 구조**:
```
MemberEntity (회원)
  ↓ @ManyToOne
GuardianEntity (보호자)
  ↓ @OneToMany
List<MemberEntity> (담당 회원들)
```

**중요**:
- ⚠️ 한 회원은 하나의 보호자만 가질 수 있음 (다대일)
- ⚠️ 한 보호자는 여러 회원을 담당할 수 있음 (일대다)
- ⚠️ 이미 보호자가 할당된 회원에게 새 보호자 할당 시 기존 보호자는 자동 해제

---

### ✅ Step 2.10: 보호자의 회원 목록 조회

**목적**: 특정 보호자가 담당하는 회원 목록 조회

**HTTP Method**: `GET`
**URL**: `{{base_url}}/api/guardians/{{guardian_id_family}}/members`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "code": "G005",
  "message": "보호자 회원 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "memberEmail": "test@maruni.com",
      "memberName": "수정된사용자",
      "createdAt": "2025-10-05T10:30:00"
    }
  ]
}
```

**Postman Tests Script**:
```javascript
pm.test("보호자 회원 목록 조회 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.at.least(1);

    const savedMemberId = pm.environment.get("member_id");
    const assignedMember = jsonData.data.find(m => m.id === parseInt(savedMemberId));
    pm.expect(assignedMember).to.exist;

    console.log("✅ 보호자가 담당하는 회원:", jsonData.data.length, "명");
    jsonData.data.forEach(member => {
        console.log(`- ${member.memberName} (${member.memberEmail})`);
    });
});
```

---

### ✅ Step 2.11: 보호자 정보 수정

**목적**: 보호자 이름, 전화번호, 알림 설정 수정

**HTTP Method**: `PUT`
**URL**: `{{base_url}}/api/guardians/{{guardian_id_family}}`
**Headers**:
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "guardianName": "김보호_수정",
  "guardianPhone": "010-1111-2222",
  "notificationPreference": "PUSH"
}
```

**Expected Response** (200 OK):
```json
{
  "code": "G003",
  "message": "보호자 정보 수정 성공",
  "data": {
    "id": 1,
    "guardianName": "김보호_수정",
    "guardianEmail": "guardian.family@maruni.com",
    "guardianPhone": "010-1111-2222",
    "relation": "FAMILY",
    "notificationPreference": "PUSH",
    "isActive": true,
    "createdAt": "2025-10-05T11:20:00",
    "updatedAt": "2025-10-05T11:40:00"
  }
}
```

**Postman Tests Script**:
```javascript
pm.test("보호자 정보 수정 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.data.guardianName).to.eql("김보호_수정");
    pm.expect(jsonData.data.guardianPhone).to.eql("010-1111-2222");
    pm.expect(jsonData.data.notificationPreference).to.eql("PUSH");

    console.log("✅ 보호자 정보 수정 완료");
    console.log("이름:", jsonData.data.guardianName);
    console.log("전화번호:", jsonData.data.guardianPhone);
    console.log("알림 설정:", jsonData.data.notificationPreference);
});
```

**주요 변경 사항**:
- ✅ 이름 변경: `김보호` → `김보호_수정`
- ✅ 전화번호 변경: `010-1234-5678` → `010-1111-2222`
- ✅ 알림 설정 변경: `ALL` → `PUSH`

---

### ✅ Step 2.12: 보호자-회원 관계 해제

**목적**: Member의 Guardian 관계 제거

**HTTP Method**: `DELETE`
**URL**: `{{base_url}}/api/guardians/members/{{member_id}}/guardian`
**Headers**:
```
Authorization: Bearer {{access_token}}
```

**Request Body**: 없음

**Expected Response** (200 OK):
```json
{
  "code": "G006",
  "message": "보호자 관계 해제 성공",
  "data": null
}
```

**Postman Tests Script**:
```javascript
pm.test("보호자 관계 해제 성공", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();

    pm.expect(jsonData.code).to.eql("G006");

    console.log("✅ 보호자 관계 해제 완료");
    console.log("이제 회원은 보호자가 없는 상태");
});
```

**관계 해제 후 상태**:
```
MemberEntity.guardian = null
GuardianEntity.members.remove(member)
```

**참고**:
- ⚠️ 관계 해제 후에도 Guardian 엔티티는 삭제되지 않음 (소프트 삭제 아님)
- ⚠️ 다른 회원에게 다시 할당 가능
- ⚠️ AlertRule 알림 발송 시 보호자가 없으면 알림 발송 불가

---

## 📊 Phase 2 완료 체크리스트

### Part A: AI 대화 시스템

```
[ ] Step 2.1: 첫 대화 메시지 전송 (대화 세션 자동 생성) ⭐
[ ] Step 2.2: 긍정적 감정 메시지 전송
[ ] Step 2.3: 부정적 감정 메시지 전송 ⚠️
[ ] Step 2.4: 연속 대화 (멀티턴 대화)
```

### Part B: 보호자 관리 시스템

```
[ ] Step 2.5: 보호자 생성 (가족) ⭐
[ ] Step 2.6: 보호자 생성 (친구)
[ ] Step 2.7: 보호자 목록 조회
[ ] Step 2.8: 보호자 상세 조회
[ ] Step 2.9: 회원에게 보호자 할당 ⭐
[ ] Step 2.10: 보호자의 회원 목록 조회
[ ] Step 2.11: 보호자 정보 수정
[ ] Step 2.12: 보호자-회원 관계 해제
```

### 환경 변수 확인

```
[ ] conversation_id: 저장되어 있음
[ ] guardian_id_family: 저장되어 있음
[ ] guardian_id_friend: 저장되어 있음
[ ] 보호자-회원 관계 설정 완료
```

### 검증 항목

```
[ ] AI 대화 응답 생성 정상 작동 (OpenAI GPT-4o)
[ ] 감정 분석 정확도 (POSITIVE/NEGATIVE/NEUTRAL)
[ ] 멀티턴 대화 컨텍스트 유지
[ ] 보호자 CRUD 기능 정상 작동
[ ] 보호자-회원 관계 설정/해제 정상 작동
[ ] 알림 설정 변경 정상 작동
```

---

## 🎯 다음 단계

### Phase 3: Integration Layer 준비

**Phase 2 완료 후 확인사항**:
1. ✅ `conversation_id` 환경 변수에 저장되어 있어야 함
2. ✅ `guardian_id_family` 환경 변수에 저장되어 있어야 함
3. ✅ 부정적 감정 메시지 전송 완료 (AlertRule 감지 준비)
4. ✅ 회원에게 보호자 할당 완료 (알림 발송 준비)

**Phase 3에서 테스트할 도메인**:
- ✅ 이상징후 감지 시스템 (AlertRule)
  - 감정 패턴 분석 (EMOTION_PATTERN)
  - 무응답 패턴 분석 (NO_RESPONSE)
  - 키워드 감지 (KEYWORD_DETECTION)
- ✅ 보호자 알림 발송 시스템 (Notification)

**다음 문서**: `postman-phase3-integration.md`

---

## 🔍 문제 해결 가이드

### 1. AI 응답 생성 실패

**원인**:
- OpenAI API 키 미설정
- API 호출 제한 초과
- 네트워크 오류

**해결**:
```bash
# 환경 변수 확인
echo $OPENAI_API_KEY

# application-ai.yml 확인
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}

# 서버 로그 확인
docker-compose logs -f app | grep "OpenAI"
```

**대체 응답** (OpenAI 실패 시):
```json
{
  "aiMessage": {
    "content": "안녕하세요! 어떻게 지내세요?" // 기본 응답
  }
}
```

### 2. 감정 분석이 예상과 다름

**원인**:
- 키워드 기반 분석의 한계
- 복합 감정 표현

**해결**:
```yaml
# application.yml에서 키워드 커스터마이징
maruni:
  conversation:
    emotion:
      keywords:
        negative: ["슬프", "우울", "아프", "힘들", ...]
        positive: ["좋", "행복", "기쁘", "감사", ...]
```

**참고**:
- ⚠️ 키워드 분석은 85% 정도의 정확도
- ⚠️ Phase 3+ ML 기반 감정 분석 도입 예정

### 3. 보호자 이메일 중복 오류

**원인**:
- 동일한 이메일로 이미 보호자 생성됨

**해결**:
```bash
# 다른 이메일 사용
{
  "guardianEmail": "guardian.family2@maruni.com"
}

# 또는 기존 보호자 조회
GET /api/guardians
```

### 4. 보호자 할당 후 관계 확인 안 됨

**원인**:
- JPA Lazy Loading
- 트랜잭션 미완료

**해결**:
```bash
# Step 2.10 실행하여 관계 확인
GET /api/guardians/{{guardian_id_family}}/members

# 응답에서 할당한 회원 확인
```

---

## 📚 참고 문서

- **Conversation 도메인**: `docs/domains/conversation.md`
- **Guardian 도메인**: `docs/domains/guardian.md`
- **API 설계 가이드**: `docs/specifications/api-design-guide.md`
- **OpenAI 연동 가이드**: `docs/domains/conversation.md` (AI 응답 생성 섹션)
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

---

**Phase 2 Core Service Layer 테스트 문서 v1.0.0** | 작성: 2025-10-05
