package com.bds.order.domain.order;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Order {

    private Long id;
    private String orderNo;
    private Long memberId;
    private OrderStatus status;
    private Long amount;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order(Long id, String orderNo, Long memberId, OrderStatus status,
                  Long amount, String cancelReason, LocalDateTime createdAt,
                  LocalDateTime updatedAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.memberId = memberId;
        this.status = status;
        this.amount = amount;
        this.cancelReason = cancelReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public static Order of(Long id, String orderNo, Long memberId, OrderStatus status, Long amount, String cancelReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, orderNo, memberId, status, amount, cancelReason, createdAt, updatedAt);
    }
}