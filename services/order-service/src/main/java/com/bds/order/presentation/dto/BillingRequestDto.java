package com.bds.order.presentation.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BillingRequestDto(
        Long fundingId,
        @NotNull List<@Valid RewardItemDto> rewards
) {

    public record RewardItemDto(
            @NotNull Long id,
            @NotNull @Min(1) Integer qty
    ) {
    }
}