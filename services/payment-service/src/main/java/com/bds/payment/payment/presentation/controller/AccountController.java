package com.bds.payment.payment.presentation.controller;

import com.bds.payment.payment.application.accounts.AccountService;
import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import com.bds.payment.payment.presentation.response.AccountVerifyResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    public AccountVerifyResponseDto accountRegister(@RequestHeader("X-Member-Id") Long memberId, @RequestBody @Valid AccountRegisterRequestDto dto) {
        return accountService.registerAccount(memberId, dto);
    }

    @PostMapping("/accounts/verify")
    public AccountVerifyResponseDto accountVerify(@RequestHeader("X-Member-Id") Long memberId, @RequestBody @Valid AccountVerifyRequestDto dto) {
        return accountService.verifyAccount(memberId, dto);
    }
}
