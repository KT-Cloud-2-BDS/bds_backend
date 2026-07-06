package com.bds.payment.payment.infrastructure.persistence;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletPersistenceAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;
    private final WalletMapper mapper;

    @Override
    public Optional<Wallet> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId).map(mapper::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(wallet)));
    }
}
