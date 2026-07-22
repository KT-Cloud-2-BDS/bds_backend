package com.bds.auth.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;

import com.bds.auth.infrastructure.security.JwtKeyProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwksService 단위 테스트")
class JwksServiceUnitTest {

    @InjectMocks
    private JwksService jwksService;

    @Mock
    private JwtKeyProvider jwtKeyProvider;

    @Test
    @DisplayName("getPublicKeySet은 JwtKeyProvider가 제공하는 JWKSet을 그대로 반환한다")
    void 공개키셋_조회_위임() throws JOSEException {
        JWKSet expected = new JWKSet(new RSAKeyGenerator(2048).keyID("test-kid").generate().toPublicJWK());
        given(jwtKeyProvider.getPublicJWKSet()).willReturn(expected);

        JWKSet actual = jwksService.getPublicKeySet();

        assertSame(expected, actual);
    }
}
