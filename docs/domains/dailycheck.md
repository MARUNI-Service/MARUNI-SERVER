# DailyCheck 도메인

**최종 업데이트**: 2025-10-09
**상태**: ✅ TDD Red-Green-Blue 완성 (SRP 리팩토링)

## 📋 개요

매일 정시 안부 메시지 자동 발송 및 재시도 시스템입니다.

### 핵심 기능
- 매일 오전 9시 자동 발송
- 중복 발송 방지 (DB 제약 조건)
- 실패 시 자동 재시도 (최대 3회)
- 점진적 지연 (5분 간격)

## 🏗️ 주요 엔티티

### DailyCheckRecord
```java
- id: Long
- memberId: Long
- checkDate: LocalDate       // 발송 날짜 (중복 방지용)
- message: String             // 발송된 메시지
- success: Boolean            // 발송 성공 여부
```

### RetryRecord
```java
- id: Long
- memberId: Long
- message: String             // 재시도할 메시지
- scheduledTime: LocalDateTime // 재시도 예정 시간
- retryCount: Integer         // 현재 재시도 횟수
- completed: Boolean          // 완료 여부
```

## 🔧 핵심 서비스

### DailyCheckScheduler (스케줄링 트리거)
- `triggerDailyCheck()`: 매일 오전 9시 실행 (`0 0 9 * * *`)
- `triggerRetryProcess()`: 5분마다 재시도 실행 (`0 */5 * * * *`)

### DailyCheckOrchestrator (비즈니스 로직)
- `processAllActiveMembers()`: 전체 회원 안부 메시지 발송
- `processAllRetries()`: 실패 건 재시도 처리
- `isAlreadySentToday(memberId)`: 중복 발송 방지 체크

### RetryService (재시도 관리)
- `scheduleRetry(memberId, message)`: 재시도 스케줄링
- `getPendingRetries()`: 대기 중인 재시도 조회
- `markCompleted(retryRecord)`: 재시도 완료 처리

## 🔗 도메인 연동

- **Member**: 활성 회원 목록 조회 (`findActiveMemberIds()`)
- **Conversation**: 시스템 메시지 저장 (`processSystemMessage()`)
- **Notification**: 푸시 알림 발송 (`sendPushNotification()`)
- **AlertRule**: 무응답 패턴 분석용 기록 제공

## ⚙️ 설정

### application.yml
```yaml
maruni:
  scheduling:
    daily-check:
      cron: "0 0 9 * * *"     # 매일 오전 9시
    retry:
      cron: "0 */5 * * * *"   # 5분마다 재시도
```

### 시간 제한
- 발송 허용 시간: 오전 7시 ~ 오후 9시
- 허용 시간 외에는 발송하지 않음

## 📁 패키지 구조

```
dailycheck/
├── application/scheduler/    # Scheduler 계층
│   ├── DailyCheckScheduler.java      # 스케줄링 트리거
│   ├── DailyCheckOrchestrator.java   # 비즈니스 로직
│   └── RetryService.java             # 재시도 관리
└── domain/
    ├── entity/               # DailyCheckRecord, RetryRecord
    └── repository/           # JPA Repository
```

## 🧪 테스트 완성도

- ✅ **15개 테스트 시나리오**: Entity(4) + Repository(4) + Service(7)
- ✅ **TDD 완전 사이클**: Red → Green → Blue
- ✅ **SRP 리팩토링**: 단일 책임 원칙 준수
- ✅ **100% 통과**: 모든 테스트 성공

## ✅ 완성도

- [x] 매일 자동 발송 (Cron)
- [x] 중복 발송 방지 (DB 제약)
- [x] 자동 재시도 (최대 3회)
- [x] 점진적 지연 (5분 간격)
- [x] SRP 클래스 분리
- [x] TDD 완전 적용

**상용 서비스 수준 완성**
