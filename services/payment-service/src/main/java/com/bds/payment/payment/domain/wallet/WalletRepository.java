package com.bds.payment.payment.domain.wallet;

import java.util.Optional;

public interface WalletRepository {

    Optional<Wallet> findByMemberId(Long id);

    Wallet save(Wallet wallet);
}
