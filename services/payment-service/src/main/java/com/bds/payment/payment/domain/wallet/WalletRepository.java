package com.bds.payment.payment.domain.wallet;

import java.util.Optional;

public interface WalletRepository {

    boolean existsByMemberId(Long memberId);

    Optional<Wallet> findByMemberId(Long memberId);

    Optional<Wallet> findByMemberIdWithLock(Long memberId);

    Wallet save(Wallet wallet);
}
