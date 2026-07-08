package com.bds.member.presentation.dto;

public record AuthLoginResponseDto(
    String accessToken,
    String refreshToken
) {

}
