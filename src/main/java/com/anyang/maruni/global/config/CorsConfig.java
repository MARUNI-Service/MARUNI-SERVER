package com.anyang.maruni.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * CORS 설정 - 개발 환경용 (모든 오리진/헤더 허용)
 */
@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		return request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOriginPatterns(List.of("*"));
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
			config.setAllowCredentials(true);
			config.setAllowedHeaders(List.of("*"));
			config.setExposedHeaders(List.of("Authorization"));
			config.setMaxAge(3600L); // 1시간
			return config;
		};
	}
}