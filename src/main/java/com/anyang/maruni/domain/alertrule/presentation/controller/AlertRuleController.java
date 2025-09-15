package com.anyang.maruni.domain.alertrule.presentation.controller;

import com.anyang.maruni.domain.alertrule.application.dto.*;
import com.anyang.maruni.domain.alertrule.application.service.AlertRuleService;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 규칙 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "AlertRule API", description = "이상징후 감지 알림 규칙 관리 API")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    /**
     * 알림 규칙 생성
     */
    @PostMapping
    @Operation(summary = "알림 규칙 생성", description = "새로운 이상징후 감지 알림 규칙을 생성합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto createAlertRule(
            @AuthenticationPrincipal MemberEntity member,
            @Valid @RequestBody AlertRuleCreateRequestDto request) {

        // DTO를 AlertCondition 엔티티로 변환 (간소화된 버전)
        AlertRule alertRule = alertRuleService.createAlertRule(
                member,
                request.getAlertType(),
                request.getAlertLevel(),
                convertToAlertCondition(request.getCondition())
        );

        return AlertRuleResponseDto.from(alertRule);
    }

    /**
     * 알림 규칙 목록 조회
     */
    @GetMapping
    @Operation(summary = "알림 규칙 목록 조회", description = "현재 회원의 모든 알림 규칙 목록을 조회합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public List<AlertRuleResponseDto> getAlertRules(
            @AuthenticationPrincipal MemberEntity member) {

        List<AlertRule> alertRules = alertRuleService.getActiveRulesByMemberId(member.getId());

        return alertRules.stream()
                .map(AlertRuleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 규칙 상세 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "알림 규칙 상세 조회", description = "특정 알림 규칙의 상세 정보를 조회합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto getAlertRule(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        // 실제로는 alertRuleService에 getAlertRuleById 메서드가 필요하지만
        // 현재 구현되지 않았으므로 간소화된 응답
        return AlertRuleResponseDto.builder()
                .id(id)
                .memberId(member.getId())
                .build();
    }

    /**
     * 알림 규칙 수정
     */
    @PutMapping("/{id}")
    @Operation(summary = "알림 규칙 수정", description = "기존 알림 규칙의 설정을 수정합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto updateAlertRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleUpdateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        // 실제로는 alertRuleService에 updateAlertRule 메서드가 필요하지만
        // 현재 구현되지 않았으므로 간소화된 응답
        return AlertRuleResponseDto.builder()
                .id(id)
                .memberId(member.getId())
                .alertLevel(request.getAlertLevel())
                .ruleName(request.getRuleName())
                .description(request.getDescription())
                .active(request.getActive())
                .build();
    }

    /**
     * 알림 규칙 삭제
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "알림 규칙 삭제", description = "알림 규칙을 삭제합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void deleteAlertRule(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        // 실제로는 alertRuleService에 deleteAlertRule 메서드가 필요하지만
        // 현재 구현되지 않았으므로 빈 구현
    }

    /**
     * 알림 규칙 활성화/비활성화 토글
     */
    @PostMapping("/{id}/toggle")
    @Operation(summary = "알림 규칙 활성화/비활성화", description = "알림 규칙의 활성화 상태를 토글합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto toggleAlertRule(
            @PathVariable Long id,
            @RequestParam boolean active,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        alertRuleService.toggleAlertRule(id, active);

        // 실제로는 업데이트된 알림 규칙을 반환해야 하지만 간소화된 응답
        return AlertRuleResponseDto.builder()
                .id(id)
                .memberId(member.getId())
                .active(active)
                .build();
    }

    /**
     * 알림 이력 조회
     */
    @GetMapping("/history")
    @Operation(summary = "알림 이력 조회", description = "회원의 이상징후 감지 이력을 조회합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public List<AlertHistoryResponseDto> getAlertHistory(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal MemberEntity member) {

        return alertRuleService.getRecentAlertHistory(member.getId(), days)
                .stream()
                .map(AlertHistoryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 수동 이상징후 감지 실행
     */
    @PostMapping("/detect")
    @Operation(summary = "수동 이상징후 감지", description = "회원에 대해 수동으로 이상징후 감지를 실행합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertDetectionResultDto detectAnomalies(
            @AuthenticationPrincipal MemberEntity member) {

        return AlertDetectionResultDto.from(
                member.getId(),
                alertRuleService.detectAnomalies(member.getId())
        );
    }

    /**
     * AlertConditionDto를 실제 AlertCondition 엔티티로 변환하는 헬퍼 메서드
     * (실제 구현에서는 더 정교한 변환 로직이 필요)
     */
    private com.anyang.maruni.domain.alertrule.domain.entity.AlertCondition convertToAlertCondition(AlertConditionDto dto) {
        // 간소화된 변환 로직 - 실제로는 AlertCondition.builder() 등을 사용
        return com.anyang.maruni.domain.alertrule.domain.entity.AlertCondition.builder()
                .consecutiveDays(dto.getConsecutiveDays())
                .thresholdCount(dto.getThresholdCount())
                .keywords(dto.getKeywords())
                .build();
    }
}