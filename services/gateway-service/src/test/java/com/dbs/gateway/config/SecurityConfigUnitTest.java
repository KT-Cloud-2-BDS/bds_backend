package com.dbs.gateway.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.dbs.gateway.security.BlockingJwtDecoderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
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
}
