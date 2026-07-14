package com.bds.auth.presentation.dto;

public record AuthLoginResponseDto(
    String accessToken,
    String refreshToken
) {


}
