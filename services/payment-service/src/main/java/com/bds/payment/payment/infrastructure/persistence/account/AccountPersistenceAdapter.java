package com.bds.payment.payment.infrastructure.persistence.account;

import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;
    private final AccountMapper mapper;

    @Override
    public boolean existByAccount(Long walletId) {
        return jpaRepository.existsById(walletId);
    }

    @Override
    public Optional<Account> findById(Long walletId) {
        return jpaRepository.findById(walletId).map(mapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(account)));
    }
}
