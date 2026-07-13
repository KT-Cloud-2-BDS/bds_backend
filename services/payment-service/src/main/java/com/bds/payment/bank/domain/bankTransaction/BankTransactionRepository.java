package com.bds.payment.bank.domain.bankTransaction;

import java.util.UUID;

public interface BankTransactionRepository {

    BankTransaction save(BankTransaction bankTransaction);

    boolean existsByTranSeqNo(UUID TranSeqNo);
}
