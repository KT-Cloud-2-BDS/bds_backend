package com.bds.payment.payment.presentation.controller;

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
    public AccountTransactionResponseDto deposit(@RequestHeader("X-Member-Id") Long memberId, @RequestBody @Valid AccountTransactionRequestDto dto) {
        return paymentService.charge(memberId, dto);
    }

    @PostMapping("/withdraw")
    public AccountTransactionResponseDto withdraw(@RequestHeader("X-Member-Id") Long memberId, @RequestBody @Valid AccountTransactionRequestDto dto) {
        return paymentService.withdraw(memberId, dto);
    }

    @GetMapping("/history")
    public PaymentHistoryPageResponseDto getHistory(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return paymentService.getHistory(memberId, from, to, pageable);
    }
}
