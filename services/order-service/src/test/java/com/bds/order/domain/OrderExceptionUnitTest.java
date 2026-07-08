package com.bds.order.domain;

import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderExceptionUnitTest {

    private Order createOrderWithStatus(OrderStatus status) {
        return Order.reconstitute(1L, "ORD-001", 1L, status,
                33000L, 3000L, List.of(),
                null, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Nested
    @DisplayName("주문 생성 예외")
    class CreateExceptionTest {

        @Test
        void totalRewardAmount가_null이면_예외를_던진다() {
            assertThatThrownBy(() -> Order.create(1L, null, 3000L, OrderStatus.PENDING))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void totalRewardAmount가_음수이면_예외를_던진다() {
            assertThatThrownBy(() -> Order.create(1L, -1L, 3000L, OrderStatus.PENDING))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void totalShippingCharge가_null이면_예외를_던진다() {
            assertThatThrownBy(() -> Order.create(1L, 33000L, null, OrderStatus.PENDING))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void totalShippingCharge가_음수이면_예외를_던진다() {
            assertThatThrownBy(() -> Order.create(1L, 33000L, -1L, OrderStatus.PENDING))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("주문 상태 변경 예외")
    class UpdateStatusExceptionTest {

        @Test
        void PENDING에서_PAID로_직접_변경하면_예외를_던진다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            assertThatThrownBy(() -> order.updateStatus(OrderStatus.PAID))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void PENDING에서_CANCELLED로_변경하면_예외를_던진다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            assertThatThrownBy(() -> order.updateStatus(OrderStatus.CANCELLED))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void PAID에서_PAYING으로_변경하면_예외를_던진다() {
            Order order = createOrderWithStatus(OrderStatus.PAID);

            assertThatThrownBy(() -> order.updateStatus(OrderStatus.PAYING))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 예외")
    class CancelOrderExceptionTest {

        @Test
        void PENDING_상태에서_취소하면_예외를_던진다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            assertThatThrownBy(() -> order.cancelOrder(CancelReason.USER_CANCEL))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}