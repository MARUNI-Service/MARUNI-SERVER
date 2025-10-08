package com.anyang.maruni.domain.alertrule.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anyang.maruni.domain.alertrule.application.dto.request.AlertConditionDto;
import com.anyang.maruni.domain.alertrule.application.dto.request.AlertRuleCreateRequestDto;
import com.anyang.maruni.domain.alertrule.application.dto.request.AlertRuleUpdateRequestDto;
import com.anyang.maruni.domain.alertrule.application.dto.response.AlertDetectionResultDto;
import com.anyang.maruni.domain.alertrule.application.dto.response.AlertHistoryResponseDto;
import com.anyang.maruni.domain.alertrule.application.dto.response.AlertRuleResponseDto;
import com.anyang.maruni.domain.alertrule.application.service.core.AlertRuleService;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertCondition;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertRule;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 알림 규칙 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "알림 규칙 관리 API", description = "이상징후 감지 알림 규칙 관리 API")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final com.anyang.maruni.domain.alertrule.application.service.core.AlertHistoryService alertHistoryService;

    @Operation(
        summary = "알림 규칙 생성",
        description = "새로운 이상징후 감지 알림 규칙을 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 규칙 생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 유효성 실패", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @PostMapping
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto createAlertRule(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member,
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

    @Operation(
        summary = "알림 규칙 목록 조회",
        description = "현재 회원의 모든 알림 규칙 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 규칙 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @GetMapping
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public List<AlertRuleResponseDto> getAlertRules(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        List<AlertRule> alertRules = alertRuleService.getActiveRulesByMemberId(member.getId());

        return alertRules.stream()
                .map(AlertRuleResponseDto::from)
                .collect(Collectors.toList());
    }

    @Operation(
        summary = "알림 규칙 상세 조회",
        description = "특정 알림 규칙의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 규칙 상세 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "알림 규칙을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{id}")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto getAlertRule(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        AlertRule alertRule = alertRuleService.getAlertRuleById(id);

        // TODO: 추후 권한 검증 로직 추가 (현재 회원이 해당 알림 규칙의 소유자인지 확인)
        // if (!alertRule.getMember().getId().equals(member.getId())) {
        //     throw new AlertRuleAccessDeniedException(id, member.getId());
        // }

        return AlertRuleResponseDto.from(alertRule);
    }

    @Operation(
        summary = "알림 규칙 수정",
        description = "기존 알림 규칙의 설정을 수정합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 규칙 수정 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 유효성 실패", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "알림 규칙을 찾을 수 없음", content = @Content)
    })
    @PutMapping("/{id}")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto updateAlertRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleUpdateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        // TODO: 추후 권한 검증 로직 추가

        AlertRule updatedAlertRule = alertRuleService.updateAlertRule(
                id,
                request.getRuleName(),
                request.getDescription(),
                request.getAlertLevel()
        );

        return AlertRuleResponseDto.from(updatedAlertRule);
    }

    @Operation(
        summary = "알림 규칙 삭제",
        description = "알림 규칙을 삭제합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 규칙 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "알림 규칙을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{id}")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void deleteAlertRule(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        // TODO: 추후 권한 검증 로직 추가

        alertRuleService.deleteAlertRule(id);
    }

    @Operation(
        summary = "알림 규칙 활성화/비활성화",
        description = "알림 규칙의 활성화 상태를 토글합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 규칙 상태 변경 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "알림 규칙을 찾을 수 없음", content = @Content)
    })
    @PostMapping("/{id}/toggle")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertRuleResponseDto toggleAlertRule(
            @PathVariable Long id,
            @RequestParam boolean active,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        // TODO: 추후 권한 검증 로직 추가

        AlertRule updatedAlertRule = alertRuleService.toggleAlertRule(id, active);

        return AlertRuleResponseDto.from(updatedAlertRule);
    }

    @Operation(
        summary = "알림 이력 조회",
        description = "회원의 이상징후 감지 이력을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 이력 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @GetMapping("/history")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public List<AlertHistoryResponseDto> getAlertHistory(
            @RequestParam(defaultValue = "30") int days,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        return alertRuleService.getRecentAlertHistory(member.getId(), days)
                .stream()
                .map(AlertHistoryResponseDto::from)
                .collect(Collectors.toList());
    }

    @Operation(
        summary = "알림 상세 조회",
        description = "특정 알림 이력의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 상세 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/history/{alertId}")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertHistoryResponseDto getAlertDetail(
            @PathVariable Long alertId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        return alertHistoryService.getAlertDetail(alertId, member.getId());
    }

    @Operation(
        summary = "수동 이상징후 감지",
        description = "회원에 대해 수동으로 이상징후 감지를 실행합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이상징후 감지 실행 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "회원이 존재하지 않음", content = @Content)
    })
    @PostMapping("/detect")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public AlertDetectionResultDto detectAnomalies(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberEntity member) {

        return AlertDetectionResultDto.from(
                member.getId(),
                alertRuleService.detectAnomalies(member.getId())
        );
    }

    /**
     * AlertConditionDto를 실제 AlertCondition 엔티티로 변환하는 헬퍼 메서드
     * (실제 구현에서는 더 정교한 변환 로직이 필요)
     */
    private AlertCondition convertToAlertCondition(AlertConditionDto dto) {
        // 간소화된 변환 로직 - 실제로는 AlertCondition.builder() 등을 사용
        return AlertCondition.builder()
                .consecutiveDays(dto.getConsecutiveDays())
                .thresholdCount(dto.getThresholdCount())
                .keywords(dto.getKeywords())
                .build();
    }
}