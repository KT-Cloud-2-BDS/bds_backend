package com.bds.chat.infrastructure.security;

import com.bds.chat.common.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtVerifier {
    private final JwtDecoder decoder;

    public JwtVerifier(@Value("${app.auth.jwks-uri}") String jwksUri,
                       @Value("${app.auth.issuer:}") String issuer) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        if (!issuer.isBlank()) {
            nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        }
        this.decoder = nimbus;
    }
    public VerifiedToken verify(String token) {
        Jwt jwt = decoder.decode(token);
        if(jwt.getExpiresAt() == null){
            throw new InvalidTokenException("토큰 만료 시간이 존재하지 않습니다.");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new VerifiedToken(
                jwt.getSubject(),
                roles == null ? Set.of() : roles.stream().collect(Collectors.toUnmodifiableSet()),
                jwt.getExpiresAt()
        );
    }

    public record VerifiedToken(String userId, Set<String> roles, Instant expiresAt) {}
}
