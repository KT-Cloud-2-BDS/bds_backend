package com.bds.backend.common.dto;

public record CurrentUser(
    Long id,
    String email,
    String role
) {
}
