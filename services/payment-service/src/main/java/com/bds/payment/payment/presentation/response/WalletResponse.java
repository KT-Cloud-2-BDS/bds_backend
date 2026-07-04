package com.bds.payment.payment.presentation.response;

import com.bds.payment.payment.domain.wallet.Wallet;

public record WalletResponse(
        Long memberId,
        Long balance
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getMemberId(), wallet.getBalance());
    }
}