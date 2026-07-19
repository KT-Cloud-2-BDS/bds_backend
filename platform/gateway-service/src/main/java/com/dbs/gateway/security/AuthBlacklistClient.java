package com.dbs.gateway.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 로그아웃된 access token 블랙리스트 여부를 auth-service에 직접 물어본다.
 * Redis는 auth-service만 알고 있고, gateway는 그 저장소 스키마에 결합되지 않는다.
 * auth-service가 응답하지 않으면(장애/타임아웃/커넥션 실패) fail-open으로 처리해,
 * auth-service 장애가 전체 서비스의 인증 자체를 막는 단일 장애점이 되지 않도록 한다.
 */
@Component
public class AuthBlacklistClient {

    private static final String BLACKLIST_CHECK_PATH = "/internal/auths/blacklist";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final Duration BLACKLIST_CHECK_TIMEOUT = Duration.ofMillis(300);

    private final WebClient webClient;
    private final String internalGatewaySecret;

    public AuthBlacklistClient(
        @Value("${auth-service.internal-uri}") String authServiceInternalUri,
        @Value("${internal.gateway-secret}") String internalGatewaySecret
    ) {
        this.webClient = WebClient.builder().baseUrl(authServiceInternalUri).build();
        this.internalGatewaySecret = internalGatewaySecret;
    }

    public Mono<Boolean> isBlacklisted(String accessToken) {
        return webClient.get()
            .uri(BLACKLIST_CHECK_PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .header(INTERNAL_SECRET_HEADER, internalGatewaySecret)
            .retrieve()
            .bodyToMono(Boolean.class)
            .timeout(BLACKLIST_CHECK_TIMEOUT)
            .onErrorReturn(false);
    }
}
