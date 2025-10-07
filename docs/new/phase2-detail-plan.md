# Phase 2 상세 진행 계획

**MARUNI Guardian 관계 관리 시스템 개발**

---

**버전**: 1.0.0
**작성일**: 2025-01-06
**최종 업데이트**: 2025-01-06
**상태**: 진행 준비
**예상 기간**: 1.5주 (7.5 영업일)
**기반 문서**: development-plan.md, user-journey.md, api-specification.md

---

## 📋 목차

1. [개요](#1-개요)
2. [Phase 2 목표](#2-phase-2-목표)
3. [상세 작업 계획](#3-상세-작업-계획)
4. [일정별 작업 분배](#4-일정별-작업-분배)
5. [리스크 및 대응방안](#5-리스크-및-대응방안)
6. [완료 기준](#6-완료-기준)

---

## 1. 개요

### 1.1 Phase 2 배경

**Phase 1 완료 상태** (98%):
- ✅ MemberEntity 스키마 확장 (자기참조, dailyCheckEnabled)
- ✅ Member CRUD API 구현
- ✅ Auth 로그인 응답 수정 (역할 정보 포함)
- ✅ API 경로 통일 (`/api/members`)

**Phase 2 필요성**:
- Phase 1에서 Member 도메인의 기반이 완성됨
- 이제 **Guardian 관계 관리 시스템**이 필요
- 사용자 여정(user-journey.md)에서 정의된 보호자 요청/수락 플로우 구현
- DailyCheck, Conversation 도메인과의 통합 검증

### 1.2 Phase 2 범위

**포함**:
- Guardian 관계 요청/수락/거절/해제 시스템
- GuardianRequest Entity 및 Repository
- GuardianRelationService 비즈니스 로직
- Guardian 관련 REST API (5개 엔드포인트)
- DailyCheck 쿼리 수정 (dailyCheckEnabled 기반)
- Conversation 검증 (기존 로직 재사용)
- 단위 테스트 + 통합 테스트 (TDD 완전 사이클)

**제외**:
- AlertRule 수정 (Phase 3으로 이연)
- Notification 실제 구현 (Mock 유지)
- 성능 최적화 (Phase 3로 이연)

---

## 2. Phase 2 목표

### 2.1 핵심 목표

1. **Guardian 관계 관리 완성**: 요청/수락/거절/해제 워크플로우
2. **DailyCheck 통합**: `dailyCheckEnabled=true`인 회원에게만 안부 메시지 발송
3. **Conversation 검증**: AI 대화 시스템 정상 동작 확인
4. **TDD 완전 적용**: Red-Green-Blue 사이클 모든 신규 코드에 적용
5. **API 완성도**: Swagger 문서화 + CommonApiResponse 래핑

### 2.2 완료 기준

| 항목 | 기준 | 측정 방법 |
|------|------|-----------|
| **Guardian API** | 5개 엔드포인트 정상 동작 | Swagger 테스트 |
| **DailyCheck** | dailyCheckEnabled 기반 발송 | 통합 테스트 |
| **Conversation** | AI 응답 정상 반환 | 단위 테스트 |
| **테스트 커버리지** | 90% 이상 | JaCoCo 리포트 |
| **빌드 성공** | 전체 테스트 통과 | Gradle test |

---

## 3. 상세 작업 계획

### 3.1 Task 1: GuardianRequest Entity 생성

**우선순위**: P0 (최우선)
**예상 소요 시간**: 0.5일
**담당 도메인**: Guardian

#### 3.1.1 요구사항 분석

**user-journey.md Journey 3** 참조:
```
김순자 할머니 → 보호자 검색 → 김영희 선택 → 등록 요청
김영희 → 푸시 알림 수신 → 요청 확인 → 수락/거절
```

**데이터 흐름**:
1. 요청자(김순자)가 보호자(김영희) 검색
2. GuardianRequest 생성 (상태: PENDING)
3. 보호자에게 푸시 알림 발송
4. 보호자가 수락 → MemberEntity.guardian 설정 + RequestStatus.ACCEPTED
5. 보호자가 거절 → RequestStatus.REJECTED

#### 3.1.2 Entity 설계

```java
@Entity
@Table(name = "guardian_request",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_requester_guardian",
           columnNames = {"requester_id", "guardian_id"}
       ),
       indexes = {
           @Index(name = "idx_guardian_id_status", columnList = "guardian_id, status"),
           @Index(name = "idx_requester_id", columnList = "requester_id"),
           @Index(name = "idx_created_at", columnList = "createdAt")
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private MemberEntity requester;  // 요청한 사람 (노인)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private MemberEntity guardian;  // 요청받은 사람 (보호자)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardianRelation relation;  // 관계 (FAMILY, FRIEND 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;  // PENDING, ACCEPTED, REJECTED

    // 정적 팩토리 메서드
    public static GuardianRequest createRequest(
        MemberEntity requester,
        MemberEntity guardian,
        GuardianRelation relation) {

        return GuardianRequest.builder()
            .requester(requester)
            .guardian(guardian)
            .relation(relation)
            .status(RequestStatus.PENDING)
            .build();
    }

    // 비즈니스 로직: 요청 수락
    public void accept() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be accepted");
        }
        this.status = RequestStatus.ACCEPTED;
        // MemberEntity의 guardian 설정은 Service에서 처리
    }

    // 비즈니스 로직: 요청 거절
    public void reject() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }
        this.status = RequestStatus.REJECTED;
    }

    // 비즈니스 로직: 취소 가능 여부
    public boolean canBeCancelled() {
        return this.status == RequestStatus.PENDING;
    }
}
```

#### 3.1.3 RequestStatus Enum 정의

```java
@Getter
@AllArgsConstructor
public enum RequestStatus {
    PENDING("대기중"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨");

    private final String displayName;
}
```

#### 3.1.4 GuardianRelation Enum (재사용)

**기존 Guardian 도메인에서 가져옴**:
```java
@Getter
@AllArgsConstructor
public enum GuardianRelation {
    FAMILY("가족"),
    FRIEND("친구"),
    CAREGIVER("돌봄제공자"),
    MEDICAL_STAFF("의료진"),
    OTHER("기타");

    private final String displayName;
}
```

#### 3.1.5 TDD Red 단계: 실패 테스트 작성

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianRequest Entity 테스트")
class GuardianRequestTest {

    @Test
    @DisplayName("보호자 요청 생성 - 정상적인 요청으로 PENDING 상태 생성")
    void createRequest_ValidData_ReturnsPendingStatus() {
        // given
        MemberEntity requester = createMember(1L, "requester@example.com");
        MemberEntity guardian = createMember(2L, "guardian@example.com");

        // when
        GuardianRequest request = GuardianRequest.createRequest(
            requester, guardian, GuardianRelation.FAMILY);

        // then
        assertThat(request.getRequester()).isEqualTo(requester);
        assertThat(request.getGuardian()).isEqualTo(guardian);
        assertThat(request.getRelation()).isEqualTo(GuardianRelation.FAMILY);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("요청 수락 - PENDING 상태에서 ACCEPTED로 변경")
    void accept_PendingStatus_ChangesToAccepted() {
        // given
        GuardianRequest request = createPendingRequest();

        // when
        request.accept();

        // then
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
    }

    @Test
    @DisplayName("요청 수락 실패 - ACCEPTED 상태에서 예외 발생")
    void accept_AlreadyAccepted_ThrowsException() {
        // given
        GuardianRequest request = createPendingRequest();
        request.accept();  // 이미 수락됨

        // when & then
        assertThrows(IllegalStateException.class, () -> request.accept());
    }

    @Test
    @DisplayName("요청 거절 - PENDING 상태에서 REJECTED로 변경")
    void reject_PendingStatus_ChangesToRejected() {
        // given
        GuardianRequest request = createPendingRequest();

        // when
        request.reject();

        // then
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }
}
```

#### 3.1.6 TDD Green 단계: 최소 구현

- 위의 Entity 코드 구현
- 테스트 통과 확인

#### 3.1.7 TDD Blue 단계: 리팩토링

- 비즈니스 로직 메서드 분리 검토
- 불필요한 중복 코드 제거
- 주석 및 문서화

#### 3.1.8 체크리스트

- [ ] GuardianRequest Entity 생성
- [ ] RequestStatus Enum 생성
- [ ] 정적 팩토리 메서드 구현
- [ ] 비즈니스 로직 메서드 (accept, reject) 구현
- [ ] 단위 테스트 4개 작성 및 통과
- [ ] BaseTimeEntity 상속 확인
- [ ] 인덱스 및 제약조건 설정 확인

---

### 3.2 Task 2: GuardianRequestRepository 생성

**우선순위**: P0
**예상 소요 시간**: 0.5일
**담당 도메인**: Guardian

#### 3.2.1 Repository 인터페이스 정의

```java
public interface GuardianRequestRepository extends JpaRepository<GuardianRequest, Long> {

    // 보호자에게 온 PENDING 요청 목록 조회 (최신순)
    @Query("SELECT gr FROM GuardianRequest gr " +
           "WHERE gr.guardian.id = :guardianId " +
           "AND gr.status = :status " +
           "ORDER BY gr.createdAt DESC")
    List<GuardianRequest> findByGuardianIdAndStatus(
        @Param("guardianId") Long guardianId,
        @Param("status") RequestStatus status
    );

    // 특정 요청자가 특정 보호자에게 보낸 PENDING 요청 존재 여부
    @Query("SELECT CASE WHEN COUNT(gr) > 0 THEN true ELSE false END " +
           "FROM GuardianRequest gr " +
           "WHERE gr.requester.id = :requesterId " +
           "AND gr.guardian.id = :guardianId " +
           "AND gr.status = :status")
    boolean existsByRequesterIdAndGuardianIdAndStatus(
        @Param("requesterId") Long requesterId,
        @Param("guardianId") Long guardianId,
        @Param("status") RequestStatus status
    );

    // 요청자가 보낸 모든 요청 조회 (상태별 필터링)
    List<GuardianRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    // ID와 보호자 ID로 요청 조회 (권한 검증용)
    Optional<GuardianRequest> findByIdAndGuardianId(Long id, Long guardianId);
}
```

#### 3.2.2 TDD Red: Repository 테스트 작성

```java
@DataJpaTest
@DisplayName("GuardianRequestRepository 테스트")
class GuardianRequestRepositoryTest {

    @Autowired
    private GuardianRequestRepository guardianRequestRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("보호자 ID와 상태로 요청 조회 - PENDING 요청 목록 반환")
    void findByGuardianIdAndStatus_PendingRequests_ReturnsRequests() {
        // given
        MemberEntity guardian = createAndSaveMember("guardian@example.com");
        MemberEntity requester1 = createAndSaveMember("requester1@example.com");
        MemberEntity requester2 = createAndSaveMember("requester2@example.com");

        GuardianRequest request1 = createAndSaveRequest(requester1, guardian, RequestStatus.PENDING);
        GuardianRequest request2 = createAndSaveRequest(requester2, guardian, RequestStatus.PENDING);
        createAndSaveRequest(requester1, guardian, RequestStatus.ACCEPTED);  // 제외되어야 함

        entityManager.flush();
        entityManager.clear();

        // when
        List<GuardianRequest> result = guardianRequestRepository
            .findByGuardianIdAndStatus(guardian.getId(), RequestStatus.PENDING);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GuardianRequest::getId)
            .containsExactlyInAnyOrder(request1.getId(), request2.getId());
    }

    @Test
    @DisplayName("중복 요청 확인 - 이미 PENDING 요청이 존재하면 true")
    void existsByRequesterIdAndGuardianIdAndStatus_ExistingPendingRequest_ReturnsTrue() {
        // given
        MemberEntity guardian = createAndSaveMember("guardian@example.com");
        MemberEntity requester = createAndSaveMember("requester@example.com");
        createAndSaveRequest(requester, guardian, RequestStatus.PENDING);

        entityManager.flush();
        entityManager.clear();

        // when
        boolean exists = guardianRequestRepository.existsByRequesterIdAndGuardianIdAndStatus(
            requester.getId(), guardian.getId(), RequestStatus.PENDING);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("중복 요청 확인 - PENDING 요청이 없으면 false")
    void existsByRequesterIdAndGuardianIdAndStatus_NoPendingRequest_ReturnsFalse() {
        // given
        MemberEntity guardian = createAndSaveMember("guardian@example.com");
        MemberEntity requester = createAndSaveMember("requester@example.com");
        createAndSaveRequest(requester, guardian, RequestStatus.ACCEPTED);  // ACCEPTED는 제외

        entityManager.flush();
        entityManager.clear();

        // when
        boolean exists = guardianRequestRepository.existsByRequesterIdAndGuardianIdAndStatus(
            requester.getId(), guardian.getId(), RequestStatus.PENDING);

        // then
        assertThat(exists).isFalse();
    }
}
```

#### 3.2.3 체크리스트

- [ ] GuardianRequestRepository 인터페이스 생성
- [ ] 쿼리 메서드 4개 구현
- [ ] Repository 테스트 3개 작성 및 통과
- [ ] @DataJpaTest 적용 확인
- [ ] 인덱스 활용 쿼리 최적화 확인

---

### 3.3 Task 3: GuardianRelationService 구현

**우선순위**: P0
**예상 소요 시간**: 1.5일
**담당 도메인**: Guardian

#### 3.3.1 Service 설계

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GuardianRelationService {

    private final GuardianRequestRepository guardianRequestRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    /**
     * 보호자 요청 생성
     * Journey 3: 김순자 → 김영희에게 보호자 요청
     */
    @Transactional
    public GuardianRequestResponse sendRequest(
        Long requesterId,
        Long guardianId,
        GuardianRelation relation) {

        // 1. 요청자와 보호자 조회
        MemberEntity requester = findMemberById(requesterId);
        MemberEntity guardian = findMemberById(guardianId);

        // 2. 비즈니스 규칙 검증
        validateCanSendRequest(requester, guardian);

        // 3. GuardianRequest 생성 (상태: PENDING)
        GuardianRequest request = GuardianRequest.createRequest(
            requester, guardian, relation);
        GuardianRequest savedRequest = guardianRequestRepository.save(request);

        // 4. 보호자에게 푸시 알림 발송
        String message = String.format("%s님이 보호자로 등록을 요청했습니다",
            requester.getMemberName());
        notificationService.sendPushNotification(guardianId, "보호자 등록 요청", message);

        log.info("Guardian request sent: requester={}, guardian={}, relation={}",
            requesterId, guardianId, relation);

        return GuardianRequestResponse.from(savedRequest);
    }

    /**
     * 보호자에게 온 요청 목록 조회
     * Journey 4 Phase 3: 김영희가 받은 요청 확인
     */
    public List<GuardianRequestResponse> getReceivedRequests(Long guardianId) {
        List<GuardianRequest> requests = guardianRequestRepository
            .findByGuardianIdAndStatus(guardianId, RequestStatus.PENDING);

        return requests.stream()
            .map(GuardianRequestResponse::from)
            .toList();
    }

    /**
     * 보호자 요청 수락
     * Journey 4 Phase 4: 김영희가 요청 수락
     */
    @Transactional
    public void acceptRequest(Long requestId, Long guardianId) {
        // 1. 요청 조회 및 권한 검증
        GuardianRequest request = findRequestByIdAndGuardianId(requestId, guardianId);

        // 2. 요청 수락 (상태: ACCEPTED)
        request.accept();

        // 3. Member-Guardian 관계 설정
        MemberEntity requester = request.getRequester();
        MemberEntity guardian = request.getGuardian();
        requester.assignGuardian(guardian, request.getRelation());

        // 4. 요청자에게 푸시 알림 발송
        String message = String.format("%s님이 보호자 요청을 수락했습니다",
            guardian.getMemberName());
        notificationService.sendPushNotification(
            requester.getId(), "보호자 요청 수락", message);

        log.info("Guardian request accepted: requestId={}, guardianId={}",
            requestId, guardianId);
    }

    /**
     * 보호자 요청 거절
     */
    @Transactional
    public void rejectRequest(Long requestId, Long guardianId) {
        // 1. 요청 조회 및 권한 검증
        GuardianRequest request = findRequestByIdAndGuardianId(requestId, guardianId);

        // 2. 요청 거절 (상태: REJECTED)
        request.reject();

        // 3. 요청자에게 푸시 알림 발송
        String message = "보호자 요청이 거절되었습니다";
        notificationService.sendPushNotification(
            request.getRequester().getId(), "보호자 요청 거절", message);

        log.info("Guardian request rejected: requestId={}, guardianId={}",
            requestId, guardianId);
    }

    /**
     * 보호자 관계 해제
     * Journey: 김순자가 보호자 관계 해제
     */
    @Transactional
    public void removeGuardian(Long memberId) {
        MemberEntity member = findMemberById(memberId);

        if (member.getGuardian() == null) {
            throw new IllegalStateException("보호자가 설정되어 있지 않습니다");
        }

        MemberEntity guardian = member.getGuardian();
        member.removeGuardian();

        // 보호자에게 알림 발송
        String message = String.format("%s님이 보호자 관계를 해제했습니다",
            member.getMemberName());
        notificationService.sendPushNotification(
            guardian.getId(), "보호자 관계 해제", message);

        log.info("Guardian relationship removed: memberId={}, guardianId={}",
            memberId, guardian.getId());
    }

    // ========== Private Helper Methods ==========

    private MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private GuardianRequest findRequestByIdAndGuardianId(Long requestId, Long guardianId) {
        return guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId)
            .orElseThrow(() -> new BaseException(ErrorCode.GUARDIAN_REQUEST_NOT_FOUND));
    }

    private void validateCanSendRequest(MemberEntity requester, MemberEntity guardian) {
        // 1. 자기 자신에게 요청 불가
        if (requester.getId().equals(guardian.getId())) {
            throw new IllegalArgumentException("자기 자신을 보호자로 등록할 수 없습니다");
        }

        // 2. 이미 보호자가 있으면 요청 불가
        if (requester.getGuardian() != null) {
            throw new IllegalStateException("이미 보호자가 설정되어 있습니다");
        }

        // 3. 이미 PENDING 요청이 있으면 중복 요청 불가
        boolean hasPendingRequest = guardianRequestRepository
            .existsByRequesterIdAndGuardianIdAndStatus(
                requester.getId(), guardian.getId(), RequestStatus.PENDING);

        if (hasPendingRequest) {
            throw new IllegalStateException("이미 대기 중인 요청이 있습니다");
        }
    }
}
```

#### 3.3.2 DTO 정의

**GuardianRequestResponse**:
```java
@Getter
@Builder
@Schema(description = "보호자 요청 응답 DTO")
public class GuardianRequestResponse {

    @Schema(description = "요청 ID", example = "1")
    private Long id;

    @Schema(description = "요청자 정보")
    private MemberInfo requester;

    @Schema(description = "보호자 정보")
    private MemberInfo guardian;

    @Schema(description = "관계", example = "FAMILY")
    private GuardianRelation relation;

    @Schema(description = "요청 상태", example = "PENDING")
    private RequestStatus status;

    @Schema(description = "요청 생성 시간")
    private LocalDateTime createdAt;

    public static GuardianRequestResponse from(GuardianRequest request) {
        return GuardianRequestResponse.builder()
            .id(request.getId())
            .requester(MemberInfo.from(request.getRequester()))
            .guardian(MemberInfo.from(request.getGuardian()))
            .relation(request.getRelation())
            .status(request.getStatus())
            .createdAt(request.getCreatedAt())
            .build();
    }

    @Getter
    @Builder
    @Schema(description = "회원 간단 정보")
    public static class MemberInfo {
        @Schema(description = "회원 ID")
        private Long id;

        @Schema(description = "회원 이름")
        private String name;

        @Schema(description = "회원 이메일")
        private String email;

        public static MemberInfo from(MemberEntity member) {
            return MemberInfo.builder()
                .id(member.getId())
                .name(member.getMemberName())
                .email(member.getMemberEmail())
                .build();
        }
    }
}
```

**GuardianRequestDto**:
```java
@Getter
@Setter
@Schema(description = "보호자 요청 생성 DTO")
public class GuardianRequestDto {

    @Schema(description = "보호자 회원 ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "보호자 ID는 필수입니다")
    private Long guardianId;

    @Schema(description = "관계", example = "FAMILY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "관계는 필수입니다")
    private GuardianRelation relation;
}
```

#### 3.3.3 TDD Red: Service 테스트 작성

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianRelationService 테스트")
class GuardianRelationServiceTest {

    @Mock private GuardianRequestRepository guardianRequestRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private GuardianRelationService guardianRelationService;

    @Test
    @DisplayName("보호자 요청 생성 - 정상적인 요청으로 GuardianRequest 생성 및 알림 발송")
    void sendRequest_ValidData_CreatesRequestAndSendsNotification() {
        // given
        Long requesterId = 1L;
        Long guardianId = 2L;
        MemberEntity requester = createMember(requesterId, "requester@example.com");
        MemberEntity guardian = createMember(guardianId, "guardian@example.com");

        given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
        given(memberRepository.findById(guardianId)).willReturn(Optional.of(guardian));
        given(guardianRequestRepository.existsByRequesterIdAndGuardianIdAndStatus(
            requesterId, guardianId, RequestStatus.PENDING)).willReturn(false);
        given(guardianRequestRepository.save(any(GuardianRequest.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        GuardianRequestResponse response = guardianRelationService.sendRequest(
            requesterId, guardianId, GuardianRelation.FAMILY);

        // then
        verify(guardianRequestRepository).save(any(GuardianRequest.class));
        verify(notificationService).sendPushNotification(
            eq(guardianId), anyString(), anyString());
        assertThat(response.getRelation()).isEqualTo(GuardianRelation.FAMILY);
        assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("보호자 요청 생성 실패 - 이미 보호자가 있으면 예외 발생")
    void sendRequest_AlreadyHasGuardian_ThrowsException() {
        // given
        Long requesterId = 1L;
        Long guardianId = 2L;
        MemberEntity requester = createMember(requesterId, "requester@example.com");
        MemberEntity existingGuardian = createMember(3L, "existing@example.com");
        requester.assignGuardian(existingGuardian, GuardianRelation.FAMILY);  // 이미 보호자 있음

        MemberEntity newGuardian = createMember(guardianId, "guardian@example.com");

        given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
        given(memberRepository.findById(guardianId)).willReturn(Optional.of(newGuardian));

        // when & then
        assertThrows(IllegalStateException.class,
            () -> guardianRelationService.sendRequest(requesterId, guardianId, GuardianRelation.FAMILY));
    }

    @Test
    @DisplayName("보호자 요청 수락 - PENDING 요청을 수락하고 관계 설정")
    void acceptRequest_PendingRequest_AcceptsAndAssignsGuardian() {
        // given
        Long requestId = 1L;
        Long guardianId = 2L;
        MemberEntity requester = createMember(1L, "requester@example.com");
        MemberEntity guardian = createMember(guardianId, "guardian@example.com");
        GuardianRequest request = createRequest(requestId, requester, guardian, RequestStatus.PENDING);

        given(guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId))
            .willReturn(Optional.of(request));

        // when
        guardianRelationService.acceptRequest(requestId, guardianId);

        // then
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(requester.getGuardian()).isEqualTo(guardian);
        verify(notificationService).sendPushNotification(
            eq(requester.getId()), anyString(), anyString());
    }

    @Test
    @DisplayName("보호자 요청 거절 - PENDING 요청을 거절")
    void rejectRequest_PendingRequest_RejectsRequest() {
        // given
        Long requestId = 1L;
        Long guardianId = 2L;
        MemberEntity requester = createMember(1L, "requester@example.com");
        MemberEntity guardian = createMember(guardianId, "guardian@example.com");
        GuardianRequest request = createRequest(requestId, requester, guardian, RequestStatus.PENDING);

        given(guardianRequestRepository.findByIdAndGuardianId(requestId, guardianId))
            .willReturn(Optional.of(request));

        // when
        guardianRelationService.rejectRequest(requestId, guardianId);

        // then
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        verify(notificationService).sendPushNotification(
            eq(requester.getId()), anyString(), anyString());
    }

    @Test
    @DisplayName("보호자 관계 해제 - 기존 보호자 관계를 해제")
    void removeGuardian_ExistingGuardian_RemovesGuardian() {
        // given
        Long memberId = 1L;
        MemberEntity member = createMember(memberId, "member@example.com");
        MemberEntity guardian = createMember(2L, "guardian@example.com");
        member.assignGuardian(guardian, GuardianRelation.FAMILY);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        guardianRelationService.removeGuardian(memberId);

        // then
        assertThat(member.getGuardian()).isNull();
        verify(notificationService).sendPushNotification(
            eq(guardian.getId()), anyString(), anyString());
    }
}
```

#### 3.3.4 체크리스트

- [ ] GuardianRelationService 클래스 생성
- [ ] sendRequest 메서드 구현 (요청 생성 + 알림)
- [ ] getReceivedRequests 메서드 구현 (요청 목록 조회)
- [ ] acceptRequest 메서드 구현 (수락 + 관계 설정)
- [ ] rejectRequest 메서드 구현 (거절)
- [ ] removeGuardian 메서드 구현 (관계 해제)
- [ ] GuardianRequestResponse DTO 생성
- [ ] GuardianRequestDto DTO 생성
- [ ] 단위 테스트 5개 작성 및 통과
- [ ] @Transactional 적용 확인

---

### 3.4 Task 4: GuardianController API 구현

**우선순위**: P0
**예상 소요 시간**: 1일
**담당 도메인**: Guardian

#### 3.4.1 API 엔드포인트 정의

**api-specification.md 기반**:

| Method | Path | Summary | Auth |
|--------|------|---------|------|
| POST | /api/guardians/requests | 보호자 요청 생성 | ✅ JWT |
| GET | /api/guardians/requests | 내가 받은 보호자 요청 목록 | ✅ JWT |
| POST | /api/guardians/requests/{requestId}/accept | 보호자 요청 수락 | ✅ JWT |
| POST | /api/guardians/requests/{requestId}/reject | 보호자 요청 거절 | ✅ JWT |
| DELETE | /api/members/me/guardian | 내 보호자 관계 해제 | ✅ JWT |

#### 3.4.2 Controller 구현

```java
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Guardian 관계 관리 API", description = "보호자 요청/수락/거절/해제 API")
@CustomExceptionDescription(SwaggerResponseDescription.GUARDIAN_ERROR)
public class GuardianRelationController {

    private final GuardianRelationService guardianRelationService;

    @Operation(
        summary = "보호자 요청 생성",
        description = "특정 회원에게 보호자 등록을 요청합니다. 요청자의 JWT 토큰 필요."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 보호자 있음, 중복 요청 등)"),
        @ApiResponse(responseCode = "404", description = "보호자 회원을 찾을 수 없음")
    })
    @PostMapping("/requests")
    @SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUEST_CREATED)
    public GuardianRequestResponse sendRequest(
        @AuthenticationPrincipal MemberEntity requester,
        @Valid @RequestBody GuardianRequestDto request
    ) {
        return guardianRelationService.sendRequest(
            requester.getId(),
            request.getGuardianId(),
            request.getRelation()
        );
    }

    @Operation(
        summary = "내가 받은 보호자 요청 목록 조회",
        description = "현재 로그인한 회원에게 온 PENDING 상태의 보호자 요청 목록을 조회합니다."
    )
    @GetMapping("/requests")
    @SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUESTS_VIEW)
    public List<GuardianRequestResponse> getReceivedRequests(
        @AuthenticationPrincipal MemberEntity guardian
    ) {
        return guardianRelationService.getReceivedRequests(guardian.getId());
    }

    @Operation(
        summary = "보호자 요청 수락",
        description = "받은 보호자 요청을 수락하고 보호자-회원 관계를 설정합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 수락 성공"),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "이미 처리된 요청")
    })
    @PostMapping("/requests/{requestId}/accept")
    @SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUEST_ACCEPTED)
    public void acceptRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal MemberEntity guardian
    ) {
        guardianRelationService.acceptRequest(requestId, guardian.getId());
    }

    @Operation(
        summary = "보호자 요청 거절",
        description = "받은 보호자 요청을 거절합니다."
    )
    @PostMapping("/requests/{requestId}/reject")
    @SuccessCodeAnnotation(SuccessCode.GUARDIAN_REQUEST_REJECTED)
    public void rejectRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal MemberEntity guardian
    ) {
        guardianRelationService.rejectRequest(requestId, guardian.getId());
    }
}

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@AutoApiResponse
public class MemberApiController {
    // 기존 코드...

    @Operation(
        summary = "내 보호자 관계 해제",
        description = "현재 설정된 보호자와의 관계를 해제합니다."
    )
    @DeleteMapping("/me/guardian")
    @SuccessCodeAnnotation(SuccessCode.GUARDIAN_REMOVED)
    public void removeMyGuardian(@AuthenticationPrincipal MemberEntity member) {
        guardianRelationService.removeGuardian(member.getId());
    }
}
```

#### 3.4.3 SuccessCode 추가

```java
public enum SuccessCode {
    // 기존 코드...

    // Guardian 관련 성공 코드
    GUARDIAN_REQUEST_CREATED("G001", "보호자 요청이 생성되었습니다"),
    GUARDIAN_REQUESTS_VIEW("G002", "보호자 요청 목록 조회 성공"),
    GUARDIAN_REQUEST_ACCEPTED("G003", "보호자 요청을 수락했습니다"),
    GUARDIAN_REQUEST_REJECTED("G004", "보호자 요청을 거절했습니다"),
    GUARDIAN_REMOVED("G005", "보호자 관계가 해제되었습니다");

    // ...
}
```

#### 3.4.4 ErrorCode 추가

```java
public enum ErrorCode {
    // 기존 코드...

    // Guardian 관련 에러 코드
    GUARDIAN_REQUEST_NOT_FOUND("G001", "보호자 요청을 찾을 수 없습니다"),
    GUARDIAN_REQUEST_ALREADY_PROCESSED("G002", "이미 처리된 요청입니다"),
    GUARDIAN_ALREADY_EXISTS("G003", "이미 보호자가 설정되어 있습니다"),
    GUARDIAN_REQUEST_DUPLICATE("G004", "이미 대기 중인 요청이 있습니다");

    // ...
}
```

#### 3.4.5 통합 테스트 작성

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GuardianRelationController 통합 테스트")
class GuardianRelationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GuardianRequestRepository guardianRequestRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Test
    @DisplayName("보호자 요청 생성 API - 정상 요청으로 GuardianRequest 생성")
    void sendRequest_ValidRequest_ReturnsSuccess() throws Exception {
        // given
        MemberEntity requester = createAndSaveMember("requester@example.com");
        MemberEntity guardian = createAndSaveMember("guardian@example.com");
        String token = jwtUtil.createAccessToken(requester.getMemberEmail());

        GuardianRequestDto request = new GuardianRequestDto();
        request.setGuardianId(guardian.getId());
        request.setRelation(GuardianRelation.FAMILY);

        // when & then
        mockMvc.perform(post("/api/guardians/requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("G001"))
                .andExpect(jsonPath("$.data.relation").value("FAMILY"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(print());

        // 데이터베이스 검증
        List<GuardianRequest> requests = guardianRequestRepository
            .findByGuardianIdAndStatus(guardian.getId(), RequestStatus.PENDING);
        assertThat(requests).hasSize(1);
    }

    @Test
    @DisplayName("보호자 요청 수락 API - PENDING 요청을 수락하고 관계 설정")
    void acceptRequest_PendingRequest_ReturnsSuccess() throws Exception {
        // given
        MemberEntity requester = createAndSaveMember("requester@example.com");
        MemberEntity guardian = createAndSaveMember("guardian@example.com");
        GuardianRequest request = createAndSaveRequest(requester, guardian, RequestStatus.PENDING);
        String token = jwtUtil.createAccessToken(guardian.getMemberEmail());

        // when & then
        mockMvc.perform(post("/api/guardians/requests/{requestId}/accept", request.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("G003"))
                .andDo(print());

        // 데이터베이스 검증
        MemberEntity updatedRequester = memberRepository.findById(requester.getId()).get();
        assertThat(updatedRequester.getGuardian()).isNotNull();
        assertThat(updatedRequester.getGuardian().getId()).isEqualTo(guardian.getId());
    }

    @Test
    @DisplayName("보호자 관계 해제 API - 기존 보호자를 해제")
    void removeGuardian_ExistingGuardian_ReturnsSuccess() throws Exception {
        // given
        MemberEntity member = createAndSaveMember("member@example.com");
        MemberEntity guardian = createAndSaveMember("guardian@example.com");
        member.assignGuardian(guardian, GuardianRelation.FAMILY);
        memberRepository.save(member);

        String token = jwtUtil.createAccessToken(member.getMemberEmail());

        // when & then
        mockMvc.perform(delete("/api/members/me/guardian")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("G005"))
                .andDo(print());

        // 데이터베이스 검증
        MemberEntity updatedMember = memberRepository.findById(member.getId()).get();
        assertThat(updatedMember.getGuardian()).isNull();
    }
}
```

#### 3.4.6 체크리스트

- [ ] GuardianRelationController 생성
- [ ] 5개 API 엔드포인트 구현
- [ ] SuccessCode 5개 추가
- [ ] ErrorCode 4개 추가
- [ ] Swagger 문서화 완료
- [ ] 통합 테스트 3개 작성 및 통과
- [ ] @AutoApiResponse 적용 확인
- [ ] JWT 인증 적용 확인

---

### 3.5 Task 5: DailyCheck 도메인 수정

**우선순위**: P1
**예상 소요 시간**: 0.5일
**담당 도메인**: DailyCheck

#### 3.5.1 변경 사항

**MemberRepository 쿼리 수정**:
```java
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    // 기존 메서드들...

    // ❌ 기존 메서드 제거 (Phase 1에서 이미 변경됨)
    // List<Long> findActiveMemberIds();

    // ✅ Phase 1에서 이미 추가됨
    @Query("SELECT m.id FROM MemberEntity m WHERE m.dailyCheckEnabled = true")
    List<Long> findDailyCheckEnabledMemberIds();
}
```

**DailyCheckOrchestrator 수정** (Phase 1에서 이미 완료):
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyCheckOrchestrator {
    private final MemberRepository memberRepository;
    private final DailyCheckService dailyCheckService;

    @Scheduled(cron = "0 0 9 * * *")  // 매일 오전 9시
    @Transactional
    public void sendDailyCheckMessages() {
        log.info("Starting daily check message sending...");

        // dailyCheckEnabled = true인 회원만 조회
        List<Long> memberIds = memberRepository.findDailyCheckEnabledMemberIds();

        for (Long memberId : memberIds) {
            try {
                dailyCheckService.processMemberDailyCheck(memberId);
            } catch (Exception e) {
                log.error("Failed to process daily check for member: {}", memberId, e);
            }
        }

        log.info("Daily check messages sent to {} members", memberIds.size());
    }
}
```

#### 3.5.2 테스트 수정

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyCheckOrchestrator 테스트")
class DailyCheckOrchestratorTest {

    @Mock private MemberRepository memberRepository;
    @Mock private DailyCheckService dailyCheckService;

    @InjectMocks
    private DailyCheckOrchestrator orchestrator;

    @Test
    @DisplayName("안부 메시지 발송 - dailyCheckEnabled=true인 회원에게만 발송")
    void sendDailyCheckMessages_OnlyEnabledMembers_ProcessesCorrectMembers() {
        // given
        List<Long> enabledMemberIds = List.of(1L, 2L, 3L);
        given(memberRepository.findDailyCheckEnabledMemberIds())
            .willReturn(enabledMemberIds);

        // when
        orchestrator.sendDailyCheckMessages();

        // then
        verify(dailyCheckService, times(3)).processMemberDailyCheck(anyLong());
        verify(dailyCheckService).processMemberDailyCheck(1L);
        verify(dailyCheckService).processMemberDailyCheck(2L);
        verify(dailyCheckService).processMemberDailyCheck(3L);
    }

    @Test
    @DisplayName("안부 메시지 발송 - dailyCheckEnabled=false이면 제외")
    void sendDailyCheckMessages_DisabledMembers_ExcludesDisabledMembers() {
        // given
        given(memberRepository.findDailyCheckEnabledMemberIds())
            .willReturn(Collections.emptyList());

        // when
        orchestrator.sendDailyCheckMessages();

        // then
        verify(dailyCheckService, never()).processMemberDailyCheck(anyLong());
    }
}
```

#### 3.5.3 체크리스트

- [x] MemberRepository 쿼리 이미 수정됨 (Phase 1)
- [x] DailyCheckOrchestrator 이미 수정됨 (Phase 1)
- [ ] 기존 테스트 수정
- [ ] 통합 테스트 추가 (dailyCheckEnabled 기반 발송 검증)

---

### 3.6 Task 6: Conversation 도메인 검증

**우선순위**: P2
**예상 소요 시간**: 0.5일
**담당 도메인**: Conversation

#### 3.6.1 검증 항목

**기존 구현 재사용 확인**:
- ✅ OpenAI GPT-4o 연동 정상 동작
- ✅ 감정 분석 (POSITIVE/NEGATIVE/NEUTRAL) 정상 동작
- ✅ 대화 세션 자동 생성
- ✅ REST API 정상 동작

#### 3.6.2 검증 테스트

```java
@SpringBootTest
@Transactional
@DisplayName("Conversation 도메인 검증 테스트")
class ConversationVerificationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("대화 메시지 전송 - 정상적인 메시지로 AI 응답 반환")
    void processUserMessage_ValidMessage_ReturnsAiResponse() {
        // given
        MemberEntity member = createAndSaveMember("test@example.com");
        member.updateDailyCheckEnabled(true);  // 안부 메시지 수신자
        memberRepository.save(member);

        String userMessage = "오늘 날씨가 좋네요";

        // when
        ConversationResponseDto response = conversationService
            .processUserMessage(member.getId(), userMessage);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAiMessage()).isNotBlank();
        assertThat(response.getEmotionType()).isIn(
            EmotionType.POSITIVE, EmotionType.NEGATIVE, EmotionType.NEUTRAL);
    }

    @Test
    @DisplayName("감정 분석 - POSITIVE 키워드에 대해 긍정 감정 반환")
    void processUserMessage_PositiveKeyword_ReturnsPositiveEmotion() {
        // given
        MemberEntity member = createAndSaveMember("test@example.com");
        String positiveMessage = "오늘 기분이 너무 좋아요!";

        // when
        ConversationResponseDto response = conversationService
            .processUserMessage(member.getId(), positiveMessage);

        // then
        assertThat(response.getEmotionType()).isEqualTo(EmotionType.POSITIVE);
    }

    @Test
    @DisplayName("감정 분석 - NEGATIVE 키워드에 대해 부정 감정 반환")
    void processUserMessage_NegativeKeyword_ReturnsNegativeEmotion() {
        // given
        MemberEntity member = createAndSaveMember("test@example.com");
        String negativeMessage = "너무 외로워요...";

        // when
        ConversationResponseDto response = conversationService
            .processUserMessage(member.getId(), negativeMessage);

        // then
        assertThat(response.getEmotionType()).isEqualTo(EmotionType.NEGATIVE);
    }
}
```

#### 3.6.3 체크리스트

- [ ] 기존 테스트 실행 및 통과 확인
- [ ] 검증 테스트 3개 작성 및 통과
- [ ] OpenAI API 연동 정상 동작 확인
- [ ] 감정 분석 키워드 기반 정상 동작 확인

---

## 4. 일정별 작업 분배

### 4.1 Week 1: Guardian 관계 관리 시스템 (Day 1-5)

#### Day 1 (2시간): GuardianRequest Entity + Repository

**오전**:
- [ ] GuardianRequest Entity 생성 (코딩 표준 준수)
- [ ] RequestStatus Enum 생성
- [ ] GuardianRelation Enum 재사용 확인
- [ ] 단위 테스트 4개 작성 (TDD Red)
- [ ] 최소 구현 (TDD Green)

**오후**:
- [ ] GuardianRequestRepository 생성
- [ ] 쿼리 메서드 4개 구현
- [ ] Repository 테스트 3개 작성 (TDD Red)
- [ ] 테스트 통과 확인 (TDD Green)
- [ ] 리팩토링 (TDD Blue)

**완료 기준**:
- ✅ GuardianRequest Entity 완성
- ✅ GuardianRequestRepository 완성
- ✅ 테스트 7개 통과

---

#### Day 2-3 (1.5일): GuardianRelationService 구현

**Day 2 오전**:
- [ ] GuardianRelationService 클래스 생성
- [ ] sendRequest 메서드 TDD Red (실패 테스트)
- [ ] sendRequest 메서드 TDD Green (최소 구현)
- [ ] getReceivedRequests 메서드 TDD Red/Green

**Day 2 오후**:
- [ ] acceptRequest 메서드 TDD Red/Green
- [ ] rejectRequest 메서드 TDD Red/Green
- [ ] removeGuardian 메서드 TDD Red/Green
- [ ] 비즈니스 규칙 검증 메서드 구현

**Day 3 오전**:
- [ ] GuardianRequestResponse DTO 생성
- [ ] GuardianRequestDto DTO 생성
- [ ] Swagger @Schema 문서화
- [ ] Bean Validation 적용

**Day 3 오후**:
- [ ] TDD Blue: 리팩토링 (50% 코드 개선 목표)
- [ ] 단위 테스트 5개 최종 검증
- [ ] 코드 리뷰 및 정리

**완료 기준**:
- ✅ GuardianRelationService 5개 메서드 완성
- ✅ DTO 2개 완성
- ✅ 단위 테스트 5개 통과
- ✅ 코드 품질 50% 개선

---

#### Day 4 (1일): GuardianController API 구현

**오전**:
- [ ] GuardianRelationController 클래스 생성
- [ ] API 엔드포인트 5개 구현
- [ ] Swagger 문서화 완료
- [ ] @AutoApiResponse 적용
- [ ] JWT 인증 적용

**오후**:
- [ ] SuccessCode 5개 추가
- [ ] ErrorCode 4개 추가
- [ ] 통합 테스트 3개 작성
- [ ] MockMvc 기반 API 테스트
- [ ] JWT 토큰 인증 테스트

**완료 기준**:
- ✅ Guardian API 5개 엔드포인트 완성
- ✅ Swagger 문서 완성
- ✅ 통합 테스트 3개 통과

---

#### Day 5 (0.5일): DailyCheck + Conversation 검증

**오전**:
- [ ] DailyCheck 테스트 수정
- [ ] dailyCheckEnabled 기반 발송 검증 테스트
- [ ] 통합 테스트 추가

**오후**:
- [ ] Conversation 기존 테스트 실행
- [ ] 검증 테스트 3개 추가
- [ ] OpenAI API 연동 확인
- [ ] 감정 분석 정상 동작 확인

**완료 기준**:
- ✅ DailyCheck 테스트 통과
- ✅ Conversation 검증 완료

---

### 4.2 Week 2: 통합 테스트 및 문서화 (Day 6-7.5)

#### Day 6 (1일): 전체 시나리오 통합 테스트

**오전**:
- [ ] User Journey 1-4 시나리오 통합 테스트 작성
- [ ] 김순자 Journey (안부 받는 역할) 테스트
- [ ] 김영희 Journey (보호자 역할) 테스트

**오후**:
- [ ] 보호자 요청/수락 전체 플로우 테스트
- [ ] 안부 메시지 발송 플로우 테스트
- [ ] 에러 케이스 테스트

**완료 기준**:
- ✅ 전체 시나리오 테스트 통과
- ✅ 테스트 커버리지 90% 이상

---

#### Day 7 (0.5일): Phase 1 테스트 보완

**오전**:
- [ ] MemberEntity 테스트 작성
- [ ] MemberService 테스트 작성
- [ ] MemberController 통합 테스트 작성

**오후**:
- [ ] Auth 로그인 테스트 작성
- [ ] 전체 테스트 실행 및 통과 확인

**완료 기준**:
- ✅ Phase 1 테스트 커버리지 90% 이상
- ✅ 전체 빌드 성공

---

#### Day 7.5 (0.5일): 문서 업데이트 및 정리

**오전**:
- [ ] development-plan.md 업데이트 (Phase 2 완료 상태)
- [ ] docs/domains/guardian.md 수정 (새로운 구조 반영)
- [ ] CLAUDE.md 업데이트

**오후**:
- [ ] Swagger UI 최종 검증
- [ ] API 명세서 최신화 확인
- [ ] README.md 업데이트 (Phase 2 완료)

**완료 기준**:
- ✅ 문서 100% 최신화
- ✅ Phase 2 완료 보고서 작성

---

## 5. 리스크 및 대응방안

### 5.1 기술적 리스크

| 리스크 | 영향도 | 발생 확률 | 대응 방안 |
|--------|--------|----------|----------|
| **GuardianRequest 중복 제약 위반** | 중 | 중 | UniqueConstraint + Repository 검증 로직 이중 적용 |
| **순환 참조 문제** (MemberEntity 자기 참조) | 중 | 낮 | @JsonIgnore, DTO 변환으로 방지 |
| **Notification Mock 동작 불안정** | 낮 | 낮 | NotificationService Mock 명확한 동작 정의 |
| **DailyCheck 쿼리 성능 저하** | 중 | 낮 | dailyCheckEnabled 인덱스 활용 |

### 5.2 일정 리스크

| 리스크 | 영향도 | 발생 확률 | 대응 방안 |
|--------|--------|----------|----------|
| **GuardianRelationService 구현 지연** | 높음 | 중 | Day 2-3 버퍼 0.5일 확보, 우선순위 조정 |
| **통합 테스트 실패** | 중 | 중 | Day 6 전체 할애, 필요 시 Day 7.5 활용 |
| **테스트 커버리지 미달** | 중 | 낮 | 각 Task별 테스트 먼저 작성 (TDD Red) |

### 5.3 비즈니스 리스크

| 리스크 | 영향도 | 발생 확률 | 대응 방안 |
|--------|--------|----------|----------|
| **보호자 요청 중복 시나리오** | 중 | 중 | 비즈니스 규칙 명확화: PENDING 요청 1개만 허용 |
| **보호자 관계 해제 후 데이터 정합성** | 높음 | 낮 | 관계 해제 시 GuardianRequest 상태 변경 고려 |

---

## 6. 완료 기준

### 6.1 기능적 완료 기준

- [x] **Guardian 관계 요청 생성**: 요청자가 보호자에게 요청 가능
- [ ] **보호자 요청 수락**: 보호자가 요청 수락 시 MemberEntity.guardian 설정
- [ ] **보호자 요청 거절**: 보호자가 요청 거절 가능
- [ ] **보호자 관계 해제**: 회원이 보호자 관계 해제 가능
- [ ] **안부 메시지**: dailyCheckEnabled=true인 회원에게만 발송
- [ ] **AI 대화**: OpenAI GPT-4o 정상 동작 확인

### 6.2 기술적 완료 기준

- [ ] **API 완성도**: 5개 엔드포인트 정상 동작 (Swagger 테스트)
- [ ] **테스트 커버리지**: 전체 90% 이상 (JaCoCo)
- [ ] **빌드 성공**: `./gradlew clean build` 전체 테스트 통과
- [ ] **문서화**: Swagger UI 완전 문서화 완료
- [ ] **코드 품질**: TDD Blue 단계 50% 코드 개선

### 6.3 검증 체크리스트

#### Guardian API 검증
- [ ] POST /api/guardians/requests → GuardianRequest 생성 + 알림 발송
- [ ] GET /api/guardians/requests → 내가 받은 요청 목록 조회
- [ ] POST /api/guardians/requests/{id}/accept → 요청 수락 + 관계 설정
- [ ] POST /api/guardians/requests/{id}/reject → 요청 거절
- [ ] DELETE /api/members/me/guardian → 보호자 관계 해제

#### DailyCheck 검증
- [ ] dailyCheckEnabled=true인 회원에게만 안부 메시지 발송
- [ ] dailyCheckEnabled=false인 회원은 제외
- [ ] 스케줄러 정시 동작 (오전 9시)

#### Conversation 검증
- [ ] AI 대화 정상 응답
- [ ] 감정 분석 정상 동작 (POSITIVE/NEGATIVE/NEUTRAL)
- [ ] 대화 세션 자동 생성

#### 통합 시나리오 검증
- [ ] Journey 1: 김순자 회원가입 → 안부 메시지 수신
- [ ] Journey 3: 김순자 → 김영희에게 보호자 요청
- [ ] Journey 4: 김영희 → 요청 수락 → 관계 설정
- [ ] Journey 4: 김영희 → 이상징후 알림 수신 (Phase 3 예고)

---

## 부록: 참고 자료

### A. 관련 문서
- **user-journey.md**: 사용자 여정 상세 시나리오
- **api-specification.md**: REST API 명세서
- **development-plan.md**: 전체 개발 계획
- **docs/specifications/coding-standards.md**: 코딩 표준
- **docs/specifications/testing-guide.md**: TDD 가이드
- **docs/specifications/database-design-guide.md**: Entity 설계 패턴

### B. 핵심 패턴 요약

#### Entity 패턴
```java
@Entity
@Table(name = "table_name", indexes = {...})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityName extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정적 팩토리 메서드
    public static EntityName create(...) { }

    // 비즈니스 로직 메서드
    public void updateXxx(...) { }
}
```

#### Service 패턴
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ServiceName {
    private final RepositoryName repository;

    @Transactional  // 쓰기 작업만
    public void save(...) { }

    private void validate...() { }  // 비즈니스 규칙 검증
}
```

#### Controller 패턴
```java
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "API 이름", description = "설명")
public class ControllerName {
    private final ServiceName service;

    @PostMapping
    @SuccessCodeAnnotation(SuccessCode.XXX)
    public ResponseDto create(@Valid @RequestBody RequestDto request) { }
}
```

---

**Version**: 1.0.0
**Created**: 2025-01-06
**Updated**: 2025-01-06
**Status**: 진행 준비 완료
**Next Step**: Task 1 (GuardianRequest Entity 생성) 시작
