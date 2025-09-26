package com.anyang.maruni.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Firebase 설정 프로퍼티
 *
 * application.yml에서 firebase 관련 설정을 바인딩합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    /**
     * Firebase 기본 설정
     */
    private String projectId;
    private String serviceAccountKeyPath;
    private boolean enabled = false;

    /**
     * FCM 푸시 알림 설정
     */
    private Push push = new Push();

    @Getter
    @Setter
    public static class Push {
        /**
         * FCM API 호출 타임아웃 (ms)
         */
        private int timeout = 5000;

        /**
         * 재시도 설정
         */
        private Retry retry = new Retry();
    }

    @Getter
    @Setter
    public static class Retry {
        /**
         * 최대 재시도 횟수
         */
        private int maxAttempts = 3;

        /**
         * 재시도 간격 배수 (지수 백오프)
         */
        private double backoffMultiplier = 2.0;

        /**
         * 초기 재시도 지연 시간 (ms)
         */
        private long initialDelay = 1000;
    }
}