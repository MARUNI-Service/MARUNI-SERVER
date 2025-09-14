# Phase 2: 스케줄링 & 알림 시스템 상세 개발 계획

## 📋 Phase 2 개요

**Phase 2**는 MARUNI의 핵심 비즈니스 로직인 **정기 안부 메시지 스케줄링**과 **보호자 알림 시스템**을 구축하는 단계입니다.
Phase 1의 AI 대화 시스템을 기반으로 실제 노인 돌봄 서비스의 자동화된 워크플로우를 완성합니다.

### 🎯 Phase 2 목표
- **매일 정시 안부 메시지**: 개인별 맞춤 시간에 자동 안부 메시지 발송
- **보호자 연결 시스템**: 가족 구성원 등록 및 관리
- **이상징후 자동 감지**: AI 분석 기반 위험 상황 판단
- **다채널 알림 발송**: 이메일, SMS, 푸시 등 다양한 알림 채널
- **완전한 TDD 적용**: 90% 이상 테스트 커버리지 달성

### 📊 Phase 2 완료 후 달성 상태
```yaml
서비스 자동화 완성도: 80%
핵심 비즈니스 로직: 100% 구현
실제 운영 가능성: 90%
사용자 가치 제공: ⭐⭐⭐⭐⭐

완성되는 핵심 플로우:
매일 정시 → 안부 메시지 발송 → 사용자 응답 → AI 분석 → 이상징후 감지 → 보호자 알림
```

---

## 🏗️ Phase 2 도메인 아키텍처

### 🔄 Scheduling 도메인 (Week 5-6)

#### 도메인 책임
- **정기 안부 메시지 스케줄링**: 사용자별 맞춤 시간 관리
- **메시지 자동 생성**: AI 기반 개인화된 안부 메시지
- **발송 상태 추적**: 메시지 발송 성공/실패 모니터링
- **재시도 메커니즘**: 발송 실패 시 자동 재시도

#### 핵심 엔티티 설계
```java
// 개인별 안부 일정 관리
@Entity
@Table(name = "daily_checks")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true)
public class DailyCheckEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    // 발송 시간 (예: 09:00)
    @Column(nullable = false)
    private LocalTime scheduledTime;

    // 발송 요일 (0=일요일, 6=토요일)
    @Column(nullable = false, columnDefinition = "integer[]")
    private Integer[] scheduledDays;

    // 개인화된 메시지 템플릿
    @Column(columnDefinition = "TEXT")
    private String messageTemplate;

    // 활성화 상태
    @Builder.Default
    private Boolean isActive = true;

    // 마지막 발송 시간
    private LocalDateTime lastSentAt;

    /**
     * 정적 팩토리 메서드: 기본 일일 체크 생성
     */
    public static DailyCheckEntity createDefault(Long memberId) {
        return DailyCheckEntity.builder()
                .memberId(memberId)
                .scheduledTime(LocalTime.of(9, 0)) // 기본 오전 9시
                .scheduledDays(new Integer[]{1, 2, 3, 4, 5, 6, 7}) // 매일
                .messageTemplate("안녕하세요! 오늘 하루는 어떻게 지내고 계세요?")
                .isActive(true)
                .build();
    }

    /**
     * 오늘 발송해야 하는지 확인
     */
    public boolean shouldSendToday(LocalDateTime now) {
        if (!isActive) return false;

        int today = now.getDayOfWeek().getValue() % 7; // 일요일=0으로 변환
        return Arrays.asList(scheduledDays).contains(today);
    }

    /**
     * 발송 시간이 되었는지 확인
     */
    public boolean isTimeToSend(LocalDateTime now) {
        return shouldSendToday(now) &&
               now.toLocalTime().isAfter(scheduledTime) &&
               (lastSentAt == null || !lastSentAt.toLocalDate().equals(now.toLocalDate()));
    }
}

// 생성된 안부 메시지 이력
@Entity
@Table(name = "check_messages")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckMessageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dailyCheckId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CheckMessageStatus status = CheckMessageStatus.PENDING;

    // 발송 실패 시 에러 메시지
    private String errorMessage;

    // 재시도 횟수
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 정적 팩토리 메서드: 안부 메시지 생성
     */
    public static CheckMessageEntity create(Long dailyCheckId, Long memberId,
                                          String content, LocalDateTime scheduledAt) {
        return CheckMessageEntity.builder()
                .dailyCheckId(dailyCheckId)
                .memberId(memberId)
                .content(content)
                .scheduledAt(scheduledAt)
                .status(CheckMessageStatus.PENDING)
                .retryCount(0)
                .build();
    }

    /**
     * 발송 성공 처리
     */
    public void markAsSent() {
        this.status = CheckMessageStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 발송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = CheckMessageStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * 재시도 가능한지 확인
     */
    public boolean canRetry() {
        return status == CheckMessageStatus.FAILED && retryCount < 3;
    }
}

// 안부 메시지 상태
public enum CheckMessageStatus {
    PENDING("대기"),
    SENT("발송완료"),
    FAILED("발송실패"),
    CANCELLED("취소됨");

    private final String description;

    CheckMessageStatus(String description) {
        this.description = description;
    }
}

// 스케줄링 설정 관리
@Entity
@Table(name = "schedule_configs")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleConfigEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 설정 이름 (DAILY_CHECK_BATCH, RETRY_FAILED_MESSAGES 등)
    @Column(nullable = false, unique = true)
    private String configName;

    // Cron 표현식
    @Column(nullable = false)
    private String cronExpression;

    // 설정 값들 (JSON)
    @Column(columnDefinition = "JSONB")
    private String configValues;

    // 활성화 상태
    @Builder.Default
    private Boolean isEnabled = true;

    // 마지막 실행 시간
    private LocalDateTime lastExecutedAt;

    /**
     * 기본 설정 생성
     */
    public static ScheduleConfigEntity createDefault() {
        return ScheduleConfigEntity.builder()
                .configName("DAILY_CHECK_BATCH")
                .cronExpression("0 0 9 * * *") // 매일 오전 9시
                .configValues("{\"batchSize\":50,\"maxRetries\":3}")
                .isEnabled(true)
                .build();
    }
}
```

#### 핵심 서비스 설계
```java
// 정기 안부 메시지 발송 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyCheckService {

    private final DailyCheckRepository dailyCheckRepository;
    private final CheckMessageRepository checkMessageRepository;
    private final MemberService memberService;
    private final PersonalizedMessageService messageService;
    private final ConversationService conversationService; // Phase 1 연동

    /**
     * 스케줄된 안부 메시지 일괄 발송
     */
    @Scheduled(cron = "${maruni.scheduling.daily-check.cron:0 0 9 * * *}")
    @Transactional
    public void sendDailyCheckMessages() {
        log.info("=== 일일 안부 메시지 발송 배치 시작 ===");

        LocalDateTime now = LocalDateTime.now();

        // 1. 오늘 발송해야 할 일정 조회
        List<DailyCheckEntity> todayChecks = dailyCheckRepository
            .findActiveSchedulesForToday(now.getDayOfWeek().getValue() % 7);

        log.info("오늘 발송 대상: {}건", todayChecks.size());

        // 2. 각 일정별로 메시지 생성 및 발송
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        todayChecks.parallelStream()
            .filter(check -> check.isTimeToSend(now))
            .forEach(check -> {
                try {
                    sendDailyCheckMessage(check, now);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("안부 메시지 발송 실패: memberId={}, error={}",
                             check.getMemberId(), e.getMessage(), e);
                    failCount.incrementAndGet();
                }
            });

        log.info("=== 일일 안부 메시지 발송 완료: 성공 {}건, 실패 {}건 ===",
                successCount.get(), failCount.get());
    }

    /**
     * 개별 안부 메시지 발송
     */
    @Transactional
    public void sendDailyCheckMessage(DailyCheckEntity dailyCheck, LocalDateTime now) {
        // 1. 개인화된 메시지 생성
        String personalizedMessage = messageService.generatePersonalizedMessage(
            dailyCheck.getMemberId(),
            dailyCheck.getMessageTemplate()
        );

        // 2. 메시지 엔티티 생성
        CheckMessageEntity checkMessage = CheckMessageEntity.create(
            dailyCheck.getId(),
            dailyCheck.getMemberId(),
            personalizedMessage,
            now
        );

        CheckMessageEntity savedMessage = checkMessageRepository.save(checkMessage);

        try {
            // 3. Phase 1 AI 시스템을 통해 메시지 전송 (미래에 실제 발송 채널 연결)
            // 현재는 대화 시스템에 시스템 메시지로 저장
            processSystemMessage(dailyCheck.getMemberId(), personalizedMessage);

            // 4. 발송 성공 처리
            savedMessage.markAsSent();
            checkMessageRepository.save(savedMessage);

            // 5. 일일 체크 마지막 발송 시간 업데이트
            dailyCheck.updateLastSentAt(now);
            dailyCheckRepository.save(dailyCheck);

            log.debug("안부 메시지 발송 성공: memberId={}, messageId={}",
                     dailyCheck.getMemberId(), savedMessage.getId());

        } catch (Exception e) {
            // 발송 실패 처리
            savedMessage.markAsFailed(e.getMessage());
            checkMessageRepository.save(savedMessage);
            throw e;
        }
    }

    /**
     * 시스템 메시지를 대화 시스템에 추가
     */
    private void processSystemMessage(Long memberId, String message) {
        // Phase 1의 ConversationService 활용
        // 시스템에서 보내는 메시지로 기록
        ConversationRequestDto request = ConversationRequestDto.builder()
            .content(message)
            .build();

        // 실제로는 사용자가 응답할 수 있도록 시스템 메시지로 저장
        // 추후 실제 SMS/푸시 발송 시스템 연결 시 이 부분 수정
        conversationService.processSystemMessage(memberId, message);
    }

    /**
     * 실패한 메시지 재시도
     */
    @Scheduled(fixedDelay = 300000) // 5분마다 실행
    @Transactional
    public void retryFailedMessages() {
        List<CheckMessageEntity> failedMessages = checkMessageRepository
            .findRetryableMessages();

        if (!failedMessages.isEmpty()) {
            log.info("실패 메시지 재시도 시작: {}건", failedMessages.size());

            failedMessages.forEach(message -> {
                try {
                    DailyCheckEntity dailyCheck = dailyCheckRepository
                        .findById(message.getDailyCheckId())
                        .orElseThrow();

                    processSystemMessage(message.getMemberId(), message.getContent());
                    message.markAsSent();
                    checkMessageRepository.save(message);

                    log.debug("재시도 성공: messageId={}", message.getId());

                } catch (Exception e) {
                    message.markAsFailed("재시도 실패: " + e.getMessage());
                    checkMessageRepository.save(message);
                    log.warn("재시도 실패: messageId={}, error={}", message.getId(), e.getMessage());
                }
            });
        }
    }

    /**
     * 회원별 일일 체크 설정 조회
     */
    public DailyCheckResponseDto getDailyCheckByMemberId(Long memberId) {
        DailyCheckEntity dailyCheck = dailyCheckRepository
            .findByMemberIdAndIsActiveTrue(memberId)
            .orElse(null);

        if (dailyCheck == null) {
            // 기본 설정 생성
            dailyCheck = DailyCheckEntity.createDefault(memberId);
            dailyCheck = dailyCheckRepository.save(dailyCheck);
        }

        return DailyCheckMapper.toResponseDto(dailyCheck);
    }

    /**
     * 일일 체크 설정 업데이트
     */
    @Transactional
    public DailyCheckResponseDto updateDailyCheck(Long memberId, DailyCheckUpdateDto updateDto) {
        DailyCheckEntity dailyCheck = dailyCheckRepository
            .findByMemberIdAndIsActiveTrue(memberId)
            .orElseGet(() -> DailyCheckEntity.createDefault(memberId));

        // 설정 업데이트
        if (updateDto.getScheduledTime() != null) {
            dailyCheck.updateScheduledTime(updateDto.getScheduledTime());
        }
        if (updateDto.getScheduledDays() != null) {
            dailyCheck.updateScheduledDays(updateDto.getScheduledDays());
        }
        if (updateDto.getMessageTemplate() != null) {
            dailyCheck.updateMessageTemplate(updateDto.getMessageTemplate());
        }
        if (updateDto.getIsActive() != null) {
            dailyCheck.updateIsActive(updateDto.getIsActive());
        }

        DailyCheckEntity saved = dailyCheckRepository.save(dailyCheck);
        return DailyCheckMapper.toResponseDto(saved);
    }
}

// 개인화된 메시지 생성 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizedMessageService {

    private final MemberService memberService;
    private final ConversationService conversationService;
    private final MessageTemplateService templateService;

    /**
     * 개인화된 안부 메시지 생성
     */
    public String generatePersonalizedMessage(Long memberId, String template) {
        try {
            // 1. 회원 정보 조회
            MemberEntity member = memberService.findById(memberId);

            // 2. 최근 대화 패턴 분석
            RecentConversationContext context = conversationService
                .getRecentConversationContext(memberId, 7); // 최근 7일

            // 3. 개인화된 메시지 생성
            return templateService.personalizeMessage(template, member, context);

        } catch (Exception e) {
            log.warn("개인화 메시지 생성 실패, 기본 템플릿 사용: memberId={}, error={}",
                    memberId, e.getMessage());
            return template != null ? template : "안녕하세요! 오늘 하루는 어떻게 지내고 계세요?";
        }
    }
}
```

### 🚨 Notification 도메인 (Week 7-8)

#### 도메인 책임
- **보호자 관리**: 가족 구성원 등록 및 연락처 관리
- **알림 규칙 엔진**: 이상징후 감지 조건 및 대응 규칙
- **다채널 알림 발송**: 이메일, SMS, 푸시 알림 통합 관리
- **알림 이력 추적**: 발송 상태 및 전달 확인

#### 핵심 엔티티 설계
```java
// 보호자 정보
@Entity
@Table(name = "guardians")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true)
public class GuardianEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 보호받는 회원 ID
    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String name;

    // 관계 (자녀, 배우자, 형제자매 등)
    private String relationship;

    // 연락처 정보 (암호화 저장)
    private String phoneNumber;
    private String email;

    // 주 보호자 여부
    @Builder.Default
    private Boolean isPrimary = false;

    // 알림 설정 (JSON)
    @Column(columnDefinition = "JSONB")
    private String notificationPreferences;

    /**
     * 정적 팩토리 메서드: 보호자 생성
     */
    public static GuardianEntity create(Long memberId, String name, String relationship) {
        return GuardianEntity.builder()
                .memberId(memberId)
                .name(name)
                .relationship(relationship)
                .isPrimary(false)
                .notificationPreferences(createDefaultNotificationPreferences())
                .build();
    }

    /**
     * 기본 알림 설정 생성
     */
    private static String createDefaultNotificationPreferences() {
        NotificationPreferences prefs = NotificationPreferences.builder()
                .emailEnabled(true)
                .smsEnabled(false) // 기본적으로 SMS는 비활성화
                .pushEnabled(true)
                .emergencyOnly(false) // 모든 알림 수신
                .quietHours(QuietHours.create(22, 7)) // 22시~7시 조용한 시간
                .build();

        return JsonUtils.toJson(prefs);
    }

    /**
     * 알림 설정 조회
     */
    public NotificationPreferences getNotificationPreferences() {
        if (notificationPreferences == null) {
            return NotificationPreferences.createDefault();
        }
        return JsonUtils.fromJson(notificationPreferences, NotificationPreferences.class);
    }

    /**
     * 주 보호자로 설정
     */
    public void setPrimary() {
        this.isPrimary = true;
    }
}

// 알림 규칙
@Entity
@Table(name = "alert_rules")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertRuleEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // 조건 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertConditionType conditionType;

    // 조건 상세 설정 (JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    private String conditionConfig;

    // 심각도 등급
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severityLevel;

    @Builder.Default
    private Boolean isActive = true;

    /**
     * 기본 알림 규칙들 생성
     */
    public static List<AlertRuleEntity> createDefaultRules() {
        return Arrays.asList(
            // 1. 부정적 감정 지속
            AlertRuleEntity.builder()
                .name("부정적 감정 지속")
                .description("3일 연속 부정적 감정이 감지되면 알림")
                .conditionType(AlertConditionType.EMOTION_NEGATIVE_STREAK)
                .conditionConfig("{\"consecutiveDays\":3,\"emotionThreshold\":\"NEGATIVE\"}")
                .severityLevel(SeverityLevel.MEDIUM)
                .isActive(true)
                .build(),

            // 2. 응답 없음
            AlertRuleEntity.builder()
                .name("안부 메시지 무응답")
                .description("안부 메시지에 2일 연속 응답하지 않으면 알림")
                .conditionType(AlertConditionType.NO_RESPONSE)
                .conditionConfig("{\"consecutiveDays\":2}")
                .severityLevel(SeverityLevel.HIGH)
                .isActive(true)
                .build(),

            // 3. 응급 키워드
            AlertRuleEntity.builder()
                .name("응급 키워드 감지")
                .description("응급 상황 키워드 감지 시 즉시 알림")
                .conditionType(AlertConditionType.EMERGENCY_KEYWORD)
                .conditionConfig("{\"keywords\":[\"아파\",\"아픈\",\"병원\",\"응급\",\"도와줘\",\"119\"]}")
                .severityLevel(SeverityLevel.CRITICAL)
                .isActive(true)
                .build()
        );
    }
}

// 알림 발송 이력
@Entity
@Table(name = "notifications")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long guardianId;

    private Long alertRuleId;

    // 알림 채널
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient; // 이메일 주소 또는 전화번호

    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 발송 상태
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    private LocalDateTime sentAt;

    // 외부 서비스 ID (이메일 서비스, SMS 서비스 등의 메시지 ID)
    private String externalId;

    // 에러 메시지
    private String errorMessage;

    /**
     * 정적 팩토리 메서드: 알림 생성
     */
    public static NotificationEntity create(Long memberId, Long guardianId,
                                          NotificationChannel channel, String recipient,
                                          String subject, String content) {
        return NotificationEntity.builder()
                .memberId(memberId)
                .guardianId(guardianId)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.PENDING)
                .build();
    }

    /**
     * 발송 성공 처리
     */
    public void markAsSent(String externalId) {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.externalId = externalId;
    }

    /**
     * 발송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}

// Enum 정의들
public enum AlertConditionType {
    EMOTION_NEGATIVE_STREAK("부정 감정 연속"),
    NO_RESPONSE("응답 없음"),
    EMERGENCY_KEYWORD("응급 키워드"),
    HEALTH_SCORE_DROP("건강 점수 하락"),
    CONVERSATION_PATTERN_CHANGE("대화 패턴 변화");

    private final String description;

    AlertConditionType(String description) {
        this.description = description;
    }
}

public enum SeverityLevel {
    LOW("낮음", 1),
    MEDIUM("보통", 2),
    HIGH("높음", 3),
    CRITICAL("긴급", 4);

    private final String description;
    private final int priority;

    SeverityLevel(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }
}

public enum NotificationChannel {
    EMAIL("이메일"),
    SMS("문자메시지"),
    PUSH("푸시알림"),
    WEBHOOK("웹훅");

    private final String description;

    NotificationChannel(String description) {
        this.description = description;
    }
}

public enum NotificationStatus {
    PENDING("대기"),
    SENT("발송완료"),
    DELIVERED("전달완료"),
    FAILED("발송실패"),
    CANCELLED("취소됨");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }
}
```

#### 핵심 서비스 설계
```java
// 보호자 관리 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final MemberService memberService;
    private final EncryptionService encryptionService; // 연락처 암호화

    /**
     * 보호자 등록
     */
    @Transactional
    public GuardianResponseDto registerGuardian(Long memberId, GuardianRegisterDto registerDto) {
        // 1. 회원 존재 확인
        memberService.validateMemberExists(memberId);

        // 2. 보호자 엔티티 생성
        GuardianEntity guardian = GuardianEntity.create(
            memberId,
            registerDto.getName(),
            registerDto.getRelationship()
        );

        // 3. 연락처 정보 암호화 저장
        if (registerDto.getPhoneNumber() != null) {
            guardian.setPhoneNumber(encryptionService.encrypt(registerDto.getPhoneNumber()));
        }
        if (registerDto.getEmail() != null) {
            guardian.setEmail(registerDto.getEmail());
        }

        // 4. 첫 번째 보호자면 주 보호자로 설정
        boolean hasExistingGuardians = guardianRepository.existsByMemberId(memberId);
        if (!hasExistingGuardians) {
            guardian.setPrimary();
        }

        GuardianEntity saved = guardianRepository.save(guardian);
        log.info("보호자 등록 완료: memberId={}, guardianId={}, name={}",
                memberId, saved.getId(), saved.getName());

        return GuardianMapper.toResponseDto(saved);
    }

    /**
     * 회원의 모든 보호자 조회
     */
    public List<GuardianResponseDto> getGuardiansByMemberId(Long memberId) {
        List<GuardianEntity> guardians = guardianRepository.findByMemberIdOrderByIsPrimaryDescCreatedAtAsc(memberId);
        return guardians.stream()
                .map(GuardianMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 주 보호자 변경
     */
    @Transactional
    public void changePrimaryGuardian(Long memberId, Long guardianId) {
        // 1. 기존 주 보호자 해제
        guardianRepository.findByMemberIdAndIsPrimaryTrue(memberId)
                .ifPresent(guardian -> {
                    guardian.setPrimary(false);
                    guardianRepository.save(guardian);
                });

        // 2. 새 주 보호자 설정
        GuardianEntity newPrimary = guardianRepository.findByIdAndMemberId(guardianId, memberId)
                .orElseThrow(() -> new GuardianNotFoundException(guardianId));

        newPrimary.setPrimary();
        guardianRepository.save(newPrimary);

        log.info("주 보호자 변경: memberId={}, newPrimaryGuardianId={}", memberId, guardianId);
    }
}

// 알림 발송 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AlertService {

    private final GuardianRepository guardianRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelService channelService;
    private final AlertRuleEngine alertRuleEngine;
    private final ConversationService conversationService; // Phase 1 연동

    /**
     * 이상징후 감지 및 알림 발송 (이벤트 기반)
     */
    @EventListener
    @Async
    @Transactional
    public void handleConversationAnalyzed(ConversationAnalyzedEvent event) {
        Long memberId = event.getMemberId();
        ConversationAnalysisResult analysis = event.getAnalysisResult();

        log.debug("대화 분석 결과 처리 시작: memberId={}, sentiment={}",
                 memberId, analysis.getSentiment());

        // 1. 활성화된 알림 규칙 조회
        List<AlertRuleEntity> activeRules = alertRuleRepository.findByIsActiveTrue();

        // 2. 각 규칙에 대해 조건 검사
        List<AlertRuleEntity> triggeredRules = activeRules.stream()
                .filter(rule -> alertRuleEngine.evaluate(rule, memberId, analysis))
                .collect(Collectors.toList());

        if (!triggeredRules.isEmpty()) {
            log.info("알림 규칙 트리거됨: memberId={}, triggeredRules={}",
                    memberId, triggeredRules.size());

            // 3. 트리거된 규칙들에 대해 알림 발송
            triggeredRules.forEach(rule -> sendAlert(memberId, rule, analysis));
        }
    }

    /**
     * 알림 발송
     */
    @Transactional
    public void sendAlert(Long memberId, AlertRuleEntity rule, ConversationAnalysisResult analysis) {
        // 1. 해당 회원의 보호자들 조회
        List<GuardianEntity> guardians = guardianRepository.findByMemberIdOrderByIsPrimaryDescCreatedAtAsc(memberId);

        if (guardians.isEmpty()) {
            log.warn("알림 발송할 보호자가 없음: memberId={}, ruleName={}", memberId, rule.getName());
            return;
        }

        // 2. 알림 내용 생성
        AlertContent alertContent = generateAlertContent(memberId, rule, analysis);

        // 3. 심각도에 따른 발송 대상 결정
        List<GuardianEntity> targetGuardians = selectTargetGuardians(guardians, rule.getSeverityLevel());

        // 4. 각 보호자에게 알림 발송
        targetGuardians.forEach(guardian -> {
            sendNotificationToGuardian(guardian, alertContent, rule);
        });
    }

    /**
     * 보호자별 알림 발송
     */
    private void sendNotificationToGuardian(GuardianEntity guardian, AlertContent content, AlertRuleEntity rule) {
        NotificationPreferences prefs = guardian.getNotificationPreferences();

        // 1. 조용한 시간 체크
        if (prefs.isQuietTime(LocalTime.now()) && rule.getSeverityLevel() != SeverityLevel.CRITICAL) {
            log.debug("조용한 시간이므로 알림 발송 연기: guardianId={}", guardian.getId());
            // 조용한 시간 종료 후 발송하도록 스케줄링 (추후 구현)
            return;
        }

        // 2. 활성화된 채널별로 알림 발송
        if (prefs.isEmailEnabled() && guardian.getEmail() != null) {
            sendEmailNotification(guardian, content, rule);
        }

        if (prefs.isSmsEnabled() && guardian.getPhoneNumber() != null) {
            sendSmsNotification(guardian, content, rule);
        }

        if (prefs.isPushEnabled()) {
            sendPushNotification(guardian, content, rule);
        }
    }

    /**
     * 이메일 알림 발송
     */
    private void sendEmailNotification(GuardianEntity guardian, AlertContent content, AlertRuleEntity rule) {
        try {
            NotificationEntity notification = NotificationEntity.create(
                guardian.getMemberId(),
                guardian.getId(),
                NotificationChannel.EMAIL,
                guardian.getEmail(),
                content.getEmailSubject(),
                content.getEmailBody()
            );

            NotificationEntity saved = notificationRepository.save(notification);

            // 실제 이메일 발송
            String externalId = channelService.sendEmail(
                guardian.getEmail(),
                content.getEmailSubject(),
                content.getEmailBody()
            );

            saved.markAsSent(externalId);
            notificationRepository.save(saved);

            log.info("이메일 알림 발송 성공: guardianId={}, notificationId={}",
                    guardian.getId(), saved.getId());

        } catch (Exception e) {
            log.error("이메일 알림 발송 실패: guardianId={}, error={}",
                     guardian.getId(), e.getMessage(), e);
        }
    }

    // SMS, 푸시 알림 메서드들도 유사하게 구현...
}

// 알림 규칙 엔진
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertRuleEngine {

    private final ConversationService conversationService;
    private final CheckMessageRepository checkMessageRepository;

    /**
     * 알림 규칙 평가
     */
    public boolean evaluate(AlertRuleEntity rule, Long memberId, ConversationAnalysisResult analysis) {
        return switch (rule.getConditionType()) {
            case EMOTION_NEGATIVE_STREAK -> evaluateNegativeStreak(rule, memberId, analysis);
            case NO_RESPONSE -> evaluateNoResponse(rule, memberId);
            case EMERGENCY_KEYWORD -> evaluateEmergencyKeyword(rule, analysis);
            case HEALTH_SCORE_DROP -> evaluateHealthScoreDrop(rule, memberId, analysis);
            case CONVERSATION_PATTERN_CHANGE -> evaluatePatternChange(rule, memberId, analysis);
        };
    }

    /**
     * 부정적 감정 연속 체크
     */
    private boolean evaluateNegativeStreak(AlertRuleEntity rule, Long memberId, ConversationAnalysisResult analysis) {
        // 규칙 설정 파싱
        NegativeStreakConfig config = parseConfig(rule.getConditionConfig(), NegativeStreakConfig.class);

        // 현재 대화가 부정적이 아니면 연속이 끊어짐
        if (analysis.getSentiment() != EmotionType.NEGATIVE) {
            return false;
        }

        // 최근 N일간의 대화 감정 분석
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(config.getConsecutiveDays() - 1);

        List<ConversationSummary> recentConversations = conversationService
            .getConversationsSummaryBetween(memberId, startDate, endDate);

        // 연속 부정적 감정 일수 계산
        long negativeConsecutiveDays = recentConversations.stream()
            .mapToLong(conv -> conv.getSentiment() == EmotionType.NEGATIVE ? 1 : 0)
            .sum();

        boolean triggered = negativeConsecutiveDays >= config.getConsecutiveDays();

        log.debug("부정적 감정 연속 평가: memberId={}, consecutiveDays={}, threshold={}, triggered={}",
                 memberId, negativeConsecutiveDays, config.getConsecutiveDays(), triggered);

        return triggered;
    }

    /**
     * 응답 없음 체크
     */
    private boolean evaluateNoResponse(AlertRuleEntity rule, Long memberId) {
        NoResponseConfig config = parseConfig(rule.getConditionConfig(), NoResponseConfig.class);

        // 최근 N일간 발송된 안부 메시지 중 응답 없는 것들 조회
        LocalDateTime since = LocalDateTime.now().minusDays(config.getConsecutiveDays());

        List<CheckMessageEntity> recentMessages = checkMessageRepository
            .findSentMessagesWithoutResponse(memberId, since);

        // 연속으로 응답하지 않은 일수 계산
        long noResponseDays = recentMessages.size();

        boolean triggered = noResponseDays >= config.getConsecutiveDays();

        log.debug("무응답 평가: memberId={}, noResponseDays={}, threshold={}, triggered={}",
                 memberId, noResponseDays, config.getConsecutiveDays(), triggered);

        return triggered;
    }

    /**
     * 응급 키워드 체크
     */
    private boolean evaluateEmergencyKeyword(AlertRuleEntity rule, ConversationAnalysisResult analysis) {
        EmergencyKeywordConfig config = parseConfig(rule.getConditionConfig(), EmergencyKeywordConfig.class);

        String messageContent = analysis.getMessageContent().toLowerCase();

        boolean hasEmergencyKeyword = config.getKeywords().stream()
            .anyMatch(keyword -> messageContent.contains(keyword.toLowerCase()));

        if (hasEmergencyKeyword) {
            log.warn("응급 키워드 감지: memberId={}, content={}", analysis.getMemberId(), messageContent);
        }

        return hasEmergencyKeyword;
    }

    // 기타 규칙 평가 메서드들...
}
```

---

## 📅 주차별 상세 TDD 구현 계획

### Week 5: Scheduling 도메인 기본 구현

#### Day 1-2: 🔴 Red Phase - 실패하는 테스트 작성
```java
// DailyCheckServiceTest.java
@ExtendWith(MockitoExtension.class)
@DisplayName("정기 안부 메시지 서비스 테스트")
class DailyCheckServiceTest {

    @Mock private DailyCheckRepository dailyCheckRepository;
    @Mock private CheckMessageRepository checkMessageRepository;
    @Mock private MemberService memberService;
    @Mock private PersonalizedMessageService messageService;
    @Mock private ConversationService conversationService;

    @InjectMocks private DailyCheckService dailyCheckService;

    @Test
    @DisplayName("오전 9시 스케줄 실행 시 오늘 발송 대상 사용자들에게 안부 메시지를 발송한다")
    void sendDailyCheckMessages_AtScheduledTime_SendsToTodayTargets() {
        // Given
        LocalDateTime now = LocalDateTime.of(2025, 9, 14, 9, 0, 0);
        int todayOfWeek = now.getDayOfWeek().getValue() % 7; // 일요일=0

        List<DailyCheckEntity> todayChecks = Arrays.asList(
            createDailyCheck(1L, LocalTime.of(9, 0), new Integer[]{0, 1, 2, 3, 4, 5, 6}),
            createDailyCheck(2L, LocalTime.of(9, 0), new Integer[]{0, 1, 2, 3, 4, 5, 6})
        );

        when(dailyCheckRepository.findActiveSchedulesForToday(todayOfWeek))
            .thenReturn(todayChecks);
        when(messageService.generatePersonalizedMessage(anyLong(), anyString()))
            .thenReturn("개인화된 안부 메시지");
        when(checkMessageRepository.save(any(CheckMessageEntity.class)))
            .thenAnswer(invocation -> {
                CheckMessageEntity entity = invocation.getArgument(0);
                entity.setId(1L); // Mock ID 설정
                return entity;
            });

        // When
        dailyCheckService.sendDailyCheckMessages();

        // Then
        verify(dailyCheckRepository).findActiveSchedulesForToday(todayOfWeek);
        verify(checkMessageRepository, times(2)).save(any(CheckMessageEntity.class));
        verify(conversationService, times(2)).processSystemMessage(anyLong(), anyString());
        verify(dailyCheckRepository, times(2)).save(any(DailyCheckEntity.class)); // lastSentAt 업데이트
    }

    @Test
    @DisplayName("발송 시간이 아직 되지 않은 경우 메시지를 발송하지 않는다")
    void sendDailyCheckMessages_BeforeScheduledTime_DoesNotSend() {
        // Given
        LocalDateTime now = LocalDateTime.of(2025, 9, 14, 8, 30, 0); // 8:30 AM
        int todayOfWeek = now.getDayOfWeek().getValue() % 7;

        List<DailyCheckEntity> todayChecks = Arrays.asList(
            createDailyCheck(1L, LocalTime.of(9, 0), new Integer[]{0, 1, 2, 3, 4, 5, 6})
        );

        when(dailyCheckRepository.findActiveSchedulesForToday(todayOfWeek))
            .thenReturn(todayChecks);

        // When
        dailyCheckService.sendDailyCheckMessages();

        // Then
        verify(checkMessageRepository, never()).save(any(CheckMessageEntity.class));
        verify(conversationService, never()).processSystemMessage(anyLong(), anyString());
    }

    @Test
    @DisplayName("이미 오늘 발송한 메시지는 중복 발송하지 않는다")
    void sendDailyCheckMessages_AlreadySentToday_DoesNotDuplicate() {
        // Given
        LocalDateTime now = LocalDateTime.of(2025, 9, 14, 9, 30, 0);
        LocalDateTime todayMorning = LocalDateTime.of(2025, 9, 14, 9, 5, 0);

        DailyCheckEntity alreadySent = createDailyCheck(1L, LocalTime.of(9, 0),
                                                       new Integer[]{0, 1, 2, 3, 4, 5, 6});
        alreadySent.setLastSentAt(todayMorning); // 이미 오늘 발송함

        when(dailyCheckRepository.findActiveSchedulesForToday(anyInt()))
            .thenReturn(Arrays.asList(alreadySent));

        // When
        dailyCheckService.sendDailyCheckMessages();

        // Then
        verify(checkMessageRepository, never()).save(any(CheckMessageEntity.class));
    }

    private DailyCheckEntity createDailyCheck(Long memberId, LocalTime scheduledTime, Integer[] days) {
        return DailyCheckEntity.builder()
            .id(memberId)
            .memberId(memberId)
            .scheduledTime(scheduledTime)
            .scheduledDays(days)
            .messageTemplate("안녕하세요! 오늘 하루는 어떻게 지내고 계세요?")
            .isActive(true)
            .build();
    }
}

// PersonalizedMessageServiceTest.java
@ExtendWith(MockitoExtension.class)
@DisplayName("개인화 메시지 서비스 테스트")
class PersonalizedMessageServiceTest {

    @Mock private MemberService memberService;
    @Mock private ConversationService conversationService;
    @Mock private MessageTemplateService templateService;

    @InjectMocks private PersonalizedMessageService messageService;

    @Test
    @DisplayName("회원 정보와 최근 대화 맥락을 기반으로 개인화된 메시지를 생성한다")
    void generatePersonalizedMessage_WithMemberContextAndHistory_GeneratesPersonalizedMessage() {
        // Given
        Long memberId = 1L;
        String template = "안녕하세요 {name}님! 오늘은 어떻게 지내세요?";

        MemberEntity member = createMember(memberId, "김할머니");
        RecentConversationContext context = createConversationContext();

        when(memberService.findById(memberId)).thenReturn(member);
        when(conversationService.getRecentConversationContext(memberId, 7)).thenReturn(context);
        when(templateService.personalizeMessage(template, member, context))
            .thenReturn("안녕하세요 김할머니님! 어제 산책 이야기를 들려주셨는데, 오늘도 좋은 하루 보내고 계신가요?");

        // When
        String result = messageService.generatePersonalizedMessage(memberId, template);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("김할머니님");
        assertThat(result).contains("산책");
        verify(memberService).findById(memberId);
        verify(conversationService).getRecentConversationContext(memberId, 7);
        verify(templateService).personalizeMessage(template, member, context);
    }

    @Test
    @DisplayName("개인화 메시지 생성 실패 시 기본 템플릿을 반환한다")
    void generatePersonalizedMessage_WhenPersonalizationFails_ReturnsDefaultTemplate() {
        // Given
        Long memberId = 1L;
        String template = "기본 안부 메시지";

        when(memberService.findById(memberId)).thenThrow(new MemberNotFoundException(memberId));

        // When
        String result = messageService.generatePersonalizedMessage(memberId, template);

        // Then
        assertThat(result).isEqualTo(template);
    }
}
```

#### Day 3-4: 🟢 Green Phase - 테스트 통과 최소 구현
- DailyCheckService 기본 구현
- Spring @Scheduled 설정
- DailyCheck, CheckMessage 엔티티 구현
- Repository 인터페이스 구현

#### Day 5-6: 🔵 Refactor Phase - 코드 품질 개선
- 스케줄링 성능 최적화 (병렬 처리)
- 예외 처리 강화
- 로깅 시스템 추가
- 설정 외부화

### Week 6: Scheduling 도메인 고도화

#### Day 8-9: 🔴 Red Phase - 고급 기능 테스트
```java
@Test
@DisplayName("실패한 메시지들을 자동으로 재시도한다")
void retryFailedMessages_WithFailedMessages_RetriesAutomatically() {
    // Given
    CheckMessageEntity failedMessage1 = createFailedMessage(1L, 1);
    CheckMessageEntity failedMessage2 = createFailedMessage(2L, 2);

    when(checkMessageRepository.findRetryableMessages())
        .thenReturn(Arrays.asList(failedMessage1, failedMessage2));
    when(dailyCheckRepository.findById(anyLong()))
        .thenReturn(Optional.of(createDailyCheck()));

    // When
    dailyCheckService.retryFailedMessages();

    // Then
    verify(conversationService, times(2)).processSystemMessage(anyLong(), anyString());
    verify(checkMessageRepository, times(2)).save(any(CheckMessageEntity.class));
}

@Test
@DisplayName("최대 재시도 횟수를 초과한 메시지는 재시도하지 않는다")
void retryFailedMessages_ExceedsMaxRetries_DoesNotRetry() {
    // Given
    CheckMessageEntity maxRetriedMessage = createFailedMessage(1L, 3); // 이미 3번 재시도

    when(checkMessageRepository.findRetryableMessages())
        .thenReturn(Arrays.asList(maxRetriedMessage));

    // When
    dailyCheckService.retryFailedMessages();

    // Then
    verify(conversationService, never()).processSystemMessage(anyLong(), anyString());
}
```

#### Day 10-11: 🟢 Green Phase - 고급 기능 구현
- 재시도 메커니즘 구현
- 배치 처리 최적화
- 동적 스케줄 설정 관리

#### Day 12: 🔵 Refactor Phase - 최종 완성
- 메모리 사용량 최적화
- 대량 처리 성능 개선
- 모니터링 지표 추가

### Week 7: Notification 도메인 기본 구현

#### Day 13-14: 🔴 Red Phase - 보호자 관리 테스트
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("보호자 관리 서비스 테스트")
class GuardianServiceTest {

    @Mock private GuardianRepository guardianRepository;
    @Mock private MemberService memberService;
    @Mock private EncryptionService encryptionService;

    @InjectMocks private GuardianService guardianService;

    @Test
    @DisplayName("새 보호자를 등록할 때 첫 번째 보호자는 자동으로 주 보호자가 된다")
    void registerGuardian_FirstGuardian_BecomsPrimary() {
        // Given
        Long memberId = 1L;
        GuardianRegisterDto registerDto = GuardianRegisterDto.builder()
            .name("김자녀")
            .relationship("자녀")
            .phoneNumber("010-1234-5678")
            .email("child@example.com")
            .build();

        when(guardianRepository.existsByMemberId(memberId)).thenReturn(false); // 첫 번째 보호자
        when(encryptionService.encrypt("010-1234-5678")).thenReturn("encrypted_phone");
        when(guardianRepository.save(any(GuardianEntity.class)))
            .thenAnswer(invocation -> {
                GuardianEntity entity = invocation.getArgument(0);
                entity.setId(1L);
                return entity;
            });

        // When
        GuardianResponseDto result = guardianService.registerGuardian(memberId, registerDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isTrue();
        verify(guardianRepository).save(argThat(guardian -> guardian.getIsPrimary()));
        verify(encryptionService).encrypt("010-1234-5678");
    }

    @Test
    @DisplayName("두 번째 이후 보호자는 일반 보호자로 등록된다")
    void registerGuardian_SubsequentGuardian_IsNotPrimary() {
        // Given
        Long memberId = 1L;
        GuardianRegisterDto registerDto = GuardianRegisterDto.builder()
            .name("이배우자")
            .relationship("배우자")
            .build();

        when(guardianRepository.existsByMemberId(memberId)).thenReturn(true); // 이미 보호자 존재
        when(guardianRepository.save(any(GuardianEntity.class)))
            .thenAnswer(invocation -> {
                GuardianEntity entity = invocation.getArgument(0);
                entity.setId(2L);
                return entity;
            });

        // When
        GuardianResponseDto result = guardianService.registerGuardian(memberId, registerDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isFalse();
        verify(guardianRepository).save(argThat(guardian -> !guardian.getIsPrimary()));
    }
}
```

#### Day 15-16: 🟢 Green Phase - 보호자 시스템 구현
- GuardianService 구현
- 연락처 암호화 시스템
- 주 보호자 관리 로직

### Week 8: Notification 도메인 알림 시스템

#### Day 17-18: 🔴 Red Phase - 알림 발송 테스트
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("알림 발송 서비스 테스트")
class AlertServiceTest {

    @Test
    @DisplayName("부정적 감정이 3일 연속 감지되면 보호자에게 알림을 발송한다")
    void handleConversationAnalyzed_NegativeEmotionStreak_SendsAlert() {
        // Given
        Long memberId = 1L;
        ConversationAnalyzedEvent event = createNegativeEmotionEvent(memberId);

        AlertRuleEntity negativeStreakRule = createNegativeStreakRule(3); // 3일 연속
        GuardianEntity primaryGuardian = createGuardian(memberId, true);

        when(alertRuleRepository.findByIsActiveTrue())
            .thenReturn(Arrays.asList(negativeStreakRule));
        when(alertRuleEngine.evaluate(negativeStreakRule, memberId, event.getAnalysisResult()))
            .thenReturn(true); // 조건 충족
        when(guardianRepository.findByMemberIdOrderByIsPrimaryDescCreatedAtAsc(memberId))
            .thenReturn(Arrays.asList(primaryGuardian));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> {
                NotificationEntity entity = invocation.getArgument(0);
                entity.setId(1L);
                return entity;
            });
        when(channelService.sendEmail(anyString(), anyString(), anyString()))
            .thenReturn("email_id_123");

        // When
        alertService.handleConversationAnalyzed(event);

        // Then
        verify(alertRuleEngine).evaluate(negativeStreakRule, memberId, event.getAnalysisResult());
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(channelService).sendEmail(eq(primaryGuardian.getEmail()), anyString(), anyString());
    }

    @Test
    @DisplayName("응급 키워드가 감지되면 모든 보호자에게 즉시 알림을 발송한다")
    void handleConversationAnalyzed_EmergencyKeyword_SendsImmediateAlert() {
        // Given
        Long memberId = 1L;
        ConversationAnalyzedEvent event = createEmergencyKeywordEvent(memberId, "아파서 도와줘");

        AlertRuleEntity emergencyRule = createEmergencyKeywordRule();
        List<GuardianEntity> allGuardians = Arrays.asList(
            createGuardian(memberId, true),   // 주 보호자
            createGuardian(memberId, false)   // 일반 보호자
        );

        when(alertRuleRepository.findByIsActiveTrue())
            .thenReturn(Arrays.asList(emergencyRule));
        when(alertRuleEngine.evaluate(emergencyRule, memberId, event.getAnalysisResult()))
            .thenReturn(true);
        when(guardianRepository.findByMemberIdOrderByIsPrimaryDescCreatedAtAsc(memberId))
            .thenReturn(allGuardians);
        when(channelService.sendEmail(anyString(), anyString(), anyString()))
            .thenReturn("email_id_456");

        // When
        alertService.handleConversationAnalyzed(event);

        // Then
        // 모든 보호자에게 알림 발송 확인 (응급상황이므로)
        verify(channelService, times(2)).sendEmail(anyString(), anyString(), anyString());
        verify(notificationRepository, times(2)).save(any(NotificationEntity.class));
    }

    @Test
    @DisplayName("조용한 시간에는 긴급 알림이 아닌 경우 발송하지 않는다")
    void sendNotificationToGuardian_DuringQuietHours_SkipsNonCriticalAlerts() {
        // Given
        GuardianEntity guardian = createGuardianWithQuietHours(22, 7); // 22시~7시 조용한 시간
        AlertContent content = createAlertContent();
        AlertRuleEntity mediumRule = createAlertRule(SeverityLevel.MEDIUM);

        // 현재 시간을 자정(조용한 시간)으로 설정
        try (MockedStatic<LocalTime> mockedLocalTime = mockStatic(LocalTime.class)) {
            mockedLocalTime.when(LocalTime::now).thenReturn(LocalTime.of(0, 0));

            // When
            alertService.sendNotificationToGuardian(guardian, content, mediumRule);

            // Then
            verify(channelService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }
}
```

#### Day 19-20: 🟢 Green Phase - 알림 시스템 구현
- AlertService 구현
- 알림 규칙 엔진 구현
- 이메일 발송 시스템 기본 구현

#### Day 21: 🔵 Refactor Phase - 알림 시스템 완성
- 다채널 알림 지원 (이메일, SMS, 푸시)
- 알림 중복 방지 로직
- 성능 최적화 및 비동기 처리

---

## 🧪 테스트 전략 및 커버리지

### TDD 품질 기준
```yaml
Domain Service 테스트:
  - DailyCheckService: 95% 커버리지
  - GuardianService: 95% 커버리지
  - AlertService: 95% 커버리지
  - PersonalizedMessageService: 90% 커버리지

Repository 테스트:
  - 각 Repository별 90% 커버리지
  - 복잡한 쿼리 메서드 100% 테스트

Controller 테스트:
  - API 엔드포인트별 85% 커버리지
  - 인증, 권한, 유효성 검사 포함

Integration 테스트:
  - 스케줄링 전체 플로우 테스트
  - 알림 발송 전체 플로우 테스트
  - Phase 1 AI 시스템 연동 테스트
```

### Mock 전략
```yaml
외부 시스템 Mock:
  - 이메일 발송 서비스 (EmailService)
  - SMS 발송 서비스 (SmsService)
  - 푸시 알림 서비스 (PushService)

내부 시스템 Mock 최소화:
  - Phase 1 ConversationService는 실제 객체 사용
  - MemberService는 실제 객체 사용
  - Repository는 TestContainer 사용
```

---

## 📊 데이터베이스 스키마 (Phase 2)

### 테이블 생성 SQL
```sql
-- 일일 체크 설정
CREATE TABLE daily_checks (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    scheduled_time TIME NOT NULL DEFAULT '09:00:00',
    scheduled_days INTEGER[] NOT NULL DEFAULT ARRAY[0,1,2,3,4,5,6],
    message_template TEXT DEFAULT '안녕하세요! 오늘 하루는 어떻게 지내고 계세요?',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_member_daily_check UNIQUE(member_id)
);

-- 안부 메시지 발송 이력
CREATE TABLE check_messages (
    id BIGSERIAL PRIMARY KEY,
    daily_check_id BIGINT NOT NULL REFERENCES daily_checks(id),
    member_id BIGINT NOT NULL REFERENCES members(id),
    content TEXT NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_status_values CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'))
);

-- 보호자 정보
CREATE TABLE guardians (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50),
    phone_number VARCHAR(200), -- 암호화된 전화번호
    email VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    notification_preferences JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 알림 규칙
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    condition_type VARCHAR(50) NOT NULL,
    condition_config JSONB NOT NULL,
    severity_level VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT condition_type_values CHECK (condition_type IN (
        'EMOTION_NEGATIVE_STREAK', 'NO_RESPONSE', 'EMERGENCY_KEYWORD',
        'HEALTH_SCORE_DROP', 'CONVERSATION_PATTERN_CHANGE'
    )),
    CONSTRAINT severity_level_values CHECK (severity_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- 알림 발송 이력
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    guardian_id BIGINT NOT NULL REFERENCES guardians(id),
    alert_rule_id BIGINT REFERENCES alert_rules(id),
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    external_id VARCHAR(100), -- 외부 서비스 메시지 ID
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT notification_channel_values CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WEBHOOK')),
    CONSTRAINT notification_status_values CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED'))
);

-- 스케줄링 설정
CREATE TABLE schedule_configs (
    id BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(50) NOT NULL UNIQUE,
    cron_expression VARCHAR(100) NOT NULL,
    config_values JSONB,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_daily_checks_member_id ON daily_checks(member_id);
CREATE INDEX idx_daily_checks_active ON daily_checks(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_daily_checks_schedule ON daily_checks(scheduled_time, scheduled_days);

CREATE INDEX idx_check_messages_daily_check_id ON check_messages(daily_check_id);
CREATE INDEX idx_check_messages_member_id ON check_messages(member_id);
CREATE INDEX idx_check_messages_status ON check_messages(status);
CREATE INDEX idx_check_messages_scheduled_at ON check_messages(scheduled_at);

CREATE INDEX idx_guardians_member_id ON guardians(member_id);
CREATE INDEX idx_guardians_primary ON guardians(member_id, is_primary);

CREATE INDEX idx_notifications_member_id ON notifications(member_id);
CREATE INDEX idx_notifications_guardian_id ON notifications(guardian_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_channel ON notifications(channel);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- 기본 데이터 삽입
INSERT INTO alert_rules (name, description, condition_type, condition_config, severity_level) VALUES
('부정적 감정 3일 연속', '3일 연속 부정적 감정이 감지되면 알림', 'EMOTION_NEGATIVE_STREAK', '{"consecutiveDays":3,"emotionThreshold":"NEGATIVE"}', 'MEDIUM'),
('안부 메시지 무응답', '안부 메시지에 2일 연속 응답하지 않으면 알림', 'NO_RESPONSE', '{"consecutiveDays":2}', 'HIGH'),
('응급 키워드 감지', '응급 상황 키워드 감지 시 즉시 알림', 'EMERGENCY_KEYWORD', '{"keywords":["아파","아픈","병원","응급","도와줘","119","구급차"]}', 'CRITICAL');

INSERT INTO schedule_configs (config_name, cron_expression, config_values) VALUES
('DAILY_CHECK_BATCH', '0 0 9 * * *', '{"batchSize":50,"maxRetries":3,"timeoutSeconds":30}'),
('RETRY_FAILED_MESSAGES', '0 */5 * * * *', '{"maxRetries":3,"retryDelayMinutes":5}'),
('CLEANUP_OLD_MESSAGES', '0 0 2 * * *', '{"retentionDays":90}');
```

---

## ⚙️ 환경 설정 및 Configuration

### application.yml 추가 설정
```yaml
# Phase 2 스케줄링 & 알림 설정
maruni:
  scheduling:
    daily-check:
      cron: "0 0 9 * * *" # 매일 오전 9시
      batch-size: 50
      timeout-seconds: 30
    retry:
      cron: "0 */5 * * * *" # 5분마다
      max-retries: 3
      delay-minutes: 5

  notification:
    email:
      enabled: true
      from-address: "noreply@maruni.co.kr"
      from-name: "마루니"
      smtp:
        host: "smtp.gmail.com"
        port: 587
        username: "${SMTP_USERNAME}"
        password: "${SMTP_PASSWORD}"
        starttls: true
    sms:
      enabled: false # Phase 2에서는 비활성화
      provider: "coolsms"
    push:
      enabled: false # Phase 2에서는 비활성화
      provider: "firebase"

  encryption:
    algorithm: "AES/GCM/NoPadding"
    key: "${ENCRYPTION_KEY}" # 32 바이트 키
```

### 환경 변수 (.env 추가)
```bash
# Phase 2 스케줄링 & 알림 시스템
SMTP_USERNAME=your_smtp_username
SMTP_PASSWORD=your_smtp_password
ENCRYPTION_KEY=your_32_byte_encryption_key_here

# 알림 설정
EMAIL_FROM_ADDRESS=noreply@maruni.co.kr
EMAIL_FROM_NAME=마루니

# 스케줄링 설정
DAILY_CHECK_BATCH_SIZE=50
NOTIFICATION_RETRY_MAX=3
```

---

## 📈 Phase 2 완료 후 달성 지표

### 기능적 완성도
- [ ] 정기 안부 메시지 자동 발송 (매일 오전 9시)
- [ ] 개인화된 메시지 생성 (회원별 맞춤)
- [ ] 보호자 등록 및 관리 시스템
- [ ] 이상징후 자동 감지 및 알림
- [ ] 다채널 알림 발송 (이메일 위주)
- [ ] 발송 실패 자동 재시도
- [ ] 알림 이력 완전 추적

### 기술적 지표
- [ ] 스케줄링 도메인 테스트 커버리지 90% 이상
- [ ] 알림 도메인 테스트 커버리지 90% 이상
- [ ] API 응답시간 평균 200ms 이내
- [ ] 일일 1000건 메시지 처리 가능
- [ ] 알림 발송 성공률 95% 이상

### 비즈니스 가치
- [ ] 실제 노인 돌봄 서비스 워크플로우 80% 완성
- [ ] 보호자가 안심할 수 있는 모니터링 시스템
- [ ] 이상징후 조기 감지 및 대응 체계
- [ ] 개인화된 케어 서비스 제공

---

## 🚀 Phase 3 연계 준비사항

### Phase 3에서 활용할 Phase 2 성과물
- **정기 메시지 시스템**: 건강 상태 분석 데이터 수집 기반
- **보호자 알림 시스템**: 건강 위험도 기반 알림 업그레이드
- **알림 이력 데이터**: 알림 효과성 분석 및 개선
- **개인화 메시지**: AI 기반 고도화된 개인화 시스템

### 확장 가능한 설계 포인트
- **알림 채널 확장**: 이메일 → SMS, 푸시, 웹훅 추가
- **알림 규칙 고도화**: 단순 키워드 → ML 기반 패턴 감지
- **개인화 진화**: 템플릿 기반 → AI 생성 메시지
- **모니터링 강화**: 기본 통계 → 고급 분석 대시보드

---

**문서 작성일**: 2025-09-14
**최종 수정일**: 2025-09-14
**작성자**: Claude Code
**버전**: v1.0 (Phase 2 Detailed Plan)
**개발 방법론**: Test-Driven Development (TDD)
**예상 개발 기간**: 4주 (Week 5-8)

---

**🎯 다음 실행 단계: Week 5 Day 1 - DailyCheckService TDD Red 테스트 작성 시작!**