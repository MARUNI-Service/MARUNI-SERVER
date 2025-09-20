# Guardian 도메인 리팩토링 계획

**MARUNI 프로젝트 - Guardian 도메인 체계적 리팩토링 로드맵**

## 🎯 **리팩토링 목표**

### **핵심 목표**
- 기존 기능 100% 보존하면서 코드 품질 향상
- 다른 리팩토링된 도메인들(Auth, Member, Conversation, DailyCheck)과 구조적 일관성 확보
- TDD Red-Green-Blue 사이클 완전 적용
- AlertRule, Notification 도메인과의 연동 안정성 보장

### **품질 지표 목표**
```
현재 상태 → 목표 상태
- 테스트 커버리지: 65% → 85%+
- 순환 복잡도: 평균 8 → 평균 5 이하
- 메서드당 라인 수: 평균 15 → 평균 10 이하
- 클래스당 책임: 3-4개 → 1-2개 (단일 책임 원칙)
```

## 📊 **현재 상태 분석**

### **✅ 좋은 점들**
- BaseTimeEntity 상속 패턴 적용
- 정적 팩토리 메서드 (createGuardian) 구현
- 도메인 로직 메서드들 (updateNotificationPreference, deactivate) 존재
- 명확한 책임 분리 (Service에서 비즈니스 로직 처리)
- 7개 REST API 완성 및 정상 동작

### **🔧 개선 필요 사항들**
1. **예외 처리 일관성**: IllegalArgumentException → BaseException 계열 변경 필요
2. **도메인 경계 위반**: MemberNotFoundException이 Guardian 도메인에 위치
3. **서비스 복잡도**: 일부 메서드가 다소 길고 복잡함 (단일 책임 위반)
4. **도메인 서비스 부재**: 복잡한 비즈니스 로직에 대한 도메인 서비스 없음
5. **매퍼 로직 혼재**: DTO 변환 로직이 서비스에 섞여 있음

## 🚀 **단계별 리팩토링 계획**

### **Phase 1: 예외 처리 표준화** (소요시간: 30분, 위험도: ⭐)

#### **작업 내용**
1. `GUARDIAN_EMAIL_ALREADY_EXISTS` ErrorCode 추가
2. `GuardianEmailAlreadyExistsException` 클래스 생성
3. GuardianService의 `validateGuardianEmailNotExists` 메서드에서 IllegalArgumentException 제거
4. 해당 테스트 케이스 업데이트

#### **TDD Red-Green-Blue 사이클**
```java
// Red: 실패 테스트 작성
@Test
@DisplayName("중복된 보호자 이메일로 생성 시 GuardianEmailAlreadyExistsException 발생")
void createGuardian_withDuplicateEmail_shouldThrowGuardianEmailAlreadyExistsException() {
    // Given: 이미 존재하는 이메일
    // When: 동일 이메일로 보호자 생성 시도
    // Then: GuardianEmailAlreadyExistsException 발생
}

// Green: 최소 구현
public class GuardianEmailAlreadyExistsException extends BaseException {
    public GuardianEmailAlreadyExistsException(String email) {
        super(ErrorCode.GUARDIAN_EMAIL_ALREADY_EXISTS, email);
    }
}

// Blue: 리팩토링
- 예외 메시지 개선
- 로깅 추가
- 성능 최적화
```

#### **예상 결과**
- ✅ IllegalArgumentException → BaseException 계열로 일관성 확보
- ✅ 더 명확한 예외 메시지 및 HTTP 상태 코드
- ✅ 글로벌 예외 처리 시스템과 완전 통합

---

### **Phase 2: 도메인 경계 정리** (소요시간: 45분, 위험도: ⭐⭐)

#### **작업 내용**
1. `MemberNotFoundException`을 Member 도메인으로 이동
2. Guardian 도메인에서 Member 도메인 예외 import 변경
3. 패키지 구조 정리 및 의존성 확인
4. 관련 테스트 업데이트

#### **TDD Red-Green-Blue 사이클**
```java
// Red: Member 도메인 예외 사용 테스트
@Test
@DisplayName("존재하지 않는 회원에게 보호자 할당 시 Member 도메인 예외 발생")
void assignGuardianToMember_withNonExistentMember_shouldThrowMemberNotFoundException() {
    // Given: 존재하지 않는 회원 ID
    // When: 보호자 할당 시도
    // Then: com.anyang.maruni.domain.member.application.exception.MemberNotFoundException 발생
}

// Green: import 변경 및 예외 위치 이동
// Blue: 의존성 최적화 및 패키지 구조 개선
```

#### **예상 결과**
- ✅ 도메인 경계 명확화
- ✅ 예외 클래스의 적절한 위치 배치
- ✅ 도메인간 의존성 관계 최적화

---

### **Phase 3: 서비스 계층 분리** (소요시간: 2시간, 위험도: ⭐⭐⭐)

#### **작업 내용**
1. `GuardianDomainService` 생성 (복잡한 비즈니스 로직 전용)
2. `GuardianValidator` 생성 (유효성 검증 로직 분리)
3. `GuardianMapper` 생성 (DTO 변환 로직 분리)
4. 기존 GuardianService 리팩토링

#### **TDD Red-Green-Blue 사이클**
```java
// Red: 도메인 서비스 테스트
@Test
@DisplayName("보호자 비활성화 시 연결된 모든 회원의 관계가 안전하게 해제되어야 함")
void deactivateGuardianWithMembers_shouldSafelyRemoveAllRelations() {
    // Given: 보호자와 여러 회원이 연결된 상태
    // When: GuardianDomainService.deactivateGuardianSafely() 호출
    // Then: 모든 회원의 guardian 필드가 null + 보호자 isActive = false
}

// Green: GuardianDomainService 구현
@Component
public class GuardianDomainService {
    public void deactivateGuardianSafely(GuardianEntity guardian, List<MemberEntity> connectedMembers) {
        // 안전한 관계 해제 로직
    }
}

// Blue: 트랜잭션 최적화, 배치 처리 개선
```

#### **예상 결과**
- ✅ 단일 책임 원칙 준수 (GuardianService 복잡도 50% 감소)
- ✅ 도메인 로직과 애플리케이션 로직 명확한 분리
- ✅ 재사용 가능한 컴포넌트 구조

---

### **Phase 4: Repository 최적화** (소요시간: 1.5시간, 위험도: ⭐⭐)

#### **작업 내용**
1. 성능 최적화 쿼리 메서드 추가
2. 페이징 지원 추가
3. 복잡한 조회 조건을 위한 QueryDSL 적용 검토
4. 인덱스 최적화 제안

#### **TDD Red-Green-Blue 사이클**
```java
// Red: 성능 테스트
@Test
@DisplayName("대량 보호자 조회 성능 테스트 - 100ms 이내 완료")
void findActiveGuardiansByCondition_shouldCompleteWithin100ms() {
    // Given: 1000개 보호자 데이터
    // When: 복합 조건으로 활성 보호자 조회
    // Then: 100ms 이내 완료 + 정확한 결과
}

// Green: 최적화된 쿼리 메서드 구현
@Query("SELECT g FROM GuardianEntity g WHERE g.isActive = true AND g.relation = :relation")
Page<GuardianEntity> findActiveGuardiansByRelation(@Param("relation") GuardianRelation relation, Pageable pageable);

// Blue: 인덱스 최적화, 페치 전략 개선
```

#### **예상 결과**
- ✅ 조회 성능 50% 향상
- ✅ 페이징 지원으로 대량 데이터 처리 개선
- ✅ AlertRule 도메인과의 연동 성능 최적화

---

### **Phase 5: 통합 테스트 강화** (소요시간: 2시간, 위험도: ⭐⭐)

#### **작업 내용**
1. AlertRule 도메인과의 연동 시나리오 테스트 추가
2. Notification 발송 플로우 테스트 강화
3. 동시성 테스트 추가 (보호자-회원 관계 변경)
4. 엣지 케이스 테스트 보강

#### **TDD Red-Green-Blue 사이클**
```java
// Red: 통합 시나리오 테스트
@Test
@DisplayName("보호자 연동 전체 플로우 테스트")
void guardianAlertFlow_shouldNotifyWhenAnomalyDetected() {
    // Given: 회원-보호자 관계 설정됨
    // When: AlertRule에서 이상 징후 감지
    // Then: 해당 보호자에게 Notification 발송
    // And: 모든 단계가 5초 이내 완료
}

// Green: 통합 테스트 구현
// Blue: 테스트 성능 최적화, 모의 객체 개선
```

#### **예상 결과**
- ✅ 전체 시스템 안정성 검증
- ✅ AlertRule, Notification과의 연동 보장
- ✅ 운영 환경 배포 신뢰도 향상

---

## 📅 **전체 일정 계획**

### **Week 1 (즉시 시작 가능)**
- **Day 1**: Phase 1 (예외 처리 표준화) - 30분
- **Day 1**: Phase 2 (도메인 경계 정리) - 45분
- **Day 2-3**: Phase 3 (서비스 계층 분리) - 2시간

### **Week 2**
- **Day 1-2**: Phase 4 (Repository 최적화) - 1.5시간
- **Day 3-4**: Phase 5 (통합 테스트 강화) - 2시간
- **Day 5**: 전체 검증 및 문서 업데이트

## ⚠️ **위험 요소 및 완화 방안**

### **위험 요소**
1. **AlertRule 도메인 연동 중단**: Guardian 변경 시 AlertRule 영향
2. **데이터 무결성**: 보호자-회원 관계 변경 중 데이터 손실
3. **성능 저하**: 리팩토링 과정에서 일시적 성능 저하

### **완화 방안**
1. **단계별 배포**: 각 Phase 완료 후 테스트 검증
2. **백업 전략**: 리팩토링 전 데이터베이스 백업
3. **모니터링 강화**: 리팩토링 중 성능 지표 실시간 모니터링
4. **롤백 계획**: 각 단계별 롤백 시나리오 준비

## 🎯 **완료 기준**

### **Phase별 완료 기준**
- ✅ 모든 기존 테스트 통과 (Regression 방지)
- ✅ 새로운 테스트 커버리지 목표 달성
- ✅ AlertRule 도메인 연동 정상 동작 확인
- ✅ 코드 리뷰 통과 (품질 지표 달성)

### **전체 완료 기준**
- ✅ 다른 리팩토링된 도메인과 구조적 일관성 확보
- ✅ 코드 커버리지 85% 달성
- ✅ 성능 지표 유지 또는 개선
- ✅ 문서 업데이트 완료 (`docs/domains/guardian.md`)

## 🚀 **시작 가이드**

### **즉시 시작할 수 있는 첫 번째 작업**
```bash
# 1. 현재 테스트 실행 (기준선 확립)
./gradlew test --tests "*Guardian*"

# 2. Phase 1 시작: ErrorCode 추가
# 파일: src/main/java/com/anyang/maruni/global/response/error/ErrorCode.java
# 추가할 코드: GUARDIAN_EMAIL_ALREADY_EXISTS(409, "G001", "Guardian with email already exists")

# 3. TDD Red 단계: 실패 테스트 작성
# 파일: src/test/java/com/anyang/maruni/domain/guardian/application/service/GuardianServiceTest.java
```

---

**🎯 이 리팩토링 계획을 통해 Guardian 도메인을 MARUNI 프로젝트의 다른 완성된 도메인들과 동일한 수준의 코드 품질로 끌어올릴 수 있습니다.**

**다음 단계**: Phase 1부터 시작하여 TDD Red-Green-Blue 사이클을 엄격히 적용하며 진행하시기 바랍니다.