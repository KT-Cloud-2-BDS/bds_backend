package com.bds.member.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResponseStatusException은 catch-all 핸들러가 아니라 원래 상태 코드로 그대로 응답한다")
    void ResponseStatusException_원래상태코드_유지() {
        ResponseStatusException e = new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 헤더에 존재하지 않습니다.");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(e);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().code());
        assertEquals("인증된 유저 정보가 헤더에 존재하지 않습니다.", response.getBody().message());
    }

    @Test
    @DisplayName("BusinessException이나 ResponseStatusException이 아닌 예외는 500 INTERNAL_ERROR로 응답한다")
    void 일반예외_500으로_응답() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), response.getBody().code());
    }
}
