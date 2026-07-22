package com.dbs.gateway.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * NimbusJwtDecoder는 블로킹(서블릿용) 구현체라 리액티브 필터체인의 이벤트 루프 스레드에서
 * 직접 호출하면 JWKS 조회/서명 검증 동안 스레드가 막힌다. boundedElastic 스케줄러로 옮겨서
 * 게이트웨이의 리액티브 체인과 안전하게 연결한다.
 * 서명 검증에 성공한 토큰이라도 auth-service의 로그아웃 처리로 블랙리스트에 등록됐을 수
 * 있어, 그 경우 만료 시간 전이라도 즉시 차단한다(AuthBlacklistClient가 auth-service에
 * 물어봄 — WebClient 호출 자체는 이미 논블로킹이라 별도 스케줄러가 필요 없다).
 */
public class BlockingJwtDecoderAdapter implements ReactiveJwtDecoder {

    private final JwtDecoder delegate;
    private final AuthBlacklistClient authBlacklistClient;

    public BlockingJwtDecoderAdapter(JwtDecoder delegate, AuthBlacklistClient authBlacklistClient) {
        this.delegate = delegate;
        this.authBlacklistClient = authBlacklistClient;
    }

    @Override
    public Mono<Jwt> decode(String token) throws JwtException {
        return Mono.fromCallable(() -> delegate.decode(token))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(jwt -> authBlacklistClient.isBlacklisted(token)
                .flatMap(blacklisted -> Boolean.TRUE.equals(blacklisted)
                    ? Mono.<Jwt>error(new JwtException("로그아웃 처리된 토큰입니다."))
                    : Mono.just(jwt)));
    }
}
