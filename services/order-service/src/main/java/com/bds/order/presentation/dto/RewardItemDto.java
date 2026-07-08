package com.bds.order.presentation.dto;

import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection;

public record RewardItemDto(
        Long id,
        Integer qty,
        String name,
        Long amount,
        BadgeType badgeType,
        Long shippingCharge
) {
    public static RewardItemDto from(Reward reward, Integer qty) {
        return new RewardItemDto(
                reward.getId(),
                qty,
                reward.getName(),
                reward.calculateAmount(qty),
                reward.getBadgeType(),
                reward.getShippingCharge()
        );
    }

    public static RewardItemDto fromOrderReward(OrderRewardDetailProjection reward) {
        return new RewardItemDto(
                reward.orderRewardId(),
                reward.qty(),
                reward.rewardName(),
                reward.amount(),
                reward.badgeType(),
                reward.shippingCharge()
        );
    }
}
