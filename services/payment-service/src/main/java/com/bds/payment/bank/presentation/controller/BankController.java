package com.bds.payment.bank.presentation.controller;

import com.bds.payment.bank.application.BankService;
import com.bds.payment.bank.presentation.request.BankAccountRequestDto;
import com.bds.payment.bank.presentation.request.BankTransactionRequestDto;
import com.bds.payment.bank.presentation.request.BankVerifyRequestDto;
import com.bds.payment.bank.presentation.response.BankAccountResponseDto;
import com.bds.payment.bank.presentation.response.BankTransactionResponseDto;
import com.bds.payment.bank.presentation.response.BankTransactionUnitResponseDto;
import com.bds.payment.bank.presentation.response.BankVerifyResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banks")
public class BankController {

    private final BankService bankService;

    @PostMapping("/accounts")
    public BankAccountResponseDto requestVerification(@RequestBody BankAccountRequestDto dto) {
        return bankService.sendVerificationCode(dto);
    }

    @PostMapping("/accounts/verify")
    public BankVerifyResponseDto confirmVerification(@RequestBody BankVerifyRequestDto dto) {
        return bankService.verifyCode(dto);
    }

    @PostMapping("/withdraw")
    public BankTransactionResponseDto withdraw(@RequestBody BankTransactionRequestDto dto) {
        return bankService.withdraw(dto);
    }

    @PostMapping("/deposit")
    public BankTransactionResponseDto deposit(@RequestBody BankTransactionRequestDto dto) {
        return bankService.deposit(dto);
    }

    /**
     * 추후 원화 환불에 대해 오류 발생시 장애 복구 용도
     */
    @GetMapping("/transactions/{tranSeqNo}")
    public BankTransactionUnitResponseDto getTransactionDetail(@PathVariable UUID tranSeqNo) {
        return bankService.getTransactionDetail(tranSeqNo);
    }
}
