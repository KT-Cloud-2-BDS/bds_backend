package com.bds.chat.infrastructure.config;

import lombok.Getter;

import java.security.Principal;
import java.util.Set;

public class StompPrincipal implements Principal {
    private final String userId;
    @Getter
    private final Set<String> roles;
    public StompPrincipal(String userId, Set<String> roles) {
        this.userId = userId;
        this.roles = roles;
    }
    @Override
    public String getName() {
        return userId;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
