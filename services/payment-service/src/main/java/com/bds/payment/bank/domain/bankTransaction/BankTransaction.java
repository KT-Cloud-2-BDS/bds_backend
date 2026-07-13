package com.bds.payment.bank.domain.bankTransaction;

import com.bds.payment.bank.domain.common.TransactionType;
import com.bds.payment.bank.presentation.request.BankTransactionRequestDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class BankTransaction {
    private Long id;
    private UUID tranSeqNo;
    private String accountNumber;
    private TransactionType type;
    private Long amount;
    private LocalDateTime createdAt;

    public static BankTransaction create(BankTransactionRequestDto dto, TransactionType type) {
        return BankTransaction.builder()
                .tranSeqNo(dto.tranSeqNo())
                .accountNumber(dto.accountNumber())
                .type(type)
                .amount(dto.amount())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
