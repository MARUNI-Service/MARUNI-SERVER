# Auth 도메인 구현 가이드 (2025-10-05 단순화 완료)

## 📐 아키텍처 구조

### DDD 패키지 구조
```
com.anyang.maruni.domain.auth/
├── application/                    # Application Layer
│   └── service/
│       └── AuthenticationService.java ✅ 로그인 성공 시 토큰 발급
├── domain/                        # Domain Layer
│   ├── service/
│   │   └── TokenManager.java      ✅ 인터페이스 (DDD 의존성 역전)
│   └── vo/
│       └── MemberTokenInfo.java   ✅ Value Object
└── infrastructure/                # (비어있음 - Redis 제거됨)
```

### Global Security 구현체 (의존성 역전)
```
com.anyang.maruni.global.security/
├── JWTUtil.java                   ✅ TokenManager 구현체
├── AuthenticationEventHandler.java ✅ 인터페이스
├── JwtAuthenticationFilter.java   ✅ Spring Security 필터
└── LoginFilter.java               ✅ 로그인 처리 필터
```

### 주요 의존성
```java
// Application Service 의존성
AuthenticationService:
  - TokenManager: JWT 토큰 생성 (Global JWTUtil로 구현)

// Global Security
JwtAuthenticationFilter:
  - JWTUtil: 토큰 추출 및 검증
  - CustomUserDetailsService: 사용자 정보 로드
```

## 🔐 핵심 기능 구현

### 1. Access Token 단일 토큰 시스템

#### 토큰 특성
```java
// Access Token: Stateless JWT
- 수명: 1시간 (설정 가능)
- 저장: HTTP 헤더 (Authorization: Bearer)
- 용도: 모든 API 호출 시 인증
- 특징: 서버 상태 저장 없음, 클라이언트가 관리
```

#### TokenManager 인터페이스 (DDD 의존성 역전)
```java
// 위치: src/main/java/com/anyang/maruni/domain/auth/domain/service/TokenManager.java
public interface TokenManager {
    // 토큰 생성
    String createAccessToken(String memberId, String email);

    // 토큰 추출
    Optional<String> extractAccessToken(HttpServletRequest request);

    // 토큰 정보 추출
    Optional<String> getEmail(String token);

    // 토큰 검증
    boolean isAccessToken(String token);
}
```

### 2. 토큰 발급 시스템

#### AuthenticationService (로그인 성공 처리)
```java
// 위치: src/main/java/com/anyang/maruni/domain/auth/application/service/AuthenticationService.java
@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticationEventHandler {

    private final TokenManager tokenManager;

    @Override
    public void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo) {
        // Access Token 발급
        String accessToken = tokenManager.createAccessToken(
            memberInfo.memberId(),
            memberInfo.email()
        );

        // HTTP 응답 헤더에 설정
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setContentType("application/json; charset=UTF-8");

        log.info("✅ Access Token 발급 완료 - Member: {}", memberInfo.email());
    }
}
```

### 3. 토큰 검증 시스템

#### JwtAuthenticationFilter (Spring Security)
```java
// 위치: src/main/java/com/anyang/maruni/global/security/JwtAuthenticationFilter.java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) {

        // 1. Authorization 헤더에서 Access Token 추출
        jwtUtil.extractAccessToken(request)
            .filter(jwtUtil::isAccessToken)  // 2. JWT 형식 및 만료 검증
            .flatMap(jwtUtil::getEmail)      // 3. 이메일 추출
            .ifPresent(email -> {
                try {
                    // 4. UserDetails 로드
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // 5. Spring Security 인증 객체 생성
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("JWT 인증 성공: {}", email);
                } catch (Exception e) {
                    log.warn("UserDetails 로딩 실패: {}", e.getMessage());
                }
            });

        filterChain.doFilter(request, response);
    }
}
```

### 4. JWT 구현체 (JWTUtil)

#### 주요 메서드
```java
// 위치: src/main/java/com/anyang/maruni/global/security/JWTUtil.java
@Component
public class JWTUtil implements TokenManager {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    // Access Token 생성
    public String createAccessToken(String id, String email) {
        return Jwts.builder()
            .claim("type", "access")
            .claim("id", id)
            .claim("email", email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() +
                jwtProperties.getAccessToken().getExpiration()))
            .signWith(secretKey)
            .compact();
    }

    // 토큰 검증
    public boolean isAccessToken(String token) {
        return safelyParseClaims(token)
            .map(claims -> "access".equals(claims.get("type", String.class)))
            .orElse(false);
    }

    // 이메일 추출
    public Optional<String> getEmail(String token) {
        return safelyParseClaims(token)
            .map(claims -> claims.get("email", String.class));
    }

    // 헤더에서 토큰 추출
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
            .filter(header -> header.startsWith("Bearer "))
            .map(header -> header.substring(7));
    }

    // 안전한 파싱
    private Optional<Claims> safelyParseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed");
            return Optional.empty();
        }
    }
}
```

## 📊 Value Object

### MemberTokenInfo
```java
// 위치: src/main/java/com/anyang/maruni/domain/auth/domain/vo/MemberTokenInfo.java
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

## 🔗 도메인 간 연동

### Member 도메인 연동 (로그인)
```java
// LoginFilter에서 로그인 성공 시 토큰 발급
// 위치: src/main/java/com/anyang/maruni/global/security/LoginFilter.java
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationEventHandler authenticationEventHandler;

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 회원 정보를 Value Object로 변환
        MemberTokenInfo memberTokenInfo = MemberTokenInfo.of(
            userDetails.getMember().getId(),
            userDetails.getMember().getMemberEmail()
        );

        // AuthenticationService를 통한 토큰 발급
        authenticationEventHandler.handleLoginSuccess(response, memberTokenInfo);

        log.info("로그인 성공 - 사용자: {}", userDetails.getUsername());
    }
}
```

### Spring Security 연동
```java
// SecurityConfig에서 필터 체인 구성
// 위치: src/main/java/com/anyang/maruni/global/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(loginFilter(),
                UsernamePasswordAuthenticationFilter.class)

            // 기본 설정
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(publicUrls).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

## ⚙️ 설정

### JWT 설정 (application-security.yml)
```yaml
# 위치: src/main/resources/application-security.yml
jwt:
  secret-key: ${JWT_SECRET_KEY:your_jwt_secret_key_at_least_32_characters}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}  # 1시간 (밀리초)
```

### 환경 변수 (.env)
```bash
# JWT 보안 설정
JWT_SECRET_KEY=c29tZS1yYW5kb20tc2VjcmV0LWtleS1mb3Itand0LXNlY3JldC1rZXktY2hhbmdlLW1lLWxhdGVyCg
JWT_ACCESS_EXPIRATION=3600000   # 1시간 (밀리초)
```

## 📈 보안 특성

### Stateless JWT의 장점
- ✅ **서버 확장성**: 상태 저장 없이 수평 확장 가능
- ✅ **단순한 구조**: Redis, DB 의존성 없음
- ✅ **빠른 검증**: 서명 검증만으로 인증 완료
- ✅ **표준 기반**: JWT RFC 7519 준수

### 보안 고려사항
```java
// 1. 토큰 탈취 시
- 짧은 수명 (1시간)으로 피해 최소화
- HTTPS 사용 필수 (프로덕션 환경)

// 2. XSS 공격 방지
- localStorage 대신 메모리에 토큰 저장 권장
- HttpOnly 쿠키 사용 가능 (필요 시)

// 3. CSRF 방어
- Custom Header (Authorization) 사용으로 자동 방어
```

## 🎯 인증 플로우

### 전체 인증 흐름
```
1. 로그인 요청
   POST /api/auth/login
   { "email": "user@example.com", "password": "password123" }
   ↓
2. LoginFilter에서 인증 처리
   - Spring Security AuthenticationManager로 인증
   - 성공 시 AuthenticationService.handleLoginSuccess() 호출
   ↓
3. Access Token 발급
   - JWTUtil.createAccessToken()으로 JWT 생성
   - Response Header에 "Authorization: Bearer {token}" 설정
   ↓
4. 클라이언트가 토큰 저장
   - 메모리 또는 localStorage에 저장
   ↓
5. API 요청 시 토큰 전송
   GET /api/users/me
   Headers: { "Authorization": "Bearer eyJhbGci..." }
   ↓
6. JwtAuthenticationFilter에서 검증
   - 헤더에서 토큰 추출
   - JWT 서명 및 만료 검증
   - 이메일로 UserDetails 로드
   - Spring Security Context에 인증 정보 설정
   ↓
7. Controller에서 인증 정보 사용
   @AuthenticationPrincipal CustomUserDetails userDetails
```

### 로그아웃 처리
```
클라이언트 측 처리:
- 메모리/localStorage에서 토큰 삭제
- 로그인 페이지로 리다이렉트

서버 측 처리:
- 없음 (Stateless이므로 서버에서 관리할 상태 없음)
- 토큰은 만료 시간까지 유효하나, 클라이언트가 삭제하면 사용 불가
```

## 🎯 Claude Code 작업 가이드

### 향후 확장 가능성
```java
// 필요 시 추가 가능한 기능들:
1. Refresh Token 추가
   - 긴 수명의 토큰으로 Access Token 재발급
   - Redis 또는 DB에 저장

2. 토큰 블랙리스트
   - 로그아웃된 토큰 무효화
   - Redis에 만료 시간까지 저장

3. 다중 기기 로그인 관리
   - 기기별 세션 추적
   - 선택적 로그아웃 기능

현재 구조는 이러한 확장을 쉽게 지원할 수 있도록 설계됨
```

### DDD 의존성 구조 유지
```java
// ✅ 올바른 의존성 방향
Domain Interface (TokenManager)
    ↑
Application Service (AuthenticationService)
    ↑
Presentation (Controller)

Global Implementation (JWTUtil) → Domain Interface 구현
```

### API 사용 예시
```bash
# 1. 로그인
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

# Response
HTTP/1.1 200 OK
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "code": "MEMBER_LOGIN_SUCCESS",
  "message": "로그인 성공"
}

# 2. 인증이 필요한 API 호출
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Response
{
  "code": "MEMBER_VIEW",
  "message": "회원 조회 성공",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동"
  }
}

# 3. 로그아웃 (클라이언트 측)
// JavaScript 예시
localStorage.removeItem('access_token');
// 또는
sessionStorage.removeItem('access_token');
window.location.href = '/login';
```

### 문제 해결 가이드
```java
// 토큰 검증 실패 시
1. "Invalid or expired token"
   → 토큰 만료: 재로그인 필요
   → JWT 형식 오류: 토큰 값 확인

2. "JWT validation failed"
   → 서명 불일치: SECRET_KEY 확인
   → 만료된 토큰: 재로그인

3. "UserDetails 로딩 실패"
   → 사용자가 DB에 없음: 회원 탈퇴 또는 삭제됨
   → DB 연결 오류: 인프라 상태 확인
```

## 📝 주요 파일 위치

```
인증 관련 핵심 파일:
├── domain/auth/application/service/
│   └── AuthenticationService.java          # 토큰 발급
├── domain/auth/domain/service/
│   └── TokenManager.java                    # 인터페이스
├── domain/auth/domain/vo/
│   └── MemberTokenInfo.java                 # Value Object
├── global/security/
│   ├── JWTUtil.java                         # TokenManager 구현
│   ├── JwtAuthenticationFilter.java         # 토큰 검증 필터
│   ├── LoginFilter.java                     # 로그인 필터
│   └── AuthenticationEventHandler.java      # 이벤트 인터페이스
├── global/config/
│   ├── SecurityConfig.java                  # Security 설정
│   └── JwtSecurityConfig.java               # JWT Bean 설정
└── global/config/properties/
    └── JwtProperties.java                   # JWT 설정 클래스
```

**Auth 도메인은 MARUNI의 인증 요구사항을 만족하는 단순하고 명확한 시스템입니다. Stateless JWT 기반으로 확장 가능하며, 필요 시 Refresh Token이나 블랙리스트 등의 기능을 쉽게 추가할 수 있습니다.** 🔐
