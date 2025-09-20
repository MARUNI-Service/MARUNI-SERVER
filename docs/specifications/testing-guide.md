# 테스트 작성 가이드

**MARUNI TDD 방법론 및 테스트 표준**

---

## 🧪 TDD 사이클

### **Red-Green-Blue 사이클**
```
🔴 Red: 실패하는 테스트 작성
🟢 Green: 테스트를 통과하는 최소 구현
🔵 Blue: 리팩토링으로 코드 품질 향상
```

### **사이클 적용 원칙**
```java
// 1. Red: 실패 테스트 먼저 작성
@Test
@DisplayName("회원가입 실패 - 중복된 이메일")
void save_DuplicateEmail_ThrowsException() {
    // given
    given(memberRepository.existsByMemberEmail(anyString())).willReturn(true);

    // when & then
    assertThrows(BaseException.class, () -> memberService.save(request));
}

// 2. Green: 테스트 통과하는 최소 구현
public void save(MemberSaveRequest req) {
    if (memberRepository.existsByMemberEmail(req.getMemberEmail())) {
        throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
    }
    // 최소 구현
}

// 3. Blue: 리팩토링
public void save(MemberSaveRequest req) {
    validateEmailDuplication(req.getMemberEmail()); // 메서드 분리
    // 개선된 구현
}
```

---

## 📋 테스트 구조 및 명명

### **테스트 클래스 구조**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    // Mock 객체들
    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MemberMapper memberMapper;

    // 테스트 대상
    @InjectMocks
    private MemberService memberService;

    // 공통 테스트 데이터
    private MemberSaveRequest validRequest;
    private MemberEntity memberEntity;

    @BeforeEach
    void setUp() {
        validRequest = createValidMemberSaveRequest();
        memberEntity = createMemberEntity();
    }

    // 테스트 메서드들...
}
```

### **테스트 메서드 명명 규칙**
```java
// 패턴: {메서드명}_{시나리오}_{예상결과}
@Test
@DisplayName("회원가입 성공 - 정상적인 요청으로 회원을 저장한다")
void save_ValidRequest_Success() { }

@Test
@DisplayName("회원가입 실패 - 중복된 이메일로 예외가 발생한다")
void save_DuplicateEmail_ThrowsException() { }

@Test
@DisplayName("회원 조회 성공 - ID로 회원을 찾는다")
void findById_ExistingId_ReturnsUser() { }

@Test
@DisplayName("회원 조회 실패 - 존재하지 않는 ID로 예외가 발생한다")
void findById_NonExistingId_ThrowsException() { }
```

---

## 🏗️ 계층별 테스트 작성

### **1. Repository 테스트**
```java
@DataJpaTest
@DisplayName("MemberRepository 테스트")
class MemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일로 회원 조회 - 존재하는 이메일로 회원을 찾는다")
    void findByMemberEmail_ExistingEmail_ReturnsUser() {
        // given
        MemberEntity savedMember = entityManager.persistAndFlush(
            createTestMember("test@example.com", "테스트 사용자")
        );

        // when
        Optional<MemberEntity> found = memberRepository.findByMemberEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedMember.getId());
        assertThat(found.get().getMemberName()).isEqualTo("테스트 사용자");
    }

    private MemberEntity createTestMember(String email, String name) {
        return MemberEntity.builder()
            .memberEmail(email)
            .memberName(name)
            .memberPassword("encodedPassword")
            .build();
    }
}
```

### **2. Service 테스트**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @Test
    @DisplayName("회원가입 성공 - 정상적인 요청으로 회원을 저장한다")
    void save_ValidRequest_Success() {
        // given
        MemberSaveRequest request = createValidRequest();
        given(memberRepository.existsByMemberEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // when
        assertDoesNotThrow(() -> memberService.save(request));

        // then
        verify(memberRepository).save(any(MemberEntity.class));
        verify(passwordEncoder).encode(request.getMemberPassword());
    }

    @Test
    @DisplayName("회원 조회 실패 - 존재하지 않는 ID로 예외가 발생한다")
    void findById_NonExistingId_ThrowsException() {
        // given
        Long nonExistingId = 999L;
        given(memberRepository.findById(nonExistingId)).willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class,
            () -> memberService.findById(nonExistingId));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
```

### **3. Controller 테스트**
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("UserApiController 통합 테스트")
class UserApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 생성 API - 정상적인 요청으로 사용자를 생성한다")
    void createUser_ValidRequest_ReturnsSuccess() throws Exception {
        // given
        MemberSaveRequest request = createValidRequest();

        // when & then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("M002"))
                .andExpect(jsonPath("$.message").exists())
                .andDo(print());
    }

    @Test
    @DisplayName("사용자 생성 API - 유효성 검사 실패로 400 에러")
    void createUser_InvalidRequest_ReturnsBadRequest() throws Exception {
        // given
        MemberSaveRequest invalidRequest = new MemberSaveRequest();
        // 필수 필드 누락

        // when & then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("V001"));
    }
}
```

---

## 🎯 테스트 작성 패턴

### **Given-When-Then 패턴**
```java
@Test
void testMethod() {
    // given (준비)
    // 테스트에 필요한 데이터, Mock 동작 설정
    MemberSaveRequest request = createValidRequest();
    given(memberRepository.existsByMemberEmail(anyString())).willReturn(false);

    // when (실행)
    // 테스트할 메서드 실행
    memberService.save(request);

    // then (검증)
    // 결과 검증, Mock 호출 검증
    verify(memberRepository).save(any(MemberEntity.class));
}
```

### **예외 테스트 패턴**
```java
// assertThrows 사용
@Test
void testException() {
    // given
    given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

    // when & then
    BaseException exception = assertThrows(BaseException.class,
        () -> memberService.findById(1L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    assertThat(exception.getMessage()).contains("회원을 찾을 수 없습니다");
}
```

### **Mock 활용 패턴**
```java
// Mock 동작 정의
given(memberRepository.findById(1L)).willReturn(Optional.of(memberEntity));
given(passwordEncoder.encode("password")).willReturn("encodedPassword");

// Mock 호출 검증
verify(memberRepository).save(any(MemberEntity.class));
verify(memberRepository, never()).delete(any());
verify(passwordEncoder, times(1)).encode(anyString());

// ArgumentCaptor 사용
ArgumentCaptor<MemberEntity> memberCaptor = ArgumentCaptor.forClass(MemberEntity.class);
verify(memberRepository).save(memberCaptor.capture());
MemberEntity savedMember = memberCaptor.getValue();
assertThat(savedMember.getMemberEmail()).isEqualTo("test@example.com");
```

---

## 📊 테스트 유틸리티

### **테스트 데이터 생성**
```java
// 공통 테스트 유틸리티 클래스
public class TestDataFactory {

    public static MemberSaveRequest createValidMemberSaveRequest() {
        MemberSaveRequest request = new MemberSaveRequest();
        request.setMemberEmail("test@example.com");
        request.setMemberName("테스트 사용자");
        request.setMemberPassword("password123");
        return request;
    }

    public static MemberEntity createMemberEntity() {
        return MemberEntity.builder()
            .id(1L)
            .memberEmail("test@example.com")
            .memberName("테스트 사용자")
            .memberPassword("encodedPassword")
            .build();
    }
}
```

### **AssertJ 활용**
```java
// 컬렉션 검증
assertThat(members)
    .hasSize(3)
    .extracting("memberEmail")
    .contains("user1@example.com", "user2@example.com");

// 객체 필드 검증
assertThat(member)
    .extracting("id", "memberEmail", "memberName")
    .contains(1L, "test@example.com", "테스트 사용자");

// 예외 검증
assertThatThrownBy(() -> memberService.findById(999L))
    .isInstanceOf(BaseException.class)
    .hasMessage("회원을 찾을 수 없습니다");
```

---

## 🔧 테스트 설정

### **Test Profile 설정**
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  profiles:
    active: test

logging:
  level:
    org.springframework.web: DEBUG
    com.anyang.maruni: DEBUG
```

### **테스트 설정 클래스**
```java
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // 테스트용 간단한 인코더
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded_" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals("encoded_" + rawPassword);
            }
        };
    }
}
```

---

## 🎯 테스트 작성 체크리스트

### **Unit Test 작성 시**
- [ ] @ExtendWith(MockitoExtension.class) 적용
- [ ] 의존성 @Mock으로 모킹
- [ ] Given-When-Then 패턴 적용
- [ ] 테스트 메서드명 명확히 작성
- [ ] @DisplayName으로 한글 설명 추가

### **Integration Test 작성 시**
- [ ] @SpringBootTest 적용
- [ ] @Transactional로 데이터 격리
- [ ] 실제 데이터베이스 상호작용 확인
- [ ] MockMvc로 HTTP 계층 테스트
- [ ] 전체 시나리오 검증

### **Repository Test 작성 시**
- [ ] @DataJpaTest 적용
- [ ] TestEntityManager 활용
- [ ] 실제 쿼리 동작 확인
- [ ] 인덱스 및 제약조건 검증

### **테스트 품질 관리**
- [ ] 테스트 커버리지 90% 이상
- [ ] Edge Case 테스트 포함
- [ ] 테스트 독립성 보장
- [ ] 테스트 실행 속도 최적화

---

**Version**: v1.0.0 | **Updated**: 2025-09-16