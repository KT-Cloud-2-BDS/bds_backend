package com.bds.order.domain.orderReward;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderReward {

    private Long id;
    private Long orderId;
    private Long rewardId;
    private int qty;
    private Long shippingCharge;
    private Long amount;

    public static OrderReward reconstitute(Long id, Long orderId, Long rewardId, int qty, Long shippingCharge, Long amount) {
        return new OrderReward(id, orderId, rewardId, qty, shippingCharge, amount);
    }

}