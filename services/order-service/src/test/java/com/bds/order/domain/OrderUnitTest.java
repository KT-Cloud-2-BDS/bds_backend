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
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderUnitTest {

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

        @Test
        void 생성_시_expiresAt이_설정된다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);

            assertThat(order.getExpiresAt()).isNotNull();
            assertThat(order.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(14));
        }
    }

    @Nested
    @DisplayName("주문 상태 전이 성공")
    class UpdateStatusTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "PENDING, PAYING",
                "RESERVED, PAYING",
                "PAYING, PAID",
                "PAYING, CANCELLED",
                "RESERVED, CANCELLED",
                "PAID, CANCELLED",
                "CANCELLED, REFUNDED",
                "RESERVED, REFUNDED"
        })
        void 허용된_상태_전이는_성공한다(OrderStatus from, OrderStatus to) {
            Order order = OrderFixture.createOrder(from);

            order.updateStatus(to);

            assertThat(order.getStatus()).isEqualTo(to);
        }
    }

    @Nested
    @DisplayName("주문 취소 성공")
    class CancelOrderTest {

        @ParameterizedTest(name = "{0} 상태에서 취소하면 CANCELLED로 변경된다")
        @EnumSource(value = OrderStatus.class, names = {"PAYING", "PAID"})
        void 취소_가능한_상태에서_취소하면_CANCELLED로_변경된다(OrderStatus from) {
            Order order = OrderFixture.createOrder(from);

            order.cancelOrder(CancelReason.USER_CANCEL);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.USER_CANCEL);
            assertThat(order.getCancelledAt()).isNotNull();
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

    @Nested
    @DisplayName("결제 시작")
    class StartPaymentTest {

        @Test
        void 결제_시작_시_expiresAt이_null이_된다() {
            Order order = OrderFixture.createOrder(OrderStatus.PENDING);

            order.startPayment();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYING);
            assertThat(order.getExpiresAt()).isNull();
        }
    }
}

