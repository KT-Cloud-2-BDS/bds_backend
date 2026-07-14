package com.bds.auth.presentation.dto;

import com.bds.auth.domain.entity.enums.Role;

public record AuthRoleResponseDto(
    Role role
) {

}