package com.bds.auth.infrastructure.security;

import com.bds.auth.domain.entity.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

    private final SecretKey signingKey;
    private final long accessTokenExpirationPeriod;

    public JwtTokenUtil(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.expiration.access}") long accessTokenExpirationPeriod
    ){
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationPeriod = accessTokenExpirationPeriod;
    }

    public String createAccessToken(Long authId, String email, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationPeriod, ChronoUnit.MILLIS);

        return Jwts.builder()
            .subject(String.valueOf(authId))
            .issuer("bds-auth")
            .claim("authId", authId)
            .claim("email", email)
            .claim("role", role.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    public String createRefreshToken(Long authId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(7, ChronoUnit.DAYS);

        return Jwts.builder()
            .subject(String.valueOf(authId))
            .issuer("bds-auth")
            .claim("authId", authId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }
}

