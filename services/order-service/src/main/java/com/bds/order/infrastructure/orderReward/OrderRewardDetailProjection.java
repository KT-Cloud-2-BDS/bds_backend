package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.reward.BadgeType;

public record OrderRewardDetailProjection(
        Long orderRewardId,
        int qty,
        Long amount,
        Long shippingCharge,
        String rewardName,
        BadgeType badgeType
) {
}