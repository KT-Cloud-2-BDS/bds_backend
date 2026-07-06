package com.bds.payment.payment.presentation.response;

import com.bds.payment.payment.domain.wallet.Wallet;

public record WalletResponseDto(
        Long memberId,
        Long balance
) {
    public static WalletResponseDto from(Wallet wallet) {
        return new WalletResponseDto(wallet.getMemberId(), wallet.getBalance());
    }
}