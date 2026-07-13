package com.bds.auth.infrastructure.security;

import com.bds.auth.domain.entity.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

    private final JwtKeyProvider jwtKeyProvider;
    private final long accessTokenExpirationPeriod;
    private final long refreshTokenExpirationPeriod;

    public JwtTokenUtil(
        JwtKeyProvider jwtKeyProvider,
        @Value("${jwt.expiration.access}") long accessTokenExpirationPeriod,
        @Value("${jwt.expiration.refresh}") long refreshTokenExpirationPeriod
    ){
        this.jwtKeyProvider = jwtKeyProvider;
        this.accessTokenExpirationPeriod = accessTokenExpirationPeriod;
        this.refreshTokenExpirationPeriod = refreshTokenExpirationPeriod;
    }

    public String createAccessToken(Long authId, String email, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationPeriod, ChronoUnit.MILLIS);

        return Jwts.builder()
            .header().keyId(jwtKeyProvider.getKeyId()).and()
            .subject(String.valueOf(authId))
            .issuer("bds-auth")
            .claim("authId", authId)
            .claim("email", email)
            .claim("roles", List.of(role.name()))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(SignatureAlgorithm.RS256, jwtKeyProvider.getPrivateKey())
            .compact();
    }

    public String createRefreshToken(Long authId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpirationPeriod, ChronoUnit.MILLIS);

        return Jwts.builder()
            .header().keyId(jwtKeyProvider.getKeyId()).and()
            .subject(String.valueOf(authId))
            .issuer("bds-auth")
            .claim("authId", authId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(SignatureAlgorithm.RS256, jwtKeyProvider.getPrivateKey())
            .compact();
    }

    public PublicKey getPublicKey() {
        return jwtKeyProvider.getPublicKey();
    }

}


