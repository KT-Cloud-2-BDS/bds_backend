package com.bds.member.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M_001", "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M_002", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "M_003", "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST,"M_004", "이미 사용중인 닉네임입니다."),
    UNAUTHORIZED_MEMBER(HttpStatus.UNAUTHORIZED, "M_005", "인증 정보가 없거나 유효하지 않습니다."),


    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G_002", "올바르지 않은 입력값입니다."),

    AUTH_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A_001", "인증 서버와의 통신에 실패했습니다.");

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
