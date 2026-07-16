package com.bds.auth.presentation.dto;

public record VerifyCodeRequestDto(
    String email,
    String verificationCode
) {

}
