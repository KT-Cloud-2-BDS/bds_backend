package com.bds.order.infrastructure.funding;

import com.bds.order.infrastructure.scheduler.FundingStatusUpdater;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    void judgeFunding에서_예외_발생_시_handle_메서드를_호출하지_않는다() {
        when(fundingStatusUpdater.judgeFunding(999L))
                .thenThrow(new IllegalStateException("[FUNDING_JUDGE] Funding not found: fundingId=999"));

        assertThatThrownBy(() -> orchestrator.execute(999L))
                .isInstanceOf(IllegalStateException.class);

        verify(fundingStatusUpdater, never()).handleFundingSuccess(anyLong(), anyLong());
        verify(fundingStatusUpdater, never()).handleFundingFailure(anyLong(), anyLong());
        verify(fundingStatusUpdater, never()).handleReservedFundingSuccess(anyLong(), anyLong());
        verify(fundingStatusUpdater, never()).handleReservedFundingFailure(anyLong());
    }

    @Nested
    @DisplayName("즉시 펀딩")
    class InstantFundingClass {

        @Test
        void 펀딩_성공_시_handleFundingSuccess를_호출한다() {
            when(fundingStatusUpdater.judgeFunding(1L))
                    .thenReturn(new JudgmentOutcome(JudgmentType.INSTANT_SUCCESS, 100L));

            orchestrator.execute(1L);

            verify(fundingStatusUpdater).handleFundingSuccess(1L, 100L);
            verify(fundingStatusUpdater, never()).handleFundingFailure(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleReservedFundingSuccess(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleReservedFundingFailure(anyLong());
        }

        @Test
        void 펀딩_실패_시_handleFundingFailure를_호출한다() {
            when(fundingStatusUpdater.judgeFunding(1L))
                    .thenReturn(new JudgmentOutcome(JudgmentType.INSTANT_FAILURE, 100L));

            orchestrator.execute(1L);

            verify(fundingStatusUpdater).handleFundingFailure(1L, 100L);
            verify(fundingStatusUpdater, never()).handleFundingSuccess(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleReservedFundingSuccess(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleReservedFundingFailure(anyLong());
        }
    }

    @Nested
    @DisplayName("예약 펀딩")
    class ReservedFundingClass {

        @Test
        void 펀딩_성공_시_handleReservedFundingSuccess를_호출한다() {
            when(fundingStatusUpdater.judgeFunding(1L))
                    .thenReturn(new JudgmentOutcome(JudgmentType.RESERVED_SUCCESS, 100L));

            orchestrator.execute(1L);

            verify(fundingStatusUpdater).handleReservedFundingSuccess(1L, 100L);
            verify(fundingStatusUpdater, never()).handleFundingSuccess(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleFundingFailure(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleReservedFundingFailure(anyLong());
        }

        @Test
        void 펀딩_실패_시_handleReservedFundingFailure를_호출한다() {
            when(fundingStatusUpdater.judgeFunding(1L))
                    .thenReturn(new JudgmentOutcome(JudgmentType.RESERVED_FAILURE, 100L));

            orchestrator.execute(1L);

            verify(fundingStatusUpdater).handleReservedFundingFailure(1L);
            verify(fundingStatusUpdater, never()).handleFundingSuccess(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleFundingFailure(anyLong(), anyLong());
            verify(fundingStatusUpdater, never()).handleReservedFundingSuccess(anyLong(), anyLong());
        }
    }
}
