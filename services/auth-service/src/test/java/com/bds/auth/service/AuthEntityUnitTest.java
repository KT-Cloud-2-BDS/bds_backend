package com.bds.auth.service;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auth 엔티티 단위 테스트 - 성공 케이스")
public class AuthEntityUnitTest {

    @Nested
    @DisplayName("Auth.create() 테스트")
    public class CreateAuth {

        @Test
        @DisplayName("정상적인 이메일과 권한이 주어지면 Auth 객체가 정상 생성된다")
        public void 생성_성공() {
            // given
            String email = "yeojin@email.com";
            Role role = Role.SUPPORTER;

            // when
            Auth auth = Auth.create(email, Status.ACTIVE, role);

            // then
            assertNotNull(auth);
            assertEquals(email, auth.getEmail());
            assertEquals(Status.ACTIVE, auth.getStatus());
            assertEquals(role, auth.getRole());
        }

        @Test
        @DisplayName("입력된 role이 null이면 기본값(USER 또는 기본 롤)으로 매핑된다")
        public void role_null_삼항연산자_분기저격() {
            // given
            String email = "yeojin@email.com";
            Role role = null;

            // when
            Auth auth = Auth.create(email, Status.ACTIVE, role);

            // then
            assertNotNull(auth);
        }
    }

    @Nested
    @DisplayName("Auth.of() 테스트")
    public class OfAuth {

        @Test
        @DisplayName("모든 필드 값이 주어지면 조회용 Auth 객체가 정상 매핑된다")
        public void 매핑_성공() {
            // given
            Long id = 1L;
            String email = "yeojin@email.com";
            Status status = Status.ACTIVE;
            Role role = Role.SUPPORTER;

            // when
            Auth auth = Auth.of(id, email, status, role);

            // then
            assertEquals(id, auth.getId());
            assertEquals(email, auth.getEmail());
            assertEquals(status, auth.getStatus());
            assertEquals(role, auth.getRole());
        }
    }

    @Nested
    @DisplayName("changeStatus() 테스트")
    public class ChangeStatus {

        @Test
        @DisplayName("정상적인 새로운 상태가 주어지면 status 필드가 변경된다")
        public void 상태변경_성공() {
            // given
            Auth auth = Auth.create("yeojin@email.com", Status.ACTIVE, Role.SUPPORTER);
            Status newStatus = Status.DELETED;

            // when
            auth.changeStatus(newStatus);

            // then
            assertEquals(newStatus, auth.getStatus());
        }
    }
}