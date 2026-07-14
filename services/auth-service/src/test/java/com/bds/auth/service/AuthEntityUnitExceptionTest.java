package com.bds.auth.service;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.global.exception.BusinessException;
import com.bds.auth.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auth 엔티티 단위 테스트 - 예외 케이스")
public class AuthEntityUnitExceptionTest {

    @Nested
    @DisplayName("Auth.create() 예외 핸들링")
    public class CreateAuthException {

        @Test
        @DisplayName("이메일이 null이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 이메일_null_예외발생() {
            // given
            String email = null;

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                Auth.create(email, Status.ACTIVE, Role.SUPPORTER);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }

        @Test
        @DisplayName("이메일이 공백(Blank)이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 이메일_공백_예외발생() {
            // given
            String email = "   ";

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                Auth.create(email, Status.ACTIVE, Role.SUPPORTER);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("changeStatus() 예외 핸들링")
    public class ChangeStatusException {

        @Test
        @DisplayName("변경하려는 상태가 null이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 상태_null_예외발생() {
            // given
            Auth auth = Auth.create("yeojin@email.com", Status.ACTIVE, Role.SUPPORTER);
            Status invalidStatus = null;

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                auth.changeStatus(invalidStatus);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }
}