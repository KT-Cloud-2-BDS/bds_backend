package com.bds.payment.bank.domain.bankVerifyCode;

import java.util.Optional;

public interface BankVerifyCodeRepository {

    Optional<BankVerifyCode> findByAccountNumber(String accountNumber);

    BankVerifyCode save(BankVerifyCode bankVerifyCode);

    void delete(BankVerifyCode bankVerifyCode);
}
