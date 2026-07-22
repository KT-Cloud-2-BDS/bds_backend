package com.bds.chat.e2e;

import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.MimeType;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
public abstract class ChatE2ETestBase {

    private static final RSAKey RSA_KEY;
    private static final WireMockServer WIRE_MOCK;

    static {
        try {
            RSA_KEY = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .generate();
            WIRE_MOCK = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
            WIRE_MOCK.start();
            WIRE_MOCK.stubFor(get(urlEqualTo("/oauth2/jwks"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(new JWKSet(RSA_KEY.toPublicJWK()).toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.jwks-uri", () -> "http://localhost:" + WIRE_MOCK.port() + "/oauth2/jwks");
        registry.add("app.auth.renew-before", () -> "PT5S");
        registry.add("app.auth.renew-grace", () -> "PT5S");
        registry.add("spring.cloud.loadbalancer.enabled", () -> "false");
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected ChatIntegrationTestFixture fixture;

    // Spring Boot 4.x 자동 구성된 Jackson 3.x ObjectMapper (tools.jackson.databind)
    @Autowired
    private ObjectMapper objectMapper;

    protected WebSocketStompClient stompClient;

    @BeforeEach
    void setUpStompClient() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new Jackson3MessageConverter(objectMapper));
    }

    @AfterEach
    void cleanUpData() {
        fixture.deleteAll();
    }

    protected String wsUrl() {
        return "ws://localhost:" + port + "/ws/chat";
    }

    protected StompSession connect(String token) throws Exception {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return stompClient.connectAsync(wsUrl(), handshakeHeaders, connectHeaders,
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    protected StompSession connectAnonymous() throws Exception {
        return connect(null);
    }

    protected String createToken(long memberId) {
        return buildToken(memberId, Instant.now().plusSeconds(3600));
    }

    // renew-before=PT5S 기준 10초 만료 → 연결 후 5초 뒤 TOKEN_RENEWAL_REQUIRED 스케줄링
    protected String createShortLivedToken(long memberId) {
        return buildToken(memberId, Instant.now().plusSeconds(10));
    }

    private static String buildToken(long memberId, Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(memberId))
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(expiresAt))
                    .claim("roles", List.of("USER"))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_KEY.getKeyID()).build(),
                    claims);
            jwt.sign(new RSASSASigner(RSA_KEY));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected BlockingQueue<Map<String, Object>> subscribeRoom(StompSession session, long roomId) {
        return subscribe(session, "/topic/chat.room." + roomId);
    }

    protected BlockingQueue<Map<String, Object>> subscribeReadReceipt(StompSession session, long roomId) {
        return subscribe(session, "/topic/chat.room." + roomId + ".read");
    }

    protected BlockingQueue<Map<String, Object>> subscribeAuth(StompSession session) {
        return subscribe(session, "/user/queue/auth");
    }

    @SuppressWarnings("unchecked")
    protected BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Map<?, ?> m) {
                    queue.add((Map<String, Object>) m);
                }
            }
        });
        return queue;
    }

    protected void sendMessage(StompSession session, long roomId, String content) {
        session.send("/app/chat/send/" + roomId,
                Map.of("clientMessageId", "cid-" + System.nanoTime(), "content", content));
    }

    protected void sendReadReceipt(StompSession session, long roomId, long lastReadMessageId) {
        session.send("/app/chat/read/" + roomId, Map.of("lastReadMessageId", lastReadMessageId));
    }

    protected void sendTokenRefresh(StompSession session, String newToken) {
        session.send("/app/auth/refresh", Map.of("token", newToken));
    }

    protected void awaitDisconnected(StompSession session, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (session.isConnected() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
    }

    // MappingJackson2MessageConverter는 Jackson 2.x(com.fasterxml)를 참조하므로
    // Jackson 3.x(tools.jackson) 환경에서 사용할 수 없어 직접 구현
    private static class Jackson3MessageConverter extends AbstractMessageConverter {

        private final ObjectMapper mapper;

        Jackson3MessageConverter(ObjectMapper mapper) {
            super(new MimeType("application", "json"));
            this.mapper = mapper;
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return true;
        }

        @Override
        protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            Object payload = message.getPayload();
            if (payload instanceof byte[] bytes) {
                return mapper.readValue(bytes, targetClass);
            }
            if (payload instanceof String str) {
                return mapper.readValue(str, targetClass);
            }
            return null;
        }

        @Override
        protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
            return mapper.writeValueAsBytes(payload);
        }
    }
}
