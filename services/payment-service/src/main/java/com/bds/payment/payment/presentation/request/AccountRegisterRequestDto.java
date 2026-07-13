package com.bds.payment.payment.presentation.request;

import jakarta.validation.constraints.NotEmpty;

public record AccountRegisterRequestDto(
        @NotEmpty
        String bankCode,
        @NotEmpty
        String accountNumber,
        @NotEmpty
        String holderName
) {
}