# 보안 구현 가이드

**MARUNI JWT 인증 및 보안 설정 가이드**

---

## 🔐 JWT 인증 시스템

### **JWT 토큰 구조**
```yaml
Access Token:
  - 만료시간: 1시간 (3,600,000ms)
  - 용도: API 호출 인증
  - 저장위치: 클라이언트 (로컬 스토리지, 세션 스토리지, 메모리)
  - 특징: Stateless (서버 측 저장소 없음)
```

### **JWT 설정**
```yaml
# application.yml
jwt:
  secret-key: ${JWT_SECRET_KEY:your_jwt_secret_key_at_least_32_characters}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}
```

### **환경변수 설정**
```env
# .env 파일
JWT_SECRET_KEY=your_super_secret_jwt_key_at_least_32_characters_long_for_security
JWT_ACCESS_EXPIRATION=3600000
```

---

## 🛡️ JWT 구현

### **JWTUtil 클래스** (TokenManager 구현체)
```java
@Component
@Slf4j
public class JWTUtil implements TokenManager {
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private Long accessExpiration;

    // Access Token 생성
    @Override
    public String createAccessToken(String memberId, String email) {
        return createJWT("access", email, accessExpiration);
    }

    // JWT 토큰 생성 (HMAC-SHA256)
    private String createJWT(String category, String email, Long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .claim("category", category)
            .claim("email", email)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    // Authorization 헤더에서 토큰 추출
    @Override
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return Optional.of(authorizationHeader.substring(7));
        }
        return Optional.empty();
    }

    // 토큰에서 이메일 추출
    @Override
    public Optional<String> getEmail(String token) {
        return safelyParseClaims(token)
            .map(claims -> claims.get("email", String.class));
    }

    // Access Token 여부 확인
    @Override
    public boolean isAccessToken(String token) {
        return safelyParseClaims(token)
            .map(claims -> "access".equals(claims.get("category", String.class)))
            .orElse(false);
    }

    // JWT 검증 및 파싱
    private Optional<Claims> safelyParseClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            return Optional.of(Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload());
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ JWT 만료: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ JWT 검증 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
```

### **TokenManager 인터페이스** (DDD 도메인 인터페이스)
```java
public interface TokenManager {
    /**
     * Access Token 생성
     * @param memberId 회원 ID
     * @param email 회원 이메일
     * @return JWT Access Token
     */
    String createAccessToken(String memberId, String email);

    /**
     * HTTP 요청에서 Access Token 추출
     * @param request HTTP 요청
     * @return Access Token (Optional)
     */
    Optional<String> extractAccessToken(HttpServletRequest request);

    /**
     * 토큰에서 이메일 추출
     * @param token JWT 토큰
     * @return 이메일 (Optional)
     */
    Optional<String> getEmail(String token);

    /**
     * Access Token 여부 확인
     * @param token JWT 토큰
     * @return Access Token이면 true
     */
    boolean isAccessToken(String token);
}
```

### **JWT 인증 필터**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenManager tokenManager;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 Access Token 추출
        Optional<String> accessToken = tokenManager.extractAccessToken(request);

        if (accessToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = accessToken.get();

        // 2. Access Token 여부 검증
        if (!tokenManager.isAccessToken(token)) {
            log.warn("⚠️ Access Token이 아닌 토큰 감지");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 이메일 추출 및 인증 처리
        tokenManager.getEmail(token).ifPresent(email -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("✅ JWT 인증 성공 - 사용자: {}", email);
            }
        });

        filterChain.doFilter(request, response);
    }
}
```

---

## 🔒 Spring Security 설정

### **SecurityConfig 클래스**
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                         AuthenticationManager authenticationManager,
                                         AuthenticationEventHandler authenticationEventHandler) throws Exception {

        // LoginFilter 생성 (동적 Bean)
        LoginFilter loginFilter = new LoginFilter(
            authenticationManager,
            authenticationEventHandler
        );
        loginFilter.setFilterProcessesUrl("/api/members/login");

        return http
            .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api-docs", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/members/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // Protected endpoints
                .anyRequest().authenticated())

            // JWT 필터 체인 (순서 중요)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10); // strength 10
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization")); // 클라이언트가 Authorization 헤더 읽을 수 있도록

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
```

### **로그인 필터**
```java
@Slf4j
@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationEventHandler authenticationEventHandler;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response) throws AuthenticationException {
        try {
            // JSON 요청 본문에서 이메일/비밀번호 추출
            MemberLoginRequest loginRequest = objectMapper.readValue(
                request.getInputStream(),
                MemberLoginRequest.class
            );

            log.info("🔐 로그인 시도 - 이메일: {}", loginRequest.memberEmail());

            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    loginRequest.memberEmail(),
                    loginRequest.memberPassword()
                );

            // AuthenticationManager를 통한 인증 시도
            return this.getAuthenticationManager().authenticate(authToken);

        } catch (IOException e) {
            log.error("❌ 로그인 요청 파싱 실패", e);
            throw new RuntimeException("로그인 요청 처리 실패", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authResult) throws IOException, ServletException {

        // CustomUserDetails에서 회원 정보 추출
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        MemberEntity member = userDetails.getMember();

        MemberTokenInfo memberInfo = new MemberTokenInfo(
            member.getMemberId(),
            member.getMemberEmail()
        );

        // AuthenticationService를 통해 토큰 발급 및 응답 처리
        authenticationEventHandler.handleLoginSuccess(response, memberInfo);

        log.info("✅ 로그인 성공 - 회원: {}", member.getMemberEmail());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            AuthenticationException failed) throws IOException, ServletException {

        log.warn("❌ 로그인 실패 - 사유: {}", failed.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");

        CommonApiResponse<Void> errorResponse = CommonApiResponse.error(ErrorCode.LOGIN_FAIL);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
```

### **AuthenticationService** (로그인 성공 처리)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService implements AuthenticationEventHandler {
    private final TokenManager tokenManager;

    @Override
    public void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo) {
        // Access Token 생성
        String accessToken = tokenManager.createAccessToken(
            memberInfo.memberId(),
            memberInfo.email()
        );

        // Authorization 헤더 설정
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setContentType("application/json; charset=UTF-8");

        log.info("✅ Access Token 발급 완료 - Member: {}", memberInfo.email());
    }
}
```

---

## 🔐 데이터 암호화

### **EncryptionService** (AES-GCM 암호화)
```java
@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    @Value("${maruni.encryption.key}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, encryptedWithIv, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new EncryptionException("암호화 실패", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];

            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("복호화 실패", e);
        }
    }
}
```

---

## 🛡️ 보안 모범 사례

### **비밀번호 정책**
```java
@Service
@RequiredArgsConstructor
public class PasswordService {
    private final PasswordEncoder passwordEncoder;

    // 비밀번호 강도 검증
    public void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new BaseException(ErrorCode.WEAK_PASSWORD);
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new BaseException(ErrorCode.PASSWORD_MISSING_UPPERCASE);
        }

        if (!password.matches(".*[a-z].*")) {
            throw new BaseException(ErrorCode.PASSWORD_MISSING_LOWERCASE);
        }

        if (!password.matches(".*\\d.*")) {
            throw new BaseException(ErrorCode.PASSWORD_MISSING_NUMBER);
        }

        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new BaseException(ErrorCode.PASSWORD_MISSING_SPECIAL);
        }
    }

    // 안전한 비밀번호 인코딩
    public String encodePassword(String rawPassword) {
        validatePasswordStrength(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }
}
```

### **입력 검증**
```java
// DTO 레벨 검증
@Getter
@Setter
public class MemberSaveRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    private String memberEmail;

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2-50자여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글, 영문, 공백만 허용됩니다")
    private String memberName;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다")
    private String memberPassword;
}
```

### **API 보안 헤더**
```java
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Security Headers 추가
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");

        chain.doFilter(request, response);
    }
}
```

---

## 🔍 보안 로깅

### **보안 이벤트 로깅**
```java
@Component
@Slf4j
public class SecurityEventLogger {

    public void logLoginSuccess(String email, String clientIp) {
        log.info("LOGIN_SUCCESS: user={}, ip={}", email, clientIp);
    }

    public void logLoginFailure(String email, String clientIp, String reason) {
        log.warn("LOGIN_FAILURE: user={}, ip={}, reason={}", email, clientIp, reason);
    }

    public void logTokenValidationFailure(String token, String reason) {
        log.warn("TOKEN_VALIDATION_FAILURE: token={}, reason={}",
            token.substring(0, Math.min(10, token.length())) + "...", reason);
    }

    public void logSuspiciousActivity(String email, String activity) {
        log.error("SUSPICIOUS_ACTIVITY: user={}, activity={}", email, activity);
    }
}
```

---

## ⚡ 보안 체크리스트

### **JWT 보안**
- [ ] Secret Key 32자 이상 사용
- [ ] 환경변수로 Secret Key 관리
- [ ] Access Token 짧은 만료시간 (1시간)
- [ ] HMAC-SHA256 서명 알고리즘 사용
- [ ] 토큰 검증 실패 시 적절한 에러 처리

### **비밀번호 보안**
- [ ] BCrypt 암호화 (strength 10 이상)
- [ ] 비밀번호 복잡성 검증
- [ ] 평문 비밀번호 로깅 금지
- [ ] 비밀번호 변경 시 재로그인 요구

### **API 보안**
- [ ] 모든 엔드포인트 인증 검증
- [ ] Bean Validation 입력 검증
- [ ] SQL Injection 방지 (JPA 사용)
- [ ] XSS 방지 (응답 인코딩)
- [ ] CSRF 방지 (Stateless JWT)

### **인프라 보안**
- [ ] HTTPS 통신 (운영환경)
- [ ] CORS 정책 설정
- [ ] 보안 헤더 추가
- [ ] 민감 정보 환경변수 관리
- [ ] 로그 보안 정보 마스킹

### **클라이언트 보안**
- [ ] 토큰을 안전한 저장소에 보관 (메모리 권장)
- [ ] HTTPS를 통한 토큰 전송
- [ ] XSS 공격 방지 (입력값 sanitize)
- [ ] 로그아웃 시 토큰 완전 삭제

---

## 🔄 인증 플로우 다이어그램

### **로그인 플로우**
```
클라이언트                  LoginFilter              CustomUserDetailsService       AuthenticationService       JWTUtil
    |                           |                               |                           |                    |
    | POST /api/members/login   |                               |                           |                    |
    |-------------------------->|                               |                           |                    |
    |  {email, password}        |                               |                           |                    |
    |                           | loadUserByUsername(email)     |                           |                    |
    |                           |------------------------------>|                           |                    |
    |                           |                               | DB 조회                   |                    |
    |                           |                               |---------------------->    |                    |
    |                           |<------------------------------|                           |                    |
    |                           | UserDetails                   |                           |                    |
    |                           |                               |                           |                    |
    |                           | BCrypt 비밀번호 검증           |                           |                    |
    |                           |------------------------------>|                           |                    |
    |                           |<------------------------------|                           |                    |
    |                           | 인증 성공                      |                           |                    |
    |                           |                               |                           |                    |
    |                           | handleLoginSuccess()          |                           |                    |
    |                           |---------------------------------------------->|                    |
    |                           |                               |               |                    |
    |                           |                               |               | createAccessToken()|
    |                           |                               |               |------------------->|
    |                           |                               |               |                    | JWT 생성
    |                           |                               |               |<-------------------|
    |                           |                               |               | Access Token       |
    |                           |                               |               |                    |
    |<---------------------------------------------------------------------------| Authorization 헤더  |
    | 200 OK                    |                               |               |                    |
    | Authorization: Bearer ... |                               |               |                    |
```

### **보호된 API 호출 플로우**
```
클라이언트              JwtAuthenticationFilter         CustomUserDetailsService       Controller
    |                           |                               |                           |
    | GET /api/protected        |                               |                           |
    | Authorization: Bearer ... |                               |                           |
    |-------------------------->|                               |                           |
    |                           | extractAccessToken()          |                           |
    |                           | isAccessToken()               |                           |
    |                           | getEmail()                    |                           |
    |                           |                               |                           |
    |                           | loadUserByUsername(email)     |                           |
    |                           |------------------------------>|                           |
    |                           |<------------------------------|                           |
    |                           | UserDetails                   |                           |
    |                           |                               |                           |
    |                           | SecurityContext 설정          |                           |
    |                           |-------------------------------------------------------------->|
    |                           |                               |                           | 비즈니스 로직 실행
    |<--------------------------------------------------------------------------| 200 OK
```

---

**Version**: v2.0.0 | **Updated**: 2025-10-05 | **Status**: Simplified JWT Authentication (Access Token Only)
