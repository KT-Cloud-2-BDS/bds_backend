package com.bds.order.presentation.dto;

import com.bds.order.domain.reward.BadgeType;

public record RewardDto(
        Long id,
        Integer qty,
        String name,
        Long amount,
        BadgeType badgeType,
        Long shippingCharge
) {
}