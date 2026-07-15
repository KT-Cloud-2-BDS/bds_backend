package com.bds.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record FundingCreateRequestDto(
        @NotBlank String title,
        @NotNull @Positive Long goalAmount,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime holdTo,
        @NotNull LocalDateTime payAt,
        @Valid @NotNull List<RewardCreateDto> rewards
) {
    public record RewardCreateDto(
            @NotBlank String name,
            String description,
            @NotNull @Positive Integer limitQty,
            String badgeType,
            @NotNull @Positive Long price,
            @NotNull LocalDateTime offerAt,
            @NotNull @Positive Long shippingCharge
    ) {
    }
}
