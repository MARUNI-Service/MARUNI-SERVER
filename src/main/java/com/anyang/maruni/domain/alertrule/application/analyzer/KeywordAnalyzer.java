package com.anyang.maruni.domain.alertrule.application.analyzer;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.alertrule.application.config.AlertConfigurationProperties;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 키워드 위험도 분석기
 *
 * 메시지 내용에서 위험 키워드를 감지합니다.
 * Phase 2 리팩토링: AnomalyAnalyzer 구현체
 */
@Component
@RequiredArgsConstructor
public class KeywordAnalyzer implements AnomalyAnalyzer {

    private final AlertConfigurationProperties alertConfig;

    @Override
    public AlertResult analyze(MemberEntity member, AnalysisContext context) {
        return analyzeKeywordRisk(context.getTargetMessage());
    }

    @Override
    public AlertType getSupportedType() {
        return AlertType.KEYWORD_DETECTION;
    }

    @Override
    public boolean supports(AlertType alertType) {
        return AlertType.KEYWORD_DETECTION.equals(alertType);
    }

    /**
     * 메시지의 키워드 위험도 분석
     * @param message 분석할 메시지
     * @return 키워드 분석 결과
     */
    public AlertResult analyzeKeywordRisk(MessageEntity message) {
        String content = message.getContent().toLowerCase();

        // 1. 긴급 키워드 감지 (최우선)
        for (String emergencyKeyword : alertConfig.getKeyword().getEmergency()) {
            if (content.contains(emergencyKeyword.toLowerCase())) {
                String alertMessage = AnalyzerUtils.createKeywordDetectionMessage(AlertLevel.EMERGENCY, emergencyKeyword);
                KeywordMatch keywordMatch = KeywordMatch.emergency(emergencyKeyword, message.getContent());
                return AlertResult.createAlert(AlertLevel.EMERGENCY, alertMessage, keywordMatch);
            }
        }

        // 2. 경고 키워드 감지
        for (String warningKeyword : alertConfig.getKeyword().getWarning()) {
            if (content.contains(warningKeyword.toLowerCase())) {
                String alertMessage = AnalyzerUtils.createKeywordDetectionMessage(AlertLevel.HIGH, warningKeyword);
                KeywordMatch keywordMatch = KeywordMatch.warning(warningKeyword, message.getContent());
                return AlertResult.createAlert(AlertLevel.HIGH, alertMessage, keywordMatch);
            }
        }

        // 3. 위험 키워드 없음
        return AlertResult.noAlert();
    }

    /**
     * 키워드 매칭 정보 VO
     */
    @Getter
	public static class KeywordMatch {
		// Getter 메서드들
		private final String matchedKeyword;
        private final String originalMessage;
        private final KeywordType keywordType;

        private KeywordMatch(String matchedKeyword, String originalMessage, KeywordType keywordType) {
            this.matchedKeyword = matchedKeyword;
            this.originalMessage = originalMessage;
            this.keywordType = keywordType;
        }

        public static KeywordMatch emergency(String keyword, String message) {
            return new KeywordMatch(keyword, message, KeywordType.EMERGENCY);
        }

        public static KeywordMatch warning(String keyword, String message) {
            return new KeywordMatch(keyword, message, KeywordType.WARNING);
        }

		public enum KeywordType {
            EMERGENCY, WARNING
        }
    }
}