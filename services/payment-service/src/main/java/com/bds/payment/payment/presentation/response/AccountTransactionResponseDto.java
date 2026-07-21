package com.bds.payment.payment.presentation.response;

import java.util.UUID;

public record AccountTransactionResponseDto(
        Long balance,
        UUID tranSeqNo
) {
}
