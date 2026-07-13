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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","인증이 필요합니다."),
    // 지갑 도메인
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "지갑을 찾을 수 없습니다."),
    WALLET_ALREADY_EXISTS(HttpStatus.CONFLICT, "WALLET_ALREADY_EXISTS", "이미 지갑이 존재합니다."),
    WALLET_AMOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "WALLET_AMOUNT_REQUIRED", "금액은 null일 수 없습니다."),
    WALLET_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "WALLET_AMOUNT_INVALID", "금액은 0보다 커야 합니다."),
    WALLET_INSUFFICIENT_BALANCE(HttpStatus.valueOf(422), "WALLET_INSUFFICIENT_BALANCE", "잔액이 부족합니다."),
    // 계좌 도메인
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "등록된 계좌를 찾을 수 없습니다."),
    ACCOUNT_ALREADY_VERIFIED(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_VERIFIED", "이미 인증된 계좌가 등록되어 있습니다."),
    ACCOUNT_VERIFICATION_FAILED(HttpStatus.valueOf(422), "ACCOUNT_VERIFICATION_FAILED", "계좌 인증에 실패했습니다."),
    // 외부 은행 API
    BANK_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BANK_SERVICE_UNAVAILABLE", "은행 서비스 호출에 실패했습니다."),
    BANK_VERIFICATION_REQUEST_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "BANK_VERIFICATION_REQUEST_FAILED", "은행 계좌 인증코드 요청에 실패했습니다."),
    BANK_VERIFICATION_CONFIRM_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "BANK_VERIFICATION_CONFIRM_FAILED", "은행 계좌 인증 확인에 실패했습니다."),
    BANK_WITHDRAW_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "BANK_WITHDRAW_FAILED", "은행 계좌로부터 충전에 실패했습니다."),
    BANK_DEPOSIT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "BANK_DEPOSIT_FAILED", "은행 계좌로의 환불에 실패했습니다."),
    BANK_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "BANK_RESPONSE_INVALID", "은행 서버로부터 유효하지 않은 응답을 받았습니다."),
    // 결제/거래 도메인
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "조회 시작일이 종료일보다 늦을 수 없습니다."),
    DATE_RANGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "DATE_RANGE_TOO_LARGE", "조회 기간은 최대 3개월까지 가능합니다."),
    PAYMENT_HISTORY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_HISTORY_SAVE_FAILED", "결제 이력 저장에 실패했습니다."),
    ACCOUNT_NOT_VERIFIED(HttpStatus.valueOf(422), "ACCOUNT_NOT_VERIFIED", "인증되지 않은 계좌입니다."),
    // 펀딩 도메인
    FUNDING_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING_NOT_FOUND", "존재하지 않는 거래입니다."),
    FUNDING_DUPLICATED(HttpStatus.CONFLICT, "FUNDING_DUPLICATED", "중복된 거래입니다."),
    FUNDING_ALREADY_REFUNDED(HttpStatus.CONFLICT, "FUNDING_ALREADY_REFUNDED", "이미 환불된 거래입니다."),
    FUNDING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FUNDING_ACCESS_DENIED", "해당 거래에 접근할 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}