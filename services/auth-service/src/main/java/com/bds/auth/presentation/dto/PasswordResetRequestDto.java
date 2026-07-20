package com.bds.auth.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
    @NotBlank @Email String email,
    @NotBlank String newPassword
) {

}
