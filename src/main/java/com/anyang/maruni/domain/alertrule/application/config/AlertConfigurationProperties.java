package com.anyang.maruni.domain.alertrule.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AlertRule 도메인 설정 프로퍼티
 *
 * 기존 하드코딩된 임계값, 키워드, 메시지 템플릿 등을 외부화하여 관리합니다.
 */
@Data
@Component
@ConfigurationProperties(prefix = "maruni.alert")
public class AlertConfigurationProperties {

    private Analysis analysis = new Analysis();
    private Emotion emotion = new Emotion();
    private Keyword keyword = new Keyword();
    private Notification notification = new Notification();
    private NoResponse noResponse = new NoResponse();

    @Data
    public static class Analysis {
        /**
         * 기본 분석 기간 (일)
         */
        private int defaultDays = 7;
    }

    @Data
    public static class Emotion {
        /**
         * 높은 위험도 연속 일수 임계값
         */
        private int highRiskConsecutiveDays = 3;

        /**
         * 높은 위험도 부정 감정 비율 임계값
         */
        private double highRiskNegativeRatio = 0.7;

        /**
         * 중간 위험도 연속 일수 임계값
         */
        private int mediumRiskConsecutiveDays = 2;

        /**
         * 중간 위험도 부정 감정 비율 임계값
         */
        private double mediumRiskNegativeRatio = 0.5;
    }

    @Data
    public static class Keyword {
        /**
         * 긴급 키워드 목록
         */
        private List<String> emergency = List.of(
                "도와주세요", "아파요", "숨이", "가슴이", "쓰러짐", "응급실", "119", "병원", "죽겠어"
        );

        /**
         * 경고 키워드 목록
         */
        private List<String> warning = List.of(
                "우울해", "외로워", "죽고싶어", "포기", "희망없어", "의미없어", "괴로워", "힘들어"
        );
    }

    @Data
    public static class Notification {
        /**
         * 보호자 알림 제목 템플릿
         */
        private String titleTemplate = "[MARUNI 알림] %s 단계 이상징후 감지";

        /**
         * 감지 상세 정보 JSON 템플릿
         */
        private String detectionDetailsJsonTemplate = "{\"alertLevel\":\"%s\",\"analysisDetails\":\"%s\"}";

        /**
         * 알림 발송 실패 로그 메시지
         */
        private String notificationFailureLog = "Guardian notification failed for member: %d";

        /**
         * 알림 발송 오류 로그 메시지
         */
        private String notificationErrorLog = "Error sending guardian notification: %s";
    }

    @Data
    public static class NoResponse {
        /**
         * 높은 위험도 연속 무응답 일수 임계값
         */
        private int highRiskConsecutiveNoResponseDays = 2;

        /**
         * 높은 위험도 최소 응답률 임계값
         */
        private double highRiskMinResponseRate = 0.3;

        /**
         * 중간 위험도 연속 무응답 일수 임계값
         */
        private int mediumRiskConsecutiveNoResponseDays = 1;

        /**
         * 중간 위험도 최소 응답률 임계값
         */
        private double mediumRiskMinResponseRate = 0.5;
    }
}