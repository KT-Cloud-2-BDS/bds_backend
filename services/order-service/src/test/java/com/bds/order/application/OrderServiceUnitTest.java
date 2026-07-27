package com.bds.order.application;

import com.bds.common.events.order.OrderProcessPayEvent;
import com.bds.common.events.order.OrderProcessRefundEvent;
import com.bds.common.events.order.OrderStatusChangedEvent;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.domain.orderReward.OrderReward;
import com.bds.order.domain.orderReward.OrderRewardRepository;
import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.infrastructure.messaging.publisher.NotificationEventPublisher;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.bds.order.infrastructure.order.OrderDetailProjection;
import com.bds.order.infrastructure.order.OrderListProjection;
import com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection;
import com.bds.order.presentation.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.bds.common.events.order.OrderProcessSettlementEvent.SettlementItem;
import static com.bds.order.fixture.BillingFixture.createBillingRequest;
import static com.bds.order.fixture.BillingFixture.rq;
import static com.bds.order.fixture.FundingFixture.createActiveFunding;
import static com.bds.order.fixture.FundingFixture.createReservedFunding;
import static com.bds.order.fixture.OrderFixture.createOrder;
import static com.bds.order.fixture.RewardFixture.createReward;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    private static final Long memberId = 1L;
    private static final Long orderId = 1L;
    private static final Long fundingId = 1L;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private RewardRepository rewardRepository;
    @Mock
    private OrderRewardRepository orderRewardRepository;
    @Mock
    private PaymentEventPublisher paymentEventPublisher;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @InjectMocks
    private OrderService orderService;


    private Order createOrderWithOrderReward(OrderStatus status) {
        OrderReward orw = OrderReward.reconstitute(1L, orderId, 1L, 2, 3000L, 20000L);
        return Order.reconstitute(
                1L, "ORD-001", memberId, status,
                20000L, 3000L, List.of(orw),
                null, LocalDateTime.now(), LocalDateTime.now(), null, null
        );
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetAllOrdersTest {

        @Test
        void 회원의_주문_목록을_반환한다() {
            Pageable pageable = PageRequest.of(0, 20);
            LocalDateTime now = LocalDateTime.now();

            OrderListProjection projection = new OrderListProjection(
                    1L, "ORD-001", OrderStatus.PENDING,
                    33000L, 3000L, now,
                    "테스트 펀딩", 100L, now.plusDays(30), false, now
            );

            given(orderRepository.findOrderListWithFunding(memberId, pageable))
                    .willReturn(List.of(projection));

            List<OrderResponseDto> result = orderService.getAllOrders(memberId, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).orderNo()).isEqualTo("ORD-001");
            assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.get(0).billingAmount()).isEqualTo(36000L);
            assertThat(result.get(0).title()).isEqualTo("테스트 펀딩");
        }

        @Test
        void 주문이_없으면_빈_목록을_반환한다() {
            Pageable pageable = PageRequest.of(0, 20);

            given(orderRepository.findOrderListWithFunding(memberId, pageable))
                    .willReturn(List.of());

            List<OrderResponseDto> result = orderService.getAllOrders(memberId, pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("주문 상세 조회")
    class GetOrderDetailTest {

        @Test
        void 주문_상세를_반환한다() {
            LocalDateTime now = LocalDateTime.now();

            OrderDetailProjection orderProjection = new OrderDetailProjection(
                    1L, "ORD-001", OrderStatus.PAID,
                    33000L, 3000L, now,
                    1L, "테스트 펀딩", 100L, now.plusDays(30), false,
                    null, null, now, null
            );
            OrderRewardDetailProjection rewardProjection = new OrderRewardDetailProjection(
                    1L, 2, 20000L, 3000L, "리워드A", BadgeType.ULTRA_EARLY_BIRD
            );

            given(orderRepository.findOrderDetailWithFunding(memberId, orderId))
                    .willReturn(Optional.of(orderProjection));
            given(orderRewardRepository.findOrderRewardDetailsWithReward(orderId))
                    .willReturn(List.of(rewardProjection));

            OrderDetailResponseDto result = orderService.getOrderDetail(memberId, orderId);

            assertThat(result.orderNo()).isEqualTo("ORD-001");
            assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result.title()).isEqualTo("테스트 펀딩");
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewards().get(0).name()).isEqualTo("리워드A");
            assertThat(result.rewardAmount()).isEqualTo(33000L);
            assertThat(result.totalShippingCharge()).isEqualTo(3000L);
            assertThat(result.totalBillingAmount()).isEqualTo(36000L);
        }
    }

    @Nested
    @DisplayName("빌링 생성")
    class CreateBillingTest {
        LocalDateTime now = LocalDateTime.now();

        private Order invocationOrder(InvocationOnMock invocation) {
            Order order = invocation.getArgument(0);
            return Order.reconstitute(
                    1L, "ORD-001", order.getMemberId(), order.getStatus(),
                    order.getTotalRewardAmount(), order.getTotalShippingCharge(),
                    order.getOrderRewards(),
                    order.getCancelReason(), now, now,
                    order.getCancelledAt(), order.getExpiresAt()
            );
        }

        @Test
        void 정상적으로_빌링을_생성한다() {
            BillingRequestDto reqDto = createBillingRequest(fundingId, rq(1L, 2));
            Funding funding = createActiveFunding(fundingId, 500000L, 1000000L);
            Reward reward = createReward(1L, fundingId, 10000L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(anyList(), eq(fundingId)))
                    .willReturn(List.of(reward));
            given(orderRepository.save(any(Order.class))).willAnswer(this::invocationOrder);

            BillingResponseDto result = orderService.createBilling(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.rewardAmount()).isEqualTo(20000L);
            assertThat(result.totalShippingCharge()).isEqualTo(3000L);
            assertThat(result.totalBillingAmount()).isEqualTo(23000L);
        }

        @Test
        void 여러_리워드로_빌링을_생성한다() {
            BillingRequestDto reqDto = createBillingRequest(fundingId, rq(1L, 2), rq(2L, 1));
            Funding funding = createActiveFunding(fundingId, 500000L, 1000000L);
            Reward reward1 = createReward(1L, fundingId, 10000L, 3000L);
            Reward reward2 = createReward(2L, fundingId, 20000L, 5000L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(anyList(), eq(fundingId)))
                    .willReturn(List.of(reward1, reward2));
            given(orderRepository.save(any(Order.class))).willAnswer(this::invocationOrder);

            BillingResponseDto result = orderService.createBilling(memberId, reqDto);

            assertThat(result.rewards()).hasSize(2);
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.rewardAmount()).isEqualTo(40000L);
            assertThat(result.totalShippingCharge()).isEqualTo(8000L);
            assertThat(result.totalBillingAmount()).isEqualTo(48000L);
        }

        @Test
        void 예약_주문으로_빌링을_생성한다() {
            BillingRequestDto reqDto = createBillingRequest(fundingId, rq(1L, 2));
            Funding funding = createActiveFunding(fundingId, 500000L, 1000000L);
            Reward reward = createReward(1L, fundingId, 10000L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(anyList(), eq(fundingId)))
                    .willReturn(List.of(reward));
            given(orderRepository.save(any(Order.class))).willAnswer(this::invocationOrder);

            BillingResponseDto result = orderService.createBilling(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.rewardAmount()).isEqualTo(20000L);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrderTest {

        @Test
        void 정상적으로_주문을_취소한다() {
            Order order = createOrderWithOrderReward(OrderStatus.PAID);
            given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));

            OrderCancelResponseDto result = orderService.cancelOrder(memberId, orderId, new OrderCancelRequestDto(fundingId));

            assertThat(result.orderNo()).isEqualTo("ORD-001");
            assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.cancelledAt()).isNotNull();
            assertThat(result.refundStatus()).isEqualTo("REFUND_REQUESTED");
            verify(rewardRepository).increaseRemainQty(1L, 2);
            verify(paymentEventPublisher).publishRefund(any(OrderProcessRefundEvent.class));
        }
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrderTest {

        @Test
        void 정상적으로_즉시펀딩의_주문을_생성한다() {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(orderId, fundingId, true);
            Funding funding = createActiveFunding(fundingId, 500000L, 1000000L);
            Order order = createOrderWithOrderReward(OrderStatus.PENDING);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));
            given(rewardRepository.decreaseStock(1L, 2)).willReturn(1);

            OrderCreateResponseDto result = orderService.createOrder(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.orderNo()).isEqualTo("ORD-001");
            assertThat(result.totalBillingAmount()).isEqualTo(23000L);
            verify(paymentEventPublisher).publishPay(any(OrderProcessPayEvent.class));
        }

        @Test
        void 정상적으로_예약펀딩의_주문을_생성한다() {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(orderId, fundingId, true);
            Funding funding = createReservedFunding(fundingId, FundingStatus.ACTIVE, 500000L, 1000000L, LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(2));
            Order order = createOrderWithOrderReward(OrderStatus.PENDING);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));
            given(rewardRepository.decreaseStock(1L, 2)).willReturn(1);

            OrderCreateResponseDto result = orderService.createOrder(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.orderNo()).isEqualTo("ORD-001");
            assertThat(result.orderStatus()).isEqualTo(OrderStatus.RESERVED);
            assertThat(result.totalBillingAmount()).isEqualTo(23000L);
            verify(paymentEventPublisher, never()).publishPay(any(OrderProcessPayEvent.class));
        }
    }

    @Nested
    @DisplayName("processStatusUpdate")
    class ProcessStatusUpdateTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "PAYING, PAID",
                "PAID, CONFIRMED",
                "PAYING, CONFIRMED",
                "CANCELLED, REFUNDED"
        })
        void 허용된_상태_전이는_저장된다(OrderStatus from, OrderStatus to) {
            Order order = createOrder(from);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            orderService.processStatusUpdate(orderId, to);

            assertThat(order.getStatus()).isEqualTo(to);
            verify(orderRepository).save(order);
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "PAYING, PAID",
                "CANCELLED, REFUNDED"
        })
        void PAID_혹은_REFUNDED상태로_변경시_알림메시지가_발행된다(OrderStatus from, OrderStatus to) {
            Order order = createOrderWithOrderReward(from);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.findFundingTitleByOrderId(orderId)).thenReturn(Optional.of("title"));

            if (to == OrderStatus.PAID) {
                when(orderRepository.findFundingIdByOrderId(orderId)).thenReturn(Optional.of(1L));
            }

            orderService.processStatusUpdate(orderId, to);

            verify(orderRepository).findFundingTitleByOrderId(anyLong());
            verify(notificationEventPublisher).publishStatusChanged(any(OrderStatusChangedEvent.class));

            if (to == OrderStatus.PAID) {
                verify(fundingRepository).increaseCurrentAmount(eq(1L), eq(order.getTotalAmount()));
            }
        }


        @Test
        void 존재하지_않는_주문이면_save를_호출하지_않는다() {
            when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            orderService.processStatusUpdate(999L, OrderStatus.PAID);

            verify(orderRepository, never()).save(any());
        }

        @Test
        void 전이_불가능한_상태면_save를_호출하지_않는다() {
            Order order = createOrder(OrderStatus.REFUNDED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            orderService.processStatusUpdate(orderId, OrderStatus.PAID);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processCancelledUpdate")
    class ProcessCancelledUpdateTest {

        @Test
        void 취소_가능한_상태에서_cancelReason과_함께_취소된다() {
            Order order = createOrder(OrderStatus.PAYING);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            orderService.processCancelledUpdate(orderId, "PAYMENT_CANCELLED");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo("PAYMENT_CANCELLED");
            verify(orderRepository).save(order);
        }

        @Test
        void 취소_시_재고가_복구된다() {
            Order order = createOrderWithOrderReward(OrderStatus.PAYING);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            orderService.processCancelledUpdate(orderId, "PAYMENT_CANCELLED");

            verify(rewardRepository).increaseRemainQty(1L, 2);
        }

        @Test
        void 존재하지_않는_주문이면_아무_동작도_하지_않는다() {
            when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            orderService.processCancelledUpdate(999L, "PAYMENT_CANCELLED");

            verify(orderRepository, never()).save(any());
            verify(rewardRepository, never()).increaseRemainQty(anyLong(), anyInt());
        }
    }


    @Nested
    @DisplayName("createSettlementItem")
    class createSettlementItemTest {

        @Test
        void 주문번호가_DB에_없으면_empty를_반환한다() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

            assertThat(orderService.createSettlementItem(orderId)).isEmpty();
        }

        @Test
        void PAID_주문의_정산_항목을_반환한다() {
            Order order = createOrder(OrderStatus.PAID);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            Optional<SettlementItem> result = orderService.createSettlementItem(1L);

            assertThat(result).isPresent();
            assertThat(result.get().orderId()).isEqualTo(order.getId());
            assertThat(result.get().memberId()).isEqualTo(order.getMemberId());
            assertThat(result.get().amount()).isEqualTo(order.getTotalAmount());
        }
    }

    @Nested
    @DisplayName("processReservedFundingConfirmed")
    class ProcessReservedFundingConfirmedTest {

        @Test
        void 주문번호가_DB에_없으면_empty를_반환한다() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

            assertThat(orderService.processReservedFundingConfirmed(orderId)).isEmpty();
        }

        @Test
        void RESERVED_주문을_PAYING으로_변경하고_정산_항목을_반환한다() {
            Order order = createOrder(OrderStatus.RESERVED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            Optional<SettlementItem> result = orderService.processReservedFundingConfirmed(orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYING);
            verify(orderRepository).save(order);
            assertThat(result).isPresent();
            assertThat(result.get().orderId()).isEqualTo(order.getId());
            assertThat(result.get().memberId()).isEqualTo(order.getMemberId());
            assertThat(result.get().amount()).isEqualTo(order.getTotalAmount());
        }

        @Test
        void 상태_전이_실패시_empty를_반환한다() {
            Order order = createOrder(OrderStatus.CANCELLED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            Optional<SettlementItem> result = orderService.processReservedFundingConfirmed(orderId);

            assertThat(result).isEmpty();
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processFundingFailedRefund")
    class ProcessFundingFailedRefundTest {

        @Test
        void 주문번호가_DB에_없으면_empty를_반환한다() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

            assertThat(orderService.processFundingFailedRefund(orderId)).isEmpty();
        }

        @Test
        void PAID_주문을_CANCELLED로_변경하고_재고를_복구하고_정산_항목을_반환한다() {
            Order order = createOrderWithOrderReward(OrderStatus.PAID);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

            Optional<SettlementItem> result = orderService.processFundingFailedRefund(1L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo("FUNDING_FAILED");
            verify(rewardRepository).increaseRemainQty(1L, 2);
            verify(orderRepository).save(order);
            assertThat(result).isPresent();
            assertThat(result.get().orderId()).isEqualTo(orderId);
            assertThat(result.get().memberId()).isEqualTo(memberId);
            assertThat(result.get().amount()).isEqualTo(23000L);
        }

        @Test
        void 상태_전이_실패시_empty를_반환한다() {
            Order order = createOrder(OrderStatus.CANCELLED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            Optional<SettlementItem> result = orderService.processFundingFailedRefund(orderId);

            assertThat(result).isEmpty();
            verify(orderRepository, never()).save(any());
        }
    }

}