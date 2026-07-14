package com.bds.auth.presentation.dto;

public record AuthLoginRequestDto(
    String email,
    String password
) {

}
