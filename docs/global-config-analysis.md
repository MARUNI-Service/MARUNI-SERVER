# Global Config 패키지 DDD 아키텍처 분석 보고서

## 📋 개요

본 문서는 MARUNI 프로젝트의 `global/config` 패키지에 대한 상세한 DDD(Domain-Driven Design) 아키텍처 분석 결과를 제공합니다. 각 설정 파일의 위치 적절성, 내용 자연스러움, DDD 원칙 준수도를 평가하고 개선 방안을 제시합니다.

---

## 🏗️ 패키지 구조 현황

```
global/config/
├── SecurityConfig.java           # Spring Security 메인 설정
├── JwtSecurityConfig.java        # JWT 관련 Bean 설정
├── RedisConfig.java              # Redis 연결 설정
├── SwaggerConfig.java            # API 문서화 설정
├── CorsConfig.java               # CORS 정책 설정
└── properties/
    ├── JwtProperties.java        # JWT 설정 프로퍼티
    └── SecurityProperties.java   # 보안 관련 프로퍼티
```

---

## 📊 종합 평가 결과

| 파일명 | 위치 적절성 | 내용 자연스러움 | DDD 준수도 | 종합 등급 |
|--------|-------------|----------------|------------|-----------|
| SecurityConfig.java | ✅ 우수 | ✅ 우수 | ✅ 양호 | **A** |
| JwtProperties.java | ✅ 우수 | ✅ 우수 | ✅ 우수 | **A** |
| RedisConfig.java | ✅ 우수 | ✅ 양호 | ✅ 양호 | **B+** |
| JwtSecurityConfig.java | ✅ 우수 | ⚠️ 보통 | ⚠️ 보통 | **B** |
| SecurityProperties.java | ✅ 우수 | ⚠️ 보통 | ⚠️ 보통 | **B** |
| SwaggerConfig.java | ✅ 우수 | ❌ 나쁨 | ⚠️ 보통 | **C** |
| CorsConfig.java | ✅ 우수 | ❌ 나쁨 | ❌ 나쁨 | **D** |

---

## 🛠️ **구체적인 개선 방안**

### 1단계: Admin 설정 제거 (User-only 서비스)

```java
// SecurityConfig.java 수정
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // 기존 코드...
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
        LoginFilter loginFilter,
        JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
            // 기존 설정들...
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(securityProperties.getPublicUrlsArray()).permitAll()
                // Admin 관련 설정 제거 - 단순 User만 존재
                .anyRequest().authenticated())  // 인증된 사용자면 모두 접근 가능
            // 나머지 설정들...
        
        return http.build();
    }
}

// SecurityProperties.java 수정  
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private List<String> publicUrls;
    // adminUrls 관련 필드 및 메서드 모두 제거
    
    public String[] getPublicUrls() {
        return publicUrls != null ? publicUrls.toArray(new String[0]) : new String[0];
    }
}
```

### 2단계: CorsProperties 생성 및 보안 수정

```java
// 새로 생성: global/config/properties/CorsProperties.java
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "Accept");
    private List<String> exposedHeaders = List.of("Authorization");
    private boolean allowCredentials = true;
    private long maxAge = 3600L;
}
```

### 3단계: AuthenticationEventHandler 인터페이스 생성

```java
// 새로 생성: global/security/AuthenticationEventHandler.java
public interface AuthenticationEventHandler {
    void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo);
}

// domain/auth/application/service/AuthenticationService.java 수정
@Service
public class AuthenticationService implements AuthenticationEventHandler {
    // 기존 코드...
    
    @Override
    public void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo) {
        issueTokensOnLogin(response, memberInfo);
    }
}
```

### 4단계: SwaggerProperties 생성

```java
// 새로 생성: global/config/properties/SwaggerProperties.java
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {
    private Api api = new Api();
    private Contact contact = new Contact();
    private Server server = new Server();
    
    @Data
    public static class Api {
        private String title = "MARUNI API Documentation";
        private String description = "REST API for MARUNI elderly care service";
        private String version = "v1.0.0";
    }
    
    @Data
    public static class Contact {
        private String name = "MARUNI Development Team";
        private String email = "dev@maruni.com";
        private String url = "https://github.com/maruni-project";
    }
    
    @Data
    public static class Server {
        private String url = "http://localhost:8080";
        private String description = "Development Server";
    }
}
```

---

## 🔍 파일별 상세 분석

### 1. SecurityConfig.java ✅ **등급: A**

**📍 위치**: `global/config/SecurityConfig.java`

**✅ 장점**
- **단일 책임 원칙 준수**: Spring Security 설정만 담당
- **의존성 외부화**: SecurityProperties, CorsConfig 주입으로 설정 분리
- **명확한 보안 정책**: Stateless JWT 인증, 적절한 필터 체인 구성
- **DDD Infrastructure 계층 적합**: 인프라 관심사의 적절한 분리

**⚠️ 개선점**
```java
// 현재 - 하드코딩된 역할
.requestMatchers(securityProperties.getAdminUrlsArray()).hasRole("ADMIN")

// 개선안
.requestMatchers(securityProperties.getAdminUrlsArray()).hasRole(securityProperties.getAdminRole())
```

**🔧 권장 조치**
- SecurityProperties에 adminRole 필드 추가
- 주석된 OAuth2 코드 정리
- 에러 메시지 외부화

---

### 2. JwtProperties.java ✅ **등급: A**

**📍 위치**: `global/config/properties/JwtProperties.java`

**✅ 장점**
- **설정 외부화 모범 사례**: `@ConfigurationProperties` 활용
- **구조화된 설정**: Inner class로 AccessToken/RefreshToken 분리
- **기본값 제공**: 적절한 만료시간 기본값
- **명확한 문서화**: 클래스와 필드 주석 완비

**⚠️ 개선점**
```java
// 현재 - 불변성 위반 가능
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

// 개선안 - 검증 추가
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {
    
    @NotBlank(message = "JWT secret key는 필수입니다")
    @Size(min = 32, message = "JWT secret key는 최소 32자 이상이어야 합니다")
    private String secretKey;
    
    @Valid
    private AccessToken accessToken = new AccessToken();
}
```

**🔧 권장 조치**
- `@Data` → `@Getter/@Setter` 변경
- Bean Validation 적용
- 보안 설정값 검증 로직 추가

---

### 3. RedisConfig.java ✅ **등급: B+**

**📍 위치**: `global/config/RedisConfig.java`

**✅ 장점**
- **명확한 단일 책임**: Redis 연결 설정 전담
- **조건부 설정**: 비밀번호 유무에 따른 적절한 분기
- **적절한 직렬화**: String 기반 안전한 직렬화

**⚠️ 개선점**
```java
// 현재 - 개별 @Value 사용
@Value("${spring.data.redis.host}")
private String host;

// 개선안 - Properties 클래스 도입
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
@Validated
public class RedisProperties {
    @NotBlank
    private String host;
    
    @Range(min = 1, max = 65535)
    private int port;
    
    private String password;
}
```

**🔧 권장 조치**
- RedisProperties 클래스 생성
- 연결 설정 검증 로직 추가
- 한글 주석 영문화
- 확장 가능한 RedisTemplate 설계 고려

---

### 4. JwtSecurityConfig.java ⚠️ **등급: B**

**📍 위치**: `global/config/JwtSecurityConfig.java`

**✅ 장점**
- **JWT 관련 Bean 집중화**: 관련 컴포넌트 한 곳에서 관리
- **표준 보안 설정**: BCrypt, AuthenticationManager 등 표준 구성

**❌ 문제점**
```java
// 도메인 의존성 - Infrastructure가 Domain을 참조
private final AuthenticationService authenticationService;

// 하드코딩된 URL
loginFilter.setFilterProcessesUrl("/api/auth/login");

// 중복 설정
loginFilter.setAuthenticationManager(authManager); // 생성자에서 이미 설정됨
```

**🔧 권장 조치**
```java
// 개선안
@Value("${auth.login.endpoint:/api/auth/login}")
private String loginEndpoint;

@Bean
public LoginFilter loginFilter(AuthenticationManager authManager) {
    LoginFilter loginFilter = new LoginFilter(authManager, objectMapper, authenticationService);
    loginFilter.setFilterProcessesUrl(loginEndpoint);
    // 중복 setAuthenticationManager 제거
    return loginFilter;
}
```

**🤔 구조적 검토 필요**
- Infrastructure 계층이 Domain 계층(AuthenticationService)을 직접 참조하는 것이 DDD 관점에서 적절한가?

---

### 5. SecurityProperties.java ⚠️ **등급: B**

**📍 위치**: `global/config/properties/SecurityProperties.java`

**✅ 장점**
- **설정 외부화**: application.yml 바인딩
- **Null 안전성**: 배열 변환 시 null 체크
- **적절한 패키지 위치**: properties 하위 배치

**❌ 문제점**
```java
// 책임 혼재 - Security와 CORS 설정이 한 클래스에
private List<String> publicUrls;
private List<String> adminUrls;
private long corsMaxAge = 3600L;  // CORS 설정이 Security에?

// 불완전한 설정
// adminRole 같은 필수 설정 누락

// 불필요한 접미사
public String[] getPublicUrlsArray() // Array 접미사 불필요
```

**🔧 권장 조치**
```java
// SecurityProperties 분리
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
@Validated
public class SecurityProperties {
    @NotEmpty
    private List<String> publicUrls;
    
    @NotEmpty
    private List<String> adminUrls;
    
    @NotBlank
    private String adminRole = "ADMIN";
    
    public String[] getPublicUrls() { // Array 접미사 제거
        return publicUrls != null ? publicUrls.toArray(new String[0]) : new String[0];
    }
}

// 별도 CorsProperties 생성
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    @NotEmpty
    private List<String> allowedOrigins;
    // ...
}
```

---

### 6. SwaggerConfig.java ❌ **등급: C**

**📍 위치**: `global/config/SwaggerConfig.java`

**✅ 장점**
- **고도한 커스터마이징**: 자동 예제 생성, 에러 코드 문서화
- **JWT 보안 스키마**: 자동 인증 헤더 설정

**🚨 심각한 문제점**
```java
// 개인정보 하드코딩 - 보안 위험
@OpenAPIDefinition(
    info = @Info(
        contact = @Contact(
            name = "김규일",                    // 개인정보 노출
            email = "rlarbdlf222@gmail.com",   // 개인 이메일 노출
            url = "https://github.com/Kimgyuilli"  // 개인 계정 노출
        )
    )
)

// 한글 하드코딩
title = "Spring Login API 문서",
description = "JWT 인증 기반 로그인 시스템의 REST API 문서입니다.",

// 과도한 복잡성 - 200줄이 넘는 단일 클래스
```

**🔧 긴급 조치 필요**
```yaml
# application.yml에 추가
swagger:
  api:
    title: MARUNI API Documentation
    description: REST API Documentation for MARUNI elderly care service
    version: v1.0.0
  contact:
    name: MARUNI Development Team
    email: dev@maruni.com
    url: https://github.com/maruni-project/maruni
```

```java
// SwaggerConfig 개선
@OpenAPIDefinition(
    info = @Info(
        title = "${swagger.api.title}",
        description = "${swagger.api.description}",
        version = "${swagger.api.version}",
        contact = @Contact(
            name = "${swagger.contact.name}",
            email = "${swagger.contact.email}",
            url = "${swagger.contact.url}"
        )
    )
)
```

---

### 7. CorsConfig.java 🚨 **등급: D**

**📍 위치**: `global/config/CorsConfig.java`

**🚨 심각한 보안 위험**
```java
// 매우 위험한 설정
config.setAllowedOriginPatterns(List.of("*"));  // 모든 도메인 허용
config.setAllowedHeaders(List.of("*"));         // 모든 헤더 허용
config.setAllowCredentials(true);               // + Credentials = 보안 취약점
```

**💥 보안 위험도**: **CRITICAL**
- **CSRF 공격 위험**: 모든 도메인에서 인증된 요청 가능
- **데이터 탈취 위험**: 악성 사이트에서 사용자 토큰으로 API 호출 가능
- **XSS 공격 확대**: 모든 헤더 허용으로 공격 벡터 확대

**🔧 긴급 수정 필요**
```yaml
# application.yml - 안전한 CORS 설정
cors:
  allowed-origins:
    - http://localhost:3000          # 개발 환경
    - https://maruni.your-domain.com # 운영 환경
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
  allowed-headers:
    - Authorization
    - Content-Type
    - Accept
  exposed-headers:
    - Authorization
  allow-credentials: true
  max-age: 3600
```

```java
// 안전한 CorsConfig
@Configuration
@RequiredArgsConstructor
public class CorsConfig {
    private final CorsProperties corsProperties;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(corsProperties.getAllowedOrigins()); // 특정 도메인만
            config.setAllowedMethods(corsProperties.getAllowedMethods());
            config.setAllowedHeaders(corsProperties.getAllowedHeaders()); // 특정 헤더만
            config.setExposedHeaders(corsProperties.getExposedHeaders());
            config.setAllowCredentials(corsProperties.isAllowCredentials());
            config.setMaxAge(corsProperties.getMaxAge());
            return config;
        };
    }
}
```

---

## 🎯 우선순위별 조치 계획

### 🚨 **우선순위 1: 긴급 보안 수정 (즉시)**

1. **CorsConfig.java 보안 수정**
   - 모든 오리진 허용(`*`) → 개발환경 도메인 지정 (`localhost:3000`)
   - 모든 헤더 허용(`*`) → 필요한 헤더만 지정 (`Authorization`, `Content-Type`)
   - CorsProperties 클래스 생성

2. **SwaggerConfig.java 개인정보 제거**
   - 하드코딩된 개발자 정보 제거
   - SwaggerProperties로 설정 외부화
   - MARUNI 프로젝트 정보로 변경

3. **Admin 관련 불필요한 설정 제거** ⚠️ **새로 추가**
   - SecurityConfig에서 Admin 권한 설정 제거
   - SecurityProperties에서 adminUrls 관련 코드 제거
   - GlobalExceptionHandler에서 Role enum 처리 제거
   - ErrorCode에서 INVALID_ROLE 제거

### ⚠️ **우선순위 2: 구조적 개선 (1주일 내)**

1. **DDD 의존성 정리 (Interface 분리 방식)**
   - `AuthenticationEventHandler` 인터페이스 생성
   - JwtSecurityConfig가 Domain 구현체 대신 인터페이스 참조
   - 의존성 방향: Domain → Global 인터페이스

2. **Properties 클래스 적당한 분리**
   - SecurityProperties에서 CORS 설정 분리 → CorsProperties 생성
   - SwaggerProperties 생성 (개인정보 하드코딩 해결)
   - JwtProperties는 현재 구조 유지

### 🔧 **우선순위 3: 코드 품질 개선 (2주일 내)**

1. **Swagger 복잡도 관리**
   - SwaggerExampleCustomizer 클래스로 예제 생성 로직 분리
   - SwaggerConfig는 설정만 담당 (100줄 이하 목표)
   - 커스텀 기능은 유지하되 구조적 분리

2. **DTO 참조 정책 확정**
   - LoginFilter의 MemberLoginRequest 참조는 현재 구조 유지
   - 명시적 의존성으로 관리 (로그인은 본질적으로 Member 도메인과 연관)
   - 별도 DTO 생성하지 않고 실용적 접근

---

## 📈 DDD 아키텍처 준수도 평가

### ✅ **잘 지켜진 DDD 원칙**

1. **Infrastructure 계층 분리**: 외부 시스템(Redis, Database) 설정 적절히 분리
2. **설정 외부화**: 대부분의 설정을 application.yml로 외부화
3. **단일 책임 원칙**: 각 Config 클래스가 명확한 책임 보유
4. **의존성 주입**: Spring의 DI 컨테이너 적절히 활용

### ❌ **위반된 DDD 원칙**

1. **도메인 경계 위반**: JwtSecurityConfig가 Auth 도메인 서비스 직접 참조
2. **Infrastructure 순수성 부족**: 하드코딩으로 인한 설정 결합도 증가
3. **보안 설계 결함**: CORS 설정으로 인한 보안 경계 무력화

### 🎯 **확정된 DDD 개선 방향**

1. **Interface 분리로 의존성 역전**
   ```java
   // 추천 방식: AuthenticationEventHandler 인터페이스 도입
   // Infrastructure(Global) → Interface ← Domain(Auth)
   ```

2. **실용적 DTO 참조 정책**
   - 로그인 기능의 MemberLoginRequest 참조는 허용
   - 명시적 의존성으로 관리하되 과도한 분리는 지양
   - 본질적으로 연관된 기능은 실용성 우선

3. **적당한 Properties 분리**
   - 과도한 세분화 지양, 3개 클래스로 제한
   - SecurityProperties, CorsProperties, SwaggerProperties
   - JwtProperties는 현재 구조 유지

---

## 📋 확정된 실행 체크리스트

### 🚨 즉시 수정 필요 (1일 내)
- [ ] **Admin 설정 제거** - SecurityConfig, SecurityProperties 단순화
- [ ] **CorsConfig 보안 수정** - 개발환경 도메인으로 제한
- [ ] **SwaggerConfig 개인정보 제거** - SwaggerProperties 외부화
- [ ] **CorsProperties 클래스 생성** - CORS 설정 분리
- [ ] **불필요한 Role enum 처리 제거** - GlobalExceptionHandler, ErrorCode 정리

### ⚠️ 구조적 개선 (1주일 내)  
- [ ] **AuthenticationEventHandler 인터페이스 생성** - DDD 의존성 정리
- [ ] **SecurityProperties CORS 설정 제거** - 책임 분리
- [ ] **SwaggerExampleCustomizer 클래스 생성** - 복잡도 관리

### 🔧 선택적 개선 (필요시)
- [ ] RedisProperties 클래스 생성 (현재는 불필요)
- [ ] Bean Validation 적용 (점진적 적용)
- [ ] 환경별 설정 분화 (운영환경 구축 시)

### ❌ 하지 않기로 결정된 항목
- ~~별도 LoginRequest DTO 생성~~ → MemberLoginRequest 사용 유지
- ~~과도한 Properties 세분화~~ → 3개 클래스로 제한  
- ~~Swagger 기능 단순화~~ → 커스텀 기능 유지하되 구조 개선

---

## 🔚 결론 및 개선 방향

MARUNI 프로젝트의 Global Config 패키지는 전반적으로 Infrastructure 계층의 역할을 적절히 수행하고 있으나, **보안과 개인정보 보호** 측면에서 즉시 수정이 필요한 중대한 문제들이 발견되었습니다.

### 📊 **최종 개선 전략**

**실용적 접근**: 과도한 아키텍처 순수주의보다는 **개발 효율성과 유지보수성**을 고려한 균형잡힌 개선

**점진적 개선**: 한 번에 모든 것을 바꾸지 않고, **우선순위에 따른 단계적 개선**

**명확한 정책**: 어떤 것은 개선하고, 어떤 것은 현상 유지할지 **명확한 기준 설정**

### 🎯 **핵심 개선 포인트**

1. **보안 우선**: CorsConfig, SwaggerConfig의 보안 위험 즉시 해결
2. **의존성 정리**: Interface 분리로 DDD 원칙 준수하되 실용성 유지  
3. **적당한 분리**: 3개 Properties 클래스로 제한하여 복잡도 관리
4. **기능 유지**: Swagger 커스텀 기능은 유지하되 구조적 분리

### 🚀 **기대 효과**

- **보안 강화**: CORS/Swagger 보안 위험 해결
- **유지보수성 향상**: 적절한 클래스 분리와 책임 분담
- **DDD 준수**: Interface 분리로 의존성 방향 정리
- **개발 효율성**: 과도한 분리 지양으로 복잡도 관리

이번 분석을 통해 **보안과 아키텍처, 실용성의 균형점**을 찾아 더욱 견고하면서도 유지보수가 용이한 설정 구조로 발전시킬 수 있을 것입니다.