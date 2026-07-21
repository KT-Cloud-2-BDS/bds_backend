package com.bds.payment.bank.infrastructure.persistence.bankVerifyCode;

import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCode;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BankVerifyCodePersistenceAdapter implements BankVerifyCodeRepository {

    private final BankVerifyCodeJpaRepository bankVerifyCodeJpaRepository;
    private final BankVerifyCodeMapper mapper;

    @Override
    public Optional<BankVerifyCode> findByAccountNumber(String accountNumber) {
        return bankVerifyCodeJpaRepository.findByAccountNumber(accountNumber).map(mapper::toDomain);
    }

    @Override
    public BankVerifyCode save(BankVerifyCode bankVerifyCode) {
        return mapper.toDomain(bankVerifyCodeJpaRepository.save(mapper.toEntity(bankVerifyCode)));
    }

    @Override
    public void delete(BankVerifyCode bankVerifyCode) {
        bankVerifyCodeJpaRepository.delete(mapper.toEntity(bankVerifyCode));
    }

}
