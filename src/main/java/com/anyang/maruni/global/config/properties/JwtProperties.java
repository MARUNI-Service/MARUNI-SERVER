package com.anyang.maruni.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * JWT 관련 설정을 관리하는 프로퍼티 클래스
 * application.yml의 jwt.* 설정을 바인딩
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private AccessToken accessToken = new AccessToken();
    private String secretKey;

    /**
     * Access Token 설정
     */
    @Data
    public static class AccessToken {
        private long expiration = 3600000L; // 1시간 (기본값)
    }
}