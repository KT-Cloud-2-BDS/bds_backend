package com.bds.member.infrastructure.persistence.feignClient;

import com.bds.common.filter.InternalGatewaySecretFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * member-service가 Feign으로 호출하는 서비스 간 내부 API 요청에
 * X-Internal-Secret 헤더를 자동으로 실어 보낸다. 메인 애플리케이션 컨텍스트에 등록된
 * RequestInterceptor는 모든 FeignClient에 전역으로 적용된다.
 */
@Component
public class InternalSecretFeignInterceptor implements RequestInterceptor {

    private final String internalGatewaySecret;

    public InternalSecretFeignInterceptor(@Value("${internal.gateway-secret}") String internalGatewaySecret) {
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(InternalGatewaySecretFilter.INTERNAL_SECRET_HEADER, internalGatewaySecret);
    }
}
