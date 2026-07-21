package com.bds.auth.infrastructure.security;

import com.bds.auth.domain.entity.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpirationPeriod;
    private final long refreshTokenExpirationPeriod;

    public JwtTokenUtil(
        @Value("${jwt.expiration.access}") long accessTokenExpirationPeriod,
        @Value("${jwt.expiration.refresh}") long refreshTokenExpirationPeriod
    ){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 알고리즘을 초기화할 수 없습니다.", e);
        }
        this.accessTokenExpirationPeriod = accessTokenExpirationPeriod;
        this.refreshTokenExpirationPeriod = refreshTokenExpirationPeriod;
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
            .signWith(SignatureAlgorithm.RS256, privateKey)
            .compact();
    }

    public String createRefreshToken(Long authId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpirationPeriod, ChronoUnit.MILLIS);

        return Jwts.builder()
            .subject(String.valueOf(authId))
            .issuer("bds-auth")
            .claim("authId", authId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(SignatureAlgorithm.RS256, privateKey)
            .compact();
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

}


