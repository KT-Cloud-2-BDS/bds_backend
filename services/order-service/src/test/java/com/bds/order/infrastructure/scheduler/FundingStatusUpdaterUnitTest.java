package com.bds.order.infrastructure.scheduler;

import com.bds.common.events.funding.FundingStatusChangedEvent;
import com.bds.common.events.order.OrderProcessSettlementEvent;
import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.fixture.FundingFixture;
import com.bds.order.infrastructure.funding.JudgmentOutcome;
import com.bds.order.infrastructure.funding.JudgmentType;
import com.bds.order.infrastructure.messaging.publisher.NotificationEventPublisher;
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
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingStatusUpdaterUnitTest {

    @Mock
    private PaymentEventPublisher paymentEventPublisher;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @InjectMocks
    private FundingStatusUpdater fundingStatusUpdater;
    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;

    @Nested
    @DisplayName("judgeFunding")
    class JudgeFundingTest {

        LocalDateTime now = LocalDateTime.now();

        @ParameterizedTest(name = "{0} 상태에서 판정 진행")
        @EnumSource(value = FundingStatus.class, names = {"ACTIVE", "HOLDING"})
        void 판정_가능_상태에서_목표_달성시_SUCCESS로_변경하고_true를_반환한다(FundingStatus status) {
            Funding funding = FundingFixture.createFunding(1L, status, 1000000L, 500000L,
                    now.minusDays(30), now.minusDays(1));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            JudgmentOutcome result = fundingStatusUpdater.judgeFunding(1L);

            assertThat(result.type()).isEqualTo(JudgmentType.INSTANT_SUCCESS);
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.SUCCESS);
            assertThat(funding.getIsSuccess()).isTrue();
            verify(fundingRepository).save(funding);
            verify(notificationEventPublisher).publishFundingStatusChanged(FundingStatusChangedEvent.of("FUNDING_SUCCESS", 1L, funding.getCreatorId()));
        }

        @ParameterizedTest(name = "{0} 상태에서 판정 진행")
        @EnumSource(value = FundingStatus.class, names = {"ACTIVE", "HOLDING"})
        void 판정_가능_상태에서_목표_미달시_FAILED로_변경하고_false를_반환한다(FundingStatus status) {
            Funding funding = FundingFixture.createFunding(1L, status, 100000L, 500000L,
                    now.minusDays(30), now.minusDays(1));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            JudgmentOutcome result = fundingStatusUpdater.judgeFunding(1L);

            assertThat(result.type()).isEqualTo(JudgmentType.INSTANT_FAILURE);
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.FAILED);
            assertThat(funding.getIsSuccess()).isFalse();
            verify(fundingRepository).save(funding);
            verify(notificationEventPublisher).publishFundingStatusChanged(FundingStatusChangedEvent.of("FUNDING_FAIL", 1L, funding.getCreatorId()));
        }

        @Test
        void 예약펀딩_목표_달성시_RESERVED_SUCCESS를_반환한다() {
            Funding funding = FundingFixture.createReservedFunding(1L, FundingStatus.ACTIVE, 1000000L, 500000L,
                    now.minusDays(30), now.minusDays(1));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            JudgmentOutcome result = fundingStatusUpdater.judgeFunding(1L);

            assertThat(result.type()).isEqualTo(JudgmentType.RESERVED_SUCCESS);
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.SUCCESS);
            assertThat(funding.getIsSuccess()).isTrue();
            verify(fundingRepository).save(funding);
        }

        @Test
        void 예약펀딩_목표_미달시_RESERVED_FAILURE를_반환한다() {
            Funding funding = FundingFixture.createReservedFunding(1L, FundingStatus.ACTIVE, 100L, 500000L,
                    now.minusDays(30), now.minusDays(1));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            JudgmentOutcome result = fundingStatusUpdater.judgeFunding(1L);

            assertThat(result.type()).isEqualTo(JudgmentType.RESERVED_FAILURE);
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.FAILED);
            assertThat(funding.getIsSuccess()).isFalse();
            verify(fundingRepository).save(funding);
        }
    }

    @Nested
    @DisplayName("handleFundingSuccess")
    class HandleFundingSuccessTest {

        @Test
        void PAID_주문에_정산을_요청한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L));
            when(orderService.createSettlementItem(1L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(1L, 10L, 30000L)));

            fundingStatusUpdater.handleFundingSuccess(1L, 100L);

            verify(orderService).createSettlementItem(1L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.type().equals("SETTLEMENT_CONFIRMED") &&
                            event.creatorMemberId().equals(100L) &&
                            event.items().size() == 1));
        }

        @Test
        void 해당_펀딩에_주문이_없으면_orderService_호출_없음() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of());

            fundingStatusUpdater.handleFundingSuccess(1L, 100L);

            verifyNoInteractions(orderService);
            verifyNoInteractions(paymentEventPublisher);
        }

        @Test
        void orderService가_빈값을_반환하면_publish하지_않는다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L));
            when(orderService.createSettlementItem(1L))
                    .thenReturn(Optional.empty());

            fundingStatusUpdater.handleFundingSuccess(1L, 100L);

            verify(orderService).createSettlementItem(1L);
            verifyNoInteractions(paymentEventPublisher);
        }

        @Test
        void 청크_사이즈만큼_조회되면_다음_청크를_조회한다() {
            List<Long> firstChunk = LongStream.rangeClosed(1, 500).boxed().toList();
            List<Long> secondChunk = List.of(501L);

            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(firstChunk);
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(500L), eq(500)))
                    .thenReturn(secondChunk);
            when(orderService.createSettlementItem(anyLong()))
                    .thenAnswer(invocation -> {
                        Long orderId = invocation.getArgument(0);
                        return Optional.of(new OrderProcessSettlementEvent.SettlementItem(orderId, 10L, 30000L));
                    });

            fundingStatusUpdater.handleFundingSuccess(1L, 100L);

            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.PAID, 0L, 500);
            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.PAID, 500L, 500);
            verify(orderService, times(501)).createSettlementItem(anyLong());
            verify(paymentEventPublisher, times(2)).publishSettlement(any());
        }

    }

    @Nested
    @DisplayName("handleReservedFundingSuccess")
    class HandleReservedFundingSuccessTest {

        @Test
        void RESERVED_주문에_결제_요청한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of(2L));
            when(orderService.processReservedFundingConfirmed(2L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(2L, 20L, 50000L)));

            fundingStatusUpdater.handleReservedFundingSuccess(1L, 100L);

            verify(orderService).processReservedFundingConfirmed(2L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.type().equals("RESERVED_FUNDING_CONFIRMED") &&
                            event.creatorMemberId().equals(100L) &&
                            event.items().size() == 1));
        }

        @Test
        void 해당_펀딩에_주문이_없으면_orderService_호출_없음() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of());

            fundingStatusUpdater.handleReservedFundingSuccess(1L, 100L);

            verifyNoInteractions(orderService);
            verifyNoInteractions(paymentEventPublisher);
        }

        @Test
        void orderService가_빈값을_반환하면_publish하지_않는다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of(2L));
            when(orderService.processReservedFundingConfirmed(2L))
                    .thenReturn(Optional.empty());

            fundingStatusUpdater.handleReservedFundingSuccess(1L, 100L);

            verify(orderService).processReservedFundingConfirmed(2L);
            verifyNoInteractions(paymentEventPublisher);
        }

        @Test
        void 청크_사이즈만큼_조회되면_다음_청크를_조회한다() {
            List<Long> firstChunk = LongStream.rangeClosed(1, 500).boxed().toList();
            List<Long> secondChunk = List.of(501L);

            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(firstChunk);
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(500L), eq(500)))
                    .thenReturn(secondChunk);
            when(orderService.processReservedFundingConfirmed(anyLong()))
                    .thenAnswer(invocation -> {
                        Long orderId = invocation.getArgument(0);
                        return Optional.of(new OrderProcessSettlementEvent.SettlementItem(orderId, 10L, 30000L));
                    });

            fundingStatusUpdater.handleReservedFundingSuccess(1L, 100L);

            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.RESERVED, 0L, 500);
            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.RESERVED, 500L, 500);
            verify(orderService, times(501)).processReservedFundingConfirmed(anyLong());
            verify(paymentEventPublisher, times(2)).publishSettlement(any());
        }

    }

    @Nested
    @DisplayName("handleFundingFailure")
    class HandleFundingFailureTest {

        @Test
        void PAID_주문에_환불_요청한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L));
            when(orderService.processFundingFailedRefund(1L))
                    .thenReturn(Optional.of(new OrderProcessSettlementEvent.SettlementItem(1L, 10L, 30000L)));

            fundingStatusUpdater.handleFundingFailure(1L, 100L);

            verify(orderService).processFundingFailedRefund(1L);
            verify(paymentEventPublisher).publishSettlement(argThat(event ->
                    event.type().equals("FUNDING_FAILED_REFUND") &&
                            event.creatorMemberId().equals(100L) &&
                            event.items().size() == 1));
        }

        @Test
        void 해당_펀딩에_주문이_없으면_orderService_호출_없음() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of());

            fundingStatusUpdater.handleFundingFailure(1L, 100L);

            verifyNoInteractions(orderService);
            verifyNoInteractions(paymentEventPublisher);
        }

        @Test
        void orderService가_빈값을_반환하면_publish하지_않는다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L));
            when(orderService.processFundingFailedRefund(1L))
                    .thenReturn(Optional.empty());

            fundingStatusUpdater.handleFundingFailure(1L, 100L);

            verify(orderService).processFundingFailedRefund(1L);
            verifyNoInteractions(paymentEventPublisher);
        }

        @Test
        void 청크_사이즈만큼_조회되면_다음_청크를_조회한다() {
            List<Long> firstChunk = LongStream.rangeClosed(1, 500).boxed().toList();
            List<Long> secondChunk = List.of(501L);

            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(firstChunk);
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(500L), eq(500)))
                    .thenReturn(secondChunk);
            when(orderService.processFundingFailedRefund(anyLong()))
                    .thenAnswer(invocation -> {
                        Long orderId = invocation.getArgument(0);
                        return Optional.of(new OrderProcessSettlementEvent.SettlementItem(orderId, 10L, 30000L));
                    });

            fundingStatusUpdater.handleFundingFailure(1L, 100L);

            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.PAID, 0L, 500);
            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.PAID, 500L, 500);
            verify(orderService, times(501)).processFundingFailedRefund(anyLong());
            verify(paymentEventPublisher, times(2)).publishSettlement(any());
        }
    }

    @Nested
    @DisplayName("handleReservedFundingFailure")
    class HandleReservedFundingFailureTest {

        @Test
        void RESERVED_주문을_취소한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of(2L));

            fundingStatusUpdater.handleReservedFundingFailure(1L);

            verify(orderService).processCancelledUpdate(2L, "FUNDING_FAILED");
        }

        @Test
        void 해당_펀딩에_주문이_없으면_orderService_호출_없음() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of());

            fundingStatusUpdater.handleReservedFundingFailure(1L);

            verifyNoInteractions(orderService);
        }

        @Test
        void 청크_사이즈만큼_조회되면_다음_청크를_조회한다() {
            List<Long> firstChunk = LongStream.rangeClosed(1, 500).boxed().toList();
            List<Long> secondChunk = List.of(501L);

            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(firstChunk);
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(500L), eq(500)))
                    .thenReturn(secondChunk);

            fundingStatusUpdater.handleReservedFundingFailure(1L);

            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.RESERVED, 0L, 500);
            verify(orderRepository).findOrderIdsByFundingIdAndStatus(1L, OrderStatus.RESERVED, 500L, 500);
            verify(orderService, times(501)).processCancelledUpdate(anyLong(), eq("FUNDING_FAILED"));
        }
    }

    @Nested
    @DisplayName("activateFunding")
    class ActivateFundingTest {

        LocalDateTime now = LocalDateTime.now();

        @Test
        void SCHEDULED_펀딩을_ACTIVE로_전환한다() {
            Funding funding = FundingFixture.createScheduledFunding(1L,
                    now.minusDays(1),
                    now.plusDays(30));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            fundingStatusUpdater.activateFunding(1L);

            assertThat(funding.getStatus()).isEqualTo(FundingStatus.ACTIVE);
            verify(fundingRepository).save(funding);
            verify(notificationEventPublisher).publishFundingStatusChanged(FundingStatusChangedEvent.of("FUNDING_START", 1L, funding.getCreatorId()));
        }
    }
}
