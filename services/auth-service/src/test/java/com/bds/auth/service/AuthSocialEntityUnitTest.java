package com.bds.auth.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthSocial 엔티티 단위 테스트 - 성공 케이스")
public class AuthSocialEntityUnitTest {

    @Nested
    @DisplayName("AuthSocial.create() 테스트")
    public class CreateAuthSocial {

        @Test
        @DisplayName("소셜 정보(providerId, provider, email, authId)가 주어지면 AuthSocial 객체가 정상 생성된다")
        public void 생성_성공() {
            // given
            String providerId = "kakao_12345";
            String provider = "KAKAO";
            String email = "yeojin_social@email.com";
            Long authId = 1L;

            // when
            AuthSocial authSocial = AuthSocial.create(providerId, provider, email, authId);

            // then
            assertNotNull(authSocial);
            assertEquals(providerId, authSocial.getProviderId());
            assertEquals(provider, authSocial.getProvider());
            assertEquals(email, authSocial.getEmail());
            assertEquals(authId, authSocial.getAuthId());
        }
    }

    @Nested
    @DisplayName("AuthSocial.of() 테스트")
    public class OfAuthSocial {

        @Test
        @DisplayName("모든 DB 필드 값이 주어지면 조회용 AuthSocial 객체가 정상 매핑된다")
        public void 매핑_성공() {
            // given
            Long id = 50L;
            String providerId = "google_67890";
            String provider = "GOOGLE";
            String email = "yeojin_google@email.com";
            Long authId = 2L;

            // when
            AuthSocial authSocial = AuthSocial.of(id, providerId, provider, email, authId);

            // then
            assertNotNull(authSocial);
            assertEquals(providerId, authSocial.getProviderId());
            assertEquals(provider, authSocial.getProvider());
            assertEquals(email, authSocial.getEmail());
            assertEquals(authId, authSocial.getAuthId());
        }
    }
}