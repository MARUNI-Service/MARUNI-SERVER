# 테스트 가이드

**MARUNI TDD Red-Green-Blue 사이클 및 테스트 작성**

## 🧪 TDD Red-Green-Blue 사이클

### 기본 원칙
```
1. Red: 실패하는 테스트 작성
2. Green: 테스트를 통과하는 최소 코드 작성
3. Blue: 리팩토링 + 코드 품질 향상
```

### 실전 예시
```java
// 1. Red: 실패 테스트
@Test
void createMember_shouldReturnMemberResponse() {
    // Given
    MemberSaveRequest request = new MemberSaveRequest(...);
    
    // When & Then
    assertThatThrownBy(() -> memberService.save(request))
        .isInstanceOf(NullPointerException.class);  // 아직 구현 안 됨
}

// 2. Green: 최소 구현
public void save(MemberSaveRequest request) {
    MemberEntity entity = MemberEntity.create(...);
    memberRepository.save(entity);
}

// 3. Blue: 리팩토링
public void save(MemberSaveRequest request) {
    validateDuplicateEmail(request.getEmail());  // 검증 추가
    String encodedPassword = passwordEncoder.encode(...);  // 암호화
    MemberEntity entity = memberMapper.toEntity(request, encodedPassword);
    memberRepository.save(entity);
}
```

## 📝 테스트 작성 패턴

### Unit Test (Service Layer)
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 테스트")
class MemberServiceTest {
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private MemberService memberService;
    
    @Test
    @DisplayName("회원 가입 성공")
    void save_shouldSaveSuccessfully() {
        // Given
        MemberSaveRequest request = new MemberSaveRequest("test@test.com", "name", "pw");
        
        given(memberRepository.findByMemberEmail(anyString()))
            .willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString()))
            .willReturn("encoded");
        
        // When
        memberService.save(request);
        
        // Then
        verify(memberRepository, times(1)).save(any(MemberEntity.class));
    }
}
```

### Integration Test (Repository Layer)
```java
@DataJpaTest
@DisplayName("MemberRepository 통합 테스트")
class MemberRepositoryTest {
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Test
    @DisplayName("이메일로 회원 조회")
    void findByMemberEmail_shouldReturnMember() {
        // Given
        MemberEntity member = MemberEntity.createRegularMember(...);
        memberRepository.save(member);
        
        // When
        Optional<MemberEntity> found = memberRepository.findByMemberEmail("test@test.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getMemberEmail()).isEqualTo("test@test.com");
    }
}
```

### Controller Test (API Layer)
```java
@WebMvcTest(MemberApiController.class)
@DisplayName("MemberApiController 테스트")
class MemberApiControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MemberService memberService;
    
    @Test
    @DisplayName("내 정보 조회")
    @WithMockUser(username = "test@test.com")
    void getMyInfo_shouldReturnMyInfo() throws Exception {
        // Given
        MemberResponse response = new MemberResponse(1L, "name", "test@test.com");
        given(memberService.getMyInfo(anyString())).willReturn(response);
        
        // When & Then
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.memberEmail").value("test@test.com"));
    }
}
```

## 🎯 Assertion 라이브러리

### AssertJ 사용
```java
// ✅ AssertJ (권장)
assertThat(result).isNotNull();
assertThat(result.getId()).isEqualTo(1L);
assertThat(result.getMembers()).hasSize(3);
assertThat(result.getEmail()).startsWith("test");

// ❌ JUnit Assertions (비권장)
assertNotNull(result);
assertEquals(1L, result.getId());
```

### 예외 검증
```java
// ✅ AssertJ
assertThatThrownBy(() -> memberService.save(request))
    .isInstanceOf(BaseException.class)
    .hasMessageContaining("DUPLICATE_EMAIL");

// 예외 세부 검증
assertThatThrownBy(() -> memberService.save(request))
    .isInstanceOf(BaseException.class)
    .satisfies(e -> {
        BaseException be = (BaseException) e;
        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    });
```

## 🔧 Mock 사용 패턴

### given-when-then
```java
@Test
void createGuardian_shouldReturnGuardianResponse() {
    // Given
    GuardianRequestDto request = new GuardianRequestDto(...);
    GuardianEntity entity = GuardianEntity.createGuardian(...);
    
    given(guardianRepository.findByGuardianEmailAndIsActiveTrue(...))
        .willReturn(Optional.empty());
    given(guardianRepository.save(any(GuardianEntity.class)))
        .willReturn(entity);
    
    // When
    GuardianResponseDto response = guardianService.createGuardian(request);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getGuardianEmail()).isEqualTo(request.getGuardianEmail());
    verify(guardianRepository).save(any(GuardianEntity.class));
}
```

### ArgumentCaptor 사용
```java
@Test
void save_shouldEncodePassword() {
    // Given
    ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
    
    // When
    memberService.save(request);
    
    // Then
    verify(memberRepository).save(captor.capture());
    MemberEntity saved = captor.getValue();
    assertThat(saved.getMemberPassword()).isNotEqualTo("rawPassword");
    assertThat(saved.getMemberPassword()).startsWith("$2a$");  // BCrypt
}
```

## 📊 테스트 커버리지

### 목표
```
Unit Test: 80% 이상
Integration Test: 주요 플로우 100%
전체 Coverage: 90% 이상
```

### Jacoco 설정
```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.12"
}

test {
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    reports {
        html.required = true
        xml.required = true
    }
}
```

## ✅ 테스트 체크리스트

### 작성 필수
- [ ] Service Layer 모든 public 메서드
- [ ] Repository Custom Query
- [ ] Controller 모든 Endpoint
- [ ] Entity 정적 팩토리 메서드
- [ ] 예외 처리 로직

### 검증 항목
- [ ] Happy Path (정상 케이스)
- [ ] Edge Case (경계값)
- [ ] Exception Case (예외 상황)
- [ ] Null Safety
- [ ] Validation (Bean Validation)

---

**상세 예시는 실제 테스트 코드 (`src/test/`)를 참조하세요.**
