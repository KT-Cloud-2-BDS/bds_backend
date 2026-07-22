package com.dbs.gateway.security;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("BlockingJwtDecoderAdapter 단위 테스트")
class BlockingJwtDecoderAdapterUnitTest {

    private final JwtDecoder delegate = mock(JwtDecoder.class);
    private final AuthBlacklistClient authBlacklistClient = mock(AuthBlacklistClient.class);
    private final BlockingJwtDecoderAdapter adapter = new BlockingJwtDecoderAdapter(delegate, authBlacklistClient);

    @Test
    @DisplayName("delegate가 정상적으로 디코딩하고 블랙리스트에 없으면 같은 Jwt를 방출한다")
    void 디코딩_성공() {
        Jwt expected = mock(Jwt.class);
        given(delegate.decode("valid-token")).willReturn(expected);
        given(authBlacklistClient.isBlacklisted("valid-token")).willReturn(Mono.just(false));

        StepVerifier.create(adapter.decode("valid-token"))
            .expectNext(expected)
            .verifyComplete();

        verify(delegate).decode("valid-token");
    }

    @Test
    @DisplayName("delegate가 JwtException을 던지면 블랙리스트 조회 없이 그대로 에러 시그널로 전파한다")
    void 디코딩_실패_에러전파() {
        JwtException exception = new JwtException("invalid signature");
        given(delegate.decode("invalid-token")).willThrow(exception);

        StepVerifier.create(adapter.decode("invalid-token"))
            .expectErrorMatches(actual -> actual == exception)
            .verify();
    }

    @Test
    @DisplayName("서명은 유효해도 로그아웃되어 블랙리스트에 등록된 토큰이면 JwtException으로 차단한다")
    void 디코딩_블랙리스트등록토큰_차단() {
        Jwt decoded = mock(Jwt.class);
        given(delegate.decode("logged-out-token")).willReturn(decoded);
        given(authBlacklistClient.isBlacklisted("logged-out-token")).willReturn(Mono.just(true));

        StepVerifier.create(adapter.decode("logged-out-token"))
            .expectError(JwtException.class)
            .verify();
    }

    @Test
    @DisplayName("이벤트 루프를 막지 않도록 boundedElastic 스케줄러 스레드에서 delegate를 호출한다")
    void 디코딩_boundedElastic에서_실행() {
        given(delegate.decode("valid-token")).willAnswer(invocation -> {
            String threadName = Thread.currentThread().getName();
            if (!threadName.contains("boundedElastic")) {
                throw new AssertionError("expected boundedElastic thread but was: " + threadName);
            }
            return mock(Jwt.class);
        });
        given(authBlacklistClient.isBlacklisted("valid-token")).willReturn(Mono.just(false));

        StepVerifier.create(adapter.decode("valid-token"))
            .expectNextCount(1)
            .verifyComplete();
    }
}
