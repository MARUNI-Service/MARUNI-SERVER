package com.anyang.maruni.global.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import com.anyang.maruni.domain.auth.domain.service.TokenValidator;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetailsService;

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
	private final TokenValidator tokenValidator;
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
		// 도메인 서비스에 토큰 검증 위임
		if (!tokenValidator.isValidAccessToken(token)) {
			log.warn("토큰 검증 실패");
			sendUnauthorized(response, "유효하지 않은 토큰입니다.");
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