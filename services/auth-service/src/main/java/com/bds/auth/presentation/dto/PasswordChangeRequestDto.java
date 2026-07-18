package com.bds.auth.presentation.dto;

public record PasswordChangeRequestDto(
    String currentPassword,
    String newPassword
) {

}
