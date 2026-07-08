package com.bds.payment.payment.presentation.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaymentHistoryPageResponseDto(
        List<PaymentHistoryResponseDto> content,
        int page,
        int size,
        long totalElements
) {
    public static PaymentHistoryPageResponseDto from(Page<PaymentHistoryResponseDto> page) {
        return new PaymentHistoryPageResponseDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
