package com.anyang.maruni.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Swagger API 문서화 관련 설정을 관리하는 프로퍼티 클래스
 * application.yml의 swagger.* 설정을 바인딩
 */
@Data
@Component
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {
    
    private Api api = new Api();
    private Contact contact = new Contact();
    private Server server = new Server();
    
    /**
     * API 기본 정보 설정
     */
    @Data
    public static class Api {
        private String title = "MARUNI API Documentation";
        private String description = "REST API Documentation for MARUNI elderly care service";
        private String version = "v1.0.0";
    }
    
    /**
     * 연락처 정보 설정
     */
    @Data
    public static class Contact {
        private String name = "MARUNI Development Team";
        private String email = "dev@maruni.com";
        private String url = "https://github.com/maruni-project";
    }
    
    /**
     * 서버 정보 설정
     */
    @Data
    public static class Server {
        private String url = "http://localhost:8080";
        private String description = "Development Server";
    }
}