package com.dbs.gateway.filter;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 인증된 요청의 JWT 클레임(sub, email, roles)을 다운스트림 서비스가 바로 쓸 수 있도록
 * X-User-Id / X-User-Email / X-User-Roles 헤더로 변환해 전달한다. 인증되지 않은 요청(permitAll 라우트)은
 * SecurityContext가 JwtAuthenticationToken이 아니므로 그대로 통과시킨다.
 * 헤더 이름은 modules/common의 LoginUserArgumentResolver가 읽는 이름과 반드시 일치해야 한다.
 */
@Component
public class JwtClaimForwardingFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(JwtAuthenticationToken.class::isInstance)
            .cast(JwtAuthenticationToken.class)
            .map(JwtAuthenticationToken::getToken)
            .map(jwt -> withClaimHeaders(exchange, jwt))
            .defaultIfEmpty(exchange)
            .flatMap(chain::filter);
    }

    private ServerWebExchange withClaimHeaders(ServerWebExchange exchange, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<String> roles = jwt.getClaimAsStringList("roles");

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header(USER_ID_HEADER, jwt.getSubject())
            .header(USER_EMAIL_HEADER, email == null ? "" : email)
            .header(USER_ROLES_HEADER, roles == null ? "" : String.join(",", roles))
            .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
