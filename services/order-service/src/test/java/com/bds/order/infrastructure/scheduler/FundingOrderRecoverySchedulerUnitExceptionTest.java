package com.bds.order.infrastructure.scheduler;

import com.bds.common.events.order.PaymentProcessSettlementEvent;
import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.fixture.FundingFixture;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingOrderRecoverySchedulerUnitExceptionTest {

    @InjectMocks
    private FundingOrderRecoveryScheduler recoveryScheduler;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private FundingStatusUpdater fundingStatusUpdater;

    @Nested
    @DisplayName("주문 단위 실패 격리")
    class OrderLevelFailureIsolation {
        @Test
        void 재발송_중_단건_실패해도_다음_주문은_계속_처리한다() {
            Funding funding = FundingFixture.createFunding(1L, FundingStatus.FAILED, 100000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.CANCELLED), eq(0L), eq(500)))
                    .thenReturn(List.of(10L, 11L));
            when(orderService.createSettlementItem(10L))
                    .thenThrow(new RuntimeException("lock timeout"));
            when(orderService.createSettlementItem(11L))
                    .thenReturn(Optional.of(new PaymentProcessSettlementEvent.SettlementItem(11L, 1L, 30000L)));

            recoveryScheduler.recover();

            verify(orderService).createSettlementItem(10L);
            verify(orderService).createSettlementItem(11L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.items().size() == 1 && event.items().get(0).orderId().equals(11L)));
        }
    }

    @Nested
    @DisplayName("펀딩 단위 실패 격리")
    class FundingLevelFailureIsolation {
        @Test
        void SUCCESS_펀딩_복구_중_예외_발생해도_다음_펀딩은_계속_처리한다() {
            Funding funding1 = FundingFixture.createFunding(1L, FundingStatus.SUCCESS, 1000000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            Funding funding2 = FundingFixture.createFunding(2L, FundingStatus.SUCCESS, 1000000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding1, funding2));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("unexpected")).when(fundingStatusUpdater).handleFundingSuccess(eq(1L), anyLong());

            recoveryScheduler.recover();

            verify(fundingStatusUpdater).handleFundingSuccess(eq(1L), anyLong());
            verify(fundingStatusUpdater).handleFundingSuccess(eq(2L), anyLong());
        }

        @Test
        void FAILED_펀딩_복구_중_예외_발생해도_다음_펀딩은_계속_처리한다() {
            Funding funding1 = FundingFixture.createFunding(1L, FundingStatus.FAILED, 100000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            Funding funding2 = FundingFixture.createFunding(2L, FundingStatus.FAILED, 100000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding1, funding2));
            when(orderRepository.findOrderIdsByFundingIdAndStatus(anyLong(), eq(OrderStatus.CANCELLED), eq(0L), eq(500)))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("unexpected")).when(fundingStatusUpdater).handleFundingFailure(eq(1L), anyLong());

            recoveryScheduler.recover();

            verify(fundingStatusUpdater).handleFundingFailure(eq(1L), anyLong());
            verify(fundingStatusUpdater).handleFundingFailure(eq(2L), anyLong());
        }
    }
}