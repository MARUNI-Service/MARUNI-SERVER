package com.anyang.maruni.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Firebase 설정
 *
 * Firebase Admin SDK를 초기화하여 푸시 알림 발송을 위한 설정을 제공합니다.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${maruni.notification.push.project-id}")
    private String projectId;

    @Value("${FIREBASE_PRIVATE_KEY_PATH:config/firebase-service-account.json}")
    private String serviceAccountKeyPath;

    /**
     * Firebase App 초기화
     *
     * 개발 환경에서는 Mock으로, 운영 환경에서는 실제 Firebase로 설정됩니다.
     */
    @Bean
    public FirebaseApp firebaseApp() {
        try {
            // Firebase가 이미 초기화되어 있으면 기존 인스턴스 반환
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase already initialized");
                return FirebaseApp.getInstance();
            }

            // 서비스 계정 키 파일이 존재하는지 확인
            ClassPathResource resource = new ClassPathResource(serviceAccountKeyPath);

            if (!resource.exists()) {
                log.warn("Firebase service account key file not found at: {}. " +
                        "Push notifications will be mocked in development.", serviceAccountKeyPath);
                return initializeMockFirebase();
            }

            // 실제 Firebase 초기화
            GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully for project: {}", projectId);
            return app;

        } catch (IOException e) {
            log.error("Failed to initialize Firebase. Using mock implementation.", e);
            return initializeMockFirebase();
        }
    }

    /**
     * 개발/테스트 환경용 Mock Firebase 초기화
     */
    private FirebaseApp initializeMockFirebase() {
        try {
            // Mock 환경에서는 자격 증명 없이 초기화
            FirebaseOptions mockOptions = FirebaseOptions.builder()
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(mockOptions, "mock-firebase");
            log.info("Mock Firebase initialized for development/testing");
            return app;

        } catch (Exception e) {
            log.error("Failed to initialize even mock Firebase", e);
            throw new RuntimeException("Firebase 초기화에 실패했습니다", e);
        }
    }
}