package com.anyang.maruni.global.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import com.anyang.maruni.domain.auth.application.service.BlacklistService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JWTUtil jwtUtil;
	private final BlacklistService blacklistService;
	private final CustomUserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		jwtUtil.extractAccessToken(request)
			.filter(token -> validateAccessToken(token, response))
			.flatMap(jwtUtil::getEmail)
			.ifPresent(email -> {
				try {
					UserDetails userDetails = userDetailsService.loadUserByUsername(email);
					Authentication authentication = new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
					);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.info("SecurityContext에 인증 객체 저장 완료: {}", email);
				} catch (Exception e) {
					log.warn("UserDetails 로딩 실패: {}", e.getMessage());
					sendUnauthorized(response, "회원 인증 실패");
				}
			});

		filterChain.doFilter(request, response);
	}

	private boolean validateAccessToken(String token, HttpServletResponse response) {
		if (!jwtUtil.validateToken(token, "access")) {
			log.warn("토큰 검증 실패");
			sendUnauthorized(response, "유효하지 않은 토큰입니다.");
			return false;
		}

		if (blacklistService.isBlacklisted(token)) {
			log.warn("블랙리스트에 등록된 토큰입니다.");
			sendUnauthorized(response, "TOKEN_BLACKLISTED");
			return false;
		}

		return true;
	}

	private void sendUnauthorized(HttpServletResponse response, String message) {
		try {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
		} catch (IOException e) {
			log.error("응답 중 에러 발생", e);
		}
	}
}