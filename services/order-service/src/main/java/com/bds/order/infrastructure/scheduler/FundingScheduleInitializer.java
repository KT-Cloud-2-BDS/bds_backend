package com.bds.order.infrastructure.scheduler;


import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.infrastructure.funding.FundingJudgmentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingScheduleInitializer {

    private final FundingRepository fundingRepository;
    private final FundingTaskScheduler fundingTaskScheduler;
    private final FundingJudgmentOrchestrator fundingJudgmentOrchestrator;
    private final FundingStatusUpdater fundingStatusUpdater;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        LocalDateTime now = LocalDateTime.now();

        // 1. SCHEDULED, startAt > now → 활성화 예약 + holdTo 판정 예약
        List<Funding> upcomingScheduled = fundingRepository.findByStatusAndStartAtAfter(FundingStatus.SCHEDULED, now);
        for (Funding funding : upcomingScheduled) {
            fundingTaskScheduler.scheduleActivation(funding.getId(), funding.getStartAt());
            fundingTaskScheduler.scheduleHoldToJudgment(funding.getId(), funding.getHoldTo());
        }
        log.info("[FUNDING_SCHEDULE_INIT] SCHEDULED 펀딩 예약 - {}건", upcomingScheduled.size());

        // 2. ACTIVE, holdTo > now → holdTo 판정 예약
        List<Funding> activeFundings = fundingRepository.findByStatusAndHoldToAfter(FundingStatus.ACTIVE, now);
        for (Funding funding : activeFundings) {
            fundingTaskScheduler.scheduleHoldToJudgment(funding.getId(), funding.getHoldTo());
        }
        log.info("[FUNDING_SCHEDULE_INIT] ACTIVE 펀딩 판정 예약 - {}건", activeFundings.size());

        // 3. not SUCCESS/FAILED, holdTo <= now → 즉시 판정
        List<Funding> missedFundings = fundingRepository.findFundingsReadyForJudgment(now);
        for (Funding funding : missedFundings) {
            fundingJudgmentOrchestrator.execute(funding.getId());
        }
        log.info("[FUNDING_SCHEDULE_INIT] 서버 시작 보정 - {}건 즉시 판정", missedFundings.size());

        // 4. SCHEDULED, startAt <= now → 즉시 ACTIVE + holdTo 판정 예약
        List<Funding> missedActivations = fundingRepository.findByStatusAndStartAtBeforeOrEqual(FundingStatus.SCHEDULED, now);
        for (Funding funding : missedActivations) {
            fundingStatusUpdater.activateFunding(funding.getId());
            fundingTaskScheduler.scheduleHoldToJudgment(funding.getId(), funding.getHoldTo());
        }
        log.info("[FUNDING_SCHEDULE_INIT] 서버 시작 보정 - {}건 즉시 ACTIVE 전환", missedActivations.size());
    }
}