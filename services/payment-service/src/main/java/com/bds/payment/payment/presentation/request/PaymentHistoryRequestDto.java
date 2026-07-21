package com.bds.payment.payment.presentation.request;

public record PaymentHistoryRequestDto(
        //TODO: 추후 from, to는 LocalDateTime으로 변경 가능성 존재
        int from,
        int to,
        int page,
        int size
) {
}
