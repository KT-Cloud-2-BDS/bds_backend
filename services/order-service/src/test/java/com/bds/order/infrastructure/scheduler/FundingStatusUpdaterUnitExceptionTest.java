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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingStatusUpdaterUnitExceptionTest {

    @Mock
    private PaymentEventPublisher paymentEventPublisher;
    @InjectMocks
    private FundingStatusUpdater fundingStatusUpdater;
    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;

    @Test
    void 존재하지_않는_fundingId로_judgeFunding_호출_시_IllegalStateException_발생() {
        when(fundingRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fundingStatusUpdater.judgeFunding(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FUNDING_JUDGE] Funding not found: fundingId=999");
    }

    @ParameterizedTest(name = "{0} 상태에서는 판정 불가")
    @EnumSource(value = FundingStatus.class, names = {"SCHEDULED", "SUCCESS", "FAILED"})
    void 판정_가능_상태가_아닌_펀딩은_IllegalStateException_발생(FundingStatus status) {
        Funding funding = FundingFixture.createFunding(1L, status, 1000000L, 500000L,
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));

        when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

        assertThatThrownBy(() -> fundingStatusUpdater.judgeFunding(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FUNDING_JUDGE] 판정 불가 상태");
    }

    @Test
    void 존재하지_않는_fundingId로_activateFunding_호출_시_IllegalStateException_발생() {
        when(fundingRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fundingStatusUpdater.activateFunding(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FUNDING_ACTIVATE] Funding not found: fundingId=999");
    }


    @ParameterizedTest(name = "{0} 상태에서는 활성화 불가")
    @EnumSource(value = FundingStatus.class, names = {"ACTIVE", "HOLDING", "SUCCESS", "FAILED"})
    void SCHEDULED가_아닌_펀딩은_활성화하지_않는다(FundingStatus status) {
        Funding funding = FundingFixture.createFunding(1L, status, 0L, 1000000L,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().plusDays(30));

        when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

        fundingStatusUpdater.activateFunding(1L);

        verify(fundingRepository, never()).save(any());
    }

    @Nested
    @DisplayName("handleFundingSuccess 예외")
    class HandleFundingSuccessExceptionTest {

        @Test
        void orderService_실패해도_다른_주문은_계속_처리한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L, 2L));
            when(orderService.createSettlementItem(1L))
                    .thenReturn(Optional.empty());
            when(orderService.createSettlementItem(2L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(2L, 20L, 50000L)));

            fundingStatusUpdater.handleFundingSuccess(1L, 100L);

            verify(orderService).createSettlementItem(1L);
            verify(orderService).createSettlementItem(2L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.items().size() == 1 && event.items().get(0).orderId().equals(2L)));
        }
    }

    @Nested
    @DisplayName("handleReservedFundingSuccess 예외")
    class HandleReservedFundingSuccessExceptionTest {

        @Test
        void orderService_실패해도_다른_주문은_계속_처리한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of(1L, 2L));
            when(orderService.processReservedFundingConfirmed(1L))
                    .thenReturn(Optional.empty());
            when(orderService.processReservedFundingConfirmed(2L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(2L, 20L, 50000L)));

            fundingStatusUpdater.handleReservedFundingSuccess(1L, 100L);

            verify(orderService).processReservedFundingConfirmed(1L);
            verify(orderService).processReservedFundingConfirmed(2L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.items().size() == 1 && event.items().get(0).orderId().equals(2L)));
        }
    }

    @Nested
    @DisplayName("handleFundingFailure 예외")
    class HandleFundingFailureExceptionTest {

        @Test
        void orderService_실패해도_다른_주문은_계속_처리한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L, 2L));
            when(orderService.processFundingFailedRefund(1L))
                    .thenReturn(Optional.empty());
            when(orderService.processFundingFailedRefund(2L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(2L, 20L, 50000L)));

            fundingStatusUpdater.handleFundingFailure(1L, 100L);

            verify(orderService).processFundingFailedRefund(1L);
            verify(orderService).processFundingFailedRefund(2L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.items().size() == 1 && event.items().get(0).orderId().equals(2L)));
        }
    }
}
