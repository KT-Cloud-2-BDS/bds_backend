package com.bds.payment.payment.presentation.request;

import jakarta.validation.constraints.NotEmpty;

public record AccountVerifyRequestDto(
        @NotEmpty
        String code
) {
}
