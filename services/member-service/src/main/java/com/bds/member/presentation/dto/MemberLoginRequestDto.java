package com.bds.member.presentation.dto;

public record MemberLoginRequestDto(
    String email,
    String password
) {

}
