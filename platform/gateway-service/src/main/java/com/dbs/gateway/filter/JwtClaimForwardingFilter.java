package com.dbs.gateway.filter;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
 * X-User-Id / X-User-Email / X-User-Roles 헤더로 변환해 전달한다.
 * 다운스트림 서비스는 이 헤더들을 게이트웨이가 검증한 값으로 100% 신뢰하므로,
 * 클라이언트가 직접 같은 이름의 헤더를 실어 보내 신원을 위조하지 못하도록
 * 매 요청마다 먼저 제거한 뒤(JWT 유무와 무관하게) 유효한 JWT가 있을 때만 새로 채운다.
 * 신원 헤더와 함께 게이트웨이만 아는 내부 시크릿(X-Internal-Secret)도 실어 보내,
 * 서비스 포트에 직접 접근해 신원 헤더를 위조하는 시도를 다운스트림에서 2차로 차단할 수 있게 한다.
 */
@Component
public class JwtClaimForwardingFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final String internalGatewaySecret;

    public JwtClaimForwardingFilter(@Value("${internal.gateway-secret}") String internalGatewaySecret) {
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = stripClientSuppliedIdentityHeaders(exchange);

        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(JwtAuthenticationToken.class::isInstance)
            .cast(JwtAuthenticationToken.class)
            .map(JwtAuthenticationToken::getToken)
            .map(jwt -> withClaimHeaders(sanitizedExchange, jwt))
            .defaultIfEmpty(sanitizedExchange)
            .flatMap(chain::filter);
    }

    private ServerWebExchange stripClientSuppliedIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest strippedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                headers.remove(USER_ID_HEADER);
                headers.remove(USER_EMAIL_HEADER);
                headers.remove(USER_ROLES_HEADER);
                headers.remove(INTERNAL_SECRET_HEADER);
            })
            .build();

        return exchange.mutate().request(strippedRequest).build();
    }

    private ServerWebExchange withClaimHeaders(ServerWebExchange exchange, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<String> roles = jwt.getClaimAsStringList("roles");

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header(USER_ID_HEADER, jwt.getSubject())
            .header(USER_EMAIL_HEADER, email == null ? "" : email)
            .header(USER_ROLES_HEADER, roles == null ? "" : String.join(",", roles))
            .header(INTERNAL_SECRET_HEADER, internalGatewaySecret)
            .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
