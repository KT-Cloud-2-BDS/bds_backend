package com.bds.auth.domain.entity;

import com.bds.auth.global.exception.BusinessException;
import com.bds.auth.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AuthLocal {

    private Long id;
    private String password;
    private Long authId;

    public static AuthLocal create(Long authId, String password) {
        if (authId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
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

    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.password = newPassword;
    }

}
