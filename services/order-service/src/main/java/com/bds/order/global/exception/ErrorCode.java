package com.bds.order.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통 에러 코드
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값 유효성 오류"),
    REQUEST_INVALID(HttpStatus.BAD_REQUEST, "Request Body 누락"),
    REQUEST_BODY_MALFORMED(HttpStatus.BAD_REQUEST, "JSON 형식 오류"),

    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스 없음"),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메소드"),

    CONFLICT(HttpStatus.CONFLICT, "리소스 충돌 (중복)"),

    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "비즈니스 규칙 위반"),

    RESOURCE_LOCKED(HttpStatus.LOCKED, "리소스가 잠겨있음"),

    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청 한도 초과"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류"),

    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스 일시적 사용 불가"),
    DEPENDENCY_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "외부 서비스 호출 실패"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다"),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 주문만 접근할 수 있습니다"),
    ORDER_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "주문 상태를 변경할 수 없습니다"),

    // Reward
    REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리워드를 찾을 수 없습니다"),
    REWARD_DUPLICATED(HttpStatus.BAD_REQUEST, "동일한 리워드를 중복으로 선택할 수 없습니다"),
    REWARD_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "리워드 재고가 부족합니다"),

    // Funding
    FUNDING_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 펀딩을 찾을 수 없습니다"),
    FUNDING_NOT_AVAILABLE(HttpStatus.FORBIDDEN, "현재 펀딩 가능한 기간이 아닙니다");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}