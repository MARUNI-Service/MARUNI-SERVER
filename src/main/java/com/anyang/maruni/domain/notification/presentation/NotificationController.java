package com.anyang.maruni.domain.notification.presentation;

import com.anyang.maruni.domain.notification.application.dto.response.NotificationResponseDto;
import com.anyang.maruni.domain.notification.application.service.NotificationQueryService;
import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 통합 알림 API 컨트롤러 (MVP 추가)
 *
 * 모든 알림 타입(DAILY_CHECK, GUARDIAN_REQUEST, ALERT 등)을 통합 조회하고,
 * 읽음 상태를 관리하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "Notification", description = "통합 알림 API")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    /**
     * 전체 알림 조회 (최신순)
     *
     * 로그인한 회원의 모든 알림(안부 메시지, 보호자 요청, 이상징후 감지 등)을
     * 최신순으로 조회합니다.
     *
     * @param userDetails 인증된 사용자 정보 (Security Context에서 자동 주입)
     * @return 알림 목록 (최신순)
     */
    @GetMapping
    @Operation(
            summary = "전체 알림 조회",
            description = "로그인한 회원의 모든 알림을 최신순으로 조회합니다. " +
                    "안부 메시지, 보호자 요청, 이상징후 감지 알림 등이 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public List<NotificationResponseDto> getAllNotifications(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return notificationQueryService.getAllNotifications(memberId);
    }

    /**
     * 안읽은 알림 개수 조회
     *
     * 로그인한 회원의 안읽은 알림 개수를 조회합니다.
     * 알림 뱃지 표시에 활용할 수 있습니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 안읽은 알림 개수
     */
    @GetMapping("/unread-count")
    @Operation(
            summary = "안읽은 알림 개수 조회",
            description = "로그인한 회원의 안읽은 알림 개수를 조회합니다. " +
                    "앱의 알림 뱃지 표시에 활용할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public Long getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return notificationQueryService.getUnreadCount(memberId);
    }

    /**
     * 알림 읽음 처리
     *
     * 특정 알림을 읽음 상태로 변경합니다.
     * 클라이언트는 사용자가 알림을 확인했을 때 이 API를 호출해야 합니다.
     *
     * @param id 알림 ID
     * @return 업데이트된 알림 정보
     */
    @PatchMapping("/{id}/read")
    @Operation(
            summary = "알림 읽음 처리",
            description = "특정 알림을 읽음 상태로 변경합니다. " +
                    "클라이언트는 사용자가 알림을 클릭/확인했을 때 이 API를 호출해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public NotificationResponseDto markAsRead(@PathVariable Long id) {
        return notificationQueryService.markAsRead(id);
    }
}
