package com.bds.order.domain.order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    private Long id;
    private String orderNo;
    private Long memberId;
    private OrderStatus status;
    private Long amount;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order(Long memberId, Long amount, OrderStatus status) {
        this.memberId = memberId;
        this.amount = amount;
        this.status = status;
    }

    public static Order create(Long memberId, Long amount, OrderStatus status) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        return new Order(memberId, amount, (status != null) ? status : OrderStatus.PENDING);
    }

    public static Order reconstitute(Long id, String orderNo, Long memberId, OrderStatus status, Long amount, String cancelReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, orderNo, memberId, status, amount, cancelReason, createdAt, updatedAt);
    }
}