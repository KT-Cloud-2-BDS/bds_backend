package com.bds.auth.presentation.dto;

public record AuthCreateRequestDto(
    String email,
    String password
) {

}
