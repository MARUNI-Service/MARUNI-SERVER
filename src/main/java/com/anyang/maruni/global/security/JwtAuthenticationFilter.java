package com.anyang.maruni.global.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

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
	private final CustomUserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		jwtUtil.extractAccessToken(request)
			.filter(jwtUtil::isAccessToken)  // JWT 형식과 만료 검증
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
					log.info("JWT 인증 성공: {}", email);
				} catch (UsernameNotFoundException e) {
					log.warn("존재하지 않는 사용자 - 이메일: {}", email);
					// SecurityContext에 인증 정보가 없으면 Spring Security가 자동으로 401 처리
				} catch (Exception e) {
					log.error("JWT 인증 처리 중 예외 발생: {}", e.getMessage(), e);
					// SecurityContext에 인증 정보가 없으면 Spring Security가 자동으로 401 처리
				}
			});

		filterChain.doFilter(request, response);
	}
}