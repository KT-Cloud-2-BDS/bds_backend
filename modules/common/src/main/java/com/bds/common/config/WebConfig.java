package com.bds.common.config;

import com.bds.common.filter.InternalGatewaySecretFilter;
import com.bds.common.resolver.LoginUserArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LoginUserArgumentResolver loginUserArgumentResolver () {
        return new LoginUserArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver());
    }

    @Bean
    public FilterRegistrationBean<InternalGatewaySecretFilter> internalGatewaySecretFilter(
        @Value("${internal.gateway-secret}") String internalGatewaySecret
    ) {
        FilterRegistrationBean<InternalGatewaySecretFilter> registration =
            new FilterRegistrationBean<>(new InternalGatewaySecretFilter(internalGatewaySecret));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
