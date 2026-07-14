package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bds.auth.domain.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenUtil 단위 테스트")
class JwtTokenUtilUnitTest {

    private static final String PRIVATE_KEY_PEM = """
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

    private static final long ACCESS_EXPIRATION_MS = 3_600_000L;
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;

    private JwtKeyProvider jwtKeyProvider;
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        jwtKeyProvider = new JwtKeyProvider(PRIVATE_KEY_PEM);
        jwtTokenUtil = new JwtTokenUtil(jwtKeyProvider, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("액세스 토큰은 authId, email, roles 클레임과 발급자를 포함하고 공개키로 검증된다")
    void 액세스토큰_발급_성공() {
        String token = jwtTokenUtil.createAccessToken(1L, "yeojin@email.com", Role.SUPPORTER);

        Jws<Claims> jws = parse(token, jwtKeyProvider.getPublicKey());
        Claims claims = jws.getPayload();

        assertEquals(jwtKeyProvider.getKeyId(), jws.getHeader().getKeyId());
        assertEquals("1", claims.getSubject());
        assertEquals("bds-auth", claims.getIssuer());
        assertEquals(1, claims.get("authId", Integer.class));
        assertEquals("yeojin@email.com", claims.get("email", String.class));
        assertEquals(List.of("SUPPORTER"), claims.get("roles", List.class));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    @DisplayName("리프레시 토큰은 authId만 포함하고 email/roles 클레임은 담지 않는다")
    void 리프레시토큰_발급_성공() {
        String token = jwtTokenUtil.createRefreshToken(1L);

        Claims claims = parse(token, jwtKeyProvider.getPublicKey()).getPayload();

        assertEquals("1", claims.getSubject());
        assertEquals("bds-auth", claims.getIssuer());
        assertEquals(1, claims.get("authId", Integer.class));
        assertNull(claims.get("email"));
        assertNull(claims.get("roles"));
    }

    @Test
    @DisplayName("getPublicKey는 JwtKeyProvider의 공개키를 그대로 반환한다")
    void 공개키_조회_위임() {
        assertEquals(jwtKeyProvider.getPublicKey(), jwtTokenUtil.getPublicKey());
    }

    private Jws<Claims> parse(String token, PublicKey publicKey) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token);
    }
}
