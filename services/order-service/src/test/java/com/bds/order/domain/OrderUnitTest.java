package com.bds.order.domain;


import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderUnitTest {

    private Order createOrderWithStatus(OrderStatus status) {
        return Order.reconstitute(1L, "ORD-001", 1L, status,
                33000L, 3000L, List.of(),
                null, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Nested
    @DisplayName("주문 생성 성공")
    class CreateTest {

        @Test
        void 정상적으로_주문을_생성한다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            assertThat(order.getMemberId()).isEqualTo(1L);
            assertThat(order.getTotalRewardAmount()).isEqualTo(33000L);
            assertThat(order.getTotalShippingCharge()).isEqualTo(3000L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        void status가_null이면_PENDING으로_생성된다() {
            Order order = Order.create(1L, 33000L, 3000L, null);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("주문 상태 전이 성공")
    class UpdateStatusTest {

        @Test
        void PENDING에서_PAYING으로_변경할_수_있다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            order.updateStatus(OrderStatus.PAYING);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYING);
        }

        @Test
        void RESERVED에서_PAYING으로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.RESERVED);

            order.updateStatus(OrderStatus.PAYING);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYING);
        }

        @Test
        void PAYING에서_PAID로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.PAYING);

            order.updateStatus(OrderStatus.PAID);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        void PAYING에서_CANCELLED로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.PAYING);

            order.updateStatus(OrderStatus.CANCELLED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void RESERVED에서_CANCELLED로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.RESERVED);

            order.updateStatus(OrderStatus.CANCELLED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void PAID에서_CANCELLED로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.PAID);

            order.updateStatus(OrderStatus.CANCELLED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void CANCELLED에서_REFUNDED로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.CANCELLED);

            order.updateStatus(OrderStatus.REFUNDED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }

        @Test
        void RESERVED에서_REFUNDED로_변경할_수_있다() {
            Order order = createOrderWithStatus(OrderStatus.RESERVED);

            order.updateStatus(OrderStatus.REFUNDED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }

    }

    @Nested
    @DisplayName("주문 취소 성공")
    class CancelOrderTest {

        @Test
        void PAYING_상태에서_취소하면_CANCELLED로_변경된다() {
            Order order = createOrderWithStatus(OrderStatus.PAYING);

            order.cancelOrder(CancelReason.USER_CANCEL);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.USER_CANCEL);
            assertThat(order.getCancelledAt()).isNotNull();
        }

        @Test
        void PAID_상태에서_취소하면_CANCELLED로_변경된다() {
            Order order = createOrderWithStatus(OrderStatus.PAID);

            order.cancelOrder(CancelReason.USER_CANCEL);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

    }

    @Nested
    @DisplayName("주문 최종 금액 계산")
    class GetTotalAmountTest {

        @Test
        void 리워드금액과_배송비를_합산한다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            assertThat(order.getTotalAmount()).isEqualTo(36000L);
        }
    }
}

