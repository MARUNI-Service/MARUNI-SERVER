🚨 현재 알림 시스템 문제점 정리

1️⃣ 알림 타입 불일치

UI에서 필요한 알림 타입:
├─ GUARDIAN_REQUEST (보호자 등록 요청)  ❌ API 없음
├─ ALERT (이상 징후)                    ✅ /api/alert-rules/history
├─ DAILY_CHECK (안부 메시지)             ❌ API 없음
└─ SYSTEM (시스템 알림)                  ❌ API 없음

결과: Alert History API는 이상 징후 알림만 제공, 다른 알림은 불가능

  ---
2️⃣ 읽음/안읽음 상태 추적 불가

// AlertHistoryResponseDto (현재 서버)
{
isNotificationSent: boolean,  // ❌ 알림 전송 여부 (읽음 여부 아님)
notificationSentAt: string    // ❌ 전송 시각 (읽은 시각 아님)
}

// Notification (UI에서 필요)
{
isRead: boolean  // ⭐ 사용자가 실제로 읽었는지
}

결과: 사용자가 알림을 읽었는지 안 읽었는지 알 수 없음

  ---
3️⃣ 통합 알림 API 부재

현재 상태:
- /api/notifications ❌ 없음
- /api/alert-rules/history ✅ 있음 (이상 징후만)

필요한 기능:
- GET /api/notifications (모든 알림 조회)
- PATCH /api/notifications/{id}/read (읽음 처리)

결과: 여러 소스에서 알림을 가져와 클라이언트에서 병합해야 함 (복잡도 증가)

  ---
4️⃣ 보호자 요청 알림 처리 불가

보호자 요청 플로우:
1. 김순자가 김영희에게 보호자 요청
2. 김영희에게 푸시 알림 발송 ✅
3. 김영희가 앱에서 알림 목록 확인 ❌ (저장된 알림 기록 없음)

결과: 푸시 알림을 놓치면 요청을 알 수 없음

  ---
📝 요약

| 문제           | 영향도   | 설명                      |
  |--------------|-------|-------------------------|
| 알림 타입 부족     | 🔴 높음 | ALERT만 지원, 나머지 3개 타입 불가 |
| 읽음 상태 추적 불가  | 🔴 높음 | 안읽은 알림 표시 불가능           |
| 통합 API 없음    | 🟡 중간 | 여러 API 병합 필요 (복잡도↑)     |
| 보호자 요청 알림 없음 | 🔴 높음 | 푸시 놓치면 요청 확인 불가         |
이 문제점들을 해결하기 위해서는 서버 측에서 새로운 알림 타입 추가, 읽음 상태 추적 기능 구현, 통합 알림 API 제공이 필요합니다.
