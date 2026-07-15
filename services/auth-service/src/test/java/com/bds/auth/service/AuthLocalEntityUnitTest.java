package com.bds.auth.service;

import com.bds.auth.domain.entity.AuthLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthLocal 엔티티 단위 테스트 - 성공 케이스")
public class AuthLocalEntityUnitTest {

    @Nested
    @DisplayName("AuthLocal.create() 테스트")
    public class CreateAuthLocal {
        @Test
        @DisplayName("정상적인 authId와 비밀번호가 주어지면 AuthLocal 객체가 정상 생성된다")
        public void 생성_성공() {
            // given
            Long authId = 1L;
            String password = "password123!";

            // when
            AuthLocal authLocal = AuthLocal.create(authId, password);

            // then
            assertNotNull(authLocal);
            assertEquals(authId, authLocal.getAuthId());
            assertEquals(password, authLocal.getPassword());
        }
    }

    @Nested
    @DisplayName("AuthLocal.of() 테스트")
    public class OfAuthLocal {
        @Test
        @DisplayName("모든 필드 값이 주어지면 조회용 AuthLocal 객체가 정상 매핑된다")
        public void 매핑_성공() {
            // given
            Long id = 10L;
            String password = "encodedPassword123!";
            Long authId = 1L;

            // when
            AuthLocal authLocal = AuthLocal.of(id, password, authId);

            // then
            assertNotNull(authLocal);
            assertEquals(password, authLocal.getPassword());
            assertEquals(authId, authLocal.getAuthId());
        }
    }
}