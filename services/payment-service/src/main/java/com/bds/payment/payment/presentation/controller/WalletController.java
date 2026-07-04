package com.bds.payment.payment.presentation.controller;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.presentation.response.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> walletInfo(@RequestHeader("X-Member-Id") Long memberId) {
        return ResponseEntity.ok(walletService.getWallet(memberId));
    }
}
