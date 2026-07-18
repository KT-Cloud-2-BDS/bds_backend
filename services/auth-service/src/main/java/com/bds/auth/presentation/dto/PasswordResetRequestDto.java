package com.bds.auth.presentation.dto;

public record PasswordResetRequestDto(
    String email,
    String newPassword
) {

}
