# 코딩 표준

**MARUNI 프로젝트 Java 코딩 표준 및 네이밍 규칙**

---

## 📝 클래스 및 파일 명명 규칙

### **도메인별 클래스 네이밍**
```java
// Entity: {Domain}Entity
MemberEntity, ConversationEntity, AlertRuleEntity

// Service: {Domain}Service
MemberService, ConversationService, AlertRuleService

// Controller: {Domain}Controller 또는 {Domain}ApiController
UserApiController, ConversationController

// DTO: {Domain}{Action}Request/Response[Dto]
MemberSaveRequest, MemberResponse
ConversationRequestDto, AlertRuleResponseDto

// Repository: {Domain}Repository
MemberRepository, ConversationRepository

// Exception: {Domain}{Reason}Exception
MemberNotFoundException, GuardianNotFoundException
```

### **패키지 네이밍 규칙**
```
도메인 패키지: 단수형, 소문자
✅ com.anyang.maruni.domain.member
✅ com.anyang.maruni.domain.conversation
❌ com.anyang.maruni.domain.members
❌ com.anyang.maruni.domain.Conversation
```

---

## 🏷️ 어노테이션 순서 및 사용

### **Entity 클래스 어노테이션 순서**
```java
@Entity
@Table(name = "member_table", indexes = {
    @Index(name = "idx_member_email", columnList = "memberEmail")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEntity extends BaseTimeEntity {
    // 구현
}
```

### **Service 클래스 어노테이션 순서**
```java
@Slf4j              // 로깅 (최상단)
@Service            // 스프링 어노테이션
@RequiredArgsConstructor  // Lombok
@Transactional(readOnly = true)  // 기본 읽기 전용
public class MemberService {

    @Transactional  // 쓰기 작업에만 명시
    public void save(MemberSaveRequest req) {
        // 구현
    }
}
```

### **Controller 클래스 어노테이션 순서**
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse        // 자동 응답 래핑
@Tag(name = "회원 관리 API", description = "사용자 CRUD API")
public class UserApiController {
    // 구현
}
```

---

## 📦 Import 및 패키지 순서

### **Import 순서 규칙**
```java
package com.anyang.maruni.domain.member.domain.entity;

// 1. Java 표준 라이브러리 (java.*, javax.*)
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 2. Spring Framework (org.springframework.*)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 3. 외부 라이브러리 (lombok, jakarta 등)
import lombok.RequiredArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

// 4. 프로젝트 내부 클래스 (com.anyang.maruni.*)
import com.anyang.maruni.global.entity.BaseTimeEntity;
import com.anyang.maruni.global.exception.BaseException;
```

---

## 🏗️ 필드 및 메서드 구성 순서

### **Entity 클래스 구성 순서**
```java
public class MemberEntity extends BaseTimeEntity {
    // 1. Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. 기본 필드 (필수 → 선택적 순서)
    @Column(nullable = false)
    private String memberEmail;

    @Column
    private String memberName;

    // 3. Enum 필드
    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    // 4. 연관 관계 필드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private GuardianEntity guardian;

    // 5. 정적 팩토리 메서드
    public static MemberEntity createRegularMember(String email, String name) {
        return MemberEntity.builder()
            .memberEmail(email)
            .memberName(name)
            .build();
    }

    // 6. 비즈니스 로직 메서드
    public void updateMemberInfo(String name) {
        this.memberName = name;
    }
}
```

### **Service 클래스 구성 순서**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    // 1. 의존성 주입 필드
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 2. 공개 메서드 (Public Methods)
    @Transactional
    public void save(MemberSaveRequest req) {
        // 구현
    }

    public MemberResponse findById(Long id) {
        // 구현
    }

    // 3. 비공개 메서드 (Private Methods)
    private void validateEmailDuplication(String email) {
        // 구현
    }

    private BaseException memberNotFound() {
        return new BaseException(ErrorCode.MEMBER_NOT_FOUND);
    }
}
```

---

## ✅ 필수 적용 패턴

### **Entity 패턴**
```java
// ✅ 필수 적용
- BaseTimeEntity 상속
- 정적 팩토리 메서드 사용
- 상태 변경은 메서드를 통해서만
- @Builder 패턴 적용

// ❌ 금지
- public setter 사용
- 기본 생성자로 객체 생성
- 필드 직접 수정
```

### **Service 패턴**
```java
// ✅ 필수 적용
- @Transactional(readOnly = true) 기본 설정
- 쓰기 작업에만 @Transactional 명시
- BaseException 상속 예외 사용
- 비즈니스 로직은 private 메서드로 분리

// ❌ 금지
- RuntimeException 직접 throw
- 트랜잭션 어노테이션 생략
- Repository 직접 반환 (DTO 변환 필수)
```

### **Controller 패턴**
```java
// ✅ 필수 적용
- @AutoApiResponse 자동 응답 래핑
- @Valid Bean Validation 적용
- Swagger 어노테이션 완전 적용
- RESTful URL 패턴 준수

// ❌ 금지
- 수동 응답 래핑
- 비즈니스 로직 포함
- 예외 직접 처리 (GlobalExceptionHandler 위임)
```

### **DTO 패턴**
```java
// ✅ 필수 적용
- Bean Validation 어노테이션
- Swagger @Schema 어노테이션
- 정적 from() 변환 메서드
- Immutable 객체 지향

// ❌ 금지
- 검증 로직 누락
- Entity 직접 반환
- public setter 남발
```

---

## 🎯 체크리스트

### **새 Entity 생성 시**
- [ ] BaseTimeEntity 상속
- [ ] @Table name 및 indexes 정의
- [ ] 정적 팩토리 메서드 생성
- [ ] 비즈니스 메서드 구현
- [ ] public setter 제거

### **새 Service 생성 시**
- [ ] @Transactional(readOnly = true) 기본 설정
- [ ] 쓰기 작업에 @Transactional 명시
- [ ] BaseException 활용
- [ ] private 유틸리티 메서드 분리
- [ ] DTO 변환 로직 포함

### **새 Controller 생성 시**
- [ ] @AutoApiResponse 적용
- [ ] @Valid 검증 적용
- [ ] Swagger 어노테이션 완전 적용
- [ ] RESTful URL 패턴 적용
- [ ] HTTP 메서드 올바른 사용

### **새 DTO 생성 시**
- [ ] Bean Validation 어노테이션
- [ ] Swagger @Schema 어노테이션
- [ ] 정적 from() 메서드 구현
- [ ] Immutable 설계

---

**Version**: v1.0.0 | **Updated**: 2025-09-16