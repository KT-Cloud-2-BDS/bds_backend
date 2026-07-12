package com.bds.payment.payment.presentation.response;

public record AccountVerifyResponseDto(
        String message
) {
    public static AccountVerifyResponseDto init(String message){
        return new AccountVerifyResponseDto(message);
    }
}
