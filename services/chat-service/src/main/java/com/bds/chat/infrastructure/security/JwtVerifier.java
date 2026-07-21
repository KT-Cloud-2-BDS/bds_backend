package com.bds.chat.infrastructure.security;

import com.bds.chat.common.InvalidTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtVerifier {
    private final JwtDecoder decoder;

    public JwtVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }
    public VerifiedToken verify(String token) {
        Jwt jwt = decoder.decode(token);
        if (jwt.getExpiresAt() == null) {
            throw new InvalidTokenException("토큰 만료 시간이 존재하지 않습니다.");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidTokenException("토큰 subject가 존재하지 않습니다.");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new VerifiedToken(
                subject,
                roles == null ? Set.of() : roles.stream().collect(Collectors.toUnmodifiableSet()),
                jwt.getExpiresAt()
        );
    }

    public record VerifiedToken(String userId, Set<String> roles, Instant expiresAt) {}
}
