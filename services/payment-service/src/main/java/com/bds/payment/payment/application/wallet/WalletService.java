package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.response.WalletResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public WalletResponseDto getWalletResponseDto(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        return WalletResponseDto.from(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Long getWalletId(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        return wallet.getId();
    }

    @Transactional
    public WalletResponseDto createWallet(Long memberId) {
        if (walletRepository.existsByMemberId(memberId)) throw new BusinessException(ErrorCode.WALLET_ALREADY_EXISTS);
        return WalletResponseDto.from(walletRepository.save(Wallet.create(memberId)));
    }

    @Transactional
    public Wallet charge(Long memberId, Long amount) {
        validateAmount(amount);
        Wallet wallet = walletRepository.findByMemberIdWithLock(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        wallet.charge(amount);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet decrease(Long memberId, Long amount) {
        validateAmount(amount);
        Wallet wallet = walletRepository.findByMemberIdWithLock(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        wallet.withdraw(amount);
        return walletRepository.save(wallet);
    }

    private void validateAmount(Long amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_REQUIRED);
        }
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID);
        }
    }
}
