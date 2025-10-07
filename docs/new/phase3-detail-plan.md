# Phase 3 상세 진행 계획

**MARUNI AlertRule & Guardian 통합 시스템 완성**

---

**버전**: 1.0.0
**작성일**: 2025-10-08
**최종 업데이트**: 2025-10-08
**상태**: 진행 준비
**예상 기간**: 0.5주 (3 영업일)
**기반 문서**: user-journey.md (Journey 4 Phase 6-7), development-plan.md

---

## 📋 목차

1. [개요](#1-개요)
2. [Phase 3 목표](#2-phase-3-목표)
3. [상세 작업 계획](#3-상세-작업-계획)
4. [일정별 작업 분배](#4-일정별-작업-분배)
5. [리스크 및 대응방안](#5-리스크-및-대응방안)
6. [완료 기준](#6-완료-기준)

---

## 1. 개요

### 1.1 Phase 3 배경

**Phase 1-2 완료 상태** (100%):
- ✅ Phase 1: Member + Auth 도메인 완성
- ✅ Phase 2: Guardian 관계 관리 시스템 완성
- ✅ 코드 단순화: 불필요한 코드 제거 완료
- ✅ 전체 테스트: 258개 100% 통과

**Phase 3 필요성**:
- AlertRule 도메인은 이미 구현되어 있지만, Guardian 통합 검증 필요
- User Journey 4 Phase 6-7의 이상징후 알림 플로우 구현 필요
- 전체 시스템 통합 시나리오 검증 (안부 → 대화 → 감정 분석 → 이상징후 → 보호자 알림)

### 1.2 Phase 3 범위

**포함**:
- AlertRule과 Guardian 연동 검증
- 이상징후 감지 시 보호자 알림 플로우 확인
- 전체 User Journey 통합 테스트 (Journey 1-4 완전 시나리오)
- AlertRule API 검증 및 테스트

**제외**:
- 실제 Firebase FCM 푸시 알림 (Mock 유지 - 추후 확장)
- 알림 UI 구현 (백엔드 API만)
- 성능 최적화 (추후 단계)

---

## 2. Phase 3 목표

### 2.1 핵심 목표

1. **AlertRule ↔ Guardian 통합 검증**: 이상징후 감지 시 보호자에게 알림 발송 확인
2. **전체 User Journey 시나리오 검증**: 회원가입 → 안부 메시지 → 보호자 등록 → 이상징후 알림
3. **AlertRule API 완성도 검증**: 기존 구현된 AlertRule 기능 테스트
4. **통합 테스트 확장**: Phase 2 통합 테스트에 AlertRule 시나리오 추가

### 2.2 완료 기준

| 항목 | 기준 | 측정 방법 |
|------|------|-----------|
| **AlertRule 연동** | Guardian 알림 정상 발송 | 통합 테스트 |
| **전체 시나리오** | Journey 1-4 완전 동작 | End-to-End 테스트 |
| **AlertRule API** | 8개 엔드포인트 정상 동작 | Swagger 테스트 |
| **테스트 커버리지** | 90% 이상 유지 | JaCoCo 리포트 |
| **빌드 성공** | 전체 테스트 통과 | Gradle test |

---

## 3. 상세 작업 계획

### 3.1 Task 1: AlertRule ↔ Guardian 연동 검증

**우선순위**: P0 (최우선)
**예상 소요 시간**: 0.5일
**담당 도메인**: AlertRule, Guardian, Notification

#### 3.1.1 현재 시스템 분석

**AlertRule 도메인 구조** (이미 구현됨):
- ✅ AlertRule Entity (이상징후 규칙 정의)
- ✅ AlertHistory Entity (알림 이력)
- ✅ 3종 감지 알고리즘:
  - EmotionPatternAnalyzer (감정 패턴 분석)
  - NoResponseAnalyzer (무응답 감지)
  - KeywordAnalyzer (키워드 감지)
- ✅ AlertRuleService (규칙 관리)
- ✅ 8개 REST API 엔드포인트

**Guardian 연동 확인 포인트**:
```java
// AlertRule이 Guardian을 찾아서 알림을 보내는 로직 확인
MemberEntity member = findMemberById(memberId);
MemberEntity guardian = member.getGuardian();

if (guardian == null) {
    log.warn("No guardian found for member {}", memberId);
    return;
}

// NotificationService를 통해 보호자에게 알림 발송
notificationService.sendPushNotification(
    guardian.getId(),
    "이상징후 감지",
    alertMessage
);
```

#### 3.1.2 검증 작업

**Step 1: 코드 분석**
- [ ] AlertRule 서비스에서 Guardian 조회 로직 확인
- [ ] Guardian이 null일 때 처리 로직 확인
- [ ] NotificationService 연동 확인

**Step 2: 단위 테스트 작성**
```java
@Test
@DisplayName("이상징후 감지 - 보호자가 있으면 알림 발송")
void detectAlert_HasGuardian_SendsNotification() {
    // given
    MemberEntity member = createMember(1L, "member@example.com");
    MemberEntity guardian = createMember(2L, "guardian@example.com");
    member.assignGuardian(guardian, GuardianRelation.FAMILY);

    AlertRule rule = createEmotionPatternRule(member.getId());

    // 3일 연속 NEGATIVE 감정 생성
    createNegativeConversations(member.getId(), 3);

    // when
    alertRuleService.checkAndTriggerAlerts(member.getId());

    // then
    verify(notificationService).sendPushNotification(
        eq(guardian.getId()),
        eq("이상징후 감지"),
        contains("3일 연속 우울한 감정")
    );
}

@Test
@DisplayName("이상징후 감지 - 보호자가 없으면 알림 미발송")
void detectAlert_NoGuardian_NoNotification() {
    // given
    MemberEntity member = createMember(1L, "member@example.com");
    // guardian 없음

    AlertRule rule = createEmotionPatternRule(member.getId());
    createNegativeConversations(member.getId(), 3);

    // when
    alertRuleService.checkAndTriggerAlerts(member.getId());

    // then
    verify(notificationService, never()).sendPushNotification(any(), any(), any());
}
```

**Step 3: 통합 테스트 확장**
- [ ] UserJourneyIntegrationTest에 이상징후 시나리오 추가
- [ ] Journey 1-4 전체 플로우 검증

#### 3.1.3 체크리스트

- [ ] AlertRule에서 Guardian 조회 로직 확인
- [ ] Guardian null 체크 로직 확인
- [ ] NotificationService 연동 확인
- [ ] 단위 테스트 2개 작성 및 통과
- [ ] 통합 테스트 확장 (이상징후 시나리오)

---

### 3.2 Task 2: AlertRule API 검증

**우선순위**: P1
**예상 소요 시간**: 0.5일
**담당 도메인**: AlertRule

#### 3.2.1 기존 AlertRule API 확인

**8개 REST API 엔드포인트** (이미 구현됨):
```
POST   /api/alert-rules                    # 규칙 생성
GET    /api/alert-rules/{id}               # 규칙 조회
PUT    /api/alert-rules/{id}               # 규칙 수정
DELETE /api/alert-rules/{id}               # 규칙 삭제
GET    /api/alert-rules/member/{memberId}  # 회원별 규칙 목록
GET    /api/alert-history/{memberId}       # 알림 이력 조회
POST   /api/alert-rules/check/{memberId}   # 수동 감지 트리거
GET    /api/alerts/{alertId}               # 알림 상세 조회 (User Journey 요구)
```

#### 3.2.2 검증 작업

**Step 1: Swagger 문서화 확인**
- [ ] 모든 API가 Swagger에 문서화되어 있는지 확인
- [ ] @AutoApiResponse 적용 확인
- [ ] SuccessCode/ErrorCode 확인

**Step 2: API 테스트 작성**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AlertRule API 통합 테스트")
class AlertRuleApiIntegrationTest {

    @Test
    @DisplayName("규칙 생성 API - 감정 패턴 규칙 생성")
    void createAlertRule_EmotionPattern_ReturnsSuccess() throws Exception {
        // given
        String token = createJwtToken(memberId);
        AlertRuleCreateRequestDto request = AlertRuleCreateRequestDto.builder()
            .alertType(AlertType.EMOTION_PATTERN)
            .condition(AlertCondition.builder()
                .consecutiveDays(3)
                .emotionType(EmotionType.NEGATIVE)
                .build())
            .alertLevel(AlertLevel.MEDIUM)
            .build();

        // when & then
        mockMvc.perform(post("/api/alert-rules")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.alertType").value("EMOTION_PATTERN"));
    }

    @Test
    @DisplayName("알림 이력 조회 API - 회원의 알림 이력 목록")
    void getAlertHistory_ValidMember_ReturnsHistory() throws Exception {
        // given
        String token = createJwtToken(memberId);
        createAlertHistory(memberId, 3); // 3개 알림 이력 생성

        // when & then
        mockMvc.perform(get("/api/alert-history/{memberId}", memberId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }
}
```

#### 3.2.3 체크리스트

- [ ] 8개 API 엔드포인트 확인
- [ ] Swagger 문서화 검증
- [ ] API 통합 테스트 5개 작성
- [ ] JWT 인증 적용 확인
- [ ] ErrorCode 정의 확인

---

### 3.3 Task 3: 전체 User Journey 통합 테스트

**우선순위**: P0
**예상 소요 시간**: 1일
**담당 도메인**: All (통합)

#### 3.3.1 완전한 User Journey 시나리오

**시나리오: 김순자 할머니 & 김영희 보호자 전체 플로우**

```
Step 1: 김순자 회원가입 (Journey 1)
  ↓
Step 2: 김영희 회원가입 (Journey 4 Phase 1)
  ↓
Step 3: 김순자 → 김영희에게 보호자 요청 (Journey 3)
  ↓
Step 4: 김영희 요청 수락 (Journey 4 Phase 4)
  ↓
Step 5: 김순자 안부 메시지 수신 (Journey 2)
  ↓
Step 6: 3일 연속 NEGATIVE 대화 (Journey 4 Phase 6)
  - Day 1: "외로워요" → NEGATIVE
  - Day 2: "아무도 안 찾아와요" → NEGATIVE
  - Day 3: "혼자 있으니 우울해요" → NEGATIVE
  ↓
Step 7: AlertRule 자동 감지
  ↓
Step 8: 김영희에게 푸시 알림 발송 ✅
  ↓
Step 9: 김영희 알림 상세 조회 (Journey 4 Phase 7)
```

#### 3.3.2 통합 테스트 구현

```java
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("전체 User Journey 통합 테스트")
class CompleteUserJourneyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GuardianRequestRepository guardianRequestRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JWTUtil jwtUtil;

    @Test
    @DisplayName("완전한 User Journey - 회원가입부터 이상징후 알림까지")
    void completeUserJourney_FromSignupToAlertNotification() throws Exception {
        // ===== Step 1: 김순자 회원가입 =====
        MemberEntity soonja = createAndSaveMember("soonja@example.com", "김순자");
        soonja.updateDailyCheckEnabled(true);
        memberRepository.save(soonja);

        // ===== Step 2: 김영희 회원가입 =====
        MemberEntity younghee = createAndSaveMember("younghee@example.com", "김영희");
        memberRepository.save(younghee);

        // ===== Step 3: 김순자 → 김영희에게 보호자 요청 =====
        String soonjaToken = jwtUtil.createAccessToken(
            String.valueOf(soonja.getId()),
            soonja.getMemberEmail()
        );

        GuardianRequestDto guardianRequest = new GuardianRequestDto();
        guardianRequest.setGuardianId(younghee.getId());
        guardianRequest.setRelation(GuardianRelation.FAMILY);

        mockMvc.perform(post("/api/guardians/requests")
                .header("Authorization", "Bearer " + soonjaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(guardianRequest)))
                .andExpect(status().isOk());

        // ===== Step 4: 김영희 요청 수락 =====
        String youngheeToken = jwtUtil.createAccessToken(
            String.valueOf(younghee.getId()),
            younghee.getMemberEmail()
        );

        GuardianRequest request = guardianRequestRepository
            .findByGuardianIdAndStatus(younghee.getId(), RequestStatus.PENDING)
            .get(0);

        mockMvc.perform(post("/api/guardians/requests/{requestId}/accept", request.getId())
                .header("Authorization", "Bearer " + youngheeToken))
                .andExpect(status().isOk());

        // ===== Step 5: AlertRule 자동 생성 (감정 패턴 규칙) =====
        AlertRule emotionRule = AlertRule.builder()
            .memberId(soonja.getId())
            .alertType(AlertType.EMOTION_PATTERN)
            .condition(AlertCondition.builder()
                .consecutiveDays(3)
                .emotionType(EmotionType.NEGATIVE)
                .build())
            .alertLevel(AlertLevel.MEDIUM)
            .enabled(true)
            .build();
        alertRuleService.createAlertRule(emotionRule);

        // ===== Step 6: 3일 연속 NEGATIVE 대화 생성 =====
        createNegativeConversation(soonja.getId(), "외로워요");  // Day 1
        createNegativeConversation(soonja.getId(), "아무도 안 찾아와요");  // Day 2
        createNegativeConversation(soonja.getId(), "혼자 있으니 우울해요");  // Day 3

        // NotificationService Mock 초기화
        reset(notificationService);

        // ===== Step 7: AlertRule 감지 트리거 =====
        alertRuleService.checkAndTriggerAlerts(soonja.getId());

        // ===== Step 8: 김영희에게 푸시 알림 발송 검증 =====
        verify(notificationService).sendPushNotification(
            eq(younghee.getId()),
            eq("이상징후 감지"),
            contains("3일 연속 우울한 감정")
        );

        // ===== Step 9: 알림 이력 확인 =====
        mockMvc.perform(get("/api/alert-history/{memberId}", soonja.getId())
                .header("Authorization", "Bearer " + soonjaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].alertLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data[0].alertMessage").exists());
    }

    // Helper Methods
    private void createNegativeConversation(Long memberId, String message) {
        ConversationEntity conversation = conversationRepository
            .findByMemberIdAndDate(memberId, LocalDate.now())
            .orElseGet(() -> {
                ConversationEntity newConv = ConversationEntity.createConversation(memberId);
                return conversationRepository.save(newConv);
            });

        MessageEntity userMessage = MessageEntity.createUserMessage(message);
        MessageEntity aiMessage = MessageEntity.createAiMessage(
            "힘든 시간을 보내고 계시는군요",
            EmotionType.NEGATIVE
        );

        conversation.addMessage(userMessage);
        conversation.addMessage(aiMessage);
        conversationRepository.save(conversation);
    }
}
```

#### 3.3.3 체크리스트

- [ ] 완전한 User Journey 통합 테스트 작성
- [ ] 9단계 시나리오 모두 검증
- [ ] Guardian 알림 발송 검증
- [ ] AlertRule 자동 감지 검증
- [ ] 알림 이력 조회 검증

---

## 4. 일정별 작업 분배

### Day 1 (1일): AlertRule ↔ Guardian 연동 검증

**오전**:
- [ ] AlertRule 코드 분석 (Guardian 조회 로직 확인)
- [ ] Guardian null 체크 로직 확인
- [ ] NotificationService 연동 확인

**오후**:
- [ ] 단위 테스트 2개 작성 (보호자 있음/없음)
- [ ] 테스트 통과 확인
- [ ] UserJourneyIntegrationTest에 이상징후 시나리오 추가

**완료 기준**:
- ✅ AlertRule ↔ Guardian 연동 검증 완료
- ✅ 단위 테스트 2개 통과

---

### Day 2 (1일): AlertRule API 검증 & 통합 테스트

**오전**:
- [ ] 8개 AlertRule API 엔드포인트 확인
- [ ] Swagger 문서화 검증
- [ ] API 통합 테스트 5개 작성

**오후**:
- [ ] 전체 User Journey 통합 테스트 작성
- [ ] 9단계 시나리오 구현
- [ ] 테스트 실행 및 디버깅

**완료 기준**:
- ✅ AlertRule API 5개 테스트 통과
- ✅ 전체 User Journey 테스트 작성 완료

---

### Day 3 (0.5일): 최종 검증 및 문서화

**오전**:
- [ ] 전체 테스트 실행 (./gradlew test)
- [ ] 테스트 커버리지 확인 (90% 이상)
- [ ] 실패한 테스트 수정

**오후**:
- [ ] Phase 3 완료 보고서 작성
- [ ] development-plan.md 업데이트
- [ ] README.md 업데이트

**완료 기준**:
- ✅ 전체 테스트 100% 통과
- ✅ 테스트 커버리지 90% 이상
- ✅ 문서 최신화 완료

---

## 5. 리스크 및 대응방안

### 5.1 기술적 리스크

| 리스크 | 영향도 | 발생 확률 | 대응 방안 |
|--------|--------|----------|----------|
| **AlertRule에서 Guardian 조회 실패** | 높음 | 낮음 | Guardian null 체크 로직 강화, 단위 테스트 충분히 작성 |
| **3일 연속 감정 패턴 감지 실패** | 중 | 낮음 | EmotionPatternAnalyzer 로직 확인, 테스트 데이터 정확히 생성 |
| **NotificationService Mock 동작 불안정** | 낮음 | 낮음 | Mockito verify 정확히 사용, reset() 적절히 호출 |

### 5.2 일정 리스크

| 리스크 | 영향도 | 발생 확률 | 대응 방안 |
|--------|--------|----------|----------|
| **통합 테스트 작성 지연** | 중 | 중 | Day 2 전체 할애, 필요 시 Day 3 활용 |
| **기존 테스트 깨짐** | 중 | 낮음 | 기존 테스트 먼저 실행, 문제 발견 시 즉시 수정 |

---

## 6. 완료 기준

### 6.1 기능적 완료 기준

- [ ] **AlertRule ↔ Guardian 연동**: 이상징후 감지 시 보호자에게 알림 발송
- [ ] **전체 User Journey**: 회원가입 → 보호자 등록 → 이상징후 → 알림 전체 플로우 동작
- [ ] **AlertRule API**: 8개 엔드포인트 정상 동작 확인
- [ ] **알림 이력 조회**: Guardian이 회원의 알림 이력 조회 가능

### 6.2 기술적 완료 기준

- [ ] **테스트 통과**: 전체 테스트 100% 통과
- [ ] **테스트 커버리지**: 90% 이상 유지
- [ ] **빌드 성공**: `./gradlew clean build` 성공
- [ ] **Swagger 문서화**: AlertRule API 완전 문서화

### 6.3 검증 체크리스트

#### AlertRule ↔ Guardian 연동
- [ ] Guardian이 있으면 알림 발송
- [ ] Guardian이 없으면 알림 미발송 (로그만 출력)
- [ ] NotificationService 정상 호출

#### AlertRule API 검증
- [ ] POST /api/alert-rules → 규칙 생성 성공
- [ ] GET /api/alert-rules/{id} → 규칙 조회 성공
- [ ] GET /api/alert-history/{memberId} → 이력 조회 성공
- [ ] GET /api/alerts/{alertId} → 알림 상세 조회 성공 (User Journey 요구)

#### 전체 User Journey 검증
- [ ] Journey 1: 회원가입 → 초기 상태 확인
- [ ] Journey 3: 보호자 요청 → 생성 성공
- [ ] Journey 4: 보호자 수락 → 관계 설정
- [ ] Journey 4 Phase 6: 3일 연속 NEGATIVE → AlertRule 감지
- [ ] Journey 4 Phase 7: Guardian 알림 발송 → 이력 조회

---

## 부록: 핵심 패턴 요약

### AlertRule 연동 패턴
```java
// AlertRule에서 Guardian 조회 및 알림 발송
@Service
public class AlertNotificationService {

    @Transactional
    public void sendGuardianAlert(Long memberId, AlertHistory alert) {
        MemberEntity member = findMemberById(memberId);
        MemberEntity guardian = member.getGuardian();

        if (guardian == null) {
            log.warn("No guardian for member {}, alert not sent", memberId);
            return;
        }

        // Guardian에게 푸시 알림 발송
        notificationService.sendPushNotification(
            guardian.getId(),
            "이상징후 감지",
            formatAlertMessage(alert)
        );

        log.info("Alert sent to guardian {}, member {}",
            guardian.getId(), memberId);
    }
}
```

### 통합 테스트 패턴
```java
// 완전한 플로우 검증
@Test
void completeFlow_SignupToAlert() {
    // 1. 회원가입
    MemberEntity member = createMember();
    MemberEntity guardian = createMember();

    // 2. 보호자 등록
    sendGuardianRequest(member, guardian);
    acceptGuardianRequest(guardian, request);

    // 3. 대화 생성 (3일 연속 NEGATIVE)
    createNegativeConversations(member, 3);

    // 4. AlertRule 감지
    alertRuleService.checkAndTriggerAlerts(member.getId());

    // 5. Guardian 알림 검증
    verify(notificationService).sendPushNotification(
        eq(guardian.getId()),
        any(),
        any()
    );
}
```

---

**Version**: 1.0.0
**Created**: 2025-10-08
**Updated**: 2025-10-08
**Status**: 진행 준비 완료
**Next Step**: Task 1 (AlertRule ↔ Guardian 연동 검증) 시작
