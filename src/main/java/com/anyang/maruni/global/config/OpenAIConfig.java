package com.anyang.maruni.global.config;

import com.anyang.maruni.global.config.properties.OpenAIProperties;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * OpenAI API 클라이언트 설정
 * 
 * OpenAI GPT API와의 연동을 위한 클라이언트를 구성합니다.
 * - OpenAiService: OpenAI API 호출을 위한 서비스 클래스
 * - 환경변수에서 API 키와 모델 설정을 자동으로 로드
 * - API 타임아웃은 30초로 고정 설정 (MVP에서는 하드코딩)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenAIProperties.class)
public class OpenAIConfig {

    private final OpenAIProperties openAIProperties;

    /**
     * OpenAI API 서비스 Bean 생성
     * 
     * @return OpenAiService 인스턴스
     * @throws IllegalArgumentException API 키가 설정되지 않은 경우
     */
    @Bean
    public OpenAiService openAiService() {
        String apiKey = openAIProperties.getApi().getKey();
        
        // API 키 유효성 검사
        if (!StringUtils.hasText(apiKey) || "your_openai_api_key_here".equals(apiKey)) {
            log.error("OpenAI API 키가 설정되지 않았습니다. .env 파일에서 OPENAI_API_KEY를 확인해주세요.");
            throw new IllegalArgumentException("OpenAI API 키가 설정되지 않았습니다.");
        }

        log.info("OpenAI API 서비스 초기화 중... 모델: {}", openAIProperties.getModel());
        
        // MVP: 타임아웃은 30초로 고정
        return new OpenAiService(apiKey, Duration.ofSeconds(30));
    }

    /**
     * 설정된 OpenAI 모델 반환
     * 
     * @return 사용할 OpenAI 모델명
     */
    @Bean
    public String openAiModel() {
        return openAIProperties.getModel();
    }
}