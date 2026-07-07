package com.bds.auth.domain.entity;

import lombok.Getter;

@Getter
public class AuthLocal {

    private Long id;
    private String password;
    private Auth auth;

    public static AuthLocal create(String password, Auth auth) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("흠..");
        }
        AuthLocal authLocal = new AuthLocal();
        authLocal.password = password;
        authLocal.auth = auth;
        return authLocal;
    }

    public static AuthLocal of(Long id, String password, Auth auth) {
        AuthLocal authLocal = new AuthLocal();
        authLocal.id = id;
        authLocal.password = password;
        authLocal.auth = auth;
        return authLocal;
    }
}
