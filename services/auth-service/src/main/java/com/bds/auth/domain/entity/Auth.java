package com.bds.auth.domain.entity;

import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import lombok.Getter;

@Getter
public class Auth {

    private final Long id;
    private final String email;
    private final Status status;
    private final Role role;

    private Auth(Long id, String email, Status status, Role role) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.role = role;
    }

    public static Auth create(String email, Role role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("흠..");
        }
        return new Auth(null, email, Status.ACTIVE, (role != null) ? role : Role.SUPPORTER);
    }

    public static Auth of(Long id, String email, Status status, Role role) {
        return new Auth(id, email, status, role);
    }
}
