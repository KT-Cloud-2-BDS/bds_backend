package com.dbs.gateway.config;

import com.dbs.gateway.security.AuthBlacklistClient;
import com.dbs.gateway.security.BlockingJwtDecoderAdapter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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

    // jwks-uri를 하드코딩된 host:port 대신 Eureka에 등록된 auth-service 인스턴스를 조회해 구성한다.
    // 인스턴스 조회는 게이트웨이 기동 시 1회만 이뤄지므로, auth-service가 Eureka에 먼저 등록되어 있어야 한다.
    // Eureka 서버의 read-only response cache가 최대 30초까지 지연될 수 있어(기본 갱신 주기),
    // 방금 등록된 인스턴스가 바로 조회되지 않는 경우를 대비해 짧은 간격으로 재시도한다.
    @Bean
    public ReactiveJwtDecoder jwtDecoder(DiscoveryClient discoveryClient,
        @Value("${jwt.jwks-service-id:auth-service}") String jwksServiceId,
        @Value("${jwt.jwks-path:/oauth2/jwks}") String jwksPath,
        @Value("${jwt.jwks-discovery-max-attempts:15}") int maxAttempts,
        @Value("${jwt.jwks-discovery-retry-delay-ms:2000}") long retryDelayMillis) {
        List<ServiceInstance> instances = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            instances = discoveryClient.getInstances(jwksServiceId);
            if (!instances.isEmpty()) {
                break;
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (instances.isEmpty()) {
            throw new IllegalStateException("Eureka에 등록된 " + jwksServiceId + " 인스턴스를 찾을 수 없습니다.");
        }

        String jwksUri = instances.get(0).getUri() + jwksPath;
        JwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        return new BlockingJwtDecoderAdapter(nimbusJwtDecoder, authBlacklistClient);
    }
}
