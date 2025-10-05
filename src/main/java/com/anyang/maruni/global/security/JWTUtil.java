package com.anyang.maruni.global.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.auth.domain.service.TokenManager;
import com.anyang.maruni.global.config.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JWTUtil implements TokenManager {

	public static final String CLAIM_ID = "id";
	public static final String CLAIM_EMAIL = "email";
	public static final String CLAIM_TYPE = "type";
	public static final String TOKEN_TYPE_ACCESS = "access";


	private final JwtProperties jwtProperties;
	private SecretKey secretKey;

	public JWTUtil(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
	}

	// Access Token 생성
	public String createAccessToken(String id, String email) {
		return createJWT(id, email, TOKEN_TYPE_ACCESS, jwtProperties.getAccessToken().getExpiration());
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

	// 토큰 파싱: 이메일
	public Optional<String> getEmail(String token) {
		return safelyParseClaims(token)
			.map(claims -> claims.get(CLAIM_EMAIL, String.class));
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

	// 헤더에서 AccessToken 추출
	public Optional<String> extractAccessToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
			.filter(header -> header.startsWith("Bearer "))
			.map(header -> header.substring(7));
	}
}
