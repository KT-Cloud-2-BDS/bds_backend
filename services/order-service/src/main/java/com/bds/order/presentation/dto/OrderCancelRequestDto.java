package com.bds.order.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderCancelRequestDto(
        @NotNull @Positive Long fundingId
) {
}
