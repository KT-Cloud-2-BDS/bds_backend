package com.bds.payment.payment.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값 유효성 오류"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND","리소스 없음"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED","지원하지 않는 HTTP 메소드"),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "리소스 충돌 (중복)"),
    BUSINESS_RULE_VIOLATION(HttpStatus.valueOf(422), "BUSINESS_RULE_VIOLATION","비즈니스 규칙 위반"),
    RESOURCE_LOCKED(HttpStatus.LOCKED, "RESOURCE_LOCKED","리소스가 잠겨있음"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR","서버 에러가 발생했습니다"),
    DEPENDENCY_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_FAILURE","외부 서비스 호출 실패"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message, String code) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.code = code;
    }
}
