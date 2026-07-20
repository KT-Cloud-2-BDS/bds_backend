package com.bds.order.domain;

import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.fixture.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderUnitExceptionTest {

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

        @ParameterizedTest(name = "{0} → {1} 전이 불가")
        @CsvSource({
                "PENDING, PENDING",
                "PENDING, PAID",
                "PENDING, CANCELLED",
                "PENDING, CONFIRMED",
                "PAYING, REFUNDED",
                "PAID, PAYING",
                "PAID, RESERVED",
                "CANCELLED, PAYING",
                "CANCELLED, CONFIRMED",
                "REFUNDED, PAYING",
                "REFUNDED, CONFIRMED"
        })
        void 허용되지_않은_상태_전이는_예외를_던진다(OrderStatus from, OrderStatus to) {
            Order order = OrderFixture.createOrder(from);

            assertThatThrownBy(() -> order.updateStatus(to))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 예외")
    class CancelOrderExceptionTest {

        @Test
        void PENDING_상태에서_취소하면_예외를_던진다() {
            Order order = OrderFixture.createOrder(OrderStatus.PENDING);

            assertThatThrownBy(() -> order.cancelOrder(CancelReason.USER_CANCEL.name()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}