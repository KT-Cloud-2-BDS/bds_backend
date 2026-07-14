package com.bds.member.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        HttpStatusCode status = e.getStatusCode();
        String code = (status instanceof HttpStatus httpStatus) ? httpStatus.name() : String.valueOf(status.value());
        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(code, e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception 발생", e);
        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
            .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}