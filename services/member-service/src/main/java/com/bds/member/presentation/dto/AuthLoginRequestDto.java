package com.bds.member.presentation.dto;

public record AuthLoginRequestDto(
    String email,
    String password
) {

}
