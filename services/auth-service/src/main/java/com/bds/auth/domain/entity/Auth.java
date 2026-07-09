package com.bds.auth.domain.entity;

import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.global.exception.BusinessException;
import com.bds.auth.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class Auth {

    private final Long id;
    private final String email;
    private  Status status;
    private  Role role;

    private Auth(Long id, String email, Status status, Role role) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.role = role;
    }

    public static Auth create(String email, Status status, Role role) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new Auth(null, email, Status.ACTIVE, (role != null) ? role : Role.SUPPORTER);
    }

    public static Auth of(Long id, String email, Status status, Role role) {
        return new Auth(id, email, status, role);
    }

    public void changeStatus(Status status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.status = status;
    }
}
