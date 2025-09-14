package com.anyang.maruni.global.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI API 관련 설정 프로퍼티
 * 
 * 환경변수에서 openai 접두사를 가진 설정들을 자동 매핑합니다.
 * - openai.api.key: OpenAI API 키
 * - openai.model: 사용할 모델 (기본값: gpt-4o)
 */
@Data
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAIProperties {

    /**
     * OpenAI API 키 (필수)
     * 환경변수: OPENAI_API_KEY
     */
    private Api api = new Api();

    /**
     * 사용할 OpenAI 모델
     * 환경변수: OPENAI_MODEL
     * 기본값: gpt-4o
     */
    private String model = "gpt-4o";

    @Data
    public static class Api {
        /**
         * OpenAI API 키
         */
        private String key;
    }
}