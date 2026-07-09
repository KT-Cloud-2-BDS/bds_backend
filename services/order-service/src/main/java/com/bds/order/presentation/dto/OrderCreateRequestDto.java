package com.bds.order.presentation.dto;

import java.util.List;

public record OrderCreateRequestDto(
        List<RewardQuantityDto> rewards,
        Long fundingId,
        String idempotencyKey,
        Boolean isNowPay
) {
}
