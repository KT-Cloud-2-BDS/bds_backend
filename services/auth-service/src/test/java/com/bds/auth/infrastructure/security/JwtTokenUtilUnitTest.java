package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.support.TestRsaKeyGenerator;
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

    private static final String PRIVATE_KEY_PEM = TestRsaKeyGenerator.generatePrivateKeyPem();

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
