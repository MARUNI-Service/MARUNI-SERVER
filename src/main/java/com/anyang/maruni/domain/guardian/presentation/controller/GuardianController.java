package com.anyang.maruni.domain.guardian.presentation.controller;

import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.application.dto.GuardianUpdateRequestDto;
import com.anyang.maruni.domain.guardian.application.service.GuardianService;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.success.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Guardian REST API Controller
 *
 * 보호자 관리 API 엔드포인트:
 * - 보호자 CRUD 작업
 * - 회원-보호자 관계 관리
 * - 보호자별 담당 회원 조회
 */
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Guardian API", description = "보호자 관리 API")
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping
    @Operation(summary = "보호자 생성", description = "새로운 보호자를 등록합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public GuardianResponseDto createGuardian(
        @Valid @RequestBody GuardianRequestDto request) {
        return guardianService.createGuardian(request);
    }

    @GetMapping("/{guardianId}")
    @Operation(summary = "보호자 조회", description = "보호자 정보를 조회합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public GuardianResponseDto getGuardian(
        @Parameter(description = "보호자 ID", example = "1")
        @PathVariable Long guardianId) {
        return guardianService.getGuardianById(guardianId);
    }

    @PutMapping("/{guardianId}")
    @Operation(summary = "보호자 정보 수정", description = "보호자의 이름과 전화번호를 수정합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public GuardianResponseDto updateGuardian(
        @Parameter(description = "보호자 ID", example = "1")
        @PathVariable Long guardianId,
        @Valid @RequestBody GuardianUpdateRequestDto request) {
        return guardianService.updateGuardianInfo(guardianId,
            request.getGuardianName(), request.getGuardianPhone());
    }

    @DeleteMapping("/{guardianId}")
    @Operation(summary = "보호자 비활성화", description = "보호자를 비활성화하고 연결된 모든 회원과의 관계를 해제합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void deactivateGuardian(
        @Parameter(description = "보호자 ID", example = "1")
        @PathVariable Long guardianId) {
        guardianService.deactivateGuardian(guardianId);
    }

    @PostMapping("/{guardianId}/members/{memberId}")
    @Operation(summary = "회원에게 보호자 할당", description = "특정 회원에게 보호자를 할당합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void assignGuardianToMember(
        @Parameter(description = "보호자 ID", example = "1")
        @PathVariable Long guardianId,
        @Parameter(description = "회원 ID", example = "1")
        @PathVariable Long memberId) {
        guardianService.assignGuardianToMember(memberId, guardianId);
    }

    @DeleteMapping("/members/{memberId}/guardian")
    @Operation(summary = "회원의 보호자 관계 해제", description = "특정 회원의 보호자 관계를 해제합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public void removeGuardianFromMember(
        @Parameter(description = "회원 ID", example = "1")
        @PathVariable Long memberId) {
        guardianService.removeGuardianFromMember(memberId);
    }

    @GetMapping("/{guardianId}/members")
    @Operation(summary = "보호자가 담당하는 회원 목록 조회", description = "특정 보호자가 담당하는 모든 회원 목록을 조회합니다.")
    @SuccessCodeAnnotation(SuccessCode.SUCCESS)
    public List<MemberResponse> getMembersByGuardian(
        @Parameter(description = "보호자 ID", example = "1")
        @PathVariable Long guardianId) {
        return guardianService.getMembersByGuardian(guardianId);
    }
}