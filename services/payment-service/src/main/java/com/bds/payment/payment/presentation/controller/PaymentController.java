package com.bds.payment.payment.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.payment.payment.application.payment.PaymentService;
import com.bds.payment.payment.presentation.request.AccountTransactionRequestDto;
import com.bds.payment.payment.presentation.response.AccountTransactionResponseDto;
import com.bds.payment.payment.presentation.response.PaymentHistoryPageResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/deposit")
    public AccountTransactionResponseDto deposit(@LoginUser CurrentUser currentUser, @RequestBody @Valid AccountTransactionRequestDto dto) {
        return paymentService.charge(currentUser.id(), dto);
    }

    @PostMapping("/withdraw")
    public AccountTransactionResponseDto withdraw(@LoginUser CurrentUser currentUser, @RequestBody @Valid AccountTransactionRequestDto dto) {
        return paymentService.withdraw(currentUser.id(), dto);
    }

    @GetMapping("/history")
    public PaymentHistoryPageResponseDto getHistory(
            @LoginUser CurrentUser currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return paymentService.getHistory(currentUser.id(), from, to, pageable);
    }
}
