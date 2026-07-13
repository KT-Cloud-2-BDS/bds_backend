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
        // 인가는 게이트웨이가 아니라 각 도메인 서비스의 책임이다.
        // 게이트웨이는 Authorization 헤더가 있으면 서명/만료를 검증해 클레임을 파싱해줄 뿐,
        // 라우트 접근 자체를 막지 않는다 (permitAll). 실제 인증 여부 판단은 다운스트림에서 처리.
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
