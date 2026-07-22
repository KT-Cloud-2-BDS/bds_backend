package com.bds.auth.service;

import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.global.exception.BusinessException;
import com.bds.auth.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthLocal 엔티티 단위 테스트 - 예외 케이스")
public class AuthLocalEntityUnitExceptionTest {

    @Nested
    @DisplayName("AuthLocal.create() 예외 핸들링")
    public class CreateAuthLocalException {

        @Test
        @DisplayName("비밀번호가 null이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 비밀번호_null_예외발생() {
            // given
            Long authId = 1L;
            String invalidPassword = null;

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                AuthLocal.create(authId, invalidPassword);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }

        @Test
        @DisplayName("비밀번호가 공백(Blank)이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 비밀번호_공백_예외발생() {
            // given
            Long authId = 1L;
            String invalidPassword = "   ";

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                AuthLocal.create(authId, invalidPassword);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("AuthLocal.changePassword() 예외 핸들링")
    public class ChangePasswordException {

        @Test
        @DisplayName("새 비밀번호가 null이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 새비밀번호_null_예외발생() {
            // given
            AuthLocal authLocal = AuthLocal.of(1L, "encodedPassword", 1L);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authLocal.changePassword(null);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }

        @Test
        @DisplayName("새 비밀번호가 공백(Blank)이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 새비밀번호_공백_예외발생() {
            // given
            AuthLocal authLocal = AuthLocal.of(1L, "encodedPassword", 1L);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authLocal.changePassword("   ");
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }
}
