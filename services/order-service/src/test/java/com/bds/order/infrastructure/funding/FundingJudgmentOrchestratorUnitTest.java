package com.bds.order.infrastructure.funding;

import com.bds.order.infrastructure.scheduler.FundingStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingJudgmentOrchestratorUnitTest {

    @InjectMocks
    private FundingJudgmentOrchestrator orchestrator;

    @Mock
    private FundingStatusUpdater fundingStatusUpdater;

    @Test
    void 펀딩_성공_시_handleFundingSuccess를_호출한다() {
        when(fundingStatusUpdater.judgeFunding(1L)).thenReturn(true);

        orchestrator.execute(1L);

        verify(fundingStatusUpdater).handleFundingSuccess(1L);
        verify(fundingStatusUpdater, never()).handleFundingFailure(1L);
    }

    @Test
    void 펀딩_실패_시_handleFundingFailure를_호출한다() {
        when(fundingStatusUpdater.judgeFunding(1L)).thenReturn(false);

        orchestrator.execute(1L);

        verify(fundingStatusUpdater).handleFundingFailure(1L);
        verify(fundingStatusUpdater, never()).handleFundingSuccess(1L);
    }

    @Test
    void judgeFunding에서_예외_발생_시_handleSuccess_handleFailure_호출하지_않는다() {
        when(fundingStatusUpdater.judgeFunding(999L))
                .thenThrow(new IllegalStateException("[FUNDING_JUDGE] Funding not found: fundingId=999"));

        assertThatThrownBy(() -> orchestrator.execute(999L))
                .isInstanceOf(IllegalStateException.class);

        verify(fundingStatusUpdater, never()).handleFundingSuccess(anyLong());
        verify(fundingStatusUpdater, never()).handleFundingFailure(anyLong());
    }
}
