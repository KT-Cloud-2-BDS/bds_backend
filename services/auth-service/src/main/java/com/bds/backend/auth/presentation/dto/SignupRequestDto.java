package com.bds.backend.auth.presentation.dto;

public record SignupRequestDto(
    String email,
    String password,
    String nickname
) {
}
