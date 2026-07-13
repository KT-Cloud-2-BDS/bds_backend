package com.bds.order.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RewardQuantityDto(
        @NotNull Long id,
        @NotNull @Min(1) Integer qty
) {
}
