package com.bds.auth.presentation.controller;

import com.bds.auth.application.JwksService;
import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2/jwks")
@RequiredArgsConstructor
public class JwksController {

    private final JwksService jwksService;

    @GetMapping
    public Map<String, Object> getJwks() {
        JWKSet jwkSet = jwksService.getPublicKeySet();
        return jwkSet.toJSONObject();
    }

}
