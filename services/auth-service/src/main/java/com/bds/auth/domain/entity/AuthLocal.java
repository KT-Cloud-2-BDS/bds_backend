package com.bds.auth.domain.entity;

import lombok.Getter;

@Getter
public class AuthLocal {

    private Long id;
    private String password;
    private Long authId;

    public static AuthLocal create(Long authId, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("흠..");
        }
        AuthLocal authLocal = new AuthLocal();
        authLocal.password = password;
        authLocal.authId = authId;
        return authLocal;
    }

    public static AuthLocal of(Long id, String password, Long authId) {
        AuthLocal authLocal = new AuthLocal();
        authLocal.id = id;
        authLocal.password = password;
        authLocal.authId = authId;
        return authLocal;
    }

}
