# 보안 구현 가이드

**MARUNI JWT 인증 및 보안 설정 가이드**

---

## 🔐 JWT 인증 시스템

### **JWT 토큰 구조**
```yaml
Access Token:
  - 만료시간: 1시간 (3,600,000ms)
  - 용도: API 호출 인증
  - 저장위치: 클라이언트 메모리

Refresh Token:
  - 만료시간: 14일 (1,209,600,000ms)
  - 용도: Access Token 재발급
  - 저장위치: Redis (서버 관리)
```

### **JWT 설정**
```yaml
# application.yml
jwt:
  secret-key: ${JWT_SECRET_KEY:your_jwt_secret_key_at_least_32_characters}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}
  refresh-token:
    expiration: ${JWT_REFRESH_EXPIRATION:1209600000}
```

### **환경변수 설정**
```env
# .env 파일
JWT_SECRET_KEY=your_super_secret_jwt_key_at_least_32_characters_long_for_security
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=1209600000
```

---

## 🛡️ JWT 구현

### **JWTUtil 클래스**
```java
@Component
public class JWTUtil {
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;

    // JWT 생성
    public String generateAccessToken(MemberTokenInfo memberInfo) {
        Date expiration = new Date(System.currentTimeMillis() + accessTokenExpiration);

        return Jwts.builder()
            .subject(memberInfo.getEmail())
            .claim("id", memberInfo.getId())
            .claim("name", memberInfo.getName())
            .issuedAt(new Date())
            .expiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // JWT 검증
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### **JWT 인증 필터**
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtUtil.validateToken(token);
                setAuthentication(claims);
            } catch (BaseException e) {
                logger.warn("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
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
    private final LoginFilter loginFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api-docs", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/join").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // Protected endpoints
                .anyRequest().authenticated())

            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, LoginFilter.class)
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

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### **로그인 필터**
```java
@Component
public class LoginFilter extends AbstractAuthenticationProcessingFilter {
    private final ObjectMapper objectMapper;
    private final JwtTokenService jwtTokenService;

    public LoginFilter(AuthenticationManager authManager,
                      ObjectMapper objectMapper,
                      JwtTokenService jwtTokenService) {
        super("/api/login", authManager);
        this.objectMapper = objectMapper;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response) {
        try {
            MemberLoginRequest loginRequest = objectMapper.readValue(
                request.getInputStream(), MemberLoginRequest.class);

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getMemberEmail(),
                    loginRequest.getMemberPassword());

            return getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException("로그인 요청 파싱 실패", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authResult) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        TokenResponse tokenResponse = jwtTokenService.generateTokens(userDetails.getMember());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
            CommonApiResponse.success(SuccessCode.LOGIN_SUCCESS, tokenResponse));
    }
}
```

---

## 🔐 데이터 암호화

### **EncryptionService**
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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
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
            token.substring(0, 10) + "...", reason);
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
- [ ] Refresh Token Redis 저장
- [ ] 토큰 블랙리스트 관리

### **비밀번호 보안**
- [ ] BCrypt 암호화 (strength 10 이상)
- [ ] 비밀번호 복잡성 검증
- [ ] 평문 비밀번호 로깅 금지
- [ ] 비밀번호 변경 시 기존 토큰 무효화

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

---

**Version**: v1.0.0 | **Updated**: 2025-09-16