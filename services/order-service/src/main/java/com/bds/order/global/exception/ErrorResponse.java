package com.bds.order.global.exception;

public record ErrorResponse(
        String code,
        String message,
        String detail
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String detail) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), detail);
    }
}