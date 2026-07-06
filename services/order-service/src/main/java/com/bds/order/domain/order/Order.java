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

    public static Order create(Long memberId, Long amount, OrderStatus status) {
        Order order = new Order();
        order.memberId = memberId;
        order.amount = amount;
        order.status = (status != null) ? status : OrderStatus.PENDING;
        return order;
    }

    public static Order of(Long id, String orderNo, Long memberId, OrderStatus status, Long amount, String cancelReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Order order = new Order();
        order.id = id;
        order.orderNo = orderNo;
        order.memberId = memberId;
        order.status = status;
        order.amount = amount;
        order.cancelReason = cancelReason;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }
}