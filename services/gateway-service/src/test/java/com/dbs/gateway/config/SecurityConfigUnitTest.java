package com.dbs.gateway.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.dbs.gateway.security.BlockingJwtDecoderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("SecurityConfig 단위 테스트")
class SecurityConfigUnitTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("jwtDecoder 빈은 NimbusJwtDecoder를 논블로킹 어댑터로 감싸 반환한다")
    void JWT디코더_빈_생성() {
        ReactiveJwtDecoder decoder = securityConfig.jwtDecoder("http://localhost:8081/oauth2/jwks");

        assertInstanceOf(BlockingJwtDecoderAdapter.class, decoder);
    }

    @Test
    @DisplayName("securityWebFilterChain은 모든 경로에 매칭되는 필터체인을 구성한다")
    void 시큐리티필터체인_모든경로_매칭() {
        ReactiveJwtDecoder jwtDecoder = mock(ReactiveJwtDecoder.class);
        SecurityWebFilterChain chain = securityConfig.securityWebFilterChain(ServerHttpSecurity.http(), jwtDecoder);

        assertNotNull(chain);

        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/members/anything/at/all").build());

        StepVerifier.create(chain.matches(exchange))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    @DisplayName("Authorization 헤더에 만료/위조 등 유효하지 않은 JWT가 있으면 401로 즉시 차단한다")
    void 유효하지않은JWT_401차단() {
        ReactiveJwtDecoder jwtDecoder = mock(ReactiveJwtDecoder.class);
        given(jwtDecoder.decode("invalid-or-expired-token"))
            .willReturn(Mono.error(new BadJwtException("만료되었거나 서명이 유효하지 않은 토큰입니다.")));

        WebTestClient client = buildClient(jwtDecoder);

        client.get().uri("/api/members/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-or-expired-token")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Authorization 헤더 자체가 없으면 인증을 시도하지 않고 그대로 통과시킨다")
    void JWT_헤더없음_통과() {
        ReactiveJwtDecoder jwtDecoder = mock(ReactiveJwtDecoder.class);

        WebTestClient client = buildClient(jwtDecoder);

        client.get().uri("/api/members/me")
            .exchange()
            .expectStatus().isOk();
    }

    private WebTestClient buildClient(ReactiveJwtDecoder jwtDecoder) {
        SecurityWebFilterChain chain = securityConfig.securityWebFilterChain(ServerHttpSecurity.http(), jwtDecoder);

        return WebTestClient.bindToWebHandler(exchange -> exchange.getResponse().setComplete())
            .webFilter(new WebFilterChainProxy(chain))
            .build();
    }
}
