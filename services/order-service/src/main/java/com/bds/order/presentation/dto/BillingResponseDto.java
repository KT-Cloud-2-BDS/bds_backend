package com.bds.order.presentation.dto;

import com.bds.order.domain.order.Order;

import java.util.List;

public record BillingResponseDto(
        Long orderId,
        Long memberId,
        List<RewardItemDto> rewards,
        Long rewardAmount,
        Long totalShippingCharge,
        Long totalBillingAmount
) {

    public static BillingResponseDto from(Order billing, List<RewardItemDto> rewards) {
        return new BillingResponseDto(
                billing.getId(),
                billing.getMemberId(),
                rewards,
                billing.getTotalRewardAmount(),
                billing.getTotalShippingCharge(),
                billing.getTotalAmount()
        );
    }
}
