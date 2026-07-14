package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtKeyProvider 단위 테스트")
class JwtKeyProviderUnitTest {

    private static final String VALID_PRIVATE_KEY_PEM = """
        -----BEGIN PRIVATE KEY-----
        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOZIiR88qn9CJp
        IWG/tLLOvnp183eM6nyDARfZW0rMf8EmKeSBzTJ0CaMykP13Eaka3B8di7l1LSmn
        Y3VSo6NmvIdVMJbLfpt1aEAO4/VRhZs5GQDUBnNluG4EsG+ZQ9t6HHQTa22fkrcU
        EcVsTGdV3/zBQ1isLGLpahshtTqqlX0IV0qeK6IJsda9q0eHK2U+GHQKFpzl1l5H
        JqaoiTKTU5+0H4RMkrH4nvGWYSON7VzudUcuADAxUCWnzU8BWTBQ6eGlNatKIP/V
        7024U6uTzBTtQ2uAHncIo9Ik7GZwW/ZsfbTIKjOFC/L60H/LX3Ktbf3aRadIQ2ea
        uAQUq4jtAgMBAAECggEAdgV/IxbpuAke9Ecmgl1uMlMx6+kBYA9mi/NAFAbkSvRz
        GsOx65fPVhN8wxmuFzx+GuHs2RRhRORCgXmaSVtRwRnluY1DbbJEa2rl1p38VlW0
        YX4uvw/BRVWldAEAkAABrV4/8iroUOuwzUUsQ7WdEbo36rv8JNhs51TJlwjvN0Ok
        ZQ7OSQf6yytsFLpaWRL94a8wydzYq/kgrfsAzP9yfk3Ndp2wyR1ScLYnbq4Qk/M+
        VD2I8Bp4qgmZQ99cip5vtiWR2b1vmkODzQua+9TSvbzdEChhFAlLvQth2CVKS5MN
        PEM0lYkufn/MyIt4sfil7EqB9xHYWzH63NSQc3KmAQKBgQD8cKrLRU68nBQTPeus
        phLCg4QPmrIXdi48feQ1N4WfrNndvnkdGnwrAykKGc9Q6DgAo24FGQ7tyid4Z1Xs
        6xpfGD5U8ED3PAKAwE2NjLv7+YmFCUxsxK158wtnbPKSEwW+ugVfxpV4TsE7BVlW
        L9pBV+Mqb+sKwDs2THAiUcTWTQKBgQDRTaF9hHvsicy2MO3RAykJi4Un56mUUn1W
        gCRsFf7ZqGuh9crbjNEueCXe7a5vsVmJKbGdvj81v5d7IRgL3ZwBCN8VQBhoJpSM
        zCJIhYuCVhbFB2v2GEzJIAoJzZhf/DCyKy9l6i3wCCcsvWwBfFyP+oXIZJ3/KhpZ
        XwHQYhINIQKBgQCS4C7PdDTakmkvxOBBpKLXn9OPjK8/7Vf7hDfqKrLJc7WTSgaH
        w2gpUn5nTRsNdP9Yh740ox7Hhc4vRNh+r0+5so5ZtyvlbXn7VWgKCq85bTfxOrbH
        titE//9Lnt0H1p+KGAuWbSF8TT4qS/NtrQvs5ujaQSHdMHmDHzhDD5ZaJQKBgQCH
        C0yv/MQseTixMN6wphZbRz8R4UIkJhkir6lQZ9y4ORfBNyK9B52MGw9sR/TtsgMw
        IcutGKwWFNk7I74tIb5fWIb/YgydXo8oTSmtmyTJYpxsYf6BmWopttdjPdkbzU7q
        Fj0Nx8v9/HSyREGkr8XVS/H3uxvKIDNCCK/V8QDuoQKBgDRqgnSj4aycgbPDHAAw
        hbZEmIss67dXorjMaecrF1f3JNpk9L0j/e0XOFR3/uWmWa4my38gGZdQCME7WwbA
        0ZgJyh7WYXl4u7YqwkFEHSpkZe3B5Czn71uR9DjtPf9PHDfzbdWUwqKz7OmdeLQx
        yz9s4ngqlDTmY7IlOrp+TDaI
        -----END PRIVATE KEY-----
        """;

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
