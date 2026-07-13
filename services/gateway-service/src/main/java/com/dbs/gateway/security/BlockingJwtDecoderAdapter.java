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
 */
public class BlockingJwtDecoderAdapter implements ReactiveJwtDecoder {

    private final JwtDecoder delegate;

    public BlockingJwtDecoderAdapter(JwtDecoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Jwt> decode(String token) throws JwtException {
        return Mono.fromCallable(() -> delegate.decode(token))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
