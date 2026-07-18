package com.bds.order.infrastructure.funding;

import com.bds.order.infrastructure.scheduler.FundingStatusUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingJudgmentOrchestrator {

    private final FundingStatusUpdater fundingStatusUpdater;

    public void execute(Long fundingId) {
        boolean isSuccess = fundingStatusUpdater.judgeFunding(fundingId);

        if (isSuccess) {
            fundingStatusUpdater.handleFundingSuccess(fundingId);
        } else {
            fundingStatusUpdater.handleFundingFailure(fundingId);
        }
    }
}