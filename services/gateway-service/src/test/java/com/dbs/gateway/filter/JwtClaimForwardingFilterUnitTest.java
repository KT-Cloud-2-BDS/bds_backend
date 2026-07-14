package com.dbs.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("JwtClaimForwardingFilter 단위 테스트")
class JwtClaimForwardingFilterUnitTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    private final JwtClaimForwardingFilter filter = new JwtClaimForwardingFilter();

    @Test
    @DisplayName("유효한 JWT 인증 정보가 있으면 sub/email/roles 클레임을 헤더로 변환해 전달한다")
    void JWT_클레임을_헤더로_변환() {
        Jwt jwt = jwt("42", "yeojin@email.com", List.of("SUPPORTER", "MAKER"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        ServerWebExchange exchange = exchangeWithClientSuppliedHeaders();
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturingChain(captured))
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                    Mono.just(new SecurityContextImpl(authentication)))))
            .verifyComplete();

        ServerHttpRequest downstreamRequest = captured.get().getRequest();
        assertEquals("42", downstreamRequest.getHeaders().getFirst(USER_ID_HEADER));
        assertEquals("yeojin@email.com", downstreamRequest.getHeaders().getFirst(USER_EMAIL_HEADER));
        assertEquals("SUPPORTER,MAKER", downstreamRequest.getHeaders().getFirst(USER_ROLES_HEADER));
    }

    @Test
    @DisplayName("email/roles 클레임이 없으면 빈 문자열 헤더로 채운다")
    void 클레임_누락시_빈문자열로_채움() {
        Jwt jwt = jwt("42", null, null);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/members/me").build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturingChain(captured))
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                    Mono.just(new SecurityContextImpl(authentication)))))
            .verifyComplete();

        ServerHttpRequest downstreamRequest = captured.get().getRequest();
        assertEquals("", downstreamRequest.getHeaders().getFirst(USER_EMAIL_HEADER));
        assertEquals("", downstreamRequest.getHeaders().getFirst(USER_ROLES_HEADER));
    }

    @Test
    @DisplayName("클라이언트가 신원 헤더를 직접 실어 보내면 인증 여부와 무관하게 항상 제거한다")
    void 클라이언트_위조헤더_항상_제거() {
        ServerWebExchange exchange = exchangeWithClientSuppliedHeaders();
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturingChain(captured)))
            .verifyComplete();

        ServerHttpRequest downstreamRequest = captured.get().getRequest();
        assertNull(downstreamRequest.getHeaders().getFirst(USER_ID_HEADER));
        assertFalse(downstreamRequest.getHeaders().containsHeader(USER_EMAIL_HEADER));
        assertFalse(downstreamRequest.getHeaders().containsHeader(USER_ROLES_HEADER));
    }

    private ServerWebExchange exchangeWithClientSuppliedHeaders() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/members/me")
            .header(USER_ID_HEADER, "spoofed-id")
            .header(USER_EMAIL_HEADER, "spoofed@evil.com")
            .header(USER_ROLES_HEADER, "ADMIN")
            .build());
    }

    private GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> captured) {
        return exchange -> {
            captured.set(exchange);
            return Mono.empty();
        };
    }

    private Jwt jwt(String subject, String email, List<String> roles) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", subject);
        if (email != null) {
            claims.put("email", email);
        }
        if (roles != null) {
            claims.put("roles", roles);
        }

        return new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            claims
        );
    }
}
