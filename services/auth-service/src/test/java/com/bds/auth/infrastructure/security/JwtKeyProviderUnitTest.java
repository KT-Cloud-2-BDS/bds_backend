package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bds.auth.support.TestRsaKeyGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtKeyProvider 단위 테스트")
class JwtKeyProviderUnitTest {

    private static final String VALID_PRIVATE_KEY_PEM = TestRsaKeyGenerator.generatePrivateKeyPem();

    @Test
    @DisplayName("유효한 PEM이 주어지면 개인키/공개키 쌍과 JWKSet을 정상적으로 생성한다")
    void 키생성_성공() {
        JwtKeyProvider jwtKeyProvider = new JwtKeyProvider(VALID_PRIVATE_KEY_PEM);

        RSAPrivateKey privateKey = (RSAPrivateKey) jwtKeyProvider.getPrivateKey();
        RSAPublicKey publicKey = (RSAPublicKey) jwtKeyProvider.getPublicKey();

        assertNotNull(privateKey);
        assertNotNull(publicKey);
        assertEquals(privateKey.getModulus(), publicKey.getModulus());
        assertNotNull(jwtKeyProvider.getKeyId());
        assertEquals(1, jwtKeyProvider.getPublicJWKSet().getKeys().size());
    }

    @Test
    @DisplayName("같은 PEM으로 생성해도 키 ID는 매번 동일하게 계산된다")
    void 키아이디_결정론적_생성() {
        JwtKeyProvider first = new JwtKeyProvider(VALID_PRIVATE_KEY_PEM);
        JwtKeyProvider second = new JwtKeyProvider(VALID_PRIVATE_KEY_PEM);

        assertEquals(first.getKeyId(), second.getKeyId());
    }

    @Test
    @DisplayName("공개키 JWKSet에는 개인키 정보가 포함되지 않는다")
    void 공개키셋_비공개키_미포함() {
        JwtKeyProvider jwtKeyProvider = new JwtKeyProvider(VALID_PRIVATE_KEY_PEM);

        boolean containsPrivateKeyData = jwtKeyProvider.getPublicJWKSet().getKeys().stream()
            .anyMatch(key -> key.isPrivate());

        assertEquals(false, containsPrivateKeyData);
    }

    @Test
    @DisplayName("Base64로 디코딩할 수 없는 PEM이 주어지면 IllegalArgumentException이 발생한다")
    void 잘못된Base64_예외발생() {
        assertThrows(IllegalArgumentException.class, () -> new JwtKeyProvider("not-a-valid-pem"));
    }

    @Test
    @DisplayName("Base64는 유효하지만 PKCS8 RSA 키 구조가 아니면 IllegalStateException이 발생한다")
    void 잘못된키구조_예외발생() {
        String invalidKeyPem = """
            -----BEGIN PRIVATE KEY-----
            YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=
            -----END PRIVATE KEY-----
            """;

        assertThrows(IllegalStateException.class, () -> new JwtKeyProvider(invalidKeyPem));
    }
}
