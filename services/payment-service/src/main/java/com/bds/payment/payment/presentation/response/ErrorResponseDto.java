package com.bds.payment.payment.presentation.response;

import com.bds.payment.payment.global.exception.ErrorCode;

public record ErrorResponseDto<T> (
        String code,
        String message,
        T detail
) {
    public static <T> ErrorResponseDto<T> of(ErrorCode code, T detail) {
        return new ErrorResponseDto<>(code.getCode(), code.getMessage(), detail);
    }
}
