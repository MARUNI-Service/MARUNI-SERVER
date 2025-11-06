# 📢 알림 시스템 API 응답 (FE용)

**작성일**: 2025-11-06
**대상**: 프론트엔드 개발팀
**상태**: ✅ 모든 요구사항 구현 완료

---

## 📋 요구사항 해결 현황

| 요구사항 | 상태 | 비고 |
|---------|------|------|
| 알림 타입 부족 | ✅ 완료 | 8종 타입 지원 |
| 읽음 상태 추적 불가 | ✅ 완료 | `isRead`, `readAt` 필드 제공 |
| 통합 API 없음 | ✅ 완료 | `/api/notifications` 제공 |
| 보호자 요청 알림 없음 | ✅ 완료 | 모든 알림이 DB에 저장됨 |

---

## 🌐 API 엔드포인트

### 1. 전체 알림 조회
```http
GET /api/notifications
Authorization: Bearer {access_token}
```

**응답 예시**:
```json
[
  {
    "id": 123,
    "title": "보호자 등록 요청",
    "message": "김순자님이 보호자로 등록을 요청했습니다",
    "type": "GUARDIAN_REQUEST",
    "sourceType": "GUARDIAN_REQUEST",
    "sourceEntityId": 456,
    "isRead": false,
    "readAt": null,
    "createdAt": "2025-11-06T09:00:00"
  },
  {
    "id": 122,
    "title": "안부 메시지",
    "message": "안녕하세요! 오늘 하루는 어떻게 지내고 계신가요?",
    "type": "DAILY_CHECK",
    "sourceType": "DAILY_CHECK",
    "sourceEntityId": null,
    "isRead": true,
    "readAt": "2025-11-06T09:05:00",
    "createdAt": "2025-11-06T09:00:00"
  },
  {
    "id": 121,
    "title": "[HIGH] 감정 패턴 이상",
    "message": "김순자님이 3일 연속 부정 감정을 보였습니다",
    "type": "EMOTION_ALERT",
    "sourceType": "ALERT_RULE",
    "sourceEntityId": 789,
    "isRead": true,
    "readAt": "2025-11-05T14:30:00",
    "createdAt": "2025-11-05T14:00:00"
  }
]
```

**특징**:
- 최신순 정렬 (최근 알림이 먼저)
- 모든 알림 타입 포함 (안부, 보호자, 이상징후 등)
- 읽음/안읽음 상태 포함

---

### 2. 안읽은 알림 개수 조회
```http
GET /api/notifications/unread-count
Authorization: Bearer {access_token}
```

**응답 예시**:
```json
5
```

**용도**:
- 알림 뱃지 표시
- 화면 상단 알림 아이콘에 숫자 표시

---

### 3. 알림 읽음 처리
```http
PATCH /api/notifications/{id}/read
Authorization: Bearer {access_token}
```

**응답 예시**:
```json
{
  "id": 123,
  "title": "보호자 등록 요청",
  "message": "김순자님이 보호자로 등록을 요청했습니다",
  "type": "GUARDIAN_REQUEST",
  "sourceType": "GUARDIAN_REQUEST",
  "sourceEntityId": 456,
  "isRead": true,
  "readAt": "2025-11-06T10:30:00",
  "createdAt": "2025-11-06T09:00:00"
}
```

**호출 시점**:
- 사용자가 알림을 클릭/탭했을 때
- 알림 상세 화면을 열었을 때

---

## 🎯 알림 타입 (NotificationType)

| 타입 | 설명 | 예시 상황 |
|------|------|----------|
| `DAILY_CHECK` | 안부 메시지 | 매일 오전 9시 자동 발송 |
| `GUARDIAN_REQUEST` | 보호자 등록 요청 | 김순자가 김영희에게 보호자 요청 |
| `GUARDIAN_ACCEPT` | 보호자 요청 수락 | 김영희가 보호자 요청 수락 |
| `GUARDIAN_REJECT` | 보호자 요청 거절 | 김영희가 보호자 요청 거절 |
| `EMOTION_ALERT` | 감정 패턴 이상 | 3일 연속 부정 감정 감지 |
| `NO_RESPONSE_ALERT` | 무응답 이상 | 3일 연속 무응답 감지 |
| `KEYWORD_ALERT` | 키워드 감지 | "죽고싶다" 등 긴급 키워드 감지 |
| `SYSTEM` | 시스템 알림 | 점검 공지, 업데이트 안내 등 |

---

## 🔧 응답 필드 설명

### NotificationResponseDto

| 필드 | 타입 | 설명 | Nullable |
|------|------|------|----------|
| `id` | Long | 알림 ID | ❌ |
| `title` | String | 알림 제목 | ❌ |
| `message` | String | 알림 내용 | ❌ |
| `type` | String | 알림 타입 (위 8종) | ❌ |
| `sourceType` | String | 알림 출처 (`DAILY_CHECK`, `ALERT_RULE`, `GUARDIAN_REQUEST`, `SYSTEM`, `CHAT`) | ❌ |
| `sourceEntityId` | Long | 출처 엔티티 ID (상세 조회 시 사용) | ✅ |
| `isRead` | Boolean | 읽음 여부 | ❌ |
| `readAt` | String | 읽은 시각 (ISO 8601) | ✅ |
| `createdAt` | String | 생성 시각 (ISO 8601) | ❌ |

---

## 💡 UI 구현 가이드

### 1. 알림 목록 화면
```javascript
// 1. 알림 목록 조회
const response = await fetch('/api/notifications', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
const notifications = await response.json();

// 2. 안읽은 알림과 읽은 알림 분리
const unreadNotifications = notifications.filter(n => !n.isRead);
const readNotifications = notifications.filter(n => n.isRead);

// 3. 안읽은 알림은 상단에 표시, 다른 스타일 적용
```

### 2. 알림 뱃지 표시
```javascript
// 안읽은 알림 개수 조회
const response = await fetch('/api/notifications/unread-count', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
const unreadCount = await response.json();

// 뱃지 표시
if (unreadCount > 0) {
  badge.show();
  badge.text = unreadCount > 99 ? '99+' : unreadCount;
}
```

### 3. 알림 클릭 시 읽음 처리
```javascript
const handleNotificationClick = async (notification) => {
  // 안읽은 알림인 경우에만 읽음 처리
  if (!notification.isRead) {
    await fetch(`/api/notifications/${notification.id}/read`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });
  }

  // 알림 타입에 따라 화면 이동
  switch (notification.type) {
    case 'GUARDIAN_REQUEST':
      navigate(`/guardian/requests/${notification.sourceEntityId}`);
      break;
    case 'EMOTION_ALERT':
    case 'NO_RESPONSE_ALERT':
    case 'KEYWORD_ALERT':
      navigate(`/alerts/${notification.sourceEntityId}`);
      break;
    case 'DAILY_CHECK':
      navigate('/conversations');
      break;
    default:
      // 기본 동작
  }
};
```

---

## ⚠️ 주의사항

### 1. 인증
- 모든 API는 JWT 인증이 필수입니다
- `Authorization: Bearer {access_token}` 헤더 필수

### 2. 읽음 처리 타이밍
- 알림을 **클릭/탭했을 때** 즉시 읽음 처리
- 목록에 표시만 했다고 자동으로 읽음 처리하지 않음

### 3. sourceEntityId 활용
- `null`일 수 있음 (안부 메시지 등)
- 상세 조회가 필요한 경우에만 사용
  - `GUARDIAN_REQUEST` → `/api/guardian/requests/{sourceEntityId}`
  - `EMOTION_ALERT` 등 → `/api/alert-rules/history/{sourceEntityId}`

### 4. 알림 타입별 UI
- `KEYWORD_ALERT` (긴급): 🔴 빨간색 표시 권장
- `EMOTION_ALERT`, `NO_RESPONSE_ALERT` (높음): 🟠 주황색 표시 권장
- `GUARDIAN_REQUEST` (요청): 🔵 파란색 표시 권장
- `DAILY_CHECK` (일반): 기본 색상

---

## 🔄 실시간 알림 업데이트 (권장)

### Polling 방식
```javascript
// 30초마다 안읽은 알림 개수 조회
setInterval(async () => {
  const count = await fetch('/api/notifications/unread-count', {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  }).then(r => r.json());

  updateBadge(count);
}, 30000);
```

### 화면 진입 시 갱신
```javascript
// 알림 화면 진입 시 최신 목록 조회
useEffect(() => {
  fetchNotifications();
}, []);
```

---

## ✅ 체크리스트

프론트엔드 구현 시 확인해야 할 사항:

- [ ] 알림 목록 조회 API 연동
- [ ] 안읽은 알림 개수 뱃지 표시
- [ ] 알림 클릭 시 읽음 처리
- [ ] 안읽은 알림과 읽은 알림 UI 구분
- [ ] 알림 타입별 아이콘/색상 적용
- [ ] 긴급 알림(KEYWORD_ALERT) 강조 표시
- [ ] sourceEntityId를 통한 상세 화면 이동
- [ ] JWT 토큰 만료 처리
- [ ] 에러 핸들링 (401, 404 등)

---

## 📞 문의사항

API 관련 추가 문의사항이 있으면 백엔드 팀에 문의해주세요.

**문서 버전**: 1.0.0
**최종 업데이트**: 2025-11-06
