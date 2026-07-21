package com.bds.payment.payment.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.payment.payment.application.accounts.AccountService;
import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import com.bds.payment.payment.presentation.response.AccountVerifyResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    public AccountVerifyResponseDto accountRegister(@LoginUser CurrentUser currentUser, @RequestBody @Valid AccountRegisterRequestDto dto) {
        return accountService.registerAccount(currentUser.id(), dto);
    }

    @PostMapping("/accounts/verify")
    public AccountVerifyResponseDto accountVerify(@LoginUser CurrentUser currentUser, @RequestBody @Valid AccountVerifyRequestDto dto) {
        return accountService.verifyAccount(currentUser.id(), dto);
    }
}
