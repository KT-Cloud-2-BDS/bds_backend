package com.bds.payment.payment.presentation.request;

import jakarta.validation.constraints.NotNull;

public record AccountTransactionRequestDto(
        @NotNull
        Long amount
) {
}
