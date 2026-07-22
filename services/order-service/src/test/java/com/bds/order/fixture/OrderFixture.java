package com.bds.order.fixture;

import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderFixture {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 12, 0);

    public static Order createOrder(OrderStatus status) {
        return Order.reconstitute(
                1L, "ORD-001", 1L, status,
                33000L, 3000L, List.of(),
                null, NOW, NOW, null, null
        );
    }

    public static Order createOrder(Long memberId, OrderStatus status) {
        return Order.reconstitute(
                1L, "ORD-001", memberId, status,
                33000L, 3000L, List.of(),
                null, NOW, NOW, null, null
        );
    }

    public static Order createOrder(Long memberId, OrderStatus status, Long totalRewardAmount, Long totalShippingCharge) {
        return Order.reconstitute(
                1L, "ORD-001", memberId, status,
                totalRewardAmount, totalShippingCharge, List.of(),
                null, NOW, NOW, null, null
        );
    }

    public static Order createCancelOrder(LocalDateTime cancelledAt) {
        return Order.reconstitute(
                1L, "ORD-001", 1L, OrderStatus.CANCELLED,
                33000L, 3000L, List.of(),
                CancelReason.USER_CANCEL.name(), NOW, NOW, cancelledAt, null
        );
    }

}