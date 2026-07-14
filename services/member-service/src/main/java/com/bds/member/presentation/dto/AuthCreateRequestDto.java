package com.bds.member.presentation.dto;

public record AuthCreateRequestDto(
    String email,
    String password
) {

}
