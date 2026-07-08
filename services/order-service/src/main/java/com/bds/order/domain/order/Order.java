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
    private Long totalRewardAmount;
    private Long totalShippingCharge;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    private Order(Long memberId, Long totalRewardAmount, Long totalShippingCharge, OrderStatus status) {
        this.memberId = memberId;
        this.totalRewardAmount = totalRewardAmount;
        this.totalShippingCharge = totalShippingCharge;
        this.status = status;
    }

    public static Order create(Long memberId, Long totalRewardAmount, Long totalShippingCharge, OrderStatus status) {
        if (totalRewardAmount == null || totalRewardAmount < 0) {
            throw new IllegalArgumentException("totalRewardAmount must be non-negative");
        }
        if (totalShippingCharge == null || totalShippingCharge < 0) {
            throw new IllegalArgumentException("totalShippingCharge must be non-negative");
        }
        return new Order(memberId, totalRewardAmount, totalShippingCharge,
                (status != null) ? status : OrderStatus.PENDING);
    }

    public static Order reconstitute(Long id, String orderNo, Long memberId, OrderStatus status,
                                     Long totalRewardAmount, Long totalShippingCharge,
                                     String cancelReason, LocalDateTime createdAt,
                                     LocalDateTime updatedAt, LocalDateTime cancelledAt) {
        return new Order(id, orderNo, memberId, status, totalRewardAmount, totalShippingCharge,
                cancelReason, createdAt, updatedAt, cancelledAt);
    }

    public Long getTotalAmount() {
        return totalRewardAmount + totalShippingCharge;
    }
}