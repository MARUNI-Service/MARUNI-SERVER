package com.anyang.maruni.global.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

	private List<String> publicUrls;
	private List<String> adminUrls;
	private long corsMaxAge = 3600L;

	public String[] getPublicUrlsArray() {
		return publicUrls != null ? publicUrls.toArray(new String[0]) : new String[0];
	}

	public String[] getAdminUrlsArray() {
		return adminUrls != null ? adminUrls.toArray(new String[0]) : new String[0];
	}
}