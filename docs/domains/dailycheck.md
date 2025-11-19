# DailyCheck 도메인

**최종 업데이트**: 2025-11-09
**상태**: ✅ TDD Red-Green-Blue 완성 (SRP 리팩토링 + 메시지 다양화)

## 📋 개요

매일 정시 안부 메시지 자동 발송 및 재시도 시스템입니다.

### 핵심 기능
- 매일 오전 9시 자동 발송
- 요일별 + 계절별 다양한 메시지 생성 (500+ 조합)
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

### DailyCheckMessageProvider (메시지 생성)
- `generateMessage()`: 오늘 날짜 기준 메시지 생성
- `generateMessage(LocalDate)`: 특정 날짜 기준 메시지 생성 (테스트용)
- 템플릿 기반 메시지 조합 (`{season}` 플레이스홀더)
- Seed 기반 의사 랜덤 선택 (결정적 + 예측 불가)

### RetryService (재시도 관리)
- `scheduleRetry(memberId, message)`: 재시도 스케줄링
- `getPendingRetries()`: 대기 중인 재시도 조회
- `markCompleted(retryRecord)`: 재시도 완료 처리

## 📦 주요 VO

### SeasonType (계절 타입)
```java
- SPRING("봄"): 3~5월
- SUMMER("여름"): 6~8월
- AUTUMN("가을"): 9~11월
- WINTER("겨울"): 12~2월
```

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
      cron: "0 0 0 * * *"     # 매일 UTC 자정 (KST 오전 9시)
    retry:
      cron: "0 */5 * * * *"   # 5분마다 재시도
```

### 시간 제한
- 발송 허용 시간: 오전 7시 ~ 오후 9시
- 허용 시간 외에는 발송하지 않음

## 📁 패키지 구조

```
dailycheck/
├── application/
│   ├── scheduler/            # Scheduler 계층
│   │   ├── DailyCheckScheduler.java      # 스케줄링 트리거
│   │   ├── DailyCheckOrchestrator.java   # 비즈니스 로직
│   │   └── RetryService.java             # 재시도 관리
│   └── service/              # Application Service
│       └── DailyCheckMessageProvider.java # 메시지 생성
└── domain/
    ├── entity/               # DailyCheckRecord, RetryRecord
    ├── repository/           # JPA Repository
    └── vo/                   # Value Object
        └── SeasonType.java   # 계절 타입 Enum
```

## 🧪 테스트 완성도

- ✅ **28개 테스트 시나리오**: Entity(4) + Repository(4) + VO(4) + Service(16)
- ✅ **TDD 완전 사이클**: Red → Green → Blue
- ✅ **SRP 리팩토링**: 단일 책임 원칙 준수
- ✅ **100% 통과**: 모든 테스트 성공

### 메시지 다양화 테스트
- 계절 경계 케이스 (12월→1월→2월 WINTER 연속성)
- 같은 날짜 결정성 (100회 호출 → 동일 메시지)
- 28개 조합 완전성 (7요일 × 4계절)
- 템플릿 플레이스홀더 치환 검증

## ✅ 완성도

- [x] 매일 자동 발송 (Cron)
- [x] 요일별 + 계절별 메시지 다양화 (500+ 조합)
- [x] 중복 발송 방지 (DB 제약)
- [x] 자동 재시도 (최대 3회)
- [x] 점진적 지연 (5분 간격)
- [x] SRP 클래스 분리
- [x] TDD 완전 적용

**상용 서비스 수준 완성**
