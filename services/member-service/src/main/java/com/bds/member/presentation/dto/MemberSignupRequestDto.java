package com.bds.member.presentation.dto;

public record MemberSignupRequestDto(
    String email,
    String password,
    String nickname
) {

}
