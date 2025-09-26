package com.anyang.maruni.global.config;

import com.anyang.maruni.global.config.properties.FirebaseProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase 설정
 *
 * Firebase Admin SDK를 초기화하고 FirebaseApp 빈을 생성합니다.
 * firebase.enabled=true인 경우에만 활성화됩니다.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    /**
     * FirebaseApp 빈 생성
     *
     * 서비스 계정 키 파일을 사용하여 Firebase를 초기화합니다.
     * 개발 환경에서는 Mock 서비스가 우선되므로 실제로는 사용되지 않을 수 있습니다.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Initializing Firebase with project: {}", firebaseProperties.getProjectId());

        // 서비스 계정 키 파일 로드
        InputStream serviceAccount = getServiceAccountInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId(firebaseProperties.getProjectId())
                .build();

        // FirebaseApp 초기화 (이미 초기화된 경우 기존 인스턴스 반환)
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp firebaseApp = FirebaseApp.initializeApp(options);
            log.info("✅ Firebase initialized successfully: {}", firebaseApp.getName());
            return firebaseApp;
        } else {
            log.info("✅ Firebase already initialized, returning existing instance");
            return FirebaseApp.getInstance();
        }
    }

    /**
     * 서비스 계정 키 파일의 InputStream을 반환
     *
     * 설정된 경로에서 서비스 계정 키 파일을 로드합니다.
     */
    private InputStream getServiceAccountInputStream() throws IOException {
        String keyPath = firebaseProperties.getServiceAccountKeyPath();

        if (keyPath == null || keyPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Firebase service account key path is not configured");
        }

        try {
            // 클래스패스에서 리소스 로드 시도
            ClassPathResource resource = new ClassPathResource(keyPath);
            if (resource.exists()) {
                log.info("Loading Firebase service account key from classpath: {}", keyPath);
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.warn("Failed to load service account key from classpath: {}", keyPath);
        }

        throw new IllegalArgumentException(
                "Firebase service account key file not found: " + keyPath +
                ". Please ensure the file exists in the classpath."
        );
    }
}