package com.bds.payment.payment.presentation.controller;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.presentation.response.WalletResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponseDto> walletInfo(@RequestHeader("X-Member-Id") Long memberId) {
        return ResponseEntity.ok(walletService.getWalletResponseDto(memberId));
    }

    @PostMapping("/wallet")
    public ResponseEntity<WalletResponseDto> createWallet(@RequestHeader("X-Member-Id") Long memberId) {
        return ResponseEntity.ok(walletService.createWallet(memberId));
    }
}
