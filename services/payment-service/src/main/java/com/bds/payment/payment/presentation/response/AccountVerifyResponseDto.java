package com.bds.payment.payment.presentation.response;

public record AccountVerifyResponseDto(
        String message,
        String code
) {
    public static AccountVerifyResponseDto init(String message, String code){
        return new AccountVerifyResponseDto(message, code);
    }
}
