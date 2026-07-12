package com.bds.common.dto;

public record CurrentUser(
    Long id,
    String email,
    String role
) {
}
