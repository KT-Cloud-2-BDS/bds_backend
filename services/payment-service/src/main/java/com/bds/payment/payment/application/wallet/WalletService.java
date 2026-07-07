package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
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
    private final WalletCreator walletCreator;

    @Transactional(readOnly = true)
    public WalletResponseDto getWallet(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseGet(() -> walletCreator.createWalletSafely(memberId));
        return WalletResponseDto.from(wallet);
    }

    @Transactional(readOnly = true)
    public Long getWalletId(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseGet(() -> walletCreator.createWalletSafely(memberId));
        return wallet.getId();
    }
}
