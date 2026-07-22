package com.dbs.gateway.config;

import com.dbs.gateway.security.BlockingJwtDecoderAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.jwks-uri}") String jwksUri) {
        JwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        return new BlockingJwtDecoderAdapter(nimbusJwtDecoder);
    }
}
