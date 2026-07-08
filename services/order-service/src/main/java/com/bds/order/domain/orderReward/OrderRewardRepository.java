package com.bds.order.domain.orderReward;

import com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection;

import java.util.List;

public interface OrderRewardRepository {
    List<OrderRewardDetailProjection> findOrderRewardDetailsWithReward(Long orderId);
}
