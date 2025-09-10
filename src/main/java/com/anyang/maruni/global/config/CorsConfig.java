package com.anyang.maruni.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.anyang.maruni.global.config.properties.SecurityProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

	private final SecurityProperties securityProperties;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		return request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOriginPatterns(List.of("*"));
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			config.setAllowCredentials(true);
			config.setAllowedHeaders(List.of("*"));
			config.setExposedHeaders(List.of("Authorization"));
			config.setMaxAge(securityProperties.getCorsMaxAge());
			return config;
		};
	}
}