package com.bds.order.infrastructure.scheduler;

import com.bds.common.events.order.OrderProcessSettlementEvent;
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
class FundingOrderRecoverySchedulerUnitTest {

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
    @DisplayName("Instant + Success 복구")
    class RecoverInstantSuccess {

        @Test
        void handleFundingSuccess를_호출한다() {
            Funding funding = FundingFixture.createFunding(1L, FundingStatus.SUCCESS, 1000000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            recoveryScheduler.recover();

            verify(fundingStatusUpdater).handleFundingSuccess(1L, funding.getCreatorId());
        }
    }

    @Nested
    @DisplayName("Instant + Failed 복구")
    class RecoverInstantFailure {

        @Test
        void CANCELLED_주문을_재발송하고_handleFundingFailure를_호출한다() {
            Funding funding = FundingFixture.createFunding(1L, FundingStatus.FAILED, 100000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.CANCELLED), eq(0L), eq(500)))
                    .thenReturn(List.of(10L));
            when(orderService.createSettlementItem(10L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(10L, 1L, 30000L)));

            recoveryScheduler.recover();

            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.type().equals("FUNDING_FAILED_REFUND") &&
                            event.items().size() == 1));
            verify(fundingStatusUpdater).handleFundingFailure(1L, funding.getCreatorId());
        }

        @Test
        void CANCELLED_주문이_없으면_재발송하지_않고_handleFundingFailure만_호출한다() {
            Funding funding = FundingFixture.createFunding(1L, FundingStatus.FAILED, 100000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.CANCELLED), eq(0L), eq(500)))
                    .thenReturn(List.of());

            recoveryScheduler.recover();

            verifyNoInteractions(paymentEventPublisher);
            verify(fundingStatusUpdater).handleFundingFailure(1L, funding.getCreatorId());
        }
    }

    @Nested
    @DisplayName("Reserved + Success 복구")
    class RecoverReservedSuccess {

        @Test
        void PAYING_주문을_재발송하고_handleReservedFundingSuccess를_호출한다() {
            Funding funding = FundingFixture.createReservedFunding(1L, FundingStatus.SUCCESS, 1000000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAYING), eq(0L), eq(500)))
                    .thenReturn(List.of(20L));
            when(orderService.createSettlementItem(20L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(20L, 2L, 50000L)));

            recoveryScheduler.recover();

            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.type().equals("RESERVED_FUNDING_CONFIRMED") &&
                            event.items().size() == 1));
            verify(fundingStatusUpdater).handleReservedFundingSuccess(1L, funding.getCreatorId());
        }

        @Test
        void PAYING_주문이_없으면_재발송하지_않고_handleReservedFundingSuccess만_호출한다() {
            Funding funding = FundingFixture.createReservedFunding(1L, FundingStatus.SUCCESS, 1000000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAYING), eq(0L), eq(500)))
                    .thenReturn(List.of());

            recoveryScheduler.recover();

            verifyNoInteractions(paymentEventPublisher);
            verify(fundingStatusUpdater).handleReservedFundingSuccess(1L, funding.getCreatorId());
        }
    }

    @Nested
    @DisplayName("Reserved + Failed 복구")
    class RecoverReservedFailure {

        @Test
        void handleReservedFundingFailure를_호출한다() {
            Funding funding = FundingFixture.createReservedFunding(1L, FundingStatus.FAILED, 100000L, 500000L,
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of(funding));

            recoveryScheduler.recover();

            verify(fundingStatusUpdater).handleReservedFundingFailure(1L);
        }
    }

    @Nested
    @DisplayName("복구 대상 없음")
    class NoRecoveryTarget {

        @Test
        void SUCCESS_FAILED_펀딩이_없으면_아무_작업도_하지_않는다() {
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.SUCCESS), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(fundingRepository.findByStatusAndUpdatedAfter(eq(FundingStatus.FAILED), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            recoveryScheduler.recover();

            verifyNoInteractions(fundingStatusUpdater);
            verifyNoInteractions(orderService);
            verifyNoInteractions(paymentEventPublisher);
        }
    }
}