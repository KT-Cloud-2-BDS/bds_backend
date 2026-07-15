package com.bds.order.infrastructure.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingTaskScheduler {

    private final TaskScheduler fundingSchedulerExecutor;
    private final FundingStatusUpdater fundingStatusUpdater;

    public void scheduleHoldToJudgment(Long fundingId, LocalDateTime holdTo) {
        Instant holdToInstant = holdTo.atZone(ZoneId.systemDefault()).toInstant();

        try {
            fundingSchedulerExecutor.schedule(() -> fundingStatusUpdater.judgeFunding(fundingId), holdToInstant);
            log.info("[FUNDING_TASK] 펀딩 판정 예약 - fundingId: {}, holdTo: {}", fundingId, holdTo);
        } catch (TaskRejectedException ex) {
            log.error("[FUNDING_TASK] 펀딩 판정 예약 거절 - fundingId: {}, holdTo: {}", fundingId, holdTo, ex);
        }
    }

    public void scheduleActivation(Long fundingId, LocalDateTime startAt) {
        Instant startInstant = startAt.atZone(ZoneId.systemDefault()).toInstant();

        try {
            fundingSchedulerExecutor.schedule(() -> fundingStatusUpdater.activateFunding(fundingId), startInstant);
            log.info("[FUNDING_TASK] 펀딩 활성화 예약 - fundingId: {}, startAt: {}", fundingId, startAt);
        } catch (TaskRejectedException ex) {
            log.error("[FUNDING_TASK] 펀딩 활성화 예약 거절 - fundingId: {}, startAt: {}", fundingId, startAt, ex);
        }
    }
}
