package com.bds.chat.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMsg;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorMsg = null;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorMsg = detail;
    }

    public ErrorCode getErrorCode() {return errorCode;}

    public String getErrorMsg() {return errorMsg;}
}
