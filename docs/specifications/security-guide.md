# 보안 가이드

**MARUNI JWT Stateless 인증 및 보안 설정**

## 🔐 JWT 인증 시스템

### Access Token Only (Stateless)
```
Token: Access Token (1시간 유효)
Storage: 클라이언트 (localStorage/sessionStorage/메모리)
Server: Stateless (세션 저장소 없음)
Logout: 클라이언트에서 토큰 삭제
```

### 환경 변수
```env
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000  # 1시간 (밀리초)
```

### application.yml
```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION}
```

## 🛡️ 주요 컴포넌트

### 1. JWTUtil (TokenManager 구현체)
```java
// 위치: global/security/JWTUtil.java
@Component
public class JWTUtil implements TokenManager {
    
    // Access Token 생성
    public String createAccessToken(String memberId, String email) {
        return Jwts.builder()
            .claim("type", "access")
            .claim("id", memberId)
            .claim("email", email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpiration))
            .signWith(secretKey, HS256)
            .compact();
    }
    
    // 토큰 검증
    public boolean isAccessToken(String token) {
        return "access".equals(getClaim(token, "type"));
    }
    
    // 이메일 추출
    public Optional<String> getEmail(String token) {
        return Optional.of(getClaim(token, "email"));
    }
}
```

### 2. JwtAuthenticationFilter
```java
// 위치: global/security/JwtAuthenticationFilter.java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(...) {
        // 1. Authorization 헤더에서 토큰 추출
        jwtUtil.extractAccessToken(request)
            .filter(jwtUtil::isAccessToken)
            .flatMap(jwtUtil::getEmail)
            .ifPresent(email -> {
                // 2. UserDetails 로드 및 인증 객체 생성
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        
        filterChain.doFilter(request, response);
    }
}
```

### 3. LoginFilter
```java
// 위치: global/security/LoginFilter.java
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    
    private final AuthenticationEventHandler authHandler;
    
    @Override
    protected void successfulAuthentication(..., Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        
        // 회원 정보를 Value Object로 변환
        MemberTokenInfo tokenInfo = MemberTokenInfo.of(
            user.getMember().getId(),
            user.getMember().getMemberEmail()
        );
        
        // Access Token 발급
        authHandler.handleLoginSuccess(response, tokenInfo);
    }
}
```

### 4. SecurityConfig
```java
// 위치: global/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(loginFilter(), 
                UsernamePasswordAuthenticationFilter.class)
            
            // 기본 설정
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(STATELESS))
            
            // 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/join/**", "/api/auth/login").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

## 🔄 인증 플로우

### 로그인
```
1. POST /api/auth/login (email, password)
2. LoginFilter: Spring Security 인증
3. AuthenticationService: Access Token 발급
4. Response: Authorization: Bearer {token}
5. 클라이언트: 토큰 저장
```

### API 요청
```
1. Request: Authorization: Bearer {token}
2. JwtAuthenticationFilter: 토큰 추출 및 검증
3. CustomUserDetailsService: 사용자 정보 로드
4. SecurityContext: 인증 정보 설정
5. Controller: @AuthenticationPrincipal로 사용자 접근
```

### 로그아웃
```
클라이언트 측:
- localStorage.removeItem('access_token')
- window.location.href = '/login'

서버 측:
- 없음 (Stateless)
```

## 🔒 보안 원칙

### 비밀번호 보안
```java
// BCryptPasswordEncoder 사용
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// 사용 예시
String encoded = passwordEncoder.encode(rawPassword);
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### JWT 보안
- ✅ HMAC-SHA256 서명
- ✅ Secret Key 최소 32자 이상
- ✅ 토큰 만료 시간 설정 (1시간)
- ✅ HTTPS 사용 (프로덕션 환경)

### API 보안
- ✅ 모든 API JWT 인증 필수
- ✅ 본인 정보만 접근 가능 (JWT에서 ID 추출)
- ✅ Bean Validation 입력 검증
- ✅ SQL Injection 방지 (JPA 사용)
- ✅ CSRF 자동 방어 (Custom Header)

## 🎯 Controller에서 인증 정보 사용

### 현재 사용자 정보 가져오기
```java
@RestController
@RequestMapping("/api/users")
public class MemberApiController {
    
    // 방법 1: @AuthenticationPrincipal
    @GetMapping("/me")
    public MemberResponse getMyInfo(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        String email = userDetails.getUsername();
        Long memberId = userDetails.getMember().getId();
        
        return memberService.getMyInfo(email);
    }
    
    // 방법 2: SecurityContextHolder (비추천)
    @GetMapping("/profile")
    public MemberResponse getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        
        return memberService.getMyInfo(user.getUsername());
    }
}
```

## ⚠️ 주의사항

### Stateless의 의미
- 서버는 로그인 상태를 저장하지 않음
- 토큰이 유효하면 항상 인증 성공
- 로그아웃은 클라이언트에서 토큰 삭제로 처리
- 토큰 탈취 시 만료 전까지 유효 (1시간)

### 보안 강화 방안
```java
// 향후 추가 가능한 기능:
1. Refresh Token 추가 (긴 수명)
2. 토큰 블랙리스트 (Redis)
3. 다중 기기 로그인 관리
4. IP 주소 검증
5. Rate Limiting
```

---

**상세 구현은 `docs/domains/auth.md`를 참조하세요.**
