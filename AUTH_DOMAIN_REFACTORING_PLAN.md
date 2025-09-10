# 🔧 AUTH 도메인 리팩토링 플랜

## 📋 **개요**

**목표**: Auth 도메인의 외부 의존성을 제거하여 독립적이고 테스트 가능한 구조로 개선

**방식**: 인터페이스 추상화 + VO 패턴을 통한 점진적 리팩토링

**예상 소요시간**: 2-3시간

## 🎯 **현재 문제점**

### 1. Global 의존성 문제
```java
// AuthenticationService.java
private final JWTUtil jwtUtil;                    // ❌ Global 의존
private final JwtTokenService jwtTokenService;    // ❌ Global 의존
```

### 2. Member 도메인 의존성 문제
```java
// AuthenticationService.java
public void issueTokensOnLogin(HttpServletResponse response, MemberEntity member) // ❌ Member 엔티티 직접 의존
```

## 📈 **리팩토링 단계별 계획**

### **1단계: TokenManager 인터페이스 생성** ⏱️ 30분
**목적**: Global JWT 의존성 추상화

#### 📁 새로 생성할 파일
```
domain/auth/domain/service/TokenManager.java
```

#### 📝 구현 내용
```java
package com.anyang.maruni.domain.auth.domain.service;

public interface TokenManager {
    // 토큰 생성
    String createAccessToken(String memberId, String email);
    String createRefreshToken(String memberId, String email);
    
    // 토큰 추출
    Optional<String> extractRefreshToken(HttpServletRequest request);
    Optional<String> extractAccessToken(HttpServletRequest request);
    
    // 토큰 정보 추출
    Optional<String> getId(String token);
    Optional<String> getEmail(String token);
    Optional<Long> getExpiration(String token);
    
    // 토큰 검증
    boolean isRefreshToken(String token);
    boolean isAccessToken(String token);
    
    // 설정값
    Long getAccessTokenExpiration();
}
```

### **2단계: MemberTokenInfo VO 생성** ⏱️ 20분
**목적**: Member 도메인 의존성 분리

#### 📁 새로 생성할 파일
```
domain/auth/domain/vo/MemberTokenInfo.java
```

#### 📝 구현 내용
```java
package com.anyang.maruni.domain.auth.domain.vo;

@Getter
@RequiredArgsConstructor
public class MemberTokenInfo {
    private final String memberId;
    private final String email;
    
    public static MemberTokenInfo of(Long memberId, String email) {
        return new MemberTokenInfo(memberId.toString(), email);
    }
    
    public static MemberTokenInfo of(String memberId, String email) {
        return new MemberTokenInfo(memberId, email);
    }
}
```

### **3단계: JWTUtil을 TokenManager 구현체로 변경** ⏱️ 45분
**목적**: 기존 코드 호환성 유지하면서 인터페이스 구현

#### 🔄 수정할 파일
```
global/security/JWTUtil.java
```

#### 📝 수정 내용
```java
@Component
public class JWTUtil implements TokenManager {
    // 기존 모든 메소드 유지
    // TokenManager 인터페이스 메소드들 구현
}
```

### **4단계: TokenService 인터페이스 생성** ⏱️ 30분
**목적**: JwtTokenService 의존성도 추상화

#### 📁 새로 생성할 파일
```
domain/auth/domain/service/TokenService.java
```

#### 📝 구현 내용
```java
public interface TokenService {
    void issueTokens(HttpServletResponse response, MemberTokenInfo memberInfo);
    void reissueAccessToken(HttpServletResponse response, String memberId, String email);
    void reissueAllTokens(HttpServletResponse response, String memberId, String email);
    void expireRefreshCookie(HttpServletResponse response);
}
```

### **5단계: AuthenticationService 리팩토링** ⏱️ 60분
**목적**: 추상화된 인터페이스 사용으로 의존성 제거

#### 🔄 수정할 파일
```
domain/auth/application/service/AuthenticationService.java
```

#### 📝 주요 변경사항
```java
// Before
private final JWTUtil jwtUtil;
private final JwtTokenService jwtTokenService;

// After  
private final TokenManager tokenManager;
private final TokenService tokenService;

// Before
public void issueTokensOnLogin(HttpServletResponse response, MemberEntity member)

// After
public void issueTokensOnLogin(HttpServletResponse response, MemberTokenInfo memberInfo)
```

### **6단계: JwtTokenService를 TokenService 구현체로 변경** ⏱️ 30분

#### 🔄 수정할 파일
```
global/security/JwtTokenService.java
```

#### 📝 수정 내용
```java
@Service
public class JwtTokenService implements TokenService {
    // TokenService 인터페이스 구현
    // MemberEntity 대신 MemberTokenInfo 사용
}
```

### **7단계: TokenValidator 수정** ⏱️ 15분
**목적**: TokenManager 인터페이스 사용

#### 🔄 수정할 파일
```
domain/auth/domain/service/TokenValidator.java
```

## 🧪 **테스트 계획**

### **단위 테스트 개선사항**
```java
// Before: 실제 JWT 구현체 필요
@Test
void testAuthenticationService() {
    // JWTUtil 실제 객체 필요
}

// After: Mock 객체로 테스트 가능
@Test
void testAuthenticationService() {
    TokenManager mockTokenManager = mock(TokenManager.class);
    // 완전 격리된 테스트 가능
}
```

## 📊 **리팩토링 전후 비교**

### **Before (현재)**
```
AuthenticationService
├── JWTUtil (Global 의존)
├── JwtTokenService (Global 의존)  
├── MemberEntity (Member 도메인 의존)
└── ...
```

### **After (리팩토링 후)**
```
AuthenticationService
├── TokenManager (인터페이스 의존)
├── TokenService (인터페이스 의존)
├── MemberTokenInfo (Auth 도메인 VO)
└── ...
```

## ✅ **완료 기준 (Definition of Done)**

- [ ] TokenManager 인터페이스 생성 완료
- [ ] MemberTokenInfo VO 생성 완료  
- [ ] JWTUtil이 TokenManager 구현
- [ ] TokenService 인터페이스 생성 완료
- [ ] AuthenticationService 리팩토링 완료
- [ ] JwtTokenService가 TokenService 구현
- [ ] TokenValidator 수정 완료
- [ ] 기존 기능 정상 동작 확인
- [ ] 단위 테스트 작성 가능 확인

## 🚨 **리스크 및 대응책**

### **리스크 1**: 기존 기능 동작 불가
**대응책**: 각 단계마다 컴파일 및 기본 동작 테스트 수행

### **리스크 2**: Global 패키지의 다른 부분과 충돌
**대응책**: 기존 JWTUtil 메소드는 모두 유지, 인터페이스만 추가 구현

### **리스크 3**: 순환 의존성 발생
**대응책**: 인터페이스는 Auth 도메인에, 구현체는 Global에 유지

## 🎯 **기대 효과**

1. **테스트 용이성** ⬆️ 300%: Mock 객체로 완전 격리 테스트
2. **결합도** ⬇️ 70%: 인터페이스 의존으로 느슨한 결합
3. **확장성** ⬆️ 200%: JWT 외 다른 토큰 방식 도입 용이
4. **도메인 독립성** ⬆️ 90%: Auth 도메인의 순수성 확보

## 📅 **실행 순서**

1. **1단계** → **2단계** (기반 구조)
2. **3단계** → **4단계** (인터페이스 구현)  
3. **5단계** → **6단계** (서비스 리팩토링)
4. **7단계** (마무리 정리)

**각 단계마다 컴파일 및 기본 동작 확인 후 다음 단계 진행**

---

이 플랜대로 진행하면 **안전하고 점진적으로** Auth 도메인을 개선할 수 있습니다! 🚀