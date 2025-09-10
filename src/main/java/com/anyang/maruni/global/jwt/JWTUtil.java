package com.anyang.maruni.global.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JWTUtil {

	public static final String CLAIM_ID = "id";
	public static final String CLAIM_EMAIL = "email";
	public static final String CLAIM_TYPE = "type";
	public static final String TOKEN_TYPE_ACCESS = "access";
	public static final String TOKEN_TYPE_REFRESH = "refresh";
	public static final String COOKIE_NAME_REFRESH = "refresh";

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.access-token-expiration:3600000}")
	private long accessTokenExpiration;

	@Value("${jwt.refresh-token-expiration:86400000}")
	private long refreshTokenExpiration;

	@Value("${cookie.secure}")
	private boolean secure;

	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	// Access Token 생성
	public String createAccessToken(String id, String email) {
		return createJWT(id, email, TOKEN_TYPE_ACCESS, accessTokenExpiration);
	}

	// Refresh Token 생성
	public String createRefreshToken(String id, String email) {
		return createJWT(id, email, TOKEN_TYPE_REFRESH, refreshTokenExpiration);
	}

	// Refresh Token 무효화
	public ResponseCookie invalidateRefreshToken() {
		return buildRefreshCookie("", 0);
	}

	// Refresh Token을 쿠키로 발급
	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return buildRefreshCookie(refreshToken, refreshTokenExpiration);
	}

	// JWT 생성 내부 메서드
	private String createJWT(String id, String email, String type, long expireMs) {
		return Jwts.builder()
			.claim(CLAIM_TYPE, type)
			.claim(CLAIM_ID, id)
			.claim(CLAIM_EMAIL, email)
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + expireMs))
			.signWith(secretKey)
			.compact();
	}

	private Optional<Claims> safelyParseClaims(String token) {
		try {
			return Optional.of(Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload());
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("JWT validation failed");
			log.debug("JWT parsing error details: {}", e.getMessage());
			return Optional.empty();
		}
	}

	// 토큰 파싱: ID
	public Optional<String> getId(String token) {
		return safelyParseClaims(token)
			.map(claims -> claims.get(CLAIM_ID, String.class));
	}

	// 토큰 파싱: 이메일
	public Optional<String> getEmail(String token) {
		return safelyParseClaims(token)
			.map(claims -> claims.get(CLAIM_EMAIL, String.class));
	}

	// 토큰 파싱: 타입 구분(access/refresh)
	public Optional<String> getTokenType(String token) {
		return safelyParseClaims(token)
			.map(claims -> claims.get(CLAIM_TYPE, String.class));
	}

	// 토큰 파싱: 만료 시간
	public Optional<Long> getExpiration(String token) {
		return safelyParseClaims(token)
			.map(Claims::getExpiration)
			.map(exp -> exp.getTime() - System.currentTimeMillis());
	}

	// 토큰 검증 (만료 포함)
	public boolean validateToken(String token, String expectedType) {
		return safelyParseClaims(token)
			.map(claims -> expectedType.equals(claims.get(CLAIM_TYPE, String.class)))
			.orElse(false);
	}

	public boolean isAccessToken(String token) {
		return validateToken(token, TOKEN_TYPE_ACCESS);
	}

	public boolean isRefreshToken(String token) {
		return validateToken(token, TOKEN_TYPE_REFRESH);
	}



	// 헤더에서 AccessToken 추출
	public Optional<String> extractAccessToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
			.filter(header -> header.startsWith("Bearer "))
			.map(header -> header.substring(7));
	}

	// 쿠키에서 RefreshToken 추출
	public Optional<String> extractRefreshToken(HttpServletRequest request) {
		if (request.getCookies() == null) return Optional.empty();
		for (Cookie cookie : request.getCookies()) {
			if (COOKIE_NAME_REFRESH.equals(cookie.getName())) {
				return Optional.ofNullable(cookie.getValue());
			}
		}
		return Optional.empty();
	}

	private ResponseCookie buildRefreshCookie(String value, long maxAge) {
		return ResponseCookie.from(COOKIE_NAME_REFRESH, value)
			.maxAge(maxAge)
			.path("/")
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			.build();
	}

	public long getAccessTokenExpiration() {
		return accessTokenExpiration;
	}

	public long getRefreshTokenExpiration() {
		return refreshTokenExpiration;
	}
}
