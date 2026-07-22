package com.bds.member.infrastructure.persistence.feignClient;

import static org.assertj.core.api.Assertions.assertThat;

import com.bds.common.filter.InternalGatewaySecretFilter;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InternalSecretFeignInterceptor 단위 테스트")
class InternalSecretFeignInterceptorUnitTest {

    @Test
    @DisplayName("Feign 요청에 X-Internal-Secret 헤더를 실어 보낸다")
    void 요청에_내부시크릿_헤더_추가() {
        InternalSecretFeignInterceptor interceptor = new InternalSecretFeignInterceptor("test-internal-secret");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get(InternalGatewaySecretFilter.INTERNAL_SECRET_HEADER))
            .containsExactly("test-internal-secret");
    }
}
