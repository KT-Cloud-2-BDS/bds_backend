package com.bds.order.application;

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
import com.bds.order.infrastructure.order.OrderDetailProjection;
import com.bds.order.infrastructure.order.OrderListProjection;
import com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection;
import com.bds.order.presentation.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private OrderRewardRepository orderRewardRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 목록 조회")
    class GetAllOrdersTest {

        @Test
        void 회원의_주문_목록을_반환한다() {
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDateTime now = LocalDateTime.now();

            OrderListProjection projection = new OrderListProjection(
                    1L, "ORD-001", OrderStatus.PENDING,
                    33000L, 3000L, now,
                    "테스트 펀딩", 100L, now.plusDays(30), false
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
            Long memberId = 1L;
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
            Long memberId = 1L;
            Long orderId = 1L;
            LocalDateTime now = LocalDateTime.now();

            OrderDetailProjection orderProjection = new OrderDetailProjection(
                    1L, "ORD-001", OrderStatus.PAID,
                    33000L, 3000L, now,
                    "테스트 펀딩", 100L, now.plusDays(30), false,
                    null, null
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

        @Test
        void 정상적으로_빌링을_생성한다() {
            Long memberId = 1L;
            LocalDateTime now = LocalDateTime.now();

            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 2)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(List.of(1L), 1L))
                    .willReturn(List.of(reward));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Order.reconstitute(
                        1L, "ORD-001", order.getMemberId(), order.getStatus(),
                        order.getTotalRewardAmount(), order.getTotalShippingCharge(),
                        order.getOrderRewards(),
                        order.getCancelReason(), LocalDateTime.now(), LocalDateTime.now(),
                        order.getCancelledAt(), order.getExpiresAt()
                );
            });

            BillingResponseDto result = orderService.createBilling(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(1L);
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.rewardAmount()).isEqualTo(20000L);
            assertThat(result.totalShippingCharge()).isEqualTo(3000L);
            assertThat(result.totalBillingAmount()).isEqualTo(23000L);
        }

        @Test
        void 여러_리워드로_빌링을_생성한다() {
            Long memberId = 1L;
            LocalDateTime now = LocalDateTime.now();

            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 2),
                    new RewardQuantityDto(2L, 1)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward1 = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, now.plusDays(60), 3000L);
            Reward reward2 = Reward.of(2L, 1L, "리워드B", "설명", 50, 30,
                    BadgeType.EARLY_BIRD, 20000L, now.plusDays(60), 5000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(any(), eq(1L)))
                    .willReturn(List.of(reward1, reward2));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Order.reconstitute(
                        1L, "ORD-001", order.getMemberId(), order.getStatus(),
                        order.getTotalRewardAmount(), order.getTotalShippingCharge(),
                        order.getOrderRewards(),
                        order.getCancelReason(), LocalDateTime.now(), LocalDateTime.now(),
                        order.getCancelledAt(), order.getExpiresAt()
                );
            });

            BillingResponseDto result = orderService.createBilling(memberId, reqDto);

            assertThat(result.rewards()).hasSize(2);
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.rewardAmount()).isEqualTo(40000L);
            assertThat(result.totalShippingCharge()).isEqualTo(8000L);
            assertThat(result.totalBillingAmount()).isEqualTo(48000L);
        }

        @Test
        void 예약_주문으로_빌링을_생성한다() {
            Long memberId = 1L;
            LocalDateTime now = LocalDateTime.now();

            BillingRequestDto reqDto = new BillingRequestDto(1L, true, List.of(
                    new RewardQuantityDto(1L, 2)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(List.of(1L), 1L))
                    .willReturn(List.of(reward));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Order.reconstitute(
                        1L, "ORD-001", order.getMemberId(), order.getStatus(),
                        order.getTotalRewardAmount(), order.getTotalShippingCharge(),
                        order.getOrderRewards(),
                        order.getCancelReason(), LocalDateTime.now(), LocalDateTime.now(),
                        order.getCancelledAt(), order.getExpiresAt()
                );
            });

            BillingResponseDto result = orderService.createBilling(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(1L);
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
            Long memberId = 1L;
            Long orderId = 1L;

            OrderReward orderReward = OrderReward.reconstitute(1L, 1L, 1L, 2, 3000L, 20000L);
            Order order = Order.reconstitute(1L, "ORD-001", 1L, OrderStatus.PAID,
                    33000L, 3000L, List.of(orderReward),
                    null, LocalDateTime.now(), LocalDateTime.now(), null, null);

            given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));

            OrderCancelResponseDto result = orderService.cancelOrder(memberId, orderId);

            assertThat(result.orderNo()).isEqualTo("ORD-001");
            assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.cancelledAt()).isNotNull();
            assertThat(result.refundStatus()).isEqualTo("REFUND_REQUESTED");
            verify(rewardRepository).increaseRemainQty(1L, 2);
        }
    }

    @Nested
    @DisplayName("주문 생성 (결제 시작)")
    class CreateOrderTest {

        @Test
        void 정상적으로_주문을_생성한다() {
            Long memberId = 1L;
            LocalDateTime now = LocalDateTime.now();

            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            OrderReward orderReward = OrderReward.reconstitute(1L, 1L, 1L, 2, 3000L, 20000L);
            Order order = Order.reconstitute(1L, "ORD-001", 1L, OrderStatus.PENDING,
                    33000L, 3000L, List.of(orderReward),
                    null, now, now, null, now.plusMinutes(15));

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));
            given(rewardRepository.decreaseStock(1L, 2)).willReturn(1);

            OrderCreateResponseDto result = orderService.createOrder(memberId, reqDto);

            assertThat(result.memberId()).isEqualTo(1L);
            assertThat(result.orderNo()).isEqualTo("ORD-001");
            assertThat(result.totalBillingAmount()).isEqualTo(36000L);
        }
    }
}