package com.bds.payment.payment.domain.wallet;

import java.util.Optional;

public interface WalletRepository {

    boolean existsByMemberId(Long memberId);

    Optional<Wallet> findByMemberId(Long id);

    Wallet save(Wallet wallet);
}
