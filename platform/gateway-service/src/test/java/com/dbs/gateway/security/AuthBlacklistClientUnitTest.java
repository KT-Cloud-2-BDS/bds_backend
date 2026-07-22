package com.dbs.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

@DisplayName("AuthBlacklistClient 단위 테스트")
class AuthBlacklistClientUnitTest {

    private MockWebServer mockWebServer;
    private AuthBlacklistClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        client = new AuthBlacklistClient(baseUrl, "test-internal-secret");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("auth-service가 true를 응답하면 블랙리스트된 토큰으로 판단하고, 필요한 헤더를 실어 보낸다")
    void 블랙리스트_true_응답() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("true")
            .setHeader("Content-Type", "application/json"));

        StepVerifier.create(client.isBlacklisted("some-token"))
            .expectNext(true)
            .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/internal/auths/blacklist", recordedRequest.getPath());
        assertEquals("Bearer some-token", recordedRequest.getHeader("Authorization"));
        assertEquals("test-internal-secret", recordedRequest.getHeader("X-Internal-Secret"));
    }

    @Test
    @DisplayName("auth-service가 false를 응답하면 블랙리스트되지 않은 토큰으로 판단한다")
    void 블랙리스트_false_응답() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("false")
            .setHeader("Content-Type", "application/json"));

        StepVerifier.create(client.isBlacklisted("some-token"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    @DisplayName("auth-service가 5xx를 응답해도 예외 없이 false(fail-open)를 반환한다")
    void 서버오류_fail_open() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.isBlacklisted("some-token"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    @DisplayName("auth-service가 4xx를 응답해도 예외 없이 false(fail-open)를 반환한다")
    void 클라이언트오류_fail_open() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        StepVerifier.create(client.isBlacklisted("some-token"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    @DisplayName("auth-service 응답이 타임아웃을 초과해도 예외 없이 false(fail-open)를 반환한다")
    void 타임아웃_fail_open() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("true")
            .setBodyDelay(5, TimeUnit.SECONDS));

        StepVerifier.create(client.isBlacklisted("some-token"))
            .expectNext(false)
            .expectComplete()
            .verify(Duration.ofSeconds(4));
    }
}
