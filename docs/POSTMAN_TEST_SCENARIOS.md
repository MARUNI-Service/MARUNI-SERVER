# MARUNI Postman 테스트 시나리오

**작성일**: 2025-10-10
**버전**: 1.0.0
**프로젝트**: MARUNI Phase 2 MVP

## 📋 목차

1. [환경 설정](#환경-설정)
2. [사용자 플로우 1: 기본 회원가입/로그인](#사용자-플로우-1-기본-회원가입로그인)
3. [사용자 플로우 2: AI 대화](#사용자-플로우-2-ai-대화)
4. [사용자 플로우 3: 보호자 관계 설정](#사용자-플로우-3-보호자-관계-설정)
5. [사용자 플로우 4: 회원 관리](#사용자-플로우-4-회원-관리)
6. [트러블슈팅](#트러블슈팅)

---

## 환경 설정

### 1. Postman 환경 변수 설정

Postman > Environments > New Environment 생성

```
Environment Name: MARUNI Local

변수 설정:
- base_url: http://localhost:8080
- access_token: (자동으로 채워짐)
- elderly_email: elderly@test.com
- elderly_password: password123
- guardian_email: guardian@test.com
- guardian_password: password123
- elderly_id: (자동으로 채워짐)
- guardian_id: (자동으로 채워짐)
```

### 2. 서버 실행 확인

```bash
# 터미널에서 실행
cd C:\Users\rlarb\coding\maruni\maruni-server
docker-compose up -d
./gradlew bootRun
```

### 3. 서버 상태 확인

**Request:**
```
GET {{base_url}}/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### 4. Swagger UI 확인

브라우저에서 접속: `http://localhost:8080/swagger-ui.html`

---

## 사용자 플로우 1: 기본 회원가입/로그인

**시나리오**: 새로운 노인 사용자가 회원가입하고 로그인하여 본인 정보를 조회합니다.

### Step 1.1: 이메일 중복 확인

**Request:**
```
GET {{base_url}}/api/join/email-check?memberEmail={{elderly_email}}
```

**Headers:**
```
(없음)
```

**Expected Response (성공 - 이메일 사용 가능):**
```json
{
  "isSuccess": true,
  "code": "M003",
  "message": "이메일 중복 확인 완료",
  "data": {
    "memberEmail": "elderly@test.com",
    "isAvailable": true
  }
}
```

**Expected Response (실패 - 이메일 중복):**
```json
{
  "isSuccess": false,
  "code": "M002",
  "message": "이미 존재하는 이메일입니다",
  "data": null
}
```

---

### Step 1.2: 회원가입

**Request:**
```
POST {{base_url}}/api/join
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "memberEmail": "elderly@test.com",
  "memberName": "김순자",
  "memberPassword": "password123",
  "dailyCheckEnabled": true
}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M004",
  "message": "회원가입이 완료되었습니다",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": true,
    "guardian": null,
    "managedMembers": []
  }
}
```

**Postman Tests (자동화):**
```javascript
// Step 1.2 Tests 탭에 추가
pm.test("회원가입 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.code).to.eql("M004");

    // elderly_id 환경 변수에 저장
    pm.environment.set("elderly_id", jsonData.data.id);
});
```

---

### Step 1.3: 로그인 (JWT 발급)

**Request:**
```
POST {{base_url}}/api/auth/login
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "elderly@test.com",
  "password": "password123"
}
```

**Expected Response (성공):**
```
Status: 200 OK

Headers:
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Body:
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": null
}
```

**Postman Tests (자동화):**
```javascript
// Step 1.3 Tests 탭에 추가
pm.test("로그인 성공 및 토큰 저장", function() {
    pm.response.to.have.status(200);

    // Authorization 헤더에서 토큰 추출
    const authHeader = pm.response.headers.get("Authorization");
    if (authHeader) {
        const token = authHeader.replace("Bearer ", "");
        pm.environment.set("access_token", token);
        console.log("Access Token 저장:", token.substring(0, 20) + "...");
    }
});
```

---

### Step 1.4: 내 정보 조회

**Request:**
```
GET {{base_url}}/api/members/me
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M005",
  "message": "회원 조회 완료",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": true,
    "guardian": null,
    "managedMembers": []
  }
}
```

**Postman Tests (자동화):**
```javascript
// Step 1.4 Tests 탭에 추가
pm.test("내 정보 조회 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.memberEmail).to.eql("elderly@test.com");
    pm.expect(jsonData.data.memberName).to.eql("김순자");
});
```

---

## 사용자 플로우 2: AI 대화

**시나리오**: 로그인한 사용자가 AI와 대화하고 대화 내역을 조회합니다.

**전제 조건**: Step 1.3에서 로그인하여 `access_token` 환경 변수 설정 완료

---

### Step 2.1: AI 대화 메시지 전송 (긍정 감정)

**Request:**
```
POST {{base_url}}/api/conversations/messages
```

**Headers:**
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "content": "오늘 날씨가 좋아서 기분이 좋아요!"
}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    "userMessage": {
      "id": 1,
      "conversationId": 1,
      "type": "USER_MESSAGE",
      "content": "오늘 날씨가 좋아서 기분이 좋아요!",
      "emotion": "POSITIVE",
      "createdAt": "2025-10-10T10:05:00"
    },
    "aiResponse": {
      "id": 2,
      "conversationId": 1,
      "type": "AI_RESPONSE",
      "content": "좋은 날씨에 기분까지 좋으시다니 정말 다행이에요! 오늘 어떤 일이 있으셨나요?",
      "emotion": null,
      "createdAt": "2025-10-10T10:05:01"
    }
  }
}
```

**Postman Tests:**
```javascript
// Step 2.1 Tests 탭에 추가
pm.test("AI 대화 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.userMessage.emotion).to.eql("POSITIVE");
    pm.expect(jsonData.data.aiResponse.content).to.not.be.empty;
});
```

---

### Step 2.2: AI 대화 메시지 전송 (부정 감정)

**Request:**
```
POST {{base_url}}/api/conversations/messages
```

**Headers:**
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "content": "오늘은 좀 외롭고 우울해요"
}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    "userMessage": {
      "id": 3,
      "conversationId": 1,
      "type": "USER_MESSAGE",
      "content": "오늘은 좀 외롭고 우울해요",
      "emotion": "NEGATIVE",
      "createdAt": "2025-10-10T10:10:00"
    },
    "aiResponse": {
      "id": 4,
      "conversationId": 1,
      "type": "AI_RESPONSE",
      "content": "힘드신 마음이 느껴져요. 무슨 일이 있으셨나요? 말씀해 주시면 들어드릴게요.",
      "emotion": null,
      "createdAt": "2025-10-10T10:10:01"
    }
  }
}
```

---

### Step 2.3: 대화 내역 조회 (최근 7일)

**Request:**
```
GET {{base_url}}/api/conversations/history?days=7
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": [
    {
      "id": 1,
      "conversationId": 1,
      "type": "USER_MESSAGE",
      "content": "오늘 날씨가 좋아서 기분이 좋아요!",
      "emotion": "POSITIVE",
      "createdAt": "2025-10-10T10:05:00"
    },
    {
      "id": 2,
      "conversationId": 1,
      "type": "AI_RESPONSE",
      "content": "좋은 날씨에 기분까지 좋으시다니 정말 다행이에요! 오늘 어떤 일이 있으셨나요?",
      "emotion": null,
      "createdAt": "2025-10-10T10:05:01"
    },
    {
      "id": 3,
      "conversationId": 1,
      "type": "USER_MESSAGE",
      "content": "오늘은 좀 외롭고 우울해요",
      "emotion": "NEGATIVE",
      "createdAt": "2025-10-10T10:10:00"
    },
    {
      "id": 4,
      "conversationId": 1,
      "type": "AI_RESPONSE",
      "content": "힘드신 마음이 느껴져요. 무슨 일이 있으셨나요? 말씀해 주시면 들어드릴게요.",
      "emotion": null,
      "createdAt": "2025-10-10T10:10:01"
    }
  ]
}
```

**Postman Tests:**
```javascript
// Step 2.3 Tests 탭에 추가
pm.test("대화 내역 조회 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.greaterThan(0);
});
```

---

## 사용자 플로우 3: 보호자 관계 설정

**시나리오**: 김순자(노인)가 김영희(보호자)를 검색하고 보호자 요청을 보내고, 김영희가 이를 수락합니다.

### 준비 단계: 보호자 회원 생성

#### Step 3.0.1: 보호자 회원가입

**Request:**
```
POST {{base_url}}/api/join
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "memberEmail": "guardian@test.com",
  "memberName": "김영희",
  "memberPassword": "password123"
}
```

**Postman Tests:**
```javascript
// Step 3.0.1 Tests 탭에 추가
pm.test("보호자 회원가입 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;

    // guardian_id 환경 변수에 저장
    pm.environment.set("guardian_id", jsonData.data.id);
});
```

---

### Step 3.1: 김순자 로그인 (노인)

**Request:**
```
POST {{base_url}}/api/auth/login
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "elderly@test.com",
  "password": "password123"
}
```

**Postman Tests:**
```javascript
// Step 3.1 Tests 탭에 추가
pm.test("김순자 로그인 성공", function() {
    pm.response.to.have.status(200);
    const authHeader = pm.response.headers.get("Authorization");
    if (authHeader) {
        const token = authHeader.replace("Bearer ", "");
        pm.environment.set("access_token", token);
    }
});
```

---

### Step 3.2: 보호자 회원 검색 (김영희 찾기)

**Request:**
```
GET {{base_url}}/api/members/search?email={{guardian_email}}
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M005",
  "message": "회원 조회 완료",
  "data": {
    "id": 2,
    "memberEmail": "guardian@test.com",
    "memberName": "김영희",
    "createdAt": "2025-10-10T10:15:00",
    "dailyCheckEnabled": true,
    "guardian": null,
    "managedMembers": []
  }
}
```

**Postman Tests:**
```javascript
// Step 3.2 Tests 탭에 추가
pm.test("보호자 검색 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.memberName).to.eql("김영희");
});
```

---

### Step 3.3: 보호자 요청 생성 (김순자 → 김영희)

**Request:**
```
POST {{base_url}}/api/guardians/requests
```

**Headers:**
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "guardianId": {{guardian_id}},
  "relation": "FAMILY"
}
```

**참고 - Relation 값:**
```
FAMILY - 가족
FRIEND - 친구
CAREGIVER - 돌봄제공자
NEIGHBOR - 이웃
OTHER - 기타
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "G001",
  "message": "보호자 요청이 생성되었습니다",
  "data": {
    "id": 1,
    "requesterId": 1,
    "requesterName": "김순자",
    "guardianId": 2,
    "guardianName": "김영희",
    "relation": "FAMILY",
    "status": "PENDING",
    "createdAt": "2025-10-10T10:20:00"
  }
}
```

**Postman Tests:**
```javascript
// Step 3.3 Tests 탭에 추가
pm.test("보호자 요청 생성 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.code).to.eql("G001");
    pm.expect(jsonData.data.status).to.eql("PENDING");

    // request_id 환경 변수에 저장
    pm.environment.set("request_id", jsonData.data.id);
});
```

---

### Step 3.4: 김영희 로그인 (보호자)

**Request:**
```
POST {{base_url}}/api/auth/login
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "guardian@test.com",
  "password": "password123"
}
```

**Postman Tests:**
```javascript
// Step 3.4 Tests 탭에 추가
pm.test("김영희 로그인 성공", function() {
    pm.response.to.have.status(200);
    const authHeader = pm.response.headers.get("Authorization");
    if (authHeader) {
        const token = authHeader.replace("Bearer ", "");
        pm.environment.set("access_token", token);
    }
});
```

---

### Step 3.5: 받은 보호자 요청 목록 조회 (김영희)

**Request:**
```
GET {{base_url}}/api/guardians/requests
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "G002",
  "message": "보호자 요청 목록 조회 완료",
  "data": [
    {
      "id": 1,
      "requesterId": 1,
      "requesterName": "김순자",
      "guardianId": 2,
      "guardianName": "김영희",
      "relation": "FAMILY",
      "status": "PENDING",
      "createdAt": "2025-10-10T10:20:00"
    }
  ]
}
```

**Postman Tests:**
```javascript
// Step 3.5 Tests 탭에 추가
pm.test("보호자 요청 목록 조회 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.greaterThan(0);
    pm.expect(jsonData.data[0].status).to.eql("PENDING");
});
```

---

### Step 3.6: 보호자 요청 수락 (김영희)

**Request:**
```
POST {{base_url}}/api/guardians/requests/{{request_id}}/accept
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "G003",
  "message": "보호자 요청이 수락되었습니다",
  "data": null
}
```

**Postman Tests:**
```javascript
// Step 3.6 Tests 탭에 추가
pm.test("보호자 요청 수락 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.code).to.eql("G003");
});
```

---

### Step 3.7: 내가 돌보는 사람 목록 조회 (김영희)

**Request:**
```
GET {{base_url}}/api/members/me/managed-members
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M005",
  "message": "회원 조회 완료",
  "data": [
    {
      "id": 1,
      "memberEmail": "elderly@test.com",
      "memberName": "김순자",
      "createdAt": "2025-10-10T10:00:00",
      "dailyCheckEnabled": true,
      "guardian": {
        "id": 2,
        "memberName": "김영희",
        "relation": "FAMILY"
      },
      "managedMembers": []
    }
  ]
}
```

**Postman Tests:**
```javascript
// Step 3.7 Tests 탭에 추가
pm.test("돌보는 사람 목록 조회 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.greaterThan(0);
    pm.expect(jsonData.data[0].memberName).to.eql("김순자");
});
```

---

### Step 3.8: 김순자 다시 로그인하여 보호자 확인

**Request:**
```
POST {{base_url}}/api/auth/login
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "elderly@test.com",
  "password": "password123"
}
```

**Postman Tests:**
```javascript
pm.test("김순자 재로그인 성공", function() {
    pm.response.to.have.status(200);
    const authHeader = pm.response.headers.get("Authorization");
    if (authHeader) {
        const token = authHeader.replace("Bearer ", "");
        pm.environment.set("access_token", token);
    }
});
```

---

### Step 3.9: 내 정보 조회 (보호자 확인)

**Request:**
```
GET {{base_url}}/api/members/me
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공 - 보호자 정보 포함):**
```json
{
  "isSuccess": true,
  "code": "M005",
  "message": "회원 조회 완료",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": true,
    "guardian": {
      "id": 2,
      "memberName": "김영희",
      "relation": "FAMILY"
    },
    "managedMembers": []
  }
}
```

**Postman Tests:**
```javascript
// Step 3.9 Tests 탭에 추가
pm.test("보호자 관계 설정 확인", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.guardian).to.not.be.null;
    pm.expect(jsonData.data.guardian.memberName).to.eql("김영희");
    pm.expect(jsonData.data.guardian.relation).to.eql("FAMILY");
});
```

---

## 사용자 플로우 4: 회원 관리

**시나리오**: 로그인한 사용자가 안부 메시지 설정을 변경하고, 정보를 수정하고, 보호자 관계를 해제합니다.

**전제 조건**: 로그인하여 `access_token` 환경 변수 설정 완료

---

### Step 4.1: 안부 메시지 설정 변경 (비활성화)

**Request:**
```
PATCH {{base_url}}/api/members/me/daily-check?enabled=false
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M006",
  "message": "회원 정보가 수정되었습니다",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": false,
    "guardian": {
      "id": 2,
      "memberName": "김영희",
      "relation": "FAMILY"
    },
    "managedMembers": []
  }
}
```

**Postman Tests:**
```javascript
// Step 4.1 Tests 탭에 추가
pm.test("안부 메시지 설정 변경 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.dailyCheckEnabled).to.be.false;
});
```

---

### Step 4.2: 안부 메시지 설정 변경 (재활성화)

**Request:**
```
PATCH {{base_url}}/api/members/me/daily-check?enabled=true
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M006",
  "message": "회원 정보가 수정되었습니다",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": true,
    "guardian": {
      "id": 2,
      "memberName": "김영희",
      "relation": "FAMILY"
    },
    "managedMembers": []
  }
}
```

---

### Step 4.3: 내 정보 수정 (이름 변경)

**Request:**
```
PUT {{base_url}}/api/members/me
```

**Headers:**
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "memberName": "김순자(수정)",
  "memberPassword": "newPassword456"
}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M006",
  "message": "회원 정보가 수정되었습니다",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자(수정)",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": true,
    "guardian": {
      "id": 2,
      "memberName": "김영희",
      "relation": "FAMILY"
    },
    "managedMembers": []
  }
}
```

**Postman Tests:**
```javascript
// Step 4.3 Tests 탭에 추가
pm.test("회원 정보 수정 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.memberName).to.eql("김순자(수정)");
});
```

---

### Step 4.4: 보호자 관계 해제

**Request:**
```
DELETE {{base_url}}/api/members/me/guardian
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "G004",
  "message": "보호자 관계가 해제되었습니다",
  "data": null
}
```

**Postman Tests:**
```javascript
// Step 4.4 Tests 탭에 추가
pm.test("보호자 관계 해제 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.code).to.eql("G004");
});
```

---

### Step 4.5: 내 정보 조회 (보호자 해제 확인)

**Request:**
```
GET {{base_url}}/api/members/me
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공 - 보호자 null):**
```json
{
  "isSuccess": true,
  "code": "M005",
  "message": "회원 조회 완료",
  "data": {
    "id": 1,
    "memberEmail": "elderly@test.com",
    "memberName": "김순자(수정)",
    "createdAt": "2025-10-10T10:00:00",
    "dailyCheckEnabled": true,
    "guardian": null,
    "managedMembers": []
  }
}
```

**Postman Tests:**
```javascript
// Step 4.5 Tests 탭에 추가
pm.test("보호자 해제 확인", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.data.guardian).to.be.null;
});
```

---

### Step 4.6: 회원 탈퇴 (선택 사항)

⚠️ **경고**: 이 작업은 되돌릴 수 없습니다. 테스트 계정에서만 실행하세요.

**Request:**
```
DELETE {{base_url}}/api/members/me
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response (성공):**
```json
{
  "isSuccess": true,
  "code": "M007",
  "message": "회원이 삭제되었습니다",
  "data": null
}
```

**Postman Tests:**
```javascript
// Step 4.6 Tests 탭에 추가
pm.test("회원 탈퇴 성공", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccess).to.be.true;
    pm.expect(jsonData.code).to.eql("M007");

    // 토큰 삭제
    pm.environment.unset("access_token");
});
```

---

## 트러블슈팅

### 1. 401 Unauthorized (인증 실패)

**원인:**
- JWT 토큰이 만료됨 (1시간 유효)
- 토큰이 올바르게 설정되지 않음

**해결책:**
```
1. 로그인 API 재실행 (Step 1.3 또는 3.1)
2. Postman Tests에서 자동으로 access_token 환경 변수가 설정되는지 확인
3. Authorization 헤더 형식 확인: Bearer {{access_token}}
```

---

### 2. 400 Bad Request (잘못된 요청)

**원인:**
- JSON 형식 오류
- 필수 필드 누락
- Validation 실패

**해결책:**
```
1. Body 탭에서 JSON 형식 확인 (중괄호, 쉼표, 따옴표)
2. 필수 필드 확인 (memberEmail, memberName, memberPassword 등)
3. Response Body의 "message" 필드 확인
```

---

### 3. 409 Conflict (중복)

**원인:**
- 이미 존재하는 이메일로 회원가입 시도
- 이미 보호자가 있는 회원에게 중복 요청

**해결책:**
```
1. 다른 이메일 주소 사용
2. 기존 데이터 삭제 후 재시도
```

---

### 4. 404 Not Found (리소스 없음)

**원인:**
- 존재하지 않는 회원 ID
- 잘못된 API 경로

**해결책:**
```
1. 회원 검색 API로 존재 여부 확인 (Step 3.2)
2. 환경 변수 값 확인 (elderly_id, guardian_id 등)
3. API 경로 확인 (/api/... 시작)
```

---

### 5. 500 Internal Server Error (서버 오류)

**원인:**
- 서버 로직 오류
- DB 연결 문제
- 외부 서비스 연동 오류 (OpenAI, Firebase)

**해결책:**
```
1. 서버 로그 확인
2. DB 연결 상태 확인 (docker-compose ps)
3. 환경 변수 확인 (.env 파일)
4. 서버 재시작 (./gradlew bootRun)
```

---

## 추가 테스트 시나리오

### A. 보호자 요청 거절 플로우

**Step A.1**: 김순자가 보호자 요청 생성 (Step 3.3 참조)
**Step A.2**: 김영희가 로그인 (Step 3.4 참조)
**Step A.3**: 받은 요청 목록 조회 (Step 3.5 참조)
**Step A.4**: 보호자 요청 거절

**Request:**
```
POST {{base_url}}/api/guardians/requests/{{request_id}}/reject
```

**Headers:**
```
Authorization: Bearer {{access_token}}
```

**Expected Response:**
```json
{
  "isSuccess": true,
  "code": "G005",
  "message": "보호자 요청이 거절되었습니다",
  "data": null
}
```

---

### B. AI 긴급 키워드 테스트

**Step B.1**: 로그인 후 긴급 키워드 포함 메시지 전송

**Request:**
```
POST {{base_url}}/api/conversations/messages
```

**Headers:**
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "content": "가슴이 아파요 도와주세요"
}
```

**예상 효과:**
- AlertRule 도메인에서 긴급 키워드 감지
- 보호자에게 HIGH 레벨 알림 발송 (Firebase FCM)

---

## 유용한 Postman 기능

### 1. Collection Runner

전체 플로우를 자동으로 순차 실행:
```
1. Postman > Collections > MARUNI Test
2. "Run" 버튼 클릭
3. 순서대로 실행 확인
```

### 2. Environment 변수 확인

현재 저장된 환경 변수 확인:
```
Postman > Environments > MARUNI Local
- access_token
- elderly_id
- guardian_id
- request_id
```

### 3. Pre-request Script (전역 설정)

Collection 수준에서 모든 요청 전에 실행:
```javascript
// Collection > Edit > Pre-request Scripts
pm.request.headers.add({
    key: 'Accept',
    value: 'application/json'
});
```

---

**문서 끝**

**다음 단계**:
1. Postman Collection 파일 생성 (`.postman_collection.json`)
2. 자동화된 통합 테스트 스크립트 작성
3. CI/CD 파이프라인 연동

**문의사항**: MARUNI 개발팀 또는 `docs/` 폴더의 도메인별 가이드 참조
