package com.anyang.maruni.domain.alertrule.application.analyzer.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.alertrule.application.analyzer.util.AnalyzerUtils;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AlertResult;
import com.anyang.maruni.domain.alertrule.application.analyzer.vo.AnalysisContext;
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
        AlertResult emergencyResult = checkKeywords(content, message.getContent(),
                alertConfig.getKeyword().getEmergency(), AlertLevel.EMERGENCY);
        if (emergencyResult.isAlert()) {
            return emergencyResult;
        }

        // 2. 경고 키워드 감지
        AlertResult warningResult = checkKeywords(content, message.getContent(),
                alertConfig.getKeyword().getWarning(), AlertLevel.HIGH);
        if (warningResult.isAlert()) {
            return warningResult;
        }

        // 3. 위험 키워드 없음
        return AlertResult.noAlert();
    }

    /**
     * 키워드 목록에서 매칭되는 키워드 검사
     * @param contentLowerCase 소문자로 변환된 메시지 내용
     * @param originalContent 원본 메시지 내용
     * @param keywords 검사할 키워드 목록
     * @param alertLevel 알림 레벨
     * @return 키워드 분석 결과
     */
    private AlertResult checkKeywords(String contentLowerCase, String originalContent,
                                    List<String> keywords, AlertLevel alertLevel) {
        for (String keyword : keywords) {
            if (contentLowerCase.contains(keyword.toLowerCase())) {
                String alertMessage = AnalyzerUtils.createKeywordDetectionMessage(alertLevel, keyword);
                KeywordMatch keywordMatch = alertLevel == AlertLevel.EMERGENCY ?
                        KeywordMatch.emergency(keyword, originalContent) :
                        KeywordMatch.warning(keyword, originalContent);
                return AlertResult.createAlert(alertLevel, AlertType.KEYWORD_DETECTION, alertMessage, keywordMatch);
            }
        }
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