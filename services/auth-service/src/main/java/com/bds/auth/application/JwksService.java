package com.bds.auth.application;

import com.bds.auth.infrastructure.security.JwtKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwksService {

    private final JwtKeyProvider jwtKeyProvider;

    public JWKSet getPublicKeySet() {
        return jwtKeyProvider.getPublicJWKSet();
    }
}
