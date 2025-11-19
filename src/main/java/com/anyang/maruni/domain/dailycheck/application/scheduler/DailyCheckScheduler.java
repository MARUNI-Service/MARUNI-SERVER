package com.anyang.maruni.domain.dailycheck.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DailyCheck 스케줄링 트리거
 *
 * 단일 책임: 스케줄링 트리거만 담당하고 실제 비즈니스 로직은 Orchestrator에 위임
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyCheckScheduler {

    private final DailyCheckOrchestrator dailyCheckOrchestrator;

    /**
     * 매일 안부 메시지 발송 트리거 (UTC 자정 = KST 오전 9시)
     */
    @Scheduled(cron = "${maruni.scheduling.daily-check.cron}")
    public void triggerDailyCheck() {
        log.info("Daily check triggered by scheduler");
        dailyCheckOrchestrator.processAllActiveMembers();
    }

    /**
     * 재시도 프로세스 트리거 (5분마다)
     */
    @Scheduled(cron = "${maruni.scheduling.retry.cron}")
    public void triggerRetryProcess() {
        log.info("Retry process triggered by scheduler");
        dailyCheckOrchestrator.processAllRetries();
    }
}