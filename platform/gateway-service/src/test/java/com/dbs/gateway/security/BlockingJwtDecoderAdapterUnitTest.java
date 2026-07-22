package com.dbs.gateway.security;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import reactor.test.StepVerifier;

@DisplayName("BlockingJwtDecoderAdapter 단위 테스트")
class BlockingJwtDecoderAdapterUnitTest {

    private final JwtDecoder delegate = mock(JwtDecoder.class);
    private final BlockingJwtDecoderAdapter adapter = new BlockingJwtDecoderAdapter(delegate);

    @Test
    @DisplayName("delegate가 정상적으로 디코딩하면 같은 Jwt를 방출한다")
    void 디코딩_성공() {
        Jwt expected = mock(Jwt.class);
        given(delegate.decode("valid-token")).willReturn(expected);

        StepVerifier.create(adapter.decode("valid-token"))
            .expectNext(expected)
            .verifyComplete();

        verify(delegate).decode("valid-token");
    }

    @Test
    @DisplayName("delegate가 JwtException을 던지면 그대로 에러 시그널로 전파한다")
    void 디코딩_실패_에러전파() {
        JwtException exception = new JwtException("invalid signature");
        given(delegate.decode("invalid-token")).willThrow(exception);

        StepVerifier.create(adapter.decode("invalid-token"))
            .expectErrorMatches(actual -> actual == exception)
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

        StepVerifier.create(adapter.decode("valid-token"))
            .expectNextCount(1)
            .verifyComplete();
    }
}
