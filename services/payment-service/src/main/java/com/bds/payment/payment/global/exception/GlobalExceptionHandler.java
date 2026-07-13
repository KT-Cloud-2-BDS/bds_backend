package com.bds.payment.payment.global.exception;

import com.bds.payment.payment.presentation.response.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponseDto<Map<String, String>>> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Optional.ofNullable(error.getDefaultMessage()).orElse("유효하지 않은 값입니다."),
                        (existing, _) -> existing
                ));
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponseDto.of(ErrorCode.INVALID_INPUT, validationErrors));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponseDto<String>> apiCustomException(BusinessException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ErrorResponseDto.of(ex.getErrorCode(), null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto<String>> resolveArgumentException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity
                .status(status)
                .body(ErrorResponseDto.of(status.name(), ex.getReason(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto<String>> handleException(Exception ex) {

        ErrorCode resultCode = switch (ex) {
            case NoHandlerFoundException _ -> ErrorCode.NOT_FOUND;
            case BindException _ -> ErrorCode.INVALID_INPUT;
            default -> ErrorCode.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity
                .status(resultCode.getHttpStatus())
                .body(ErrorResponseDto.of(resultCode, null));
    }
}