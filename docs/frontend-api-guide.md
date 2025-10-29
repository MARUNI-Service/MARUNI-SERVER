# MARUNI Frontend API 연동 가이드

**최종 업데이트**: 2025-10-29
**버전**: 2.0.0
**상태**: Phase 2 MVP 완성

## 📋 목차

1. [개요](#-개요)
2. [인증 (Authentication)](#-인증-authentication)
3. [공통 응답 포맷](#-공통-응답-포맷)
4. [API 엔드포인트](#-api-엔드포인트)
5. [에러 코드](#-에러-코드)
6. [데이터 타입 & Enum](#-데이터-타입--enum)

---

## 🎯 개요

### 서비스 설명
MARUNI는 노인 돌봄을 위한 AI 기반 소통 플랫폼입니다.

**핵심 플로우**:
```
매일 오전 9시 안부 메시지 자동 발송
    ↓
사용자 응답 → AI 분석 (OpenAI GPT-4o)
    ↓
이상징후 감지 (3종 알고리즘)
    ↓
보호자 알림 발송 (Firebase FCM)
```

### Base URL
```
개발 환경: http://localhost:8080
운영 환경: https://api.maruni.com (예정)
```

### API 특징
- **RESTful API**: HTTP 표준 메서드 사용
- **JWT 인증**: Access Token Only, 1시간 유효
- **JSON 포맷**: 모든 요청/응답은 JSON
- **표준 응답**: CommonApiResponse 래핑 구조
- **Swagger 문서**: `/swagger-ui.html` 에서 확인 가능

---

## 🔐 인증 (Authentication)

### JWT Access Token 방식

#### 1. 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
  "memberEmail": "user@example.com",
  "memberPassword": "securePassword123!"
}
```

**응답 성공 (200)**:
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": null
}
```
- **중요**: Access Token은 응답 헤더 `Authorization`에 포함됩니다
- 형식: `Bearer {access_token}`
- 유효기간: 1시간

#### 2. 인증이 필요한 API 호출
```http
GET /api/members/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 3. 로그아웃
- **서버 처리 불필요**: 클라이언트에서 토큰 삭제
```javascript
// 클라이언트 로그아웃 처리
localStorage.removeItem('access_token');
// 또는
sessionStorage.removeItem('access_token');
```

### 인증 에러 처리
```json
{
  "isSuccess": false,
  "code": "A402",
  "message": "유효하지 않은 토큰입니다",
  "data": null
}
```

---

## 📦 공통 응답 포맷

### CommonApiResponse 구조

모든 API 응답은 다음 구조로 래핑됩니다:

```typescript
interface CommonApiResponse<T> {
  isSuccess: boolean;    // 성공 여부
  code: string;          // 응답 코드 (S001, M404 등)
  message: string;       // 응답 메시지
  data: T | null;        // 실제 데이터 (실패 시 null)
}
```

### 성공 응답 예시

**단일 객체**:
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    "id": 1,
    "memberEmail": "user@example.com",
    "memberName": "홍길동"
  }
}
```

**배열 응답**:
```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": [
    {"id": 1, "memberName": "홍길동"},
    {"id": 2, "memberName": "김철수"}
  ]
}
```

### 실패 응답 예시
```json
{
  "isSuccess": false,
  "code": "M404",
  "message": "회원을 찾을 수 없습니다",
  "data": null
}
```

---

## 🌐 API 엔드포인트

### 1. 인증 (Auth)

#### 1.1 로그인
```http
POST /api/auth/login
```

**Request Body**:
```typescript
{
  memberEmail: string;      // 이메일 (필수)
  memberPassword: string;   // 비밀번호 (필수)
}
```

**Response**: Access Token이 응답 헤더에 포함

---

### 2. 회원가입 (Join)

#### 2.1 회원가입
```http
POST /api/join
```

**Request Body**:
```typescript
{
  memberEmail: string;           // 이메일 (필수)
  memberName: string;            // 이름 (필수)
  memberPassword: string;        // 비밀번호 (필수)
  dailyCheckEnabled: boolean;    // 안부 메시지 수신 여부 (필수)
}
```

**Response**: `MemberResponse`

#### 2.2 이메일 중복 확인
```http
GET /api/join/email-check?memberEmail={email}
```

**Response**:
```typescript
{
  available: boolean;  // 사용 가능 여부
  email: string;       // 확인한 이메일
}
```

---

### 3. 회원 관리 (Member)

#### 3.1 회원 검색 (이메일)
```http
GET /api/members/search?email={email}
Authorization: Bearer {token}
```

**Response**: `MemberResponse`

#### 3.2 내 정보 조회
```http
GET /api/members/me
Authorization: Bearer {token}
```

**Response**: `MemberResponse`
```typescript
{
  id: number;
  memberName: string;
  memberEmail: string;
  dailyCheckEnabled: boolean;
  hasPushToken: boolean;
  guardian: {                      // 보호자 정보 (없으면 null)
    memberId: number;
    memberName: string;
    memberEmail: string;
    relation: "FAMILY" | "FRIEND" | "CAREGIVER" | "NEIGHBOR" | "OTHER";
  } | null;
  managedMembers: [                // 내가 돌보는 사람들 (없으면 빈 배열)
    {
      memberId: number;
      memberName: string;
      memberEmail: string;
      relation: "FAMILY" | "FRIEND" | "CAREGIVER" | "NEIGHBOR" | "OTHER";
      dailyCheckEnabled: boolean;
      lastDailyCheckAt: string | null;  // ISO 8601 형식
    }
  ];
  createdAt: string;               // ISO 8601 형식
  updatedAt: string;               // ISO 8601 형식
}
```

#### 3.3 내 정보 수정
```http
PUT /api/members/me
Authorization: Bearer {token}
```

**Request Body**:
```typescript
{
  memberEmail: string;      // 이메일 (필수)
  memberName: string;       // 이름 (필수)
  memberPassword: string;   // 비밀번호 (필수)
}
```

**Response**: `MemberResponse`

#### 3.4 내 계정 삭제
```http
DELETE /api/members/me
Authorization: Bearer {token}
```

**Response**: 204 No Content

#### 3.5 내가 돌보는 사람들 목록
```http
GET /api/members/me/managed-members
Authorization: Bearer {token}
```

**Response**: `MemberResponse[]` (배열)

#### 3.6 안부 메시지 설정 변경
```http
PATCH /api/members/me/daily-check?enabled={true|false}
Authorization: Bearer {token}
```

**Response**: `MemberResponse`

#### 3.7 내 보호자 관계 해제
```http
DELETE /api/members/me/guardian
Authorization: Bearer {token}
```

**Response**: 200 OK

---

### 4. AI 대화 (Conversation)

#### 4.1 AI 대화 메시지 전송
```http
POST /api/conversations/messages
Authorization: Bearer {token}
```

**Request Body**:
```typescript
{
  content: string;  // 메시지 내용 (필수, 최대 500자)
}
```

**Response**: `ConversationResponseDto`
```typescript
{
  conversationId: number;
  userMessage: {
    id: number;
    type: "USER_MESSAGE" | "AI_RESPONSE" | "SYSTEM_MESSAGE";
    content: string;
    emotion: "POSITIVE" | "NEGATIVE" | "NEUTRAL" | null;
    createdAt: string;  // ISO 8601 형식
  };
  aiMessage: {
    id: number;
    type: "USER_MESSAGE" | "AI_RESPONSE" | "SYSTEM_MESSAGE";
    content: string;
    emotion: "POSITIVE" | "NEGATIVE" | "NEUTRAL" | null;
    createdAt: string;  // ISO 8601 형식
  };
}
```

#### 4.2 내 대화 전체보기
```http
GET /api/conversations/history?days={7}
Authorization: Bearer {token}
```

**Query Parameters**:
- `days`: 조회 기간 (일), 기본값: 7

**Response**: `MessageDto[]` (배열)
```typescript
[
  {
    id: number;
    type: "USER_MESSAGE" | "AI_RESPONSE" | "SYSTEM_MESSAGE";
    content: string;
    emotion: "POSITIVE" | "NEGATIVE" | "NEUTRAL" | null;
    createdAt: string;  // ISO 8601 형식
  }
]
```

---

### 5. 보호자 관계 (Guardian)

#### 5.1 보호자 요청 생성
```http
POST /api/guardians/requests
Authorization: Bearer {token}
```

**Request Body**:
```typescript
{
  guardianId: number;                                             // 보호자 회원 ID (필수)
  relation: "FAMILY" | "FRIEND" | "CAREGIVER" | "NEIGHBOR" | "OTHER";  // 관계 (필수)
}
```

**Response**: `GuardianRequestResponse`
```typescript
{
  id: number;
  requester: {                 // 요청자 정보
    id: number;
    name: string;
    email: string;
  };
  guardian: {                  // 보호자 정보
    id: number;
    name: string;
    email: string;
  };
  relation: "FAMILY" | "FRIEND" | "CAREGIVER" | "NEIGHBOR" | "OTHER";
  status: "PENDING" | "ACCEPTED" | "REJECTED";
  createdAt: string;           // ISO 8601 형식
}
```

#### 5.2 내가 받은 보호자 요청 목록
```http
GET /api/guardians/requests
Authorization: Bearer {token}
```

**Response**: `GuardianRequestResponse[]` (배열)

#### 5.3 보호자 요청 수락
```http
POST /api/guardians/requests/{requestId}/accept
Authorization: Bearer {token}
```

**Response**: 200 OK

#### 5.4 보호자 요청 거절
```http
POST /api/guardians/requests/{requestId}/reject
Authorization: Bearer {token}
```

**Response**: 200 OK

---

### 6. 이상징후 감지 (AlertRule)

#### 6.1 알림 규칙 생성
```http
POST /api/alert-rules
Authorization: Bearer {token}
```

**Request Body**:
```typescript
{
  alertType: "EMOTION_PATTERN" | "NO_RESPONSE" | "KEYWORD_DETECTION";  // 알림 유형 (필수)
  alertLevel: "EMERGENCY" | "HIGH" | "MEDIUM" | "LOW";                  // 알림 레벨 (필수)
  condition: {                                                          // 감지 조건 (필수)
    consecutiveDays?: number;    // 연속 일수
    thresholdCount?: number;     // 임계값
    keywords?: string[];         // 키워드 목록
  };
}
```

**Response**: `AlertRuleResponseDto`
```typescript
{
  id: number;
  memberId: number;
  alertType: "EMOTION_PATTERN" | "NO_RESPONSE" | "KEYWORD_DETECTION";
  alertLevel: "EMERGENCY" | "HIGH" | "MEDIUM" | "LOW";
  ruleName: string;
  condition: {
    consecutiveDays?: number;
    thresholdCount?: number;
    keywords?: string[];
  };
  description: string;
  active: boolean;
  createdAt: string;  // ISO 8601 형식
  updatedAt: string;  // ISO 8601 형식
}
```

#### 6.2 알림 규칙 목록 조회
```http
GET /api/alert-rules
Authorization: Bearer {token}
```

**Response**: `AlertRuleResponseDto[]` (배열)

#### 6.3 알림 규칙 상세 조회
```http
GET /api/alert-rules/{id}
Authorization: Bearer {token}
```

**Response**: `AlertRuleResponseDto`

#### 6.4 알림 규칙 수정
```http
PUT /api/alert-rules/{id}
Authorization: Bearer {token}
```

**Request Body**:
```typescript
{
  ruleName: string;                                 // 규칙 이름
  description: string;                              // 설명
  alertLevel: "EMERGENCY" | "HIGH" | "MEDIUM" | "LOW";  // 알림 레벨
}
```

**Response**: `AlertRuleResponseDto`

#### 6.5 알림 규칙 삭제
```http
DELETE /api/alert-rules/{id}
Authorization: Bearer {token}
```

**Response**: 200 OK

#### 6.6 알림 규칙 활성화/비활성화
```http
POST /api/alert-rules/{id}/toggle?active={true|false}
Authorization: Bearer {token}
```

**Response**: `AlertRuleResponseDto`

#### 6.7 알림 이력 조회
```http
GET /api/alert-rules/history?days={30}
Authorization: Bearer {token}
```

**Query Parameters**:
- `days`: 조회 기간 (일), 기본값: 30

**Response**: `AlertHistoryResponseDto[]` (배열)
```typescript
[
  {
    id: number;
    alertRuleId: number;
    memberId: number;
    alertLevel: "EMERGENCY" | "HIGH" | "MEDIUM" | "LOW";
    alertMessage: string;
    detectionDetails: string;       // JSON 형태 문자열
    isNotificationSent: boolean;
    notificationSentAt: string | null;  // ISO 8601 형식
    notificationResult: string;
    alertDate: string;              // ISO 8601 형식
    createdAt: string;              // ISO 8601 형식
  }
]
```

#### 6.8 알림 상세 조회
```http
GET /api/alert-rules/history/{alertId}
Authorization: Bearer {token}
```

**Response**: `AlertHistoryResponseDto`

#### 6.9 수동 이상징후 감지
```http
POST /api/alert-rules/detect
Authorization: Bearer {token}
```

**Response**:
```typescript
{
  memberId: number;
  detectedAnomalies: [...];  // 감지된 이상징후 목록
}
```

---

## ⚠️ 에러 코드

### HTTP 상태 코드
| 상태 코드 | 의미 | 설명 |
|---------|------|------|
| 200 | OK | 요청 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 잘못된 요청 |
| 401 | Unauthorized | 인증 실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 중복 (이메일 등) |
| 429 | Too Many Requests | 요청 한도 초과 |
| 500 | Internal Server Error | 서버 오류 |

### 도메인별 에러 코드

#### General (G)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| G400 | 잘못된 형식의 토큰입니다 | 400 |
| G401 | 잘못된 입력값입니다 | 400 |
| G402 | 파라미터 검증에 실패했습니다 | 400 |

#### Authentication (A)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| A401 | 이메일 또는 비밀번호가 틀렸습니다 | 401 |
| A402 | 유효하지 않은 토큰입니다 | 401 |
| A403 | 만료된 토큰입니다 | 401 |
| A405 | 액세스 토큰이 필요합니다 | 401 |

#### Member (M)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| M404 | 회원을 찾을 수 없습니다 | 404 |
| M409 | 이미 가입된 이메일입니다 | 409 |

#### Guardian (GU, GR)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| GU404 | 보호자를 찾을 수 없습니다 | 404 |
| GU405 | 회원에게 보호자가 설정되지 않았습니다 | 404 |
| GU410 | 이미 보호자가 설정된 회원입니다 | 409 |
| GU412 | 자기 자신을 보호자로 설정할 수 없습니다 | 400 |
| GR404 | 보호자 요청을 찾을 수 없습니다 | 404 |
| GR400 | 이미 처리된 요청입니다 | 400 |
| GR409 | 이미 대기 중인 요청이 있습니다 | 409 |
| GR403 | 보호자 권한이 없습니다 | 403 |

#### Conversation (C)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| C404 | 대화를 찾을 수 없습니다 | 404 |
| C400 | 메시지 내용은 필수입니다 | 400 |
| C401 | 메시지는 500자를 초과할 수 없습니다 | 400 |
| C429 | 일일 메시지 한도를 초과했습니다 | 429 |

#### AI (AI)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| AI500 | AI 응답 생성에 실패했습니다 | 500 |
| AI429 | AI API 사용 한도를 초과했습니다 | 429 |

#### AlertRule (AR)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| AR404 | 알림 규칙을 찾을 수 없습니다 | 404 |
| AR403 | 알림 규칙에 접근할 권한이 없습니다 | 403 |
| AR400 | 유효하지 않은 알림 조건입니다 | 400 |
| AR401 | 지원하지 않는 알림 타입입니다 | 400 |

#### Notification (N)
| 코드 | 메시지 | HTTP |
|-----|--------|------|
| N500 | Firebase 연결에 실패했습니다 | 500 |
| N400 | 유효하지 않은 푸시 토큰입니다 | 400 |
| N405 | 푸시 토큰을 찾을 수 없습니다 | 404 |
| N503 | 알림 서비스를 사용할 수 없습니다 | 503 |

---

## 📘 데이터 타입 & Enum

### GuardianRelation (보호자 관계)
```typescript
type GuardianRelation =
  | "FAMILY"      // 가족
  | "FRIEND"      // 친구
  | "CAREGIVER"   // 돌봄제공자
  | "NEIGHBOR"    // 이웃
  | "OTHER";      // 기타
```

### RequestStatus (요청 상태)
```typescript
type RequestStatus =
  | "PENDING"     // 대기중
  | "ACCEPTED"    // 수락됨
  | "REJECTED";   // 거절됨
```

### MessageType (메시지 타입)
```typescript
type MessageType =
  | "USER_MESSAGE"     // 사용자 메시지
  | "AI_RESPONSE"      // AI 응답
  | "SYSTEM_MESSAGE";  // 시스템 메시지
```

### EmotionType (감정 분석 결과)
```typescript
type EmotionType =
  | "POSITIVE"   // 긍정
  | "NEGATIVE"   // 부정
  | "NEUTRAL";   // 중립
```

### AlertType (알림 유형)
```typescript
type AlertType =
  | "EMOTION_PATTERN"      // 감정 패턴 분석
  | "NO_RESPONSE"          // 무응답 패턴 분석
  | "KEYWORD_DETECTION";   // 키워드 감지
```

### AlertLevel (알림 레벨)
```typescript
type AlertLevel =
  | "EMERGENCY"   // 긴급 (즉시 알림)
  | "HIGH"        // 높음
  | "MEDIUM"      // 중간
  | "LOW";        // 낮음
```

---

## 📌 중요 사항

### 1. 날짜/시간 형식
- **ISO 8601 형식**: `YYYY-MM-DDTHH:mm:ss`
- 예시: `2025-10-29T14:30:00`
- **타임존**: 서버 기준 (KST, Asia/Seoul)

### 2. 문자열 길이 제한
- 메시지 내용: 최대 500자
- 이메일: 표준 이메일 형식
- 비밀번호: 최소 8자 권장

### 3. 인증 토큰 관리
- Access Token 유효기간: 1시간
- 만료 시 재로그인 필요
- 토큰은 로컬 스토리지에 안전하게 보관
- HTTPS 사용 권장

### 4. 에러 처리
- 모든 에러는 `isSuccess: false` 로 응답
- `code`와 `message`를 활용한 에러 처리
- 네트워크 에러는 클라이언트에서 별도 처리

### 5. 페이징
- 현재 페이징은 `days` 파라미터로 기간 제한
- 향후 offset/limit 기반 페이징 추가 예정

### 6. CORS 설정
- 개발 환경: `localhost:3000`, `localhost:5173` 허용
- 운영 환경: 프론트엔드 도메인 화이트리스트

---

## 🔗 참고 문서

- **Swagger API 문서**: `http://localhost:8080/swagger-ui.html`
- **도메인 가이드**: `docs/domains/`
- **기술 규격서**: `docs/specifications/`
- **프로젝트 개요**: `docs/README.md`

---

**Contact**: dev@maruni.com
**GitHub**: https://github.com/maruni/maruni-server
