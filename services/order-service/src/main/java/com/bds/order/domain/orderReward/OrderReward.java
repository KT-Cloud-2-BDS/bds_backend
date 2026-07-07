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

    public static OrderReward of(Long id, Long orderId, Long rewardId, int qty) {
        return new OrderReward(id, orderId, rewardId, qty);
    }
}