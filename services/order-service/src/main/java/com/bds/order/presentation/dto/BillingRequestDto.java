package com.bds.order.presentation.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BillingRequestDto(
        @NotNull Long fundingId,
        boolean isReservedOrder,
        @NotEmpty List<@Valid RewardQuantityDto> rewards
) {
}