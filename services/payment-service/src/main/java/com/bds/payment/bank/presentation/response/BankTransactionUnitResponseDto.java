package com.bds.payment.bank.presentation.response;

import java.util.UUID;

public record BankTransactionUnitResponseDto(
        UUID tranSeqNo,
        Boolean exists
) {
}
