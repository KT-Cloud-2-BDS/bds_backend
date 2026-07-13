package com.bds.payment.payment.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.presentation.response.WalletResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponseDto> walletInfo(@LoginUser CurrentUser currentUser) {
        return ResponseEntity.ok(walletService.getWalletResponseDto(currentUser.id()));
    }

    @PostMapping("/wallet")
    public ResponseEntity<WalletResponseDto> createWallet(@LoginUser CurrentUser currentUser) {
        return ResponseEntity.ok(walletService.createWallet(currentUser.id()));
    }
}
