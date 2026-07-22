package com.bds.common.dto;

import java.util.List;

public record CurrentUser(
    Long id,
    String email,
    List<String> roles
) {
}
