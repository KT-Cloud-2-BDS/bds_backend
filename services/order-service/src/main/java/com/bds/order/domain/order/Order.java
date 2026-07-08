package com.bds.order.domain.order;

import com.bds.order.domain.orderReward.OrderReward;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    private Long id;
    private String orderNo;
    private Long memberId;
    private OrderStatus status;
    private Long totalRewardAmount;
    private Long totalShippingCharge;
    private List<OrderReward> orderRewards;
    private CancelReason cancelReason;
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
                                     Long totalRewardAmount, Long totalShippingCharge, List<OrderReward> orderRewards,
                                     CancelReason cancelReason, LocalDateTime createdAt,
                                     LocalDateTime updatedAt, LocalDateTime cancelledAt) {
        return new Order(id, orderNo, memberId, status, totalRewardAmount, totalShippingCharge, orderRewards,
                cancelReason, createdAt, updatedAt, cancelledAt);
    }

    public Long getTotalAmount() {
        return totalRewardAmount + totalShippingCharge;
    }

    public void updateStatus(OrderStatus newStatus) {
        if (!canTransitTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("주문 상태를 %s에서 %s로 변경할 수 없습니다", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public void cancelOrder(CancelReason cancelReason) {
        updateStatus(OrderStatus.CANCELLED);
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = cancelReason;
    }

    private boolean canTransitTo(OrderStatus newStatus) {
        return switch (newStatus) {
            case PAYING -> this.status == OrderStatus.PENDING || this.status == OrderStatus.RESERVED;
            case PAID -> this.status == OrderStatus.PAYING;
            case CANCELLED ->
                    this.status == OrderStatus.PAYING || this.status == OrderStatus.RESERVED || this.status == OrderStatus.PAID;
            case REFUNDED -> this.status == OrderStatus.CANCELLED || this.status == OrderStatus.RESERVED;
            default -> false;
        };
    }
}