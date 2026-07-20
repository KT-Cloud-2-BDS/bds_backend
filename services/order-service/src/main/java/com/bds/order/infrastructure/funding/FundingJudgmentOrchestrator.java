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
        JudgmentOutcome outcome = fundingStatusUpdater.judgeFunding(fundingId);

        switch (outcome.type()) {
            case INSTANT_SUCCESS -> fundingStatusUpdater.handleFundingSuccess(fundingId, outcome.creatorMemberId());
            case INSTANT_FAILURE -> fundingStatusUpdater.handleFundingFailure(fundingId, outcome.creatorMemberId());
            case RESERVED_SUCCESS ->
                    fundingStatusUpdater.handleReservedFundingSuccess(fundingId, outcome.creatorMemberId());
            case RESERVED_FAILURE -> fundingStatusUpdater.handleReservedFundingFailure(fundingId);
        }
    }
}