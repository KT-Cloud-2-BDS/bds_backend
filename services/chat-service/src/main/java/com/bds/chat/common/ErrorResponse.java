package com.bds.chat.common;

public record ErrorResponse(String code, String message, String detail) {

    public static ErrorResponse of(ErrorCode errorCode, String detail) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), detail);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
    }
}
