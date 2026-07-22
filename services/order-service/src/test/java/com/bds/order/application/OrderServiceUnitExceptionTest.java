package com.bds.order.application;


import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.domain.orderReward.OrderReward;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.fixture.OrderFixture;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.OrderCancelRequestDto;
import com.bds.order.presentation.dto.OrderCreateRequestDto;
import com.bds.order.presentation.dto.RewardQuantityDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class OrderServiceUnitExceptionTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 상세 조회 예외")
    class GetOrderDetailExceptionTest {

        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            given(orderRepository.findOrderDetailWithFunding(1L, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("빌링 생성 예외")
    class CreateBillingExceptionTest {

        @Test
        void 펀딩이_존재하지_않으면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(999L, false, List.of(
                    new RewardQuantityDto(1L, 1)
            ));

            given(fundingRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 펀딩_기간이_아니면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(30), now.minusDays(1), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 리워드가_존재하지_않으면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1),
                    new RewardQuantityDto(2L, 1)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    null, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(any(), eq(1L)))
                    .willReturn(List.of(reward));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 동일한_리워드를_중복_선택하면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1),
                    new RewardQuantityDto(1L, 2)
            ));

            LocalDateTime now = LocalDateTime.now();
            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 리워드_재고가_부족하면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 100)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 3,
                    null, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(any(), eq(1L)))
                    .willReturn(List.of(reward));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 예외")
    class CancelOrderExceptionTest {

        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            given(orderRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L, new OrderCancelRequestDto(1L)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 본인의_주문이_아니면_예외를_던진다() {
            Order order = OrderFixture.createOrder(2L, OrderStatus.PAID);

            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L, new OrderCancelRequestDto(1L)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 취소_불가_상태이면_예외를_던진다() {
            Order order = OrderFixture.createOrder(1L, OrderStatus.PENDING);

            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L, new OrderCancelRequestDto(1L)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 생성 예외")
    class CreateOrderExceptionTest {

        @Test
        void 주문이_존재하지_않으면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(999L, 1L, true);

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 본인의_주문이_아니면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Order order = OrderFixture.createOrder(2L, OrderStatus.PENDING);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 상태_전이가_불가하면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Order order = OrderFixture.createOrder(1L, OrderStatus.PAID);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 재고가_부족하면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            OrderReward orderReward = OrderReward.reconstitute(1L, 1L, 1L, 2, 3000L, 20000L);
            Order order = Order.reconstitute(1L, "ORD-001", 1L, OrderStatus.PENDING,
                    33000L, 3000L, List.of(orderReward),
                    null, now, now, null, now.plusMinutes(15));

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));
            given(rewardRepository.decreaseStock(1L, 2)).willReturn(0);

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("processStatusUpdate 예외 테스트")
    class ProcessStatusUpdateExceptionTest {

        @Test
        void 존재하지_않는_주문이면_warn_로그를_남긴다(CapturedOutput output) {
            when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            orderService.processStatusUpdate(999L, OrderStatus.PAYING);

            assertThat(output.getOut()).contains("Order not found: orderId=999");
        }

        @Test
        void 상태_전이_실패시_warn_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new IllegalStateException("주문 상태를 CANCELLED에서 PAYING로 변경할 수 없습니다"))
                    .when(mockOrder).updateStatus(OrderStatus.PAYING);

            orderService.processStatusUpdate(1L, OrderStatus.PAYING);

            assertThat(output.getOut()).contains("[OrderService] processStatusUpdate failed - invalid state: orderId=1");
        }

        @Test
        void 예상치_못한_예외_발생시_error_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new RuntimeException("DB connection failed"))
                    .when(mockOrder).updateStatus(OrderStatus.PAYING);

            orderService.processStatusUpdate(1L, OrderStatus.PAYING);

            assertThat(output.getOut()).contains("[OrderService] processStatusUpdate failed - unexpected: orderId=1");
            assertThat(output.getOut()).contains("RuntimeException");
        }
    }

    @Nested
    @DisplayName("processCancelledUpdate 예외 테스트")
    class ProcessCancelledUpdateExceptionTest {

        @Test
        void 존재하지_않는_주문이면_warn_로그를_남긴다(CapturedOutput output) {
            when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            orderService.processCancelledUpdate(999L, "FUNDING_FAILED");

            assertThat(output.getOut()).contains("Order not found: orderId=999");
        }

        @Test
        void 상태_전이_실패시_warn_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new IllegalStateException("주문 상태를 CANCELLED에서 CANCELLED로 변경할 수 없습니다"))
                    .when(mockOrder).cancelOrder("FUNDING_FAILED");

            orderService.processCancelledUpdate(1L, "FUNDING_FAILED");

            assertThat(output.getOut()).contains("[OrderService] processCancelledUpdate failed - invalid state: orderId=1");
        }

        @Test
        void 예상치_못한_예외_발생시_error_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new RuntimeException("DB connection failed"))
                    .when(mockOrder).cancelOrder("FUNDING_FAILED");

            orderService.processCancelledUpdate(1L, "FUNDING_FAILED");

            assertThat(output.getOut()).contains("[OrderService] processCancelledUpdate failed - unexpected: orderId=1");
            assertThat(output.getOut()).contains("RuntimeException");
        }
    }

    @Nested
    @DisplayName("processReservedFundingConfirmed 예외 테스트")
    class ProcessPayingAndPublishSettlementExceptionTest {

        @Test
        void 존재하지_않는_주문이면_warn_로그를_남긴다(CapturedOutput output) {
            when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            orderService.processReservedFundingConfirmed(999L);

            assertThat(output.getOut()).contains("Order not found: orderId=999");
        }

        @Test
        void 상태_전이_실패시_warn_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new IllegalStateException("주문 상태를 CANCELLED에서 PAYING로 변경할 수 없습니다"))
                    .when(mockOrder).updateStatus(OrderStatus.PAYING);

            orderService.processReservedFundingConfirmed(1L);

            assertThat(output.getOut()).contains("[OrderService] processReservedFundingConfirmed failed - invalid state: orderId=1");
        }

        @Test
        void 예상치_못한_예외_발생시_error_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new RuntimeException("DB connection failed"))
                    .when(mockOrder).updateStatus(OrderStatus.PAYING);

            orderService.processReservedFundingConfirmed(1L);

            assertThat(output.getOut()).contains("[OrderService] processReservedFundingConfirmed failed - unexpected: orderId=1");
            assertThat(output.getOut()).contains("RuntimeException");
        }
    }

    @Nested
    @DisplayName("processFundingFailedRefund 예외 테스트")
    class processFundingFailedRefundExceptionTest {

        @Test
        void 존재하지_않는_주문이면_warn_로그를_남긴다(CapturedOutput output) {
            when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            orderService.processFundingFailedRefund(999L);

            assertThat(output.getOut()).contains("Order not found: orderId=999");
        }

        @Test
        void 상태_전이_실패시_warn_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new IllegalStateException("주문 상태를 CANCELLED에서 CANCELLED로 변경할 수 없습니다"))
                    .when(mockOrder).cancelOrder(CancelReason.FUNDING_FAILED.name());

            orderService.processFundingFailedRefund(1L);

            assertThat(output.getOut()).contains("[OrderService] processFundingFailedRefund failed - invalid state: orderId=1");
        }

        @Test
        void 예상치_못한_예외_발생시_error_로그를_남긴다(CapturedOutput output) {
            Order mockOrder = mock(Order.class);
            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockOrder));
            doThrow(new RuntimeException("DB connection failed"))
                    .when(mockOrder).cancelOrder(CancelReason.FUNDING_FAILED.name());

            orderService.processFundingFailedRefund(1L);

            assertThat(output.getOut()).contains("[OrderService] processFundingFailedRefund failed - unexpected: orderId=1");
            assertThat(output.getOut()).contains("RuntimeException");
        }
    }
}
