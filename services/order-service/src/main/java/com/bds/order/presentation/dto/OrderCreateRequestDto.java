package com.bds.order.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequestDto(
        @NotNull Long orderId,
        @NotNull Long fundingId,
        Boolean isNowPay
) {
}
