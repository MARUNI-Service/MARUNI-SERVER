# MARUNI 개선 TODO (MVP 최소 구현)

**작성일**: 2025-10-08
**기반 문서**: user-journey-validation-report.md
**현재 완성도**: 94.3%
**목표**: MVP 핵심 기능 완성 (94.3% → 100%)

---

## 📋 Phase 1: 핵심 기능 구현 (MVP 필수) 🔴

**목표**: Journey 4 Phase 7 완성 (보호자 경험 완성)
**예상 소요**: 1-2일
**영향도**: 높음 (사용자 경험 직접 영향)
**원칙**: MVP 수준의 최소 구현만 진행

---

### 1.1 대화 전체보기 API 구현 (MVP)

**위치**: `ConversationController.java`

**MVP 범위**:
- [ ] Controller 엔드포인트 추가 (`GET /api/conversations/{memberId}/history`)
- [ ] Service 메서드 구현 (최소 기능)
- [ ] Repository 쿼리 메서드 추가
- [ ] 보호자 권한 검증 (기본)
- [ ] MessageDto 반환

**제외 사항** (MVP 이후):
- ~~페이징 처리~~
- ~~복잡한 필터링~~
- ~~캐싱~~
- ~~성능 최적화~~

**구현 예시**:
```java
@GetMapping("/{memberId}/history")
@Operation(summary = "대화 전체보기", description = "보호자가 노인의 대화 내역을 조회합니다.")
@SuccessCodeAnnotation(SuccessCode.SUCCESS)
public List<MessageDto> getConversationHistory(
    @PathVariable Long memberId,
    @RequestParam(defaultValue = "7") int days,
    @AuthenticationPrincipal CustomUserDetails userDetails) {

    // 1. 보호자 권한 검증
    // 2. 최근 N일간 메시지 조회
    // 3. DTO 변환 후 반환
    return conversationService.getConversationHistory(memberId, days, userDetails.getMemberId());
}
```

**최소 구현 체크리스트**:
- [ ] Controller 엔드포인트 추가
- [ ] Service 메서드 작성 (권한 검증 + 조회)
- [ ] Repository 쿼리 메서드 (`findRecentMessagesByMemberId`)
- [ ] 기본 예외 처리 (권한 없음, 회원 없음)

---

### 1.2 알림 상세 조회 API 구현 (MVP)

**위치**: `AlertRuleController.java`

**MVP 범위**:
- [ ] Controller 엔드포인트 추가 (`GET /api/alert-rules/history/{alertId}`)
- [ ] Service 메서드 구현 (최소 기능)
- [ ] AlertHistoryDetailDto 생성 (필수 필드만)
- [ ] 보호자 권한 검증 (기본)

**제외 사항** (MVP 이후):
- ~~관련 대화 내역 포함~~ (나중에 추가)
- ~~상세 분석 정보~~
- ~~그래프 데이터~~

**구현 예시**:
```java
@GetMapping("/history/{alertId}")
@Operation(summary = "알림 상세 조회", description = "이상징후 알림의 상세 정보를 조회합니다.")
@SuccessCodeAnnotation(SuccessCode.SUCCESS)
public AlertHistoryDetailDto getAlertDetail(
    @PathVariable Long alertId,
    @AuthenticationPrincipal CustomUserDetails userDetails) {

    // 1. 보호자 권한 검증
    // 2. AlertHistory 조회
    // 3. DTO 변환 후 반환
    return alertHistoryService.getAlertDetail(alertId, userDetails.getMemberId());
}
```

**최소 구현 체크리스트**:
- [ ] Controller 엔드포인트 추가
- [ ] Service 메서드 작성 (권한 검증 + 조회)
- [ ] AlertHistoryDetailDto 작성 (alertId, memberId, alertType, alertLevel, detectedAt, message)
- [ ] 기본 예외 처리 (권한 없음, 알림 없음)

---

## 📋 Phase 2: 문서 최신화 (최종) 📝

**시점**: Phase 1 완료 후 최종적으로 진행
**예상 소요**: 0.5일
**영향도**: 중간 (개발자 혼란 방지)

**TODO**:
- [ ] user-journey.md API 경로 수정 (6개 항목)
- [ ] Swagger 문서 확인 (자동 생성)
- [ ] 도메인 가이드 업데이트 (conversation.md, alertrule.md)

**상세 항목은 Phase 1 완료 시 작성**

---

## 📊 MVP 진행 상황 체크리스트

### ✅ Phase 1: 핵심 기능 (필수)

#### 대화 전체보기 API
- [ ] ConversationController 엔드포인트
- [ ] ConversationService 메서드
- [ ] MessageRepository 쿼리 메서드
- [ ] 보호자 권한 검증 로직
- [ ] 기본 예외 처리

#### 알림 상세 조회 API
- [ ] AlertRuleController 엔드포인트
- [ ] AlertHistoryService 메서드
- [ ] AlertHistoryDetailDto
- [ ] 보호자 권한 검증 로직
- [ ] 기본 예외 처리

### 📝 Phase 2: 문서 최신화 (최종)
- [ ] user-journey.md 수정
- [ ] 도메인 가이드 업데이트
- [ ] Swagger 확인

---

## 🎯 MVP 최종 목표

**Phase 1 완료 시**:
- 94.3% → **100% 일치 달성**
- Journey 4 Phase 7 완성
- 보호자가 대화 내역 및 알림 상세 확인 가능

**Phase 2 완료 시**:
- 문서-코드 완벽 일치
- 개발자 온보딩 용이

---

## 📝 MVP 구현 원칙

### 1. 최소 기능만 구현
- 페이징, 필터링, 성능 최적화는 제외
- 핵심 CRUD만 구현
- 복잡한 비즈니스 로직은 나중에

### 2. 기본 예외 처리만
- 권한 없음 (403)
- 리소스 없음 (404)
- 입력값 검증 (400)
- 세부 에러 핸들링은 제외

### 3. 테스트 범위
- **포함**: Controller 통합 테스트 (MockMvc)
- **제외**: 단위 테스트 상세 케이스 (MVP 이후)

### 4. DDD 계층 구조 준수
- **Controller**: @AutoApiResponse, Swagger 기본 문서화
- **Service**: @Transactional, 최소 비즈니스 로직
- **Repository**: 기본 쿼리 메서드만
- **DTO**: 필수 필드만

### 5. 보안 최소 요구사항
- JWT 인증 확인
- 보호자 권한 검증 (기본)
- SQL Injection 방지 (JPA 사용)

---

## 🚫 MVP에서 제외되는 항목

### 대화 전체보기 API
- ~~페이징 처리~~
- ~~날짜별 그룹핑~~
- ~~감정 통계~~
- ~~검색 기능~~
- ~~캐싱~~

### 알림 상세 조회 API
- ~~관련 대화 내역 포함~~
- ~~감정 분석 그래프~~
- ~~알림 이력 타임라인~~
- ~~상세 분석 정보~~

### 문서화
- ~~API 사용 예시~~
- ~~에러 코드 상세 설명~~
- ~~성능 가이드~~

---

**작성일**: 2025-10-08
**MVP 목표일**: Phase 1 완료 후 즉시 배포
**담당자**: 개발팀
