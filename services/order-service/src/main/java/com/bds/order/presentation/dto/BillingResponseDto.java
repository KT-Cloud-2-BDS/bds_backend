package com.bds.order.presentation.dto;

import java.util.List;

public record BillingResponseDto(
        Long memberId,
        List<RewardItemDto> rewards,
        Long rewardAmount,
        Long totalShippingCharge,
        Long totalBillingAmount
) {
}
