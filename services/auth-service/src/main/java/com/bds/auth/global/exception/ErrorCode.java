package com.bds.auth.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "A_001", "이미 존재하는 이메일입니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "A_002", "인증번호가 만료되었거나 발송되지 않았습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "A_003", "인증번호가 일치하지 않습니다."),
    UNVERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "A_004", "인증되지 않은 이메일입니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "A_005", "존재하지 않는 계정입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "A_006", "비밀번호가 일치하지 않습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "A_007", "올바르지 않은 입력값입니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.BAD_REQUEST,"A_008" ,"이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A_009", "유효하지 않거나 만료된 refresh token입니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G_005", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}