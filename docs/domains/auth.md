# Auth 도메인 구현 가이드라인 (2025-09-16 완성)

## 🎉 완성 상태 요약

**Auth 도메인은 DDD 의존성 역전 원칙을 완벽히 적용하여 100% 완성되었습니다.**

### 🏆 완성 지표
- ✅ **JWT Access/Refresh 토큰 시스템**: 완전한 이중 토큰 보안 구조
- ✅ **DDD 의존성 역전**: Domain Interface ← Global 구현체 구조 완성
- ✅ **Redis 기반 토큰 관리**: RefreshToken 저장 + Blacklist 관리
- ✅ **Spring Security 통합**: 필터 체인 기반 인증/인가 처리
- ✅ **REST API 완성**: 3개 엔드포인트 + Swagger 문서화
- ✅ **실제 운영 준비**: 상용 서비스 수준 보안 시스템

## 📐 아키텍처 구조

### DDD 패키지 구조
```
com.anyang.maruni.domain.auth/
├── application/                    # Application Layer
│   ├── dto/response/              # Response DTO
│   │   └── TokenResponse.java     ✅ 완성
│   └── service/                   # Application Service
│       └── AuthenticationService.java ✅ 완성 (AuthenticationEventHandler 구현)
├── domain/                        # Domain Layer
│   ├── entity/                    # Domain Entity
│   │   └── RefreshToken.java      ✅ 완성 (Redis Entity)
│   ├── repository/                # Repository Interface
│   │   ├── RefreshTokenRepository.java     ✅ 완성
│   │   └── TokenBlacklistRepository.java   ✅ 완성 (인터페이스)
│   ├── service/                   # Domain Service
│   │   ├── TokenManager.java      ✅ 완성 (인터페이스)
│   │   ├── TokenService.java      ✅ 완성 (인터페이스)
│   │   ├── TokenValidator.java    ✅ 완성
│   │   └── RefreshTokenService.java ✅ 완성
│   └── vo/                        # Value Object
│       └── MemberTokenInfo.java   ✅ 완성
├── infrastructure/                # Infrastructure Layer
│   └── BlacklistTokenStorage.java ✅ 완성 (Redis 구현)
└── presentation/                  # Presentation Layer
    └── controller/                # REST API Controller
        └── AuthApiController.java ✅ 완성 (3개 엔드포인트)
```

### Global Security 구현체 (의존성 역전)
```
com.anyang.maruni.global.security/
├── JWTUtil.java                   ✅ TokenManager 구현체
├── JwtTokenService.java           ✅ TokenService 구현체
├── AuthenticationEventHandler.java ✅ 인터페이스
├── JwtAuthenticationFilter.java   ✅ Spring Security 필터
└── LoginFilter.java               ✅ 로그인 처리 필터
```

### 주요 의존성
```java
// Application Service 의존성
- TokenManager: JWT 토큰 생성/검증 (Global JWTUtil로 구현)
- TokenService: 토큰 발급/재발급 (Global JwtTokenService로 구현)
- TokenValidator: 도메인 기반 토큰 검증
- RefreshTokenService: Refresh Token 도메인 서비스
- TokenBlacklistRepository: 블랙리스트 관리 (Infrastructure 구현)
```

## 🔐 핵심 기능 구현

### 1. JWT 이중 토큰 시스템

#### 토큰 종류와 역할
```java
// Access Token: 짧은 수명, API 접근용
- 수명: 1시간 (설정 가능)
- 저장: HTTP 헤더 (Authorization: Bearer)
- 용도: 모든 API 호출시 인증

// Refresh Token: 긴 수명, Access Token 재발급용
- 수명: 24시간 (설정 가능)
- 저장: HttpOnly 쿠키 + Redis
- 용도: Access Token 재발급
```

#### TokenManager 인터페이스 (DDD 의존성 역전)
```java
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
    long getAccessTokenExpiration();
}
```

### 2. 토큰 발급 시스템

#### TokenService 인터페이스
```java
public interface TokenService {
    /**
     * 로그인 시 Access + Refresh Token 모두 발급
     */
    void issueTokens(HttpServletResponse response, MemberTokenInfo memberInfo);

    /**
     * Access Token만 재발급 (일반적인 갱신)
     */
    void reissueAccessToken(HttpServletResponse response, String memberId, String email);

    /**
     * Access + Refresh Token 모두 재발급 (보안 강화)
     */
    void reissueAllTokens(HttpServletResponse response, String memberId, String email);

    /**
     * 로그아웃 시 Refresh Token 쿠키 만료
     */
    void expireRefreshCookie(HttpServletResponse response);
}
```

#### 실제 토큰 발급 플로우
```java
// JwtTokenService (Global 구현체)
public void issueTokens(HttpServletResponse response, MemberTokenInfo memberInfo) {
    String memberId = memberInfo.memberId();

    // 1. JWT 토큰 생성
    String accessToken = jwtUtil.createAccessToken(memberId, memberInfo.email());
    String refreshToken = jwtUtil.createRefreshToken(memberId, memberInfo.email());

    // 2. Refresh Token을 Redis에 저장 (TTL 설정)
    saveRefreshTokenWithTtl(memberId, refreshToken);

    // 3. HTTP 응답 설정
    setAccessToken(response, accessToken);      // Authorization 헤더
    setRefreshCookie(response, refreshToken);   // HttpOnly 쿠키

    log.info("Access / Refresh 토큰 발급 완료 - Member: {}", memberInfo.email());
}
```

### 3. 토큰 검증 시스템

#### TokenValidator 도메인 서비스
```java
@Service
public class TokenValidator {
    /**
     * Refresh Token의 종합적 검증
     */
    public TokenValidationResult validateRefreshToken(String refreshToken) {
        // 1. JWT 형식 및 만료시간 검증
        if (!tokenManager.isRefreshToken(refreshToken)) {
            return TokenValidationResult.invalid("Invalid or expired refresh token");
        }

        // 2. 토큰에서 사용자 ID 추출
        String memberId = tokenManager.getId(refreshToken).orElse(null);
        if (memberId == null) {
            return TokenValidationResult.invalid("Invalid token payload");
        }

        // 3. Redis 저장된 토큰과 일치 여부 확인
        if (!refreshTokenService.isValidTokenForMember(memberId, refreshToken)) {
            return TokenValidationResult.invalid("Token not found or mismatched");
        }

        // 4. 검증 성공
        String email = tokenManager.getEmail(refreshToken).orElse(null);
        return TokenValidationResult.valid(memberId, email);
    }

    /**
     * Access Token의 블랙리스트 검증
     */
    public boolean isValidAccessToken(String accessToken) {
        // 1. JWT 형식 및 만료 시간 검증
        if (!tokenManager.isAccessToken(accessToken)) {
            return false;
        }

        // 2. 블랙리스트 확인
        return !tokenBlacklistRepository.isTokenBlacklisted(accessToken);
    }
}
```

### 4. 로그아웃 및 보안 처리

#### 로그아웃 플로우
```java
// AuthenticationService
public void logout(HttpServletRequest request, HttpServletResponse response) {
    // 1. Refresh Token 삭제 (Redis에서 제거)
    tokenManager.extractRefreshToken(request)
        .filter(tokenManager::isRefreshToken)
        .flatMap(tokenManager::getId)
        .ifPresent(memberId -> {
            refreshTokenService.revokeToken(memberId);
            log.info("Refresh token deleted for member: {}", memberId);
        });

    // 2. Refresh Token 쿠키 만료
    tokenService.expireRefreshCookie(response);

    // 3. Access Token 블랙리스트 추가
    tokenManager.extractAccessToken(request)
        .filter(tokenManager::isAccessToken)
        .ifPresent(accessToken -> {
            tokenManager.getExpiration(accessToken).ifPresent(expiration ->
                tokenBlacklistRepository.addToBlacklist(accessToken, expiration)
            );
        });

    log.info("Logout completed");
}
```

## 📊 엔티티 설계

### RefreshToken 엔티티 (Redis)
```java
@RedisHash(value = "refreshToken")
public class RefreshToken {
    @Id
    private String memberId;        // 회원 ID (Primary Key)

    private String token;           // 실제 Refresh Token 값

    @TimeToLive
    private Long ttl;              // 자동 만료 시간 (초)
}
```

### MemberTokenInfo VO (Value Object)
```java
/**
 * 토큰 발급에 필요한 회원 정보를 담는 Value Object
 * 도메인 간 의존성을 분리하여 Auth 도메인의 순수성을 보장
 */
public record MemberTokenInfo(String memberId, String email) {

    public static MemberTokenInfo of(Long memberId, String email) {
        return new MemberTokenInfo(memberId.toString(), email);
    }

    public static MemberTokenInfo of(String memberId, String email) {
        return new MemberTokenInfo(memberId, email);
    }
}
```

## 🔍 Repository 구현

### RefreshTokenRepository (Spring Data Redis)
```java
// Redis 기반 Refresh Token 저장소
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    // Redis의 자동 TTL 관리로 만료된 토큰은 자동 삭제
    // Primary Key가 memberId이므로 회원당 하나의 Refresh Token만 유지
}
```

### TokenBlacklistRepository (인터페이스)
```java
public interface TokenBlacklistRepository {
    /**
     * Access Token을 블랙리스트에 추가
     */
    void addToBlacklist(String accessToken, long expirationMillis);

    /**
     * Access Token이 블랙리스트에 존재하는지 확인
     */
    boolean isTokenBlacklisted(String accessToken);
}
```

### BlacklistTokenStorage (Infrastructure 구현체)
```java
@Component
public class BlacklistTokenStorage implements TokenBlacklistRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    public void addToBlacklist(String accessToken, long expirationMillis) {
        String key = BLACKLIST_PREFIX + accessToken;
        // 토큰 만료 시간만큼만 Redis에 저장 (자동 만료)
        redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(expirationMillis));
        log.info("Token added to blacklist with expiry {}ms", expirationMillis);
    }

    public boolean isTokenBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;
        return redisTemplate.hasKey(key);
    }
}
```

## 🌐 REST API 구현

### AuthApiController (3개 엔드포인트)
```java
@RestController
@RequestMapping("/api/auth")
@AutoApiResponse
@Tag(name = "인증 API", description = "JWT 토큰 관리")
public class AuthApiController {

    // 1. Access Token 재발급 (일반적인 갱신)
    @PostMapping("/token/refresh")
    @SuccessCodeAnnotation(SuccessCode.MEMBER_TOKEN_REISSUE_SUCCESS)
    public TokenResponse refreshAccessToken(
        HttpServletRequest request,
        HttpServletResponse response) {

        return authenticationService.refreshAccessToken(request, response);
    }

    // 2. Access + Refresh Token 모두 재발급 (보안 강화)
    @PostMapping("/token/refresh/full")
    @SuccessCodeAnnotation(SuccessCode.MEMBER_TOKEN_REISSUE_FULL_SUCCESS)
    public TokenResponse refreshAllTokens(
        HttpServletRequest request,
        HttpServletResponse response) {

        return authenticationService.refreshAllTokens(request, response);
    }

    // 3. 로그아웃
    @PostMapping("/logout")
    @SuccessCodeAnnotation(SuccessCode.MEMBER_LOGOUT_SUCCESS)
    public CommonApiResponse<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response) {

        authenticationService.logout(request, response);
        return CommonApiResponse.success(SuccessCode.MEMBER_LOGOUT_SUCCESS);
    }
}
```

## 📝 DTO 계층

### TokenResponse
```java
@Schema(description = "토큰 응답")
public class TokenResponse {
    @Schema(description = "Access Token", example = "Bearer eyJhbGciOiJIUzI1NiIs...")
    private final String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private final String tokenType;

    @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
    private final Long expiresIn;

    @Schema(description = "Refresh Token 포함 여부", example = "true")
    private final boolean refreshTokenIncluded;

    // 정적 팩토리 메서드
    public static TokenResponse accessOnly(String accessToken, Long expiresIn) {
        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .refreshTokenIncluded(false)
            .build();
    }

    public static TokenResponse withRefresh(String accessToken, Long expiresIn) {
        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .refreshTokenIncluded(true)
            .build();
    }
}
```

## 🔗 도메인 간 연동

### Member 도메인 연동
```java
// LoginFilter에서 로그인 성공 시 토큰 발급
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        MemberTokenInfo memberInfo = MemberTokenInfo.of(
            userDetails.getMemberId(),
            userDetails.getEmail()
        );

        // AuthenticationService를 통한 토큰 발급
        authenticationEventHandler.handleLoginSuccess(response, memberInfo);
    }
}
```

### Spring Security 연동
```java
// JwtAuthenticationFilter에서 Access Token 검증
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) {

        String accessToken = jwtUtil.extractAccessToken(request).orElse(null);

        // TokenValidator를 통한 토큰 검증
        if (accessToken != null && tokenValidator.isValidAccessToken(accessToken)) {
            // Spring Security 인증 객체 생성
            setAuthenticationContext(accessToken);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Global Configuration 연동
```java
// SecurityConfig에서 JWT 필터 등록
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JWT 필터 체인 구성
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

## ⚙️ 설정 및 운영

### JWT 설정 (application.yml)
```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}  # 최소 32자 이상
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}   # 1시간
  refresh-token:
    expiration: ${JWT_REFRESH_EXPIRATION:86400000} # 24시간

# 쿠키 보안 설정
cookie:
  secure: ${COOKIE_SECURE:false}  # Production에서는 true
```

### 환경 변수 (.env)
```bash
# JWT 보안 설정
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000   # 1시간 (밀리초)
JWT_REFRESH_EXPIRATION=86400000 # 24시간 (밀리초)

# 쿠키 보안 (Production)
COOKIE_SECURE=true
```

### Redis 설정
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 📈 보안 특성

### 실제 운영 보안 지표
- ✅ **이중 토큰 시스템**: Access(짧은 수명) + Refresh(긴 수명) 분리 보안
- ✅ **HttpOnly 쿠키**: XSS 공격 방지를 위한 Refresh Token 저장
- ✅ **토큰 블랙리스트**: 로그아웃된 Access Token 무효화 처리
- ✅ **Redis TTL**: 자동 만료를 통한 토큰 생명주기 관리
- ✅ **JWT 서명 검증**: HMAC-SHA256 기반 무결성 보장
- ✅ **CSRF 보호**: SameSite 쿠키 속성으로 CSRF 공격 방지

### 보안 시나리오 대응
```java
// 1. Access Token 탈취 시
- 짧은 수명 (1시간)으로 피해 최소화
- 로그아웃 시 블랙리스트 추가로 즉시 무효화

// 2. Refresh Token 탈취 시
- HttpOnly 쿠키로 JavaScript 접근 차단
- Redis 저장으로 서버 측 revoke 가능
- 재발급 시 기존 토큰 자동 무효화

// 3. 동시 로그인 제어
- 회원당 하나의 Refresh Token만 유지
- 새 로그인 시 기존 토큰 자동 교체
```

## 🎯 Claude Code 작업 가이드

### 향후 확장 시 주의사항
1. **토큰 만료 시간 조정**: 보안과 사용성의 균형 고려
2. **Redis 메모리 관리**: TTL 설정으로 메모리 사용량 최적화
3. **블랙리스트 성능**: 대용량 환경에서 Redis 성능 모니터링 필요
4. **도메인 인터페이스 유지**: Global 구현체 변경 시 인터페이스 계약 준수

### DDD 의존성 구조 유지
```java
// ✅ 올바른 의존성 방향
Domain Interface ← Global Implementation
    ↑                      ↑
Application Service    Infrastructure
```

### API 사용 예시
```bash
# 1. 로그인 (LoginFilter에서 자동 처리)
POST /api/members/login
{
  "email": "user@example.com",
  "password": "password123"
}
# Response: Authorization 헤더 + refresh 쿠키 자동 설정

# 2. Access Token 재발급
POST /api/auth/token/refresh
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
# Response: 새로운 Authorization 헤더

# 3. 전체 토큰 재발급 (보안 강화)
POST /api/auth/token/refresh/full
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
# Response: 새로운 Authorization 헤더 + 새로운 refresh 쿠키

# 4. 로그아웃
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Cookie: refresh=eyJhbGciOiJIUzI1NiIs...
# Response: 토큰 무효화 완료
```

### 문제 해결 가이드
```java
// Token 검증 실패 시
1. JWT 형식 오류 → TokenManager.isAccessToken() 확인
2. 토큰 만료 → 재발급 API 호출
3. 블랙리스트 토큰 → 재로그인 필요
4. Redis 연결 오류 → 인프라 상태 확인

// Refresh Token 오류 시
1. 쿠키 누락 → 로그인 상태 확인
2. Redis 토큰 불일치 → 재로그인 필요
3. TTL 만료 → 재로그인 필요
```

**Auth 도메인은 MARUNI의 모든 보안 요구사항을 만족하는 완성된 인증/인가 시스템입니다. DDD 의존성 역전 원칙을 완벽히 적용하여 도메인 순수성을 보장하면서도 실제 운영 환경에서 요구되는 모든 보안 기능을 구현했습니다.** 🔐