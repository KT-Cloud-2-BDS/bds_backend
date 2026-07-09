package com.bds.order.presentation.dto;

import java.util.List;

public record OrderCreateRequestDto(
        Long orderId,
        List<RewardQuantityDto> rewards,
        Long fundingId,
        Boolean isNowPay
) {
}
