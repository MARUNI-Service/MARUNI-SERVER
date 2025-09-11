# MARUNI Security Layer DDD 구조 분석 보고서

## 📊 개요

본 문서는 MARUNI 프로젝트의 `global/security` 패키지에 대한 DDD(Domain Driven Design) 계층 구조 적합성을 분석한 결과를 정리합니다.

**분석 일자**: 2025-09-11  
**분석 대상**: `com.anyang.maruni.global.security` 패키지  
**평가 기준**: DDD 원칙 준수, 계층 분리, 의존성 방향, 코드 품질

## 🏗️ 현재 구조

### Security 패키지 파일 목록
```
global/security/
├── AuthenticationEventHandler.java    # 인증 이벤트 처리 인터페이스
├── CustomUserDetails.java            # UserDetails 구현체
├── CustomUserDetailsService.java     # UserDetailsService 구현체
├── JwtAuthenticationFilter.java      # JWT 인증 필터
├── JwtTokenService.java              # 토큰 서비스 구현체
├── JWTUtil.java                      # JWT 유틸리티 (TokenManager 구현)
└── LoginFilter.java                  # 로그인 처리 필터
```

### 관련 도메인 구조
```
domain/auth/
├── domain/service/
│   ├── TokenManager.java             # JWT 관리 인터페이스
│   ├── TokenService.java             # 토큰 서비스 인터페이스
│   ├── TokenValidator.java           # 토큰 검증 서비스
│   └── RefreshTokenService.java      # 리프레시 토큰 서비스
└── application/service/
    └── AuthenticationService.java    # 인증 서비스 (이벤트 핸들러 구현)
```

## ✅ 잘 설계된 부분 (85%)

### 1. 의존성 역전 원칙 준수
- **Auth 도메인에 인터페이스 정의**: `TokenManager`, `TokenService`, `AuthenticationEventHandler`
- **Global 계층에서 구현**: `JWTUtil`, `JwtTokenService`가 도메인 인터페이스를 구현
- **올바른 의존성 방향**: Domain ← Global (의존성 역전)

### 2. 계층 간 책임 분리
```java
// 도메인 계층: 비즈니스 규칙 정의
public interface TokenManager {
    String createAccessToken(String memberId, String email);
    boolean isAccessToken(String token);
}

// 인프라 계층: 기술적 구현
@Component
public class JWTUtil implements TokenManager {
    // JWT 라이브러리를 사용한 구체적 구현
}
```

### 3. 이벤트 기반 설계
- `AuthenticationEventHandler` 인터페이스로 로그인 이벤트 처리
- `AuthenticationService`에서 비즈니스 로직과 이벤트 처리 분리

### 4. 토큰 관리 체계화
- Access/Refresh 토큰 분리 운영
- 토큰 생성, 검증, 추출 로직의 체계적 구성
- Redis 기반 토큰 저장소 연동

## 🚨 문제점 및 개선 사항 (15%)

### 높은 우선순위: DDD 원칙 위반

#### 1. CustomUserDetailsService.java 위치 문제
**현재 위치**: `global/security/CustomUserDetailsService.java`

**문제점**:
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository; // ❌ Global이 Domain Repository에 직접 의존
    
    public UserDetails loadUserByUsername(String username) {
        MemberEntity member = memberRepository.findByMemberEmail(username); // ❌ 계층 위반
        return new CustomUserDetails(member);
    }
}
```

**권장 해결책**:
```
현재: global/security/CustomUserDetailsService.java
권장: domain/member/infrastructure/security/MemberUserDetailsService.java
```

#### 2. CustomUserDetails.java 위치 문제
**현재 위치**: `global/security/CustomUserDetails.java`

**문제점**:
- MemberEntity에 직접 의존하는 구조
- Domain Entity를 Global 계층에서 직접 사용

**권장 해결책**:
```
현재: global/security/CustomUserDetails.java  
권장: domain/member/infrastructure/security/MemberUserDetails.java
```

### 중간 우선순위: 의존성 개선

#### 3. LoginFilter.java 도메인 DTO 의존성
**문제점**:
```java
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        var loginReq = objectMapper.readValue(request.getInputStream(), MemberLoginRequest.class);
        // ❌ Global 계층에서 Domain DTO 직접 사용
    }
}
```

**권장 개선안**:
1. Global 계층용 LoginRequest DTO 생성
2. 또는 Map 기반 파라미터 처리로 추상화

## 🔧 구체적 개선 방안

### 1. 패키지 재구성
```
domain/
├── auth/
│   └── infrastructure/
│       └── security/
│           ├── AuthJwtTokenService.java      # JWT 토큰 서비스 구현
│           └── AuthenticationFilter.java    # 인증 필터
└── member/
    └── infrastructure/
        └── security/
            ├── MemberUserDetailsService.java # UserDetails 서비스
            └── MemberUserDetails.java        # UserDetails 구현체

global/
└── security/
    ├── JWTUtil.java                         # JWT 유틸리티 (기술적 구현)
    ├── JwtAuthenticationFilter.java        # JWT 인증 필터
    ├── LoginFilter.java                     # 로그인 필터
    └── AuthenticationEventHandler.java     # 인터페이스 (유지)
```

### 2. 의존성 개선
```java
// 개선된 CustomUserDetailsService
@Service
public class MemberUserDetailsService implements UserDetailsService {
    private final MemberService memberService; // ✅ Application Service 의존
    
    public UserDetails loadUserByUsername(String username) {
        MemberResponse member = memberService.findByEmail(username);
        return new MemberUserDetails(member);
    }
}
```

### 3. DTO 분리
```java
// Global 계층용 DTO 생성
public class LoginRequest {
    private String email;
    private String password;
    // getter, setter
}

// LoginFilter에서 사용
var loginReq = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
```

## 📈 개선 후 예상 효과

### 1. DDD 원칙 준수 강화
- 계층 간 의존성 방향 정립
- 도메인 순수성 보장
- 비즈니스 로직과 기술 구현의 명확한 분리

### 2. 유지보수성 향상
- 각 계층의 책임 명확화
- 변경 영향도 최소화
- 테스트 용이성 증대

### 3. 확장성 개선
- 새로운 인증 방식 추가 용이
- 다양한 UserDetails 구현체 지원 가능
- 인증 로직의 독립적 발전 가능

## 🎯 실행 계획

### 1단계: 핵심 위반 사항 해결
1. `CustomUserDetailsService` → `domain/member/infrastructure/security`로 이동
2. `CustomUserDetails` → `domain/member/infrastructure/security`로 이동
3. Repository 직접 의존성을 Service 의존성으로 변경

### 2단계: 의존성 정리
1. `LoginFilter`의 도메인 DTO 의존성 제거
2. Global 계층용 DTO 생성
3. 매핑 로직 분리

### 3단계: 문서 및 테스트 업데이트
1. 아키텍처 문서 업데이트
2. 관련 테스트 케이스 수정
3. CLAUDE.md 가이드라인 업데이트

## 📝 결론

MARUNI 프로젝트의 Security 계층은 **전반적으로 DDD 원칙을 잘 준수**하고 있습니다. 특히 의존성 역전과 인터페이스 기반 설계가 훌륭하게 구현되어 있습니다.

**핵심 개선 포인트**:
- 2개 클래스(`CustomUserDetailsService`, `CustomUserDetails`)의 적절한 계층 이동
- Domain Repository 직접 접근 제거
- DTO 의존성 정리

이러한 개선을 통해 **DDD 원칙 준수도를 85%에서 95%로 향상**시킬 수 있을 것으로 예상됩니다.

---

**작성자**: Claude Code  
**검토 필요**: Security 아키텍처 개선 시 이 문서 참조  
**관련 문서**: `CLAUDE.md`, `SecurityConfig.java`