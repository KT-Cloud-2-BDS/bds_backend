package com.bds.payment.payment.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Object detail) {
        super(errorCode.getMessage() + " : " + detail);
        this.errorCode = errorCode;
    }
}
