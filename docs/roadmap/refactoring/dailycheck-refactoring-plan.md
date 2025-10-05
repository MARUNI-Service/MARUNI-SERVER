# DailyCheck 도메인 리팩토링 플랜 (수정본)

## 🎯 리팩토링 목표 및 우선순위

**현실적이고 실용적인 관점에서 DailyCheck 도메인의 최소 필요 개선만 진행**

### 📊 현재 상황 재평가
- ✅ **Phase 2 MVP 100% 완성**: 안정적으로 운영 중
- ✅ **TDD 완전 사이클 완료**: 83% 코드 개선, 100% 테스트 커버리지
- ✅ **실제 문제 없음**: 스케줄링 시스템 정상 작동
- 🎯 **다음 우선순위**: Phase 3 고급 건강 분석 + 모바일 연동

### 🔄 수정된 리팩토링 원칙
- ✅ **최소 필요 개선**: 명확한 구조적 문제만 해결
- ✅ **조건부 확장**: 실제 요구사항 발생 시에만 추가 기능 구현
- ✅ **우선순위 존중**: Phase 3 핵심 기능 개발이 우선
- ❌ **오버 엔지니어링 금지**: 현재 필요하지 않은 기능 선제 구현 지양

## 📊 현재 상태 분석

### ✅ 잘 구현된 부분 (유지)
- **안정적인 스케줄링**: Cron 기반 매일 발송 + 재시도 시스템
- **견고한 중복 방지**: DB 제약 조건 + 비즈니스 로직 이중 보호
- **TDD 품질**: 83% 코드 개선, 100% 테스트 커버리지
- **Entity 설계**: BaseTimeEntity 상속, 정적 팩토리 메서드 적용
- **비즈니스 로직 캡슐화**: Entity에 적절한 메서드 분리

### 🔄 실제 개선이 필요한 부분 (재평가)
1. **스케줄링과 비즈니스 로직 혼재**: DailyCheckService가 @Scheduled와 복합 비즈니스 로직을 함께 처리
   - **실제 영향**: 테스트 복잡성, 책임 경계 모호성
   - **개선 필요도**: ⭐⭐⭐ (높음)

2. ~~**DTO 계층 부재**~~ → **보류**: 외부 API 노출 계획 없음
3. ~~**Presentation Layer 없음**~~ → **조건부**: 실제 관리 요구사항 발생 시에만
4. ~~**글로벌 표준 미적용**~~ → **불필요**: 내부 자동화 시스템에 과도한 표준화

## 🛠️ 수정된 리팩토링 플랜 (최소한의 개선)

### ✅ 즉시 진행: Service 메서드 분리 (현재 클래스 내에서)

#### 현재 상황
```java
// DailyCheckService - 스케줄링 트리거와 비즈니스 로직 혼재
public class DailyCheckService {
    @Scheduled(cron = "...")
    public void sendDailyCheckMessages() {
        // 복합적인 비즈니스 로직 (7가지 책임)
        List<Long> activeMemberIds = memberRepository.findActiveMemberIds();
        for (Long memberId : activeMemberIds) {
            processMemberDailyCheck(memberId);  // 개별 처리
        }
    }
}
```

#### 최소한의 개선 (클래스 분리 없이)
```java
// DailyCheckService - 책임 경계만 명확히 분리
@Service
@Transactional(readOnly = true)
public class DailyCheckService {

    // 스케줄링 트리거 (단순 위임)
    @Scheduled(cron = "${maruni.scheduling.daily-check.cron}")
    public void triggerDailyCheck() {
        log.info("Daily check triggered by scheduler");
        processAllActiveMembers();
    }

    @Scheduled(cron = "${maruni.scheduling.retry.cron}")
    public void triggerRetryProcess() {
        log.info("Retry process triggered by scheduler");
        processAllRetries();
    }

    // 실제 비즈니스 로직들 (기존 구현 유지)
    @Transactional
    public void processAllActiveMembers() {
        List<Long> activeMemberIds = memberRepository.findActiveMemberIds();
        log.info("Found {} active members", activeMemberIds.size());
        for (Long memberId : activeMemberIds) {
            processMemberDailyCheck(memberId);
        }
    }

    @Transactional
    public void processAllRetries() {
        List<RetryRecord> pendingRetries = retryRecordRepository.findPendingRetries(LocalDateTime.now());
        log.info("Found {} pending retries", pendingRetries.size());
        for (RetryRecord retryRecord : pendingRetries) {
            processRetryRecord(retryRecord);
        }
    }

    // 기존 private 메서드들 그대로 유지
    private void processMemberDailyCheck(Long memberId) { ... }
    private void processRetryRecord(RetryRecord retryRecord) { ... }
    // ... 기타 메서드들
}
```

### ⏳ 조건부 진행: 관리 기능 (실제 요구사항 발생 시에만)

#### 진행 조건
다음 중 **하나라도 만족**하는 경우에만 관리 API 개발 진행:

1. **운영팀 요구사항**: "DailyCheck 발송 현황을 실시간으로 확인하고 싶다"
2. **모니터링 필요성**: 시스템 장애 시 빠른 상황 파악 필요
3. **Phase 3 완료 후**: 핵심 기능 개발 완료 후 여유 시점
4. **대시보드 계획**: 관리자 대시보드 구축 계획 확정 시

#### 최소 관리 기능 (3개 API만)
```java
@RestController
@RequestMapping("/api/admin/daily-checks")
@RequiredArgsConstructor
@AutoApiResponse
public class DailyCheckController {

    private final DailyCheckService dailyCheckService;

    @GetMapping("/today/stats")
    @Operation(summary = "오늘 발송 통계", description = "성공/실패 개수, 성공률")
    public DailyCheckStatsDto getTodayStats() {
        return dailyCheckService.getTodayStats();
    }

    @GetMapping("/retries/pending")
    @Operation(summary = "재시도 대기 건수", description = "현재 재시도 대기 중인 건수")
    public Long getPendingRetryCount() {
        return dailyCheckService.getPendingRetryCount();
    }

    @PostMapping("/manual/{memberId}")
    @Operation(summary = "수동 발송", description = "긴급 시 특정 회원에게 수동 발송")
    public void sendManualMessage(@PathVariable Long memberId) {
        dailyCheckService.sendManualMessage(memberId);
    }
}
```

#### 최소 DTO (1개만)
```java
// DailyCheckStatsDto - 핵심 통계만
public class DailyCheckStatsDto {
    private LocalDate date;
    private Long totalMembers;
    private Long successCount;
    private Long failureCount;
    private Double successRate;

    // 정적 팩토리 메서드
    public static DailyCheckStatsDto of(LocalDate date, Long totalMembers,
                                       Long successCount, Long failureCount) {
        double successRate = totalMembers > 0 ?
            (double) successCount / totalMembers * 100 : 0;
        return new DailyCheckStatsDto(date, totalMembers, successCount,
                                     failureCount, Math.round(successRate * 100.0) / 100.0);
    }
}
```

## 📋 수정된 리팩토링 실행 계획

### ✅ 즉시 진행 (0.5일)

#### Service 메서드 분리 (기존 클래스 내에서)
- [ ] `triggerDailyCheck()` 메서드 추가 - @Scheduled 트리거만 담당
- [ ] `triggerRetryProcess()` 메서드 추가 - @Scheduled 트리거만 담당
- [ ] 기존 `sendDailyCheckMessages()` → `processAllActiveMembers()` 리네이밍
- [ ] 기존 `processRetries()` 유지
- [ ] 기존 테스트 코드 메서드명 변경에 맞춰 수정
- [ ] 기능 동작 확인 테스트

### ⏳ 조건부 진행 (실제 요구사항 발생 시)

#### 관리 기능 추가 (1-2일)
**진행 조건 확인**:
- [ ] 운영팀 요구사항 확인
- [ ] Phase 3 핵심 기능 완료 여부 확인
- [ ] 모니터링/대시보드 계획 확인

**구현 순서** (조건 만족 시에만):
- [ ] `DailyCheckStatsDto` 최소 DTO 추가
- [ ] `getTodayStats()`, `getPendingRetryCount()` Service 메서드 추가
- [ ] 3개 API만 포함한 최소 Controller 추가
- [ ] 통계 조회용 Repository 쿼리 추가
- [ ] Controller 통합 테스트 작성

## ⚠️ 수정된 원칙: 실용성 우선

### 🚫 하지 않을 것들 (명확한 기준)

#### 즉시 불필요한 것들
```java
❌ 클래스 분리 (DailyCheckScheduler 별도 클래스)
   → 현재 클래스 내 메서드 분리로 충분

❌ 5개 관리 API → 3개로 축소, 조건부 진행
   → 실제 필요성 검증 후 진행

❌ 복잡한 DTO 계층 (3개 DTO)
   → 1개 통계 DTO만 필요시 추가

❌ 커스텀 예외 체계
   → 현재 로깅으로 충분, 과도한 분류 불필요

❌ TDD Red-Green-Blue 사이클 강제 적용
   → 리팩토링에는 부적절, 기존 테스트 유지 방식 사용
```

#### 조건부 보류
```java
⏳ 글로벌 표준 적용 (CommonApiResponse, Swagger)
   → 관리 API 추가 시에만 적용

⏳ Repository 쿼리 확장
   → 통계 기능 요구사항 발생 시에만

⏳ 복잡한 패턴 적용 (Strategy, Observer, Factory)
   → 현재 단순한 구조로 충분
```

### ✅ 현실적 개선 기준

#### 최소한의 구조 개선
- **메서드 분리**: 스케줄링 트리거와 비즈니스 로직 책임 경계만 명확히
- **기존 안정성 유지**: 검증된 로직은 최대한 보존

#### 조건부 확장성
- **실제 요구사항 기반**: 운영팀이나 모니터링 필요시에만 API 추가
- **Phase 3 우선순위**: 핵심 기능 완료 후 여유 시점에만 확장

## 🎯 최종 리팩토링 범위 (수정)

### ✅ 즉시 진행 (필수)
1. **Service 메서드 분리**: 기존 클래스 내에서 트리거와 비즈니스 로직 분리 (0.5일)

### ⏳ 조건부 진행 (요구사항 발생 시)
1. **최소 관리 API**: 3개 API + 1개 DTO (1-2일)
2. **통계 쿼리**: 실제 사용 시에만 Repository 확장

### 🚫 진행하지 않음
1. ~~클래스 분리~~ → 메서드 분리로 충분
2. ~~복잡한 DTO 계층~~ → 최소한만 필요시 추가
3. ~~글로벌 표준 강제 적용~~ → API 추가 시에만
4. ~~커스텀 예외 체계~~ → 현재 로깅으로 충분

## 📈 현실적 예상 효과

### 즉시 얻는 이점
- ✅ **책임 분리**: 스케줄링 트리거와 비즈니스 로직 경계 명확화
- ✅ **테스트 단순화**: 각 책임별 개별 테스트 가능
- ✅ **코드 가독성**: 메서드명으로 역할 구분 명확

### 유지되는 안정성
- ✅ **기존 로직 보존**: 83% 개선된 스케줄링 로직 그대로 유지
- ✅ **테스트 커버리지**: 100% 테스트 커버리지 유지
- ✅ **운영 안정성**: 실제 작동 중인 시스템 영향 최소화

### 미래 확장성
- 🔮 **API 확장 준비**: 필요시 관리 기능 추가 가능한 구조
- 🔮 **모니터링 연동**: 대시보드 구축 시 연동 가능

---

**🎯 수정된 플랜: 현재 문제 해결에 집중하고, 미래 확장은 실제 필요시에만 진행하는 실용적 접근**