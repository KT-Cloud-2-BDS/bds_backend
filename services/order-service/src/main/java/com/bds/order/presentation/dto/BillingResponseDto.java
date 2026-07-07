package com.bds.order.presentation.dto;

import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;

import java.util.List;

public record BillingResponseDto(
        Long memberId,
        List<RewardDto> rewards,
        Long rewardAmount,
        Long totalShippingCharge,
        Long totalBillingAmount
) {

    public record RewardDto(
            Long id,
            Integer qty,
            String name,
            Long amount,
            BadgeType badgeType,
            Long shippingCharge
    ) {
        public static RewardDto from(Reward reward, Integer qty) {
            return new RewardDto(
                    reward.getId(),
                    qty,
                    reward.getName(),
                    reward.calculateAmount(qty),
                    reward.getBadgeType(),
                    reward.getShippingCharge()
            );
        }
    }

}
