package com.anyang.maruni.domain.alertrule.application.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 스케줄러
 *
 * 매일 정시에 이상징후 감지를 트리거하는 스케줄러입니다.
 * 실제 비즈니스 로직은 AlertTriggerService에 위임합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final AlertTriggerService alertTriggerService;

    /**
     * 매일 오후 10시 이상징후 감지 (하루 데이터 집계 후)
     */
    @Scheduled(cron = "${maruni.scheduling.alert-detection.cron}")
    public void triggerDailyAnomalyDetection() {
        log.info("📅 [AlertScheduler] Daily anomaly detection triggered");
        alertTriggerService.detectAnomaliesForAllMembers();
    }
}
