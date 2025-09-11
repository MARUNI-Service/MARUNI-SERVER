package com.anyang.maruni.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.anyang.maruni.domain.auth.domain.service.TokenValidator;
import com.anyang.maruni.global.security.AuthenticationEventHandler;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetailsService;
import com.anyang.maruni.global.security.JWTUtil;
import com.anyang.maruni.global.security.JwtAuthenticationFilter;
import com.anyang.maruni.global.security.LoginFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtSecurityConfig {

	private final JWTUtil jwtUtil;
	private final ObjectMapper objectMapper;
	private final TokenValidator tokenValidator;
	private final CustomUserDetailsService customUserDetailsService;
	private final AuthenticationEventHandler authenticationEventHandler;

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public LoginFilter loginFilter(AuthenticationManager authManager) {
		LoginFilter loginFilter = new LoginFilter(authManager, objectMapper, authenticationEventHandler);
		loginFilter.setFilterProcessesUrl("/api/auth/login");
		loginFilter.setAuthenticationManager(authManager);
		return loginFilter;
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtUtil, tokenValidator, customUserDetailsService);
	}
}