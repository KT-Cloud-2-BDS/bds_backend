package com.bds.payment.payment.domain.account;

import java.util.Optional;

public interface AccountRepository {

    boolean existByAccount(Long WalletId);

    Optional<Account> findById(Long walletId);

    Account save(Account account);
}
