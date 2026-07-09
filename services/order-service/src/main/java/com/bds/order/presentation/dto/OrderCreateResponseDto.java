package com.bds.order.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateResponseDto(
        Long memberId,
        String orderNo,
        List<RewardItemDto> rewards,
        Long rewardAmount,
        Long totalShippingCharge,
        Long totalBillingAmount,
        String paymentStatus,
        LocalDateTime paidAt
) {
}
