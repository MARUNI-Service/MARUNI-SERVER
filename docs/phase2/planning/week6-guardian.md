# Week 6: Guardian 도메인 TDD 개발 계획

## 🎯 Week 6 목표

**Guardian(보호자) 도메인 완전 구현**을 통해 MARUNI의 핵심 비즈니스 로직인 **이상징후 감지 시 보호자 알림** 시스템을 완성합니다.

### 📋 주요 구현 사항
- Guardian 엔티티 및 Member와의 관계 설정
- 보호자 등록/수정/삭제/조회 CRUD 시스템
- 보호자별 알림 설정 관리 (알림 방법, 시간, 긴급도)
- Guardian-Member 다대다 관계 시스템
- 완전한 TDD 적용 (Red-Green-Refactor)

## 📅 Week 6 일정 계획

### 🔴 **Day 1-2: Red 단계** - 실패하는 테스트 작성
#### Day 1 (09-16 월): Guardian 엔티티 TDD Red
- [ ] Guardian 엔티티 기본 구조 및 실패 테스트 작성
- [ ] Guardian-Member 관계 매핑 실패 테스트
- [ ] GuardianRepository 기본 CRUD 실패 테스트

#### Day 2 (09-17 화): Guardian Service TDD Red
- [ ] GuardianService 핵심 비즈니스 로직 실패 테스트
- [ ] 보호자 등록/수정/삭제 시나리오 실패 테스트
- [ ] 알림 설정 관리 실패 테스트

### 🟢 **Day 3-4: Green 단계** - 최소 구현으로 테스트 통과
#### Day 3 (09-18 수): Guardian 엔티티 및 Repository 구현
- [ ] Guardian, GuardianMemberMapping 엔티티 최소 구현
- [ ] GuardianRepository, GuardianMemberMappingRepository 구현
- [ ] JPA 관계 매핑 및 기본 쿼리 구현

#### Day 4 (09-19 목): Guardian Service 구현
- [ ] GuardianService 핵심 비즈니스 로직 구현
- [ ] CRUD 기능 최소 구현으로 테스트 통과
- [ ] 모든 테스트 통과 확인

### 🔵 **Day 5-6: Refactor 단계** - 코드 품질 향상
#### Day 5 (09-20 금): 체계적 리팩토링
- [ ] 하드코딩 제거 및 상수화
- [ ] 중복 로직 추출 및 메서드 분리
- [ ] 예외 처리 및 검증 로직 개선

#### Day 6 (09-21 토): Controller 및 최종 통합
- [ ] GuardianController REST API 구현
- [ ] DTO 계층 구현 및 Bean Validation 적용
- [ ] API 문서화 및 통합 테스트

## 🏗️ Guardian 도메인 아키텍처 설계

### DDD 구조
```
com.anyang.maruni.domain.guardian/
├── application/                 # Application Layer
│   ├── dto/                    # Request/Response DTO
│   │   ├── GuardianRequestDto.java
│   │   ├── GuardianResponseDto.java
│   │   └── GuardianMemberMappingDto.java
│   └── service/                # Application Service
│       └── GuardianService.java
├── domain/                     # Domain Layer
│   ├── entity/                 # Domain Entity
│   │   ├── GuardianEntity.java
│   │   └── GuardianMemberMapping.java
│   └── repository/             # Repository Interface
│       ├── GuardianRepository.java
│       └── GuardianMemberMappingRepository.java
└── presentation/               # Presentation Layer
    └── controller/             # REST API Controller
        └── GuardianController.java
```

### 핵심 엔티티 설계

#### GuardianEntity
```java
@Entity
@Table(name = "guardian")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuardianEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String guardianName;

    @Column(nullable = false, unique = true, length = 150)
    private String guardianEmail;

    @Column(length = 20)
    private String guardianPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuardianRelation relation; // FAMILY, FRIEND, CAREGIVER 등

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPreference notificationPreference; // PUSH, EMAIL, SMS, ALL

    @Column(nullable = false)
    private Boolean isActive = true;

    // 정적 팩토리 메서드
    public static GuardianEntity createGuardian(
        String name, String email, String phone,
        GuardianRelation relation, NotificationPreference preference) {
        return GuardianEntity.builder()
            .guardianName(name)
            .guardianEmail(email)
            .guardianPhone(phone)
            .relation(relation)
            .notificationPreference(preference)
            .isActive(true)
            .build();
    }

    // 비즈니스 로직 메서드
    public void updateNotificationPreference(NotificationPreference preference) {
        this.notificationPreference = preference;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
```

#### GuardianMemberMapping (다대다 관계)
```java
@Entity
@Table(name = "guardian_member_mapping")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuardianMemberMapping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianEntity guardian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel; // LOW, MEDIUM, HIGH, EMERGENCY

    @Column(nullable = false)
    private Boolean isActive = true;

    // 정적 팩토리 메서드
    public static GuardianMemberMapping createMapping(
        GuardianEntity guardian, MemberEntity member, AlertLevel alertLevel) {
        return GuardianMemberMapping.builder()
            .guardian(guardian)
            .member(member)
            .alertLevel(alertLevel)
            .isActive(true)
            .build();
    }

    // 비즈니스 로직
    public void updateAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }
}
```

### Enum 정의
```java
public enum GuardianRelation {
    FAMILY("가족"),
    FRIEND("친구"),
    CAREGIVER("돌봄제공자"),
    NEIGHBOR("이웃"),
    OTHER("기타");
}

public enum AlertLevel {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    EMERGENCY("긴급");
}

public enum NotificationPreference {
    PUSH("푸시알림"),
    EMAIL("이메일"),
    SMS("SMS"),
    PUSH_EMAIL("푸시+이메일"),
    ALL("모든알림");
}
```

## 🧪 TDD 테스트 시나리오 (총 8개)

### Guardian Entity & Repository 테스트 (4개)
1. **Guardian 생성 테스트**: `createGuardian_shouldCreateValidGuardian`
2. **Guardian-Member 매핑 테스트**: `createGuardianMemberMapping_shouldCreateValidMapping`
3. **활성 Guardian 조회 테스트**: `findActiveGuardiansByMemberId_shouldReturnActiveGuardians`
4. **Guardian 비활성화 테스트**: `deactivateGuardian_shouldSetInactive`

### Guardian Service 테스트 (4개)
5. **보호자 등록 테스트**: `registerGuardian_shouldRegisterNewGuardian`
6. **회원별 보호자 조회 테스트**: `getGuardiansByMemberId_shouldReturnMemberGuardians`
7. **보호자 알림 설정 수정 테스트**: `updateNotificationPreference_shouldUpdatePreference`
8. **보호자-회원 매핑 생성 테스트**: `createGuardianMemberMapping_shouldCreateMapping`

## 🔗 다른 도메인과의 연동 지점

### Phase 3에서 AlertRule과 연동
```java
// AlertRule에서 이상징후 감지 시 Guardian에게 알림 발송
public interface GuardianNotificationService {
    void notifyGuardians(Long memberId, AlertLevel alertLevel, String alertMessage);
    List<GuardianEntity> getGuardiansByMemberAndAlertLevel(Long memberId, AlertLevel alertLevel);
}
```

### NotificationService와 연동
```java
// Guardian의 NotificationPreference에 따른 다중 채널 알림
public void sendGuardianAlert(GuardianEntity guardian, String alertMessage) {
    switch (guardian.getNotificationPreference()) {
        case PUSH -> notificationService.sendPushNotification(guardian.getId(), alertMessage);
        case EMAIL -> notificationService.sendEmail(guardian.getGuardianEmail(), alertMessage);
        case ALL -> sendMultiChannelAlert(guardian, alertMessage);
    }
}
```

## 📊 Week 6 완료 시 달성 목표

```yaml
✅ Guardian 도메인: 100% TDD 구현
✅ 테스트 커버리지: 100% (8개 시나리오)
✅ CRUD API: 완전한 REST API 제공
✅ 관계 매핑: Member-Guardian 다대다 관계 완성
✅ 알림 설정: 세분화된 알림 채널 및 레벨 관리
✅ Phase 2 진행률: 40% → 70%로 향상

비즈니스 가치:
- 보호자 시스템 완성으로 실제 서비스 운영 가능
- 다양한 알림 채널로 상황별 적절한 알림 제공
- 세분화된 Alert Level로 긴급도별 차등 대응
```

**Week 6 Guardian 도메인 완성 후, MARUNI는 실제 노인 돌봄 서비스로서의 핵심 기능을 모두 갖춘 상태가 됩니다!** 🚀