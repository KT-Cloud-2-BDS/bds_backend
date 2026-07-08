package com.bds.member.presentation.dto;


public record MemberLoginResponseDto(
    String accessToken,
    String grantType
) {
    public static MemberLoginResponseDto of(String accessToken) {
        return new MemberLoginResponseDto(accessToken, "Bearer");
    }
}
